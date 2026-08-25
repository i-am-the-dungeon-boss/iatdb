# Honorable mentions & leaderboard bosses — village

Same **data** as the homepage lists. Village **display is its own** — standing bodies plus inspect, not the web row/panel layout.

---

## Current

### Homepage (web)

Two columns on [`page.tsx`](<../../../hero-echoes/src/app/(frontend)/page.tsx>):

- [`HonorableMentionsPanel`](../../../hero-echoes/src/lib/frontend/honorable-mentions-panel.tsx) — five mention rows + web `BossSprite`
- `LeaderboardPanel` / `DepthEchoRow` — current echo at depths 5 / 10 / 15 / 20 / 25 (or default Goo/Tengu/…)

Layout/sprites are web-only: [`pixel-styles.ts`](../../../hero-echoes/src/lib/frontend/pixel-styles.ts), [`boss-sprite.tsx`](../../../hero-echoes/src/lib/frontend/boss-sprite.tsx).

### Data

Honorable mentions — **no game HTTP route**; Payload queries used only by the homepage.

Kinds: Highest kills, Boss Slayer, Hero Slayer, 1st Halls Boss, 1st Sewers Boss.

- [`honorable-mentions.ts`](../../../hero-echoes/src/lib/echo/honorable-mentions.ts) — shape, `toEchoMention`, standings, `fetchHonorableMentions`
- [`honorable-mentions-data.ts`](../../../hero-echoes/src/lib/frontend/honorable-mentions-data.ts)
- [`honorable-mentions-copy.ts`](../../../hero-echoes/src/lib/frontend/honorable-mentions-copy.ts) — web titles / extras / detail
- [`echoes.queries.ts`](../../../hero-echoes/src/lib/echo/echoes.queries.ts) — `findHighestKillEcho`, `findFirstBossAtDepth`, `findDeepestEchoForPlayer`

Fields: `echo_id`, `hero_class`, `depth`, `lvl`, `kill_count`, `echo_data_base64`, `timestamp`, `user_name` (+ `echo_count` / `total_kill_count` for the two player standings).

Current boss per depth — **game already has this read**:

- [`echo-data.ts`](../../../hero-echoes/src/lib/frontend/echo-data.ts) — `fetchAllDepthEchoes`
- [`findBossEchoesAtDepths` / `findBossEchoAtDepth`](../../../hero-echoes/src/lib/echo/echoes.queries.ts)
- [`depths.ts`](../../../hero-echoes/src/lib/frontend/depths.ts) — labels + default boss names
- [`GET /v1/echoes/[depth]`](../../../hero-echoes/src/app/v1/echoes/[depth]/route.ts) — public, no auth
- [`services.ts`](../../../hero-echoes/src/lib/echo/services.ts) `fetchEcho` → [`mappers.ts`](../../../hero-echoes/src/lib/echo/mappers.ts) `EchoWire`

Game fetch (dungeon ranked lookup, not village):

- [`EchoClient.fetchEcho`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoClient.java)
- [`EchoWireCodec.decodeEchoFetch`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoWireCodec.java)
- [`CompositeEchoLookup.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/CompositeEchoLookup.java)
- [`Echo.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/Echo.java)

Fight-result ranks are a **different list** ([`GET /v1/leaderboard/{depth}`](../../../hero-echoes/src/app/v1/leaderboard/[depth]/route.ts), [`WndLeaderboard`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndLeaderboard.java)).

### Village connectivity

Two pipes. Neither currently loads honorable mentions or depth-bosses into town.

**HTTP (mandatory to _enter_ town).** Title probes [`GET /v1/game-version`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoClient.java) via [`EchoBackendProbe`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoBackendProbe.java). Village / Solo / Ranked stay off until `isOnlineReady()`. [`TitleScene.enterVillage`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java) then [`EchoPlayerAuthGate`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoPlayerAuthGate.java) (session) → [`VillageGateway.enterVillage`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageGateway.java). Settings: [`EchoOnlineSettings`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoOnlineSettings.java). Echo HTTP is this same client; `game-version` and `echoes/{depth}` send neither API key nor bearer.

