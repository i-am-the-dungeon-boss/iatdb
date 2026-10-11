# Steam store page: key values

Game: I am the Dungeon Boss
AppID: 5020980 (Demo AppID: 5307440)
Store admin: https://partner.steamgames.com/admin/game/edit/1264414
Landing page: https://partner.steamgames.com/apps/landing/5020980
Reference game: Shattered Pixel Dungeon (AppID 1769170)

Status key: SAVED = saved in Steamworks but not published. PUBLISHED = live. TODO = not done.

---

## Already set before (done)
- Basic Info, Support Info, Developer/Publisher: Dungeon Boss
- 20 store tags, in order: Turn-Based Tactics, Traditional Roguelike, PvP, Perma Death, PvE, Strategy RPG, Turn-Based Strategy, 2D, Top-Down, Turn-Based Combat, Pixel Graphics, Class-Based, Mystery, Asynchronous Multiplayer, Singleplayer, Loot, Action-Adventure, Action RPG, Action Roguelike, Combat

## Descriptions (SAVED, English)

### Short description
Descend a cursed dungeon where every victory leaves a scar. Dethrone the boss, and your legend rises as the next guardian — an echo that waits in the dark. Roguelike permadeath, shifting floors, and optional ranked play against other players’ legacies.

### About this game
The dungeon does not forget.

In I am the Dungeon Boss, you carve a path through a living underworld of traps, relics, and hungry floors. When you finally cast down a boss, that triumph does not end — it becomes legacy. Your Hero Echo can return as the next floor’s tyrant, standing where you once stood, guarding what you once sought.

Solo runs pit you against the ghosts of your own conquests. Ranked play lets those echoes cross into other players’ dungeons, so your legend may rise — or fall — far from the path you walked.

Built on the bones of classic pixel-dungeon roguelikes: permadeath, procedural depths, class fantasy, and a fight that gets personal the deeper you go.

Dethrone the boss. Guard the dungeon. Your legacy holds the door.

## Content survey (PUBLISHED, 2026-10-05)
Survey URL: https://partner.steamgames.com/contentdescriptors/editsurvey/5020980/
- Fantasy / Mild Violence: Unrealistic blood color; Cartoon violence / Fantasy violence; Fights without gore or blood, weapons, bones/skeletons, derogatory language, anguish
- Interactive Elements: Online interactivity
- Everything else: unchecked
- Mature content flags (all 5): unchecked
- Generative AI: Yes
  - Store page note: "Store page description text was drafted with AI assistance and edited by the developer. No AI-generated content is used in the game itself."
  - AI generates content or code during gameplay: No
  - Connects to third-party AI service during gameplay: No
- Generated ratings: Brazil 10 anos (violence, online interactivity); Germany (violence, online interactivity)

## Platforms (SAVED)
Windows and Linux + SteamOS (macOS removed from store 2026-10-06)
Note: Store and Steamworks backend both list Windows + Linux/SteamOS only (macOS removed 2026-10-06). Platform Support Matches ✔. Steamworks settings Publish tab not used (separate from store publish).

## System requirements (SAVED)
Windows
- Minimum: OS Windows 10; Graphics OpenGL 2.0+ compatible; Storage 500 MB
- Recommended: OS Windows 11
macOS (REMOVED from store platforms 2026-10-06; values may still exist hidden)
- Minimum: OS macOS 10.12 Sierra; Processor Intel or Apple Silicon; Graphics OpenGL 2.0+ compatible; Storage 500 MB
- Recommended: OS macOS 26 Tahoe; Processor Apple Silicon
- Minimum Additional Notes: "Rosetta 2 is currently required for Apple Silicon macs" (desktop/build.gradle bundles an x64 macOS JDK)
Linux + SteamOS
- Minimum: OS Any distro with glibc 2.17+ should work; Graphics OpenGL 2.0+ compatible; Storage 500 MB
- Recommended: OS SteamOS 3, Ubuntu 24.04 LTS, or equivalent

## Accessibility features (SAVED)
- Gameplay: Save Anytime
- Visual: Adjustable Text Size, Camera Comfort
- Audio: Custom Volume Controls
- Input: Playable without Quick Time Events, Mouse Only Option, Touch Only Option, Playable at Your Own Pace

## Controller support (SAVED, wizard complete)
- Devices: Mouse and keyboard, plus gamepads
- Xbox: Yes, full support (correct glyphs + on-screen keyboard for text entry)
- PlayStation: DualShock (USB), DualShock (USB + Bluetooth), DualSense (USB), DualSense (USB + Bluetooth)
- Steam Input API: No (game uses its own libGDX controller code)
- Store shows: Full Controller Support, Xbox Controllers, PlayStation Controllers
## Release date (SAVED)
- Specified date: December 3, 2026 (Thursday)
- Customers see: "December 2026"
- Edit: Store Admin > Basic Info > Release Dates (pencil icon)
- Rule: the store page must be visible as Coming Soon for 2+ weeks before release, so it needs to be live by about Nov 19.
## Remaining checklist (TODO)
- 5+ screenshots (1920x1080)
- Capsules: header 460x215, small 231x87, main 616x353
- Library: capsule 600x900, hero 3840x1240, logo
- App icon 32x32 + shortcut .ico (files exist: iatdb\desktop\src\main\assets\icons\)
- Game build: pricing, trailer, default build, launch options
- Optional: Cloud saves, Achievements (SPD has both through closed Steamworks code; IATDB has no Steamworks integration)
- Languages: SPD lists 19; waiting on the user to say whether IATDB's new texts are translated or English only
- Check: the on-screen keyboard appears for controller text entry (part of the full Xbox support claim)

---

## Demo: I am the Dungeon Boss Demo (AppID 5307440)
Store admin: https://partner.steamgames.com/admin/game/edit/1337745
Landing page: https://partner.steamgames.com/apps/landing/5307440
Rule: keep the demo in sync with the main game. It shows as a button on the main store page, with no separate demo store page.

- Descriptions: SAVED (same as main game; the tab is called "Demo Description")
- Platforms: SAVED (Windows and Linux + SteamOS (macOS removed from store 2026-10-06))
- System requirements: SAVED (same as main game, including the Rosetta 2 note)
- Controller support: SAVED (same as main game)
- Accessibility: SAVED (same 8 features)
- Developer/Publisher: SAVED (Dungeon Boss / Dungeon Boss)
- Content survey: SAVED, NOT PUBLISHED (same answers as main game, survey 838007). Needs "Publish Generated Ratings".
- Tags: TODO. The same 20 tags are set up, but the Tag Wizard only saves through its own Publish button. The wizard also needs one top-level genre, so "Strategy" sits 21st.
- Release date: TODO. The demo has no release date editor; the package "I am the Dungeon Boss Demo - 1834462" has a blank date.
- Still needed: screenshots, capsules, library assets, app icon, shortcut icon, plus a build, launch options, and a trailer

## Publishing rule
Don't publish anything (store page, ratings, tags) until the user says so. Main game ratings were published on 2026-10-05, before this rule.