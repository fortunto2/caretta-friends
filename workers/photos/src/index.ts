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

/** A phone photo, generously. Beyond this something is wrong — or someone is filling the bucket. */
const MAX_PHOTO_BYTES = 15 * 1024 * 1024;

/** What a nest photo may be. The magic bytes are checked, not the caller's Content-Type. */
const IMAGE_SIGNATURES: { name: string; type: string; test: (b: Uint8Array) => boolean }[] = [
	{ name: 'jpeg', type: 'image/jpeg', test: (b) => b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff },
	{
		name: 'png',
		type: 'image/png',
		test: (b) => b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47,
	},
	{
		// HEIC/HEIF: "....ftyp" then a brand.
		name: 'heic',
		type: 'image/heic',
		test: (b) => b[4] === 0x66 && b[5] === 0x74 && b[6] === 0x79 && b[7] === 0x70,
	},
];

function base64UrlToBytes(input: string): Uint8Array {
	const padded = input.replace(/-/g, '+').replace(/_/g, '/');
	const binary = atob(padded + '='.repeat((4 - (padded.length % 4)) % 4));
	const bytes = new Uint8Array(binary.length);
	for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
	return bytes;
}

async function jwks(env: Env, force = false): Promise<JsonWebKey[]> {
	const fresh = !force && jwksCache && Date.now() - jwksCache.fetchedAt < JWKS_TTL_MS;
	if (fresh) return jwksCache!.keys;
	const res = await fetch(env.SUPABASE_JWKS_URL);
	if (!res.ok) throw new Error(`jwks ${res.status}`);
	const body = (await res.json()) as { keys: (JsonWebKey & { kid?: string })[] };
	jwksCache = { keys: body.keys, fetchedAt: Date.now() };
	return body.keys;
}

/** The signing key for this token's `kid`, refetching once if the cache predates a key rotation —
 *  otherwise a rotation silently 401s every upload and download until the hour-long cache expires. */
async function keyFor(kid: string, env: Env): Promise<JsonWebKey | undefined> {
	const cached = (await jwks(env)).find((k) => (k as { kid?: string }).kid === kid);
	if (cached) return cached;
	return (await jwks(env, true)).find((k) => (k as { kid?: string }).kid === kid);
}

/** Verify a Supabase access token and return its claims, or null if it doesn't hold up. */
async function verify(token: string, env: Env): Promise<Claims | null> {
	const parts = token.split('.');
	if (parts.length !== 3) return null;
	const [headerPart, payloadPart, signaturePart] = parts;

	const header = JSON.parse(new TextDecoder().decode(base64UrlToBytes(headerPart)));
	// ES256 only: never let a token pick its own algorithm (that's how "alg: none" gets in).
	if (header.alg !== 'ES256' || !header.kid) return null;

	const jwk = await keyFor(header.kid, env);
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

			// The bucket has no size limit or type allowlist of its own (Supabase Storage did; R2
			// doesn't), and anonymous sign-up is open to anyone holding the app's key — so the cap
			// and the format check live here, or they live nowhere.
			const declared = Number(request.headers.get('Content-Length') ?? '0');
			if (declared > MAX_PHOTO_BYTES) return new Response('Payload too large', { status: 413 });

			const body = await request.arrayBuffer();
			if (body.byteLength === 0) return new Response('Empty body', { status: 400 });
			if (body.byteLength > MAX_PHOTO_BYTES) return new Response('Payload too large', { status: 413 });

			// Trust the bytes, not the header: a Content-Type is whatever the caller typed.
			const head = new Uint8Array(body.slice(0, 12));
			const format = IMAGE_SIGNATURES.find((s) => s.test(head));
			if (!format) return new Response('Unsupported media type', { status: 415 });

			await env.PHOTOS.put(key, body, { httpMetadata: { contentType: format.type } });
			return new Response(JSON.stringify({ key }), {
				status: 201,
				headers: { 'Content-Type': 'application/json' },
			});
		}

		return new Response('Method not allowed', { status: 405, headers: { Allow: 'GET, HEAD, PUT' } });
	},
} satisfies ExportedHandler<Env>;