**WebSocket (always joined in town).** Presence + chat only. [`WorldSceneChannel.enter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/WorldSceneChannel.java) calls [`WorldNet.enterVillage`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNet.java) when `VillageSession.inVillage()` and the probe is still ready; otherwise the village is just empty of remotes. [`GET /v1/world/socket`](../../../hero-echoes/src/app/v1/world/socket/route.ts) needs bearer. Game surface is [`WorldNet`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNet.java) / [`WorldNetEngine`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNetEngine.java); [`GameScene.update`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java) ticks it. Forced update from the socket is held until town: [`VillageUpdateGate`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/VillageUpdateGate.java).

The rule is **no village without the channel** — town always joins, and `SPDSettings.worldChat()` deliberately does not gate it. Worth noting that the code does not currently *enforce* it once inside: [`WorldChannel.onWorldStatus`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/WorldChannel.java) answers a `DEGRADED` channel by logging and letting the engine retry, so a socket that drops mid-visit leaves the player in a town with no remotes rather than ejecting them. Nothing in the Plan depends on which way that goes.

See [architecture.md — World channel](../architecture.md).

### Village display (existing bodies)

[`VillageSession.inVillage`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageSession.java) — town avatar, not a run. [`VillageLevel.createMobs`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/VillageLevel.java) plants four flavour NPCs.

|                                                                                                                               | Visible                                                                                                                     | Inspect (magnify / right-click)                                                                                                               | Interact (walk up)                                                                                                         |
| ----------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| [`Villager`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/Villager.java)                | NPC on a cell                                                                                                               | [`WndInfoMob`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndInfoMob.java) (`name` / `description`)            | [`WndTitledMessage`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTitledMessage.java) chat |
| [`RemotePlayerSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/RemotePlayerSprite.java) | Sprite + [`NameTag`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/NameTag.java), no `Char` | Missed — [`examineCell`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java) only finds `Actor.findChar` | None                                                                                                                       |

Visible plumbing: [`RemotePlayers`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/RemotePlayers.java), [`WorldPresence`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldPresence.java), [`GameScene.addRemotePlayer`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java). Local look: [`VillageHeroSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageHeroSprite.java) (warrior cloth; not shared with remotes).

Echo _fight_ look (dungeon, not town): [`EchoBossSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java), [`EchoHeroLoader`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/EchoHeroLoader.java), [`EchoHeroSnapshot`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoHeroSnapshot.java), [`HeroSprite.film`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/HeroSprite.java). Inspect text already on [`EchoBoss.name` / `description`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java). Web field analog: [`boss-visual.ts`](../../../hero-echoes/src/lib/frontend/boss-visual.ts), [`echo-appearance.ts`](../../../hero-echoes/src/lib/echo/echo-appearance.ts), [`boss-display.ts`](../../../hero-echoes/src/lib/frontend/boss-display.ts).

Inspect entry: [`Toolbar`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/Toolbar.java) → `examineCell` / `examineObject`; [`Char.interact`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java). Villager strings: [`actors.properties`](../../core/src/main/assets/messages/actors/actors.properties).

**The Inspect column above is dungeon behaviour, not town behaviour.** `Toolbar.layout` short-circuits to `emptyBarForVillage()`, which hides *and deactivates* `btnSearch` — and `Toolbar.informer` is the only caller of `examineCell` anywhere. Deactivating also kills the `SPDAction.EXAMINE` keybind, so in the village there is no magnify button, no key and no right-click: the villagers' `description()` is currently unreachable. See Plan §6a.

### Tests

