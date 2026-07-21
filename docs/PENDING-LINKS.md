# Pending Links Registry

External URLs that still point at Shattered Pixel Dungeon / Evan resources, or that need
store/support destinations we do not have yet.

**Single source of truth:** [`project-links.properties`](../services/src/main/resources/project-links.properties)
— edit that file only. [`ProjectLinks.java`](../services/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ProjectLinks.java)
loads it at runtime. Public docs use markdown reference links from
[`project-link-refs.md`](project-link-refs.md); `ProjectLinksTest` fails if they drift.

### Current resolved values (must match the properties file)

| Key                | Value                                        |
| ------------------ | -------------------------------------------- |
| homepage / backend | [homepage][homepage]                         |
| GitHub repo        | [github-repo][github-repo]                   |
| GitHub releases    | [github-releases][github-releases]           |
| Developer email    | [dungeonbossteam@gmail.com][developer-email] |

| Status  | URL                                                                                      | Used in                                                                   | Suggested replacement / notes                        |
| ------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- | ---------------------------------------------------- |
| LINK-1  | `http://ShatteredPixel.com`                                                              | docs\recommended-changes.md                                               | Docs-only; news still upstream until we have a feed  |
| LINK-2  | `http://shatteredpixel.com/feed_by_tag/SHPD_INGAME.xml`                                  | services\news\shatteredNews\...\ShatteredNews.java                        | Own blog/RSS or keep debugNews (current)             |
| LINK-3  | `https://apps.apple.com/app/shattered-pixel-dungeon/id1563121109`                        | (removed from README)                                                     | App Store listing when published                     |
| LINK-4  | `https://explore.transifex.com/shattered-pixel/shattered-pixel-dungeon/`                 | AboutScene.java; docs                                                     | Own Transifex project (or remove)                    |
| LINK-5  | `https://play.google.com/store/apps/details?id=com.shatteredpixel.shatteredpixeldungeon` | (removed from README)                                                     | Play Store listing when published                    |
| LINK-6  | `https://shattered-pixel.itch.io/shattered-pixel-dungeon`                                | (removed from README)                                                     | itch.io page when published                          |
| LINK-7  | `https://ShatteredPixel.com`                                                             | AboutScene.java (Evan / Shattered credits layer — **keep**)               | Upstream attribution; IATDB site is homepage.url     |
| LINK-8  | `https://shatteredpixel.com/assets/images/badges/appstore.png`                           | (removed from README)                                                     | Own badge assets when stores exist                   |
| LINK-9  | `https://shatteredpixel.com/assets/images/badges/github.png`                             | (removed from README)                                                     | Optional                                             |
| LINK-10 | `https://shatteredpixel.com/assets/images/badges/gog.png`                                | (removed from README)                                                     | Own badge when on GOG                                |
| LINK-11 | `https://shatteredpixel.com/assets/images/badges/gplay.png`                              | (removed from README)                                                     | Own badge when on Play                               |
| LINK-12 | `https://shatteredpixel.com/assets/images/badges/itch.png`                               | (removed from README)                                                     | Own badge when on itch                               |
| LINK-13 | `https://shatteredpixel.com/assets/images/badges/steam.png`                              | (removed from README)                                                     | Own badge when on Steam                              |
| LINK-14 | `https://shatteredpixel.com/feed_by_tag/SHPD_INGAME.xml`                                 | services\news\shatteredNews\...\ShatteredNews.java                        | Own blog/RSS or keep debugNews (current)             |
| LINK-15 | ~~shatteredpd homepage~~ → homepage.url                                                  | AboutScene; NewsScene; EchoOnlineSettings                                 | **Resolved**                                         |
| LINK-16 | `https://store.steampowered.com/app/1769170/Shattered_Pixel_Dungeon/`                    | (removed from README)                                                     | Steam page when published                            |
| LINK-17 | `https://www.gog.com/game/shattered_pixel_dungeon`                                       | (removed from README)                                                     | GOG page when published                              |
| LINK-18 | `https://www.patreon.com/ShatteredPixel`                                                 | SupporterScene / WndSupportPrompt (surfaces may be hidden on Play builds) | Own Patreon/support URL                              |
| LINK-19 | `https://www.shatteredpixel.com/blog/`                                                   | (removed from README)                                                     | Own blog when it exists                              |
| LINK-21 | ~~Evan GitHub releases API~~ → derived from github.owner.repo                            | GitHubUpdates.java                                                        | **Resolved** (desktop/Android use echoUpdates today) |
| LINK-22 | ~~Evan GitHub repo~~ → github.owner.repo                                                 | docs/getting-started-\*.md                                                | **Resolved**                                         |
| LINK-23 | ~~Evan GitHub releases page~~ → derived releases URL                                     | README.md; docs/release                                                   | **Resolved**                                         |
| LINK-24 | `https://github.com/00-Evan/pixel-dungeon-gradle`                                        | README.md                                                                 | **Keep** as upstream attribution                     |
| LINK-25 | ~~placeholder email~~ → developer.email                                                  | DesktopLauncher.java; AndroidMissingNativesHandler.java                   | **Resolved**                                         |

<!-- Project link refs: keep identical to docs/project-link-refs.md -->

[homepage]: https://i-am-the-dungeon-boss.vercel.app
[github-repo]: https://github.com/i-am-the-dungeon-boss/iatdb
[github-releases]: https://github.com/i-am-the-dungeon-boss/iatdb/releases
[developer-email]: mailto:dungeonbossteam@gmail.com
