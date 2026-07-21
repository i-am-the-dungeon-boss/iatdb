### Task 4: UI/UX & Polish

**Goal**: Seamlessly communicate the presence of a hero-based boss and apply appropriate visuals.

#### Scope

- Messaging and localization updates.
- Visual presentation (sprite, aura/effects, intro banner).

#### Files/Systems to Touch

- Message bundles under `core/src/main/assets/messages/` (e.g., `windows/windows.properties`, `scenes/*.properties`).
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/` and `.../scenes/` for on-screen text.
- Online backend settings — see [online-integration.md](online-integration.md)
- Sprite: `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java` and related effect assets.

#### Implementation Steps

1. Messages & Banners:
   - Add new localized strings: boss intro line, defeat line, and hints.
   - On boss level entry, if echo boss is active, show a banner (existing banner system) with the boss name (e.g., the echo hero’s alias) and subtitle.

2. Visuals:
   - Ensure `EchoBossSprite` uses hero-alike animations with a distinct tint/aura to distinguish from the player.
   - Optional: add a subtle shader effect or particles on spawn.

3. Settings:
   - **Online mode** — ranked play uses Hero Echoes fetch/upload (solo uses local storage)
   - Backend URL + API key in advanced settings or build config (dev builds)

4. UX Safeguards:
   - If online fetch fails, display nothing special; silently fall back.
   - Keep intro text brief to avoid interrupting gameplay.

#### Edge Cases

- Accessibility/readability: ensure color contrast is sufficient for any new tints.
- Localization completeness: provide English fallback if translations missing.

#### Testing Checklist

- Toggle off → default bosses always spawn.
- Toggle on + echo present → banner and custom messages appear.
- Sprite renders correctly across platforms (desktop/mobile) without asset glitches.

#### Acceptance Criteria

- Players see a clear, localized indication when a hero boss appears.
- A persistent setting allows opting out of the feature.
- Visuals distinguish the boss from the player without confusion.
