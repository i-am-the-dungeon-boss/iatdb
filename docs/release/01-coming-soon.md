# Stage 1 — Coming Soon (Pre-registration)

Build awareness on Google Play before the game is playable. Users can pre-register.

**Play track:** Pre-registration  
**Prerequisite:** Production access (may require closed test first — see [README.md](README.md#stage-flow))

### Decisions (this pass)

| Decision                                | Choice                                                                                                           |
| --------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `appPackageName` / Play `applicationId` | `com.dungeonboss.iatdb`                                                                                          |
| Shared semver                           | `0.0.1` — keep [`hero-echoes/package.json`](../../../../hero-echoes/package.json) `version` === `appVersionName` |
| `appVersionCode`                        | `1` (Play integer; increment each upload; keep `GAME_VERSION_CODE` in sync)                                      |
| Release update checker                  | hero-echoes `GET /v1/game-version` via `echoUpdates` (not GitHub/Shattered)                                      |
| News feed                               | Still stubbed (`debugNews`)                                                                                      |
| Branding assets (icons, title banner)   | Text title + hero logo animation; launcher icon = About `Icons.IATDB` (tier-6 Cleric)                            |
| Supporter                               | Play Billing tip ladder (≥ $2.99 SKUs); Patreon stays disabled                                                   |
| Developer contact email                 | [dungeonbossteam@gmail.com][developer-email]                                                                     |

---

## Non-code tasks

### Developer account and access

- [ ] Confirm Play Console account type (personal vs organization) and note any testing requirements
- [ ] Complete Play Console identity verification and payment profile setup
- [ ] Apply for production access if required (closed test → application → approval)
- [ ] Set up Play Console user permissions for release management
- [ ] Create Play Console in-app products: `support_299`, `support_499`, `support_999`, `support_1999`, `support_4999`

### Legal, identity, and policy

- [x] Finalize application ID (`com.dungeonboss.iatdb`) — permanent once published
- [ ] Confirm fork is visually and nominally distinct from Shattered Pixel Dungeon (impersonation risk — see [getting-started-android.md](../getting-started-android.md#distributing-your-app))
- [ ] Confirm GPLv3 source-availability plan (public fork, attribution, credits)
- [ ] Draft privacy policy URL (required even if app collects minimal data)
- [ ] Complete Play Console app content declarations (target audience, ads, etc.)
- [ ] Complete Data Safety form based on current app behavior (network, backup, etc.)

### Branding and store listing

- [ ] Finalize app name: **I am the Dungeon Boss**
- [ ] Write store listing copy that clearly identifies this as a fork/mod of Shattered Pixel Dungeon / Pixel Dungeon
- [ ] Prepare store assets: feature graphic, screenshots, short and full descriptions (launcher icon done in-repo)
- [ ] Choose initial launch countries/regions
- [ ] Set up developer contact email and support channel
- [ ] Resolve upstream links flagged in [PENDING-LINKS.md](../PENDING-LINKS.md) that appear in store-facing or in-app surfaces

### Testing and operations

- [ ] Run closed test if needed for production-access eligibility
- [ ] Recruit and manage closed-test tester list (minimum 12 opted-in for 14 days if required)
- [ ] Define pre-registration campaign duration and early-access transition plan (90-day limit)
- [ ] Plan launch notification and auto-install expectations for pre-registrants

---

## Code tasks

### Application identity

- [x] Change `appPackageName` from upstream default (see [recommended-changes.md](../recommended-changes.md)) → `com.dungeonboss.iatdb`
- [x] Set initial `appVersionCode` / `appVersionName` → `1` / `0.0.1` (synced with hero-echoes `package.json`)
- [x] Replace placeholder developer contact email in crash dialogs → [dungeonbossteam@gmail.com][developer-email]

### Branding and attribution

- [x] Replace app icon and launcher assets with IATDB Cleric mark
- [x] Update title screen: text brand + landing-style hero logo animation
- [x] Update About scene and credits to reflect fork identity (IATDB layer above restored Shattered/Evan)
- [x] Hide Patreon / external payment entry points; Support uses Play tip billing instead

### Release build readiness

- [ ] Generate signed AAB for Play upload
- [ ] Secure and document signing key (Play App Signing setup)
- [ ] Verify release build runs cleanly on target devices (min SDK 21+)
- [x] Replace upstream update service with hero-echoes `/v1/game-version`; news stays stubbed

### Privacy-sensitive behavior

- [ ] Audit permissions (`INTERNET`, backup, etc.) against Data Safety declarations
- [x] Patreon disabled; Play Billing tip UI wired (create SKUs in Console before testing purchases)

---

## Exit criteria

- [ ] Production access granted in Play Console
- [ ] Signed AAB uploaded and approved for pre-registration track
- [ ] Store listing complete and reviewed for impersonation clarity
- [ ] Privacy policy live and linked in Play Console
- [ ] Data Safety form submitted and consistent with app behavior
- [ ] All [PENDING-LINKS.md](../PENDING-LINKS.md) items affecting public surfaces resolved or deferred with documented rationale
- [ ] Pre-registration live in chosen regions
- [ ] Transition plan to early access documented (date, scope, billing readiness)

<!-- Project link refs: keep identical to docs/project-link-refs.md -->

[developer-email]: mailto:dungeonbossteam@gmail.com