- [`honorable-mentions.unit.spec.ts`](../../../hero-echoes/tests/unit/honorable-mentions.unit.spec.ts)
- [`honorable-mentions-panel.unit.spec.ts`](../../../hero-echoes/tests/unit/honorable-mentions-panel.unit.spec.ts) — web copy
- [`EchoBossSpriteTest.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSpriteTest.java)
- [`VillageLevelBuildTest.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageLevelBuildTest.java)
- [`RemotePlayersDiffTest.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/RemotePlayersDiffTest.java)

---

## Target

The homepage lists stand in the village as people, not as a menu. Same echoes the site already shows — current floor bosses and honorable mentions.

You see them in town: each one looks like that hero (gear and class). A depth with no echo yet shows the usual regional boss instead. They occupy the village, alongside villagers and other players.

Looking closer or talking tells you _why_ they are here — Boss Slayer, first Halls, depth, level, kills — the facts the site puts in a row.

They arrive the same way other village online data does. Live players and chat stay their own thing. Town still works if these figures never appear, the way it already works when nobody else is online.

---

## Plan

The village shows **bodies** and, on inspect, **the whole hero behind one of them**. Those are two different payloads with two different costs, and the plan turns on keeping them apart:

- **Broadcast tier** — appearance plus the facts on the site's rows. Ten bodies, a couple of kilobytes, held in hub memory and pushed down the world socket the same way chat is. No HTTP route.
- **Inspect tier** — the full `echo_data_base64` bundle for the *one* figure a player clicked, pulled on demand.

Everything else is supporting work: store appearance instead of re-deriving it (§2), maintain the standings instead of recomputing them (§3), and restore the inspect verb, which town does not currently have at all (§6).

### 1. Two tiers, and where the base64 goes

**Standing there** needs: hero class, armour tier, display name, and the row facts (kind, depth, level, kills, echo/kill totals, timestamp), plus `echo_id` as identity.

**Inspecting** needs the real hero — stats, equipped kit, talents — and that only comes from `echo_data_base64`. [`EchoHeroLoader.load`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/EchoHeroLoader.java) → `EchoHeroSnapshot.restoreHero` → `hero.restoreFromBundle(echo.echoData)` rebuilds a full `Hero`, talents included, from that bundle and nothing else. `policy_input.items` cannot substitute: it is inventory class names with no equipped slot and no talent tiers.

**So yes to base64 — but pulled per figure, on inspect, not bundled into the broadcast.** Ten gzipped hero bundles on every greeting, re-sent on every ~290s socket rotation, on mobile, is the heaviest thing town would ever pull — and most visits open none of them. Lazy costs the same code and a fraction of the bytes.

**Fetch it over the socket, not a new route.** One client frame `{ t: 'echo_req', echo_id }`, one server frame `{ t: 'echo', echo_id, echo_data_base64 }`, symmetric with chat send. The hub reads the echo once and **keeps it in memory** — the figure set is ten documents that change rarely, so the first player to inspect a given body pays a read and everyone after that is free, evicted when the figures refresh. Cheaper and smaller than a `GET /v1/echoes/by-id/{echo_id}` route, which would want its own validation, CORS, auth posture, client method and codec to deliver something the open connection can already carry.

That is also why the figures are not `GET /v1/echoes/{depth}` five times over: [that route](../../../hero-echoes/src/app/v1/echoes/[depth]/route.ts) picks *an* echo for a fight and returns the whole fight payload, which is the inspect tier for all five bodies at once and still not the right selection.

### 2. Store appearance at upload

Armour tier is currently recovered by gunzipping and JSON-parsing the whole bundle on every render — [`echo-appearance.ts`](../../../hero-echoes/src/lib/echo/echo-appearance.ts) `decodeEchoFileBundle` then `resolveEchoArmorTier`, called from [`boss-visual.ts`](../../../hero-echoes/src/lib/frontend/boss-visual.ts). Doing that ten times per broadcast is the wrong shape.

Add a typed `appearance` group to [`Echoes.ts`](../../../hero-echoes/src/collections/Echoes.ts) (`type: 'group'`, never `type: 'json'`, so `payload generate:types` yields a real shape):

| Field | Type | Source |
| --- | --- | --- |
| `appearance.armor_tier` | `number` (1-6), **optional** | decoded once in `uploadEcho` |
| `appearance.subclass` | `text`, optional | `policy_input.subclass` |

Written server-side in [`services.ts`](../../../hero-echoes/src/lib/echo/services.ts) `uploadEcho`, not sent by the game — the client already ships the bundle the tier is in, so asking it for the tier separately adds a trust surface for nothing.

Optional, not defaulted: an undecodable bundle stores **nothing** rather than a hollow `1`. The tier-1 cloth fallback is a *display* decision and belongs in the renderer.

The decoder moves out of `src/lib/frontend/` to [`src/lib/echo/echo-appearance.ts`](../../../hero-echoes/src/lib/echo/echo-appearance.ts) — it stops being a render helper and becomes upload-time domain logic. The landing page then reads the stored field and drops its per-render gunzip too.

Backfill with a `regenerateEchoAppearance` collection endpoint plus admin button, copied from the [`regenerateEchoPolicies`](../../../hero-echoes/src/collections/endpoints/regenerateEchoPolicies.ts) precedent. Until it runs the assembler falls back to decoding the blob, so nothing waits on the migration.

### 3. Fix the standings calculations

Worth doing before a second caller exists.

**`kill_count` is not indexed.** [`Echoes.ts`](../../../hero-echoes/src/collections/Echoes.ts) indexes `echo_id`, `easy_mode`, `game_version`, `player` and `timestamp` — but `findHighestKillEcho` sorts on `kill_count`, so it sorts the whole collection in memory. One line, largest single win: `index: true`.

**`playerStandingPipeline` is a full-collection double `$group`, run twice through `$facet`.** [`honorable-mentions.ts`](../../../hero-echoes/src/lib/echo/honorable-mentions.ts) groups every echo by `(player, depth)`, rolls up per player, then sorts — no index serving the leading `$match`, no pre-filter. O(all echoes) per call on Atlas M0. Caching hides it; it does not fix it.

Maintain the standings instead. `uploadEcho` already touches the player through `updatePlayerById`; add to [`Players.ts`](../../../hero-echoes/src/collections/Players.ts), all indexed:

| Field | Maintained when |
| --- | --- |
| `echo_count` | echo uploaded (Boss Slayer) |
| `total_kill_count` | echo uploaded, and a ranked win bumps `kill_count` (Hero Slayer) |
| `deepest_depth` | echo uploaded, if deeper |
| `deepest_at` | set with `deepest_depth`, the tie-break key |

Boss Slayer and Hero Slayer become `sort + limit 1` over an indexed field on one-row-per-player, instead of an aggregate over one-row-per-echo. The tie-break (`score` → `maxDepth` → earliest at that depth) survives as a compound sort.

Standings count soft-deleted echoes and prune soft-deletes, so pruning does not drift the counters; hard deletes from the admin do, so ship a `recomputeStandings` repair endpoint alongside rather than pretending drift is impossible.

`firstDepth5Boss` / `firstDepth25Boss` never change once set and are already cheap (indexed `depth` + `timestamp`, `limit 1`).

### 4. Delivery — hub memory, pushed down the socket

**There is no village without the socket.** Town always joins the channel — being seen and being able to talk is what the village is *for*, and [`WorldNet.enterVillage`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNet.java) says so outright: `SPDSettings.worldChat()` deliberately does not gate it. There is no state where a player stands in town without a channel, so there is no second delivery path to design.

The hub already holds a change stream ([`watch.ts`](../../../hero-echoes/src/lib/world/store/watch.ts)), so it is already told when an echo changes. Holding the figures is then one field and one broadcast — the same problem chat already solves, one global stream of rarely-changing content fanned out from memory.

**Server** ([`hub.ts`](../../../hero-echoes/src/lib/world/hub.ts)), alongside `ChatLog` and `VillageRoster`:

- Holds the current figures in memory, plus the lazily-fetched inspect bundles from §1.
- Fills once per hub, like `primeHistory`, so a fresh instance does not show a blank village beside a full one. Never throws — a failed read leaves the figures empty and says nothing.
- Refreshes on the change-stream `echoes` event, **debounced onto the tick**: a burst of uploads is one read and one broadcast, not five. Standings are an aggregate, so this re-runs the assembler rather than patching the named document.
- Sends on `hello` (entering town costs no extra round trip) and on change, serialised once and fanned out exactly like `broadcastChat`. Skips the broadcast when the projection is unchanged, the same signature trick [`village-roster.ts`](../../../hero-echoes/src/lib/world/village-roster.ts) uses for presence.

Caching the assembler in `unstable_cache` is optional — it would dedupe across hub instances, but it is no longer a correctness or load-shedding mechanism. No tags, no hooks, no stamps.

**Widening the change stream.** No new stream: Atlas charges one as a connection, so a second would take one a chat send could have used. Widen the namespace regex from `^(?:chat-messages|village-snapshots)$` to include `echoes`, and move the `fullDocument.instance` predicate out of the `$match` into the callback — echoes carry no `instance`, so leaving it in the pipeline filters every echo event out. Two consequences: `delete` events carry no `fullDocument`, so the handler must tolerate its absence; and dropping the predicate delivers other shards' chat events to be filtered in process, which is free today (`WorldNet.INSTANCE` is 0, one shard) but is the first thing to revisit if sharding lands.

**Client.** [`WorldNetEngine`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNetEngine.java) gains `onWorldFigures(List<VillageFigure>)` and a request/response pair for the inspect bundle. That is new nouns on an interface deliberately kept small, so it owes a line in [architecture.md](../architecture.md): global village content on the same push channel as presence, not a new subsystem.

**Staleness.** [`WorldChannel.onWorldStatus`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/WorldChannel.java) answers `DEGRADED` by logging and letting the engine retry, so a town whose socket blinks keeps its figures until the next `hello` refreshes them. Right behaviour for scenery — stale bodies read better than vanishing ones — and it needs no code.

### 5. The read and the wire

[`village-figures.ts`](../../../hero-echoes/src/lib/echo/village-figures.ts) composes what exists rather than re-querying: [`fetchHonorableMentions`](../../../hero-echoes/src/lib/echo/honorable-mentions.ts) for the five mentions, [`findBossEchoesAtDepths`](../../../hero-echoes/src/lib/echo/echoes.queries.ts) for the current boss per depth, then dedup, merge and project. The hub is its only caller. No `easy_mode` parameter anywhere: [`VillageSession.prepareGlobals`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageSession.java) pins `Dungeon.easyMode = false` and mentions are normal-mode only already.

One flat list of bodies — the server merges so the game never has to:

```json
{
  "figures": [
    {
      "post": "depth",
      "depth": 5,
      "echo_id": "5-1771000000000",
      "user_name": "Somebody",
      "hero_class": "WARRIOR",
      "armor_tier": 3,
      "lvl": 14,
      "hp": 30,
      "ht": 40,
      "kill_count": 7,
      "timestamp": 1771000000000,
      "badges": [
        { "kind": "first-depth-5" },
        { "kind": "hero-slayer", "count": 42 }
      ]
    }
  ]
}
```

`post` is `"depth"` or `"mention"`. `armor_tier` is absent when unknown. `hp` / `ht` are the echo's recorded health: the inspect window reads health off the body itself, so the broadcast has to carry it or the Info tab would have nothing truthful to show before the bundle arrives. `kind` values reuse [`HonorableMentionKind`](../../../hero-echoes/src/lib/frontend/honorable-mentions-copy.ts) verbatim so the two surfaces cannot drift; `count` carries `echo_count` / `total_kill_count` where the kind has one. No `echo_data_base64`, `echo_policy`, `game_seed` or `policy_input` — those are the inspect tier.

**Dedup.** One echo can be several things at once: the first Sewers boss may still hold depth 5, and `mostEchoesPlayer` / `highestKillsPlayer` both resolve through `findDeepestEchoForPlayer` onto possibly the same doc. One body per `echo_id`, badges concatenated, depth post wins placement.

**Empty depth.** `echo_id` absent, `post: "depth"`, `depth` set — the game draws the regional boss instead, mirroring `DEFAULT_BOSS_BY_DEPTH` in [`depths.ts`](../../../hero-echoes/src/lib/frontend/depths.ts) with the real sprites. The default name comes from the boss's own `Messages` entry, not from a string on the wire.

Decoding is a `figures` case in the existing world frame codec, next to `chat` and `roster` — not a new codec. A malformed entry is rejected the way a malformed chat row is, without taking the connection down.

### 6. Inspect in town — two prerequisites

**(a) Town has no examine at all.** [`Toolbar.layout`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/Toolbar.java) short-circuits to `emptyBarForVillage()`, which hides *and deactivates* every tool including `btnSearch` — and `Toolbar.informer` is the **only** caller of `GameScene.examineCell` in the codebase. Deactivating also kills the `SPDAction.EXAMINE` keybinding. So the four existing [`Villager`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/Villager.java) NPCs already carry a `description()` nothing can reach.

Split examine from search — one button, two actions, only one of them meaningless in town. [`VillageHero.search`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageHero.java) already returns false because the map is fully revealed; that stays. What comes back is `GameScene.selectCell(informer)` → `examineCell` → the info window.

- Replace `emptyBarForVillage()` with a village layout that keeps `btnSearch` visible, active and positioned, hiding wait / inventory / swap / quickslots as now. A separate layout method, not a flag threaded through the dungeon branches — the existing comment is right that the village must not re-enter that width arithmetic.
- In town the button is examine-only: guard the second and long click on `VillageSession.inVillage()` so it reads as "examine again" rather than falling through to a dead `search(true)`.
- Keyboard, controller and desktop right-click all come free once `active` is true.

**(b) The inspect window already exists — generalise it.** [`WndEchoBossInfo`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndEchoBossInfo.java) is exactly this window: an Info tab (mob title + `info()` text) and a Kit tab rendering the equipped weapon, armour, artifact, misc, ring and second weapon through [`EchoKitSlot`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/EchoKitSlot.java) — and it does **not** swap `Dungeon.hero`. [`GameScene`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java) already routes every examined mob through `WndEchoBossInfo.windowFor(mob)`.

Two changes:

- **Widen the branch.** `hasInspectableKit` / `windowFor` test `instanceof EchoBoss`. Replace with a small interface — `getEchoHero()` — implemented by both `EchoBoss` and `VillageEcho`, so the window stops naming a concrete mob class and village inspect works without new UI.
- **Add a Talents tab.** Missing today, and the Target asks for it. Use `TalentsPane(TalentButton.Mode.INFO, talents)` — the explicit-list constructor [`WndHeroInfo`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndHeroInfo.java) uses — fed from the restored hero's `talents`. `EchoBoss` inspect gains it for free.

**Two standing rules bind this work.** No echo-viewing UI may reassign `Dungeon.hero`, even temporarily, and none may edit `Hero`, `Belongings`, `Dungeon` or `InventorySlot` to make echo data display. `WndEchoBossInfo` and `EchoKitSlot` already honour both — that is why they exist, and why [`WndEchoDetail`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndEchoDetail.java) (which does swap `Dungeon.hero`) is legacy to replace, never a pattern to copy. The no-arg `TalentsPane(Mode.INFO)` that [`WndRanking`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndRanking.java) uses reads `Dungeon.hero`; the two-arg one is the compliant path.

Left alone: the [`RemotePlayerSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/RemotePlayerSprite.java) inspect gap. Those have no `Char`, so `getObjectsAtCell` cannot find them; `VillageEcho` sets the pattern for closing it later.

