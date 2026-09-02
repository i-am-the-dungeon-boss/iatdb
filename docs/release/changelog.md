# What's new

Player-facing release notes. Newest first. Paste the current section into the GitHub
Release body (`scripts/release.ps1 -NotesFile`) when shipping.

---

## 0.0.14

### The village has people in it

Town is no longer empty. Other players' dungeon bosses now stand around the village, each
one holding the spot it earned, with its name over its head and a crier calling out its
title. Walk up to any of them and take a look — you can see who they were and what they
were carrying before you ever have to fight something like it.

The crowd keeps itself up to date. When someone takes a depth off its previous holder,
that one figure changes hands; everybody else stays exactly where they were standing.

### Bosses look like what they're actually carrying

A dungeon boss now wears its loadout. The armour and weapon you see on it are the ones it
will use on you — so what's on screen is a genuine read on the fight you're about to have.

### Size one up before you commit

You can inspect a dungeon boss directly and get its own info window and health bar, the
same way you'd examine anything else in the dungeon.

### Bosses fight with the whole pack, not just a sword

This is the big one. A boss is a real player's run, and it now plays like one.

- **It throws things.** Javelins, knives, tomahawks, shurikens, poison and blinding darts —
  whatever the captured run was carrying. Ammo is finite: it spends the free stuff first
  (wands, bow) and only then starts throwing, and once the pack is empty it closes to melee.
- **It can blind you.** A boss with tipped darts but no wand finally has an answer to being
  shot at from across the room.
- **Doors won't save you.** Keep ducking behind the same door and a boss carrying a fire
  tool will simply put it through. If it isn't carrying anything for the job, the door holds
  — bosses replay a real loadout and don't get free answers.
- **Running is harder.** A retreating boss now moves well ahead of you, so a Potion of Haste
  closes real distance but never lets you catch one for free. It also refuses to back into a
  spot it couldn't shoot you from.
- **It won't stand in its own fire.** Boss AI now reads burning ground and gas honestly: it
  goes around, heads for clean water when it's on fire rather than water that's under a
  cloud — and if it's completely surrounded, it pushes through one tile to reach open ground
  instead of standing there burning.
- **Shields don't make it leave.** Put up a Barrier and the boss stays on you, because its
  hits are eating the shield. It only backs off from true invulnerability, where hitting you
  really would be wasted.
- **Throwing gives away a hiding boss.** A shot from stealth still lands with its full
  surprise bonus — and then reveals the thrower, exactly like it does for you.

### Stuns can't be chained

Paralysis and the other hard locks now share a cooldown in boss fights. Once one lands, a
second can't be stacked on top of it. That cuts both ways: it applies to you and to the
boss.

### Tell us when something breaks

There's a new option in the game menu to send an error report. The description is optional —
just sending it is genuinely useful.

### Also

Boss turns are smoother and more reliable across the board.
