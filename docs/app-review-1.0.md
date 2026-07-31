# App Review — 1.0

Kept in the repo so the next submission starts from what Apple actually asked, not from memory.

## Rejection — submission `0734850b-0ea1-4b1b-903d-101f621641ad`, build 1.0 (3)

Reviewed 31 July 2026 on iPhone 17 Pro Max and iPad Air 11-inch (M3). Three items:

| Guideline | Issue | Fix |
|---|---|---|
| 5.1.1(v) Data Collection and Storage | App supports account creation but offers no account deletion | Code — shipped in build 4 |
| 2.3.6 Accurate Metadata | Age rating did not declare user-generated content | Metadata — done |
| 2.1 Information Needed | Asked whether the app uses ARKit, where to find it, and for AR markers | Answer below |

## Reply drafted for Resolution Center

> Thank you for the detailed review. All three items are addressed in build 1.0 (4), now uploaded.
>
> **Guideline 2.1 — ARKit**
>
> Yes, the app has one ARKit feature: the AR nest finder.
>
> Where to find it: launch the app → you land on the **Map** tab → tap the round coral button with
> the ARKit icon at the bottom-right of the map, just above the "locate me" button. It opens a
> full-screen AR camera view.
>
> What it does: it pins each nearby sea-turtle nest in AR at its real-world position, so a volunteer
> walking the beach can see which direction a nest is and how far away it is. It uses ARKit world
> tracking combined with GPS and the compass — the device's location fix is the world origin and
> each nest is placed at its East/North offset from it.
>
> No AR markers are needed: the feature uses neither image nor object tracking, so there is nothing
> to print or scan. Two notes for testing: ARKit does not run in the Simulator, so a physical device
> is required; and nest pins only appear when a nest is within range of your location. The seeded
> nests are on the beaches of Gazipaşa, Turkey, so from your location the AR view will open and
> track normally but show no pins. The camera view, tracking status and on-screen distance readout
> confirm the feature is running.
>
> **Guideline 5.1.1(v) — Account deletion**
>
> Added in build 1.0 (4). The app signs each volunteer in anonymously on first launch and can
> upgrade that to an email account, so deletion is now offered in the app:
>
> **Profile** tab (last tab) → scroll to the bottom → **Delete account** (in red) → a dialog explains
> exactly what will happen → **Delete for good** → "Your account has been deleted", and the app
> returns to its first-launch state.
>
> The account is destroyed on the server: the auth user and the profile record are deleted outright,
> together with the session. No deactivation, no waiting period, no customer-service step.
>
> The nests, patrols and beach markers the volunteer recorded stay on the shared map, detached from
> them — the owner reference is cleared, so nothing links the records to a person. These are the
> community's field observations of a protected species, kept for scientific and legal continuity by
> the conservation project that runs the app; volunteers cannot delete them while using the app
> either. Once detached they hold no personal data.
>
> A screen recording of the flow on a physical device is attached.
>
> **Guideline 2.3.6 — Age rating**
>
> Corrected: "User-Generated Content" is now set to Yes in App Store Connect.

## What was changed

- `supabase/migrations/0006_account_deletion.sql` — `delete_account()`, SECURITY DEFINER, acts only
  on `auth.uid()`. Verified against the live project: anonymous sign-up 200 → RPC 204 → token 403.
- `AuthBackend.deleteAccount()` / `clearSession()`, `CarettaRepository.deleteAccount()`,
  `LocalStore.delete()`, `ProfileScreen` delete row + confirm dialog, strings in EN/RU/TR.
- Age rating `userGeneratedContent = true` via `asc age-rating edit`.
- Keywords reworked for search: added `loggerhead` (the species' English name, previously missing)
  and Turkish terms; dropped `caretta`, which the app name already indexes.

## Still needed before resubmitting

- [ ] Screen recording of the deletion flow on a physical device, attached to the reply.
- [ ] Reply in Resolution Center, then Resubmit with build 4 attached.
