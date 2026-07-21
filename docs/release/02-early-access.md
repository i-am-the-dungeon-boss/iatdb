# Stage 2 — Early Access (Open Testing)

Ship a playable build to testers via Google Play open testing. Introduce optional supporter-tier purchases through Google Play Billing.

**Play track:** Open testing (requires production access)  
**Monetization:** Optional supporter tiers — digital products via Play Billing  
**Builds on:** [01-coming-soon.md](01-coming-soon.md) exit criteria

**Carryover from Stage 1:** Patreon UI was hidden (not replaced). This stage wires the real Play Billing supporter link/UI.

---

## Non-code tasks

### Play Console setup

- [ ] Create open-testing track release from pre-registration or fresh upload
- [ ] Configure open-test countries/regions (may differ from pre-registration)
- [ ] Set tester limits or opt-in URL strategy if needed
- [ ] End or transition pre-registration campaign in overlapping markets

### Supporter-tier product design

- [ ] Define supporter tiers (names, benefits, pricing)
- [ ] Confirm all tier benefits are digital goods requiring Play Billing (no external payment links)
- [ ] Write clear in-app and store copy explaining what each tier grants
- [ ] Plan pricing per region and any launch promotions
- [ ] Define refund and support policy for purchases

### Policy and compliance

- [ ] Review Play payments policy for supporter/digital-tip products
- [ ] Update privacy policy if billing adds data collection (purchase history, account linking)
- [ ] Update Data Safety form for billing-related data
- [ ] Confirm supporter benefits do not violate gambling, loot-box, or misleading-practices policies

### Store listing and marketing

- [ ] Update store listing for early-access status (clearly labeled as early access / open test)
- [ ] Refresh screenshots and description to reflect current playable content
- [ ] Prepare feedback channel (email, Discord, in-app link, etc.)
- [ ] Plan communication to pre-registrants about early-access availability

### Operations

- [ ] Set up crash and ANR monitoring for test builds
- [ ] Define release cadence for open-test updates
- [ ] Establish process for triaging tester feedback
- [ ] Document known issues and limitations for support responses

---

## Code tasks

### Google Play Billing integration

- [ ] Add Play Billing Library dependency and wire into Android build
- [ ] Define supporter-tier product IDs in Play Console and map in app
- [ ] Implement purchase flow (browse tiers → purchase → acknowledge)
- [ ] Implement purchase restoration on reinstall / new device
- [ ] Handle billing edge cases (pending, cancelled, already owned, network errors)

### Replace upstream monetization

- [ ] Remove or gate Patreon supporter button and victory nag ([recommended-changes.md](../recommended-changes.md))
- [ ] Replace SupporterScene with Play Billing–backed supporter UI
- [ ] Remove external payment URLs from all in-app surfaces and locale strings

### Supporter benefit delivery

- [ ] Implement entitlement storage for purchased tiers (local and/or server-backed)
- [ ] Grant tier benefits in-app (cosmetic, badge, ad-free, etc. — per product design)
- [ ] Verify benefits persist across app updates within open test

### Quality and testing

- [ ] Test purchase flow with Play license testers and test product IDs
- [ ] Test purchase restoration after reinstall
- [ ] Verify release AAB with billing enabled passes Play pre-launch report
- [ ] Regression-test core gameplay on target devices

---

## Exit criteria

- [ ] Open test live and installable via Play Store listing or opt-in URL
- [ ] Supporter tiers purchasable through Play Billing in test regions
- [ ] No external payment links remain in release builds
- [ ] Purchase restoration works for testers
- [ ] Crash rate and critical bugs within acceptable thresholds for wider launch
- [ ] Feedback loop operational (channel, triage, release cadence)
- [ ] Store listing accurately reflects early-access scope and supporter tiers
- [ ] Full-release scope defined (gameplay/content IAP catalog, launch criteria)
