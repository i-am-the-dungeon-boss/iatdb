# I am the Dungeon Boss

**I am the Dungeon Boss** (IATDB) is an unofficial fork / modification of
[Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon)
by [Evan Debenham](https://shatteredpixel.com/), which is itself based on
[Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle) by
[Watabou](https://watabou.itch.io/).

This project is **not** affiliated with, endorsed by, or published by Shattered
Pixel or Watabou.

## License

I am the Dungeon Boss is free software under the
[GNU General Public License v3.0](LICENSE.txt) (GPLv3), the same license as
Shattered Pixel Dungeon. If you distribute binaries built from this source, you
must also make the corresponding source available under GPLv3.

## What this is

A Pixel Dungeon–family roguelike that adds **Hero Echoes**: fallen heroes can
return as floor bosses, with optional online ranked play via a separate Hero
Echoes service.

It currently builds for **Android**, **desktop**, and **iOS**.

## Downloads

Alpha and other community builds are published on
[GitHub Releases](https://github.com/marwanelzainy/iatdb/releases).

To build distributable APK + desktop JAR locally:

```bash
./gradlew prepareRelease
```

Output lands in `dist/<version>/` (see [docs/release/prepare-release.md](docs/release/prepare-release.md)).

Store listings (Google Play, etc.) may come later; until then, treat GitHub as
the only official distribution channel for this fork.

## Source & attribution

- This repository: https://github.com/marwanelzainy/iatdb
- Upstream Shattered Pixel Dungeon: https://github.com/00-Evan/shattered-pixel-dungeon
- Original Pixel Dungeon (gradle port): https://github.com/00-Evan/pixel-dungeon-gradle

In-game **About** credits list IATDB (Marwan Elzainy), then Shattered Pixel
Dungeon (Evan Debenham), then Pixel Dungeon (Watabou), plus contributors
required by GPLv3.

## Building

Guides in `/docs`:

- [Compiling for Android](docs/getting-started-android.md)
  - **[If you plan to distribute on Google Play, read the end of that guide.](docs/getting-started-android.md#distributing-your-app)**
- [Compiling for desktop](docs/getting-started-desktop.md)
- [Compiling for iOS](docs/getting-started-ios.md)
- [Recommended changes for your own fork](docs/recommended-changes.md)

Issue reports are welcome. This repository does not currently accept pull
requests for general contribution.