### 7. Game side

**`village/VillageFigure.java`** — plain value type mirroring one wire entry, badges as a small typed list. Ordinary loops and `ArrayList`; no streams, `List.of` or `java.util.function`, per the RoboVM subset.

**`village/VillageFigures.java`** — sink for `onWorldFigures` and owner of the bodies. Sits beside [`RemotePlayers`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/RemotePlayers.java): same push source, same render-thread-only discipline, different content. Fed by `WorldChannel` on the render thread — no worker thread, no fetch of its own. `spawnInto(Level)` on the first push after the level exists; a push arriving earlier is held and applied by `GameScene.create()`. Later pushes **reconcile** rather than rebuild, as `RemotePlayers.diff` does, so an unchanged figure does not blink. `clear()` on `VillageSession.leave()`.

**`actors/mobs/npcs/VillageEcho.java`** — the body, built like [`Villager`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/Villager.java): `NPC`, `state = PASSIVE`, `Property.IMMOVABLE`, `INFINITE_EVASION`, `chooseEnemy()` null, `damage()` no-op, `add(Buff)` false, `act()` spending `TICK`.

Being a real `Char` is the point: `GameScene.getObjectsAtCell` finds objects through `Actor.findChar`, so a `Char` is inspectable and interactable on walk-up — the Target's two verbs, given §6a restores the button.

