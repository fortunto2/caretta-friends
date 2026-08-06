/**
 * Caretta Friends — nest photo storage.
 *
 * The app keeps its records in Supabase and its files here in R2. R2 has no notion of a user, so
 * this Worker is the only way in: it verifies the volunteer's Supabase access token (ES256, checked
 * against Supabase's published JWKS — no shared secret to leak or rotate) and applies the same
 * trust model as the nest rows themselves:
 *
 *   READ   any signed-in volunteer. Nest photos are shared evidence, never public — the images
 *          carry the GPS of a protected nesting beach.
 *   WRITE  only under your own uid prefix, so nobody can overwrite someone else's photo.
 *   DELETE not offered at all: field records are not destroyed from the app.
 *
 * Object keys are `<uid>/<nest-id>/<photo-id>.jpg`.
 */

interface Env {
	PHOTOS: R2Bucket;
	SUPABASE_ISSUER: string;
	SUPABASE_JWKS_URL: string;
}

interface Claims {
	sub: string;
	exp: number;
	iss: string;
	role?: string;
}

/** Cached across requests on a warm isolate; Supabase rotates keys rarely and by kid. */
let jwksCache: { keys: JsonWebKey[]; fetchedAt: number } | null = null;
const JWKS_TTL_MS = 60 * 60 * 1000;

function base64UrlToBytes(input: string): Uint8Array {
	const padded = input.replace(/-/g, '+').replace(/_/g, '/');
	const binary = atob(padded + '='.repeat((4 - (padded.length % 4)) % 4));
	const bytes = new Uint8Array(binary.length);
	for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
	return bytes;
}

async function jwks(env: Env): Promise<JsonWebKey[]> {
	const fresh = jwksCache && Date.now() - jwksCache.fetchedAt < JWKS_TTL_MS;
	if (fresh) return jwksCache!.keys;
	const res = await fetch(env.SUPABASE_JWKS_URL);
	if (!res.ok) throw new Error(`jwks ${res.status}`);
	const body = (await res.json()) as { keys: (JsonWebKey & { kid?: string })[] };
	jwksCache = { keys: body.keys, fetchedAt: Date.now() };
	return body.keys;
}

/** Verify a Supabase access token and return its claims, or null if it doesn't hold up. */
async function verify(token: string, env: Env): Promise<Claims | null> {
	const parts = token.split('.');
	if (parts.length !== 3) return null;
	const [headerPart, payloadPart, signaturePart] = parts;

	const header = JSON.parse(new TextDecoder().decode(base64UrlToBytes(headerPart)));
	// ES256 only: never let a token pick its own algorithm (that's how "alg: none" gets in).
	if (header.alg !== 'ES256' || !header.kid) return null;

	const jwk = (await jwks(env)).find((k) => (k as { kid?: string }).kid === header.kid);
	if (!jwk) return null;

	const key = await crypto.subtle.importKey('jwk', jwk, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify']);
	const signed = new TextEncoder().encode(`${headerPart}.${payloadPart}`);
	const ok = await crypto.subtle.verify(
		{ name: 'ECDSA', hash: 'SHA-256' },
		key,
		base64UrlToBytes(signaturePart),
		signed,
	);
	if (!ok) return null;

	const claims = JSON.parse(new TextDecoder().decode(base64UrlToBytes(payloadPart))) as Claims;
	if (claims.iss !== env.SUPABASE_ISSUER) return null;
	if (!claims.exp || claims.exp * 1000 <= Date.now()) return null;
	if (!claims.sub) return null;
	// Anonymous sign-ins carry role "authenticated" too — that's deliberate: a volunteer must be
	// able to photograph a nest before deciding to create an account.
	if (claims.role !== 'authenticated') return null;
	return claims;
}

function bearer(request: Request): string | null {
	const header = request.headers.get('Authorization') ?? '';
	return header.startsWith('Bearer ') ? header.slice(7) : null;
}

/** `/o/<uid>/<nest>/<photo>.jpg` → the object key, or null if the shape is wrong. */
function objectKey(url: URL): string | null {
	const path = decodeURIComponent(url.pathname);
	if (!path.startsWith('/o/')) return null;
	const key = path.slice(3);
	// No traversal, no empty segments — the uid prefix is what write permission rests on.
	if (!key || key.includes('..') || key.split('/').some((s) => s.length === 0)) return null;
	return key;
}

export default {
	async fetch(request: Request, env: Env): Promise<Response> {
		const url = new URL(request.url);

		if (url.pathname === '/health') return new Response('ok');

		const key = objectKey(url);
		if (!key) return new Response('Not found', { status: 404 });

		const token = bearer(request);
		if (!token) return new Response('Unauthorized', { status: 401 });
		const claims = await verify(token, env).catch(() => null);
		if (!claims) return new Response('Unauthorized', { status: 401 });

		if (request.method === 'GET' || request.method === 'HEAD') {
			const object = await env.PHOTOS.get(key);
			if (!object) return new Response('Not found', { status: 404 });
			const headers = new Headers();
			object.writeHttpMetadata(headers);
			headers.set('etag', object.httpEtag);
			// A nest photo is a record of a moment: it never changes, so it can be cached hard.
			headers.set('Cache-Control', 'private, max-age=31536000, immutable');
			return new Response(request.method === 'HEAD' ? null : object.body, { headers });
		}

		if (request.method === 'PUT') {
			if (!key.startsWith(`${claims.sub}/`)) return new Response('Forbidden', { status: 403 });
			const type = request.headers.get('Content-Type') ?? 'image/jpeg';
			if (!type.startsWith('image/')) return new Response('Unsupported media type', { status: 415 });
			await env.PHOTOS.put(key, request.body, { httpMetadata: { contentType: type } });
			return new Response(JSON.stringify({ key }), {
				status: 201,
				headers: { 'Content-Type': 'application/json' },
			});
		}

		return new Response('Method not allowed', { status: 405, headers: { Allow: 'GET, HEAD, PUT' } });
	},
} satisfies ExportedHandler<Env>;
