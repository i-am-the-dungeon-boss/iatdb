### Task 2: Multiplayer Village & World Chat

**Goal**: Turn the ground level from task 1 into a place where players see each other and talk.
Not implemented, not scheduled — this document exists so task 1's shapes don't foreclose it.

**Status**: forward plan. Task 1 ships single-player and is complete on its own.

#### What task 1 already gets right for this

- The village is **persistent and outside any run**, in its own save slot under the solo
  namespace. There is a place that exists while nobody is descending — the precondition for
  shared presence.
- The town avatar is **disposable**. It is a real `Hero`, but nothing it carries reaches a run:
  choosing a mode at the dungeon mouth starts a fresh one. So the presence payload can be just
  class skin, position and facing, and there is no run state that could leak between players.
- The village and house levels are **hand-authored and deterministic**, generated from code
  rather than from a seed. Every client renders the identical map with no map synchronisation.
- **The house is explicitly always solo.** It is the documented private space, so there is
  already a defined answer to "where do I go to not be seen," and no retrofit is needed.
- Player identity already exists: `heroechoes/online/EchoPlayerAuth` + `EchoPlayerAuthGate`,
  backed by `POST /v1/auth/device|username|credentials` and `GET /v1/auth/me`.

#### Shape

The **hero-echoes** service (Next.js / Payload, separate repo) is the natural host — it already
owns player identity, and the game client already speaks to it through
`heroechoes/online/EchoClient.java` over `EchoHttpTransport`.

Two concerns, deliberately separable:

**Presence** — who is standing where.

- Client sends its avatar (player id, display name, class skin, cell, facing) on a low tick while
  the player is in `VillageScene`, and receives the other occupants.
- Rendered as ordinary sprites on the existing tilemap. They are **not** `Char`s: no collision,
  no combat, no turn scheduling. Ghost-through movement avoids every desync question that
  authoritative movement would raise, and the village has no mechanics to protect.
- Interpolate between received positions; never rewind the local player.

**World chat** — a single global channel to start.

- Send: `POST /v1/chat/messages`. Receive: poll `GET /v1/chat/messages?since=` first; upgrade to
  SSE or WebSocket only if polling proves visibly laggy.
- Rendered both as a scrollable window and as short-lived speech bubbles over the speaker's
  avatar, reusing the existing floating-text visuals.
- Moderation is a hard requirement before this is user-visible: server-side rate limiting, a
  length cap, a block list, a report path, and a server-side mute that the client honours. This
  is the largest non-technical cost in the task and should be scoped before any client work.

#### Transport choice

Both concerns want the same thing — "give me what changed since X." Start with **HTTP polling on
a 1–3s tick, only while the village scene is focused**, because:

- `EchoHttpTransport` / `JavaEchoHttpTransport` already exist and are already known to work on
  all three platforms; a socket layer is new surface on desktop, Android and RoboVM alike.
- Presence in a hub tolerates a second of staleness.
- It degrades to single-player cleanly: if the backend is unreachable, the village is simply
  empty and chat is disabled — the same failure posture Ranked already has via
  `EchoBackendProbe.isOnlineReady()`.

Revisit only with a measurement, not a preference.

#### Open questions

- **Sharding.** One global village instance does not survive popularity. Plan for N instances of
  the same map with a soft cap, and a "join friend's instance" affordance later.
- **Village vs. run.** Does presence persist while a player is descending (shown as "in the
  dungeon"), or do they vanish from the village? Showing them as away is friendlier and cheap.
- **Names and cosmetics.** World chat makes display names public; the auth system's username
  path (`POST /v1/auth/username`) becomes user-facing, and needs its own uniqueness and
  profanity rules.
- **Does the house need a door lock?** If houses ever become visitable, "always solo" becomes a
  toggle rather than an invariant. Task 1 should not build the toggle, but should not assume the
  invariant anywhere outside `HouseLevel` either.