It implements §6b's `getEchoHero()` **lazily**: null until the player inspects, at which point it requests the bundle over the socket, restores through `EchoHeroLoader.load` and caches it on the figure. First inspect opens the Info tab with what the broadcast already carried and fills the Kit and Talents tabs when the bundle lands; a failed request leaves those tabs absent rather than empty-but-wrong.

Bundling: they live in `Dungeon.level.mobs`, so `storeInBundle` / `restoreFromBundle` must round-trip the figure (not the fetched hero). The village is never restored from disk, but a mid-session `Dungeon.saveAll()` still writes it. Pick the class name deliberately now — a later rename needs `Bundle.addAlias`.

**`sprites/VillageEchoSprite.java`** — hero spritesheet by class, `HeroSprite.film(tier)` for armour tier, idle only. Separate from [`EchoBossSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java), which carries invisibility handling plus `zap`, `fly` and `read` poses town has no use for. This is a third independent appearance source alongside the local avatar and remote players — do not hoist the three into one constant; they coincide by accident, not by invariant.

Name over the head: reuse [`NameTag`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/ui/NameTag.java), added through `GameScene.addRemotePlayer` as remote players are.

**Placement.** Fixed deterministic anchors in [`VillageLevel`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/VillageLevel.java), asserted by a test rather than eyeballed. Proposal: five depth posts along the waterfront at `y = 5` (below the bank at `y = 4`, above `PATH_TOP`), `x` in {2, 4, 6, 10, 12}, clear of the path column `x = 8`; mention bodies round the well at `(16,14)` — `(14,13)`, `(18,13)`, `(14,16)`, `(18,16)`, `(16,12)`. The test pins: passable, unoccupied, never `arrivalCell()` or `dungeonEntrance()`, never the path column, disjoint from the four `createMobs` villagers.

**Text.** `name()` and `description()` off `Messages.get` with new keys in [`actors.properties`](../../core/src/main/assets/messages/actors/actors.properties) — no hardcoded strings. The description lists every badge with its facts and is what the Info tab renders; `interact` opens the same window on walk-up. The village wording is its own, but the *facts* are the ones [`honorable-mentions-copy.ts`](../../../hero-echoes/src/lib/frontend/honorable-mentions-copy.ts) renders, so the two stay checkable.

### 8. Degradation

Falls out of the design rather than needing a branch: figures arrive after the level is built and are added to a level that is already complete. Entry is never blocked on them — it is already gated on [`EchoBackendProbe`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoBackendProbe.java), unchanged.

| What fails | What the player sees |
| --- | --- |
| The hub's figures read | Village with no figures; chat and other players normal |
| The socket, mid-visit | Figures already standing stay standing, stale, until the next `hello` |
| The socket, before the first push | Town builds and plays as it does when nobody else is online, which the Target allows |
| The inspect bundle request | Info tab with the broadcast facts; Kit and Talents tabs absent, never blank-but-plausible |

Not in this table: a town with no channel at all. There is no such state.

### 9. Tests, in TDD order

**hero-echoes** (Vitest, red → implement → green, mirrored under `tests/unit|int`)

- `tests/unit/echo-appearance.unit.spec.ts` — tier decoded from a real bundle fixture; undecodable bundle yields *absent*, not `1`
- `tests/unit/player-standings.unit.spec.ts` — counters move on upload and on a ranked win; tie-break order matches the pipeline it replaces, against the same fixtures, so the swap is provably behaviour-preserving
- `tests/unit/village-figures.unit.spec.ts` — dedup by `echo_id`, badge merge, depth-post placement precedence, empty-depth entry shape, and no `echo_data_base64` anywhere in the projection
- `tests/unit/hub-figures.unit.spec.ts` — driven through `HubSink` with no socket, as [`hub.ts`](../../../hero-echoes/src/lib/world/hub.ts) is designed for: filled once per hub not once per join; sent on `hello`; a burst of echo events in one tick is one read and one broadcast; an unchanged projection broadcasts nothing; a failed read leaves figures empty and does not throw
- `tests/unit/hub-echo-request.unit.spec.ts` — an `echo_req` for a figure returns the bundle and reads once across repeat requests; a request for an `echo_id` not in the figure set is refused rather than serving arbitrary echoes; the cache clears when the figures refresh
- `tests/unit/watch-namespace.unit.spec.ts` — the widened filter routes chat, snapshot and echo events correctly; the relocated instance predicate rejects exactly what the pipeline used to; a `delete` with no `fullDocument` is tolerated
- extend [`honorable-mentions.unit.spec.ts`](../../../hero-echoes/tests/unit/honorable-mentions.unit.spec.ts) for the stored-appearance read path with blob-decode fallback

**iatdb** (`./gradlew :core:test -q -PerrorProneOff`)

- `ToolbarVillageExamineTest` — characterize the current empty bar first, then: `btnSearch` visible and active in town, everything else hidden; long click does not call `search`
- `WorldFrameCodecTest` (extend) — the `figures` and `echo` frames decode; an unknown frame type is still ignored rather than fatal; a malformed figure entry throws rather than yielding a half-built one
- `VillageFiguresTest` — fills from the greeting push, holds a push arriving before the scene exists, reconciles a later push (added / removed / changed) without rebuilding unmoved bodies, `clear()` on leave, ignores a push outside the village
- `WndEchoBossInfoTest` — `windowFor` picks the kit window for anything exposing an echo hero and plain `WndInfoMob` otherwise; the Talents tab renders the echo's own tiers; **no test path assigns `Dungeon.hero`**, asserted explicitly since that is the standing rule this window exists to honour
- `VillageEchoTest` — passive / immovable / undamageable; `name()` and `description()` through `Messages`; bundle round-trip; `getEchoHero()` null before the request resolves and populated after, with a failed request leaving it null
- `VillageEchoPlacementTest` — the anchor constraints above on a freshly built `VillageLevel`
- `VillageEchoSpriteTest` — class and tier selection, and the tier-1 fallback when armour tier is absent
- characterization first where the surface exists: [`VillageLevelBuildTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageLevelBuildTest.java), [`VillageSessionTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageSessionTest.java) and [`WorldNetTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNetTest.java) must keep passing untouched

