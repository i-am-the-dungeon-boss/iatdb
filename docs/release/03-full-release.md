# Stage 3 — Full Release (Production)

Promote the app to production on Google Play. Expand monetization to gameplay/content in-app purchases alongside existing supporter tiers.

**Play track:** Production  
**Monetization:** Gameplay/content IAP (Play Billing) + existing supporter tiers  
**Builds on:** [02-early-access.md](02-early-access.md) exit criteria

---

## Non-code tasks

### Play Console production setup

- [ ] Create production release from tested open-test build
- [ ] Configure staged rollout percentage and expansion plan
- [ ] Set production countries/regions (expand beyond early-access if desired)
- [ ] End open-testing track or maintain parallel test track for future betas
- [ ] Submit production release for review

### Gameplay/content IAP catalog

- [ ] Define gameplay/content product catalog (items, expansions, cosmetics, etc.)
- [ ] Set pricing, regional pricing, and any launch bundles
- [ ] Write store and in-app descriptions for each product
- [ ] Confirm products comply with Play policies (no loot-box odds issues, clear pricing)
- [ ] Plan product lifecycle (new items, deprecations, seasonal content)

### Policy, legal, and compliance

- [ ] Final privacy policy review (Hero Echoes online features if enabled)
- [ ] Final Data Safety form (network, leaderboard, echo upload, billing, backup)
- [ ] Confirm GPLv3 obligations for any new assets or code in production build
- [ ] Review content rating questionnaire for production scope
- [ ] Confirm target audience and families policy compliance if applicable

### Store listing and launch

- [ ] Update store listing from early-access to full-release positioning
- [ ] Finalize screenshots, feature graphic, and promotional video (if any)
- [ ] Prepare launch announcement (blog, social, pre-registrant notification)
- [ ] Set up Play Store listing experiments if desired (A/B icon or description)

### Operations and support

- [ ] Define on-call or response process for launch-day issues
- [ ] Prepare rollback plan (halt rollout, revert to previous version)
- [ ] Set up production crash/ANR alerting thresholds
- [ ] Document support playbook for purchase, refund, and entitlement issues
- [ ] Plan post-launch update cadence

---

## Code tasks

### Gameplay/content IAP

- [ ] Define product IDs in Play Console and map in app
- [ ] Implement purchase flow for gameplay/content products
- [ ] Implement entitlement checks gating content behind purchases
- [ ] Handle consumable vs non-consumable vs subscription product types as designed
- [ ] Ensure supporter-tier and content IAP coexist without conflicts

### Entitlement and migration

- [ ] Migrate or preserve early-access supporter entitlements in production
- [ ] Verify purchase restoration covers all product types (supporter + content)
- [ ] Handle edge cases: tier upgrades, duplicate purchases, expired subscriptions

### Hero Echoes online (if shipping)

- [ ] Configure Android backend URL and API key securely (not in public builds)
- [ ] Implement opt-out setting for online features
- [ ] Ensure network calls are non-blocking and fail gracefully offline
- [ ] Align client data collection with Data Safety declarations

### Production quality gates

- [ ] Full regression pass on production candidate build
- [ ] Verify R8/release obfuscation does not break billing or online features
- [ ] Run Play pre-launch report on production AAB
- [ ] Confirm version code/name bumped and changelog prepared
- [ ] Smoke-test staged rollout build before expanding percentage

### Post-launch readiness

- [ ] Verify in-app update mechanism (if using GitHub updates service, point at own repo)
- [ ] Confirm news/announcements service points at own feed or is disabled
- [ ] Remove debug-only code paths and `.indev` suffix from production build

---

## Exit criteria

- [ ] Production release approved and rolling out (staged or 100%)
- [ ] Gameplay/content IAP purchasable and entitlements granted correctly
- [ ] Early-access supporter entitlements preserved for existing buyers
- [ ] Purchase restoration works for all product types
- [ ] Crash rate and ANR rate within acceptable thresholds at launch scale
- [ ] Monitoring and alerting active for production
- [ ] Store listing reflects full-release positioning
- [ ] Post-launch update and support processes documented