### 10. Suggested order

1. `kill_count` index — one line, immediate, independent
2. Toolbar examine in town (§6a) — unblocks the Target's second verb, ships on its own and makes the existing villagers readable
3. Generalise `WndEchoBossInfo` + Talents tab (§6b) — improves `EchoBoss` inspect immediately, before any village work
4. `appearance` field + backfill (§2) — also removes the landing page's per-render gunzip
5. Player standing counters (§3) — behaviour-preserving swap, guarded by shared fixtures
6. Assembler + wire shape (§5) — pure projection, no transport
7. Widened change stream + hub memory + `figures` frame (§4) — the delivery path
8. Game side: bodies standing and reconciling (§7)
9. Inspect bundle request/response (§1, §4) — last, because everything above is complete and useful without it

Steps 2 and 3 are independently shippable improvements to code that already exists. One transport throughout, no HTTP route.

**Built so far.** All of it except the standing counters (step 5).

In `hero-echoes`: the `kill_count` index; the stored `appearance` group with its upload write, admin backfill and landing-page read; the assembler [`village-figures.ts`](../../../hero-echoes/src/lib/echo/village-figures.ts); the widened change stream; hub figure memory with the debounced refresh, the unchanged-projection skip and the lazy bundle cache; the `figures` and `echo` server frames and the `echo_req` client frame; and [`store/figures.ts`](../../../hero-echoes/src/lib/world/store/figures.ts) giving the hub its two real reads.

In `iatdb` on `feature/echo-boss-health-bar-inspect`: the town examine bar; the `EchoInspectable` interface that takes the inspect window off `EchoBoss`; the bodies — `VillageFigure`, `VillageEcho`, `VillageEchoSprite`, `VillageFigurePlacement`, `VillageFigures`; the `figures` / `echo` cases in the frame codec and `echo_req` on the way out; `WorldNetEngine.onWorldFigures` / `onEchoBundle` / `requestEchoBundle`; and `VillageEchoBundles`, which asks for a figure's hero as the player walks up to it and restores it when the bundle lands.

Still open: **step 5**, the player standing counters (§3). It is a behaviour-preserving performance swap — the village works without it, on the aggregate pipeline it has always used.

### 11. Out of scope

Fight-result ranks ([`WndLeaderboard`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndLeaderboard.java)) stay a separate list and route. Making the figures fightable, tradeable or animated is not this change. Replacing `WndEchoDetail`'s `Dungeon.hero` swap is a known debt, tracked elsewhere, not blocking here.
