---
name: explain-game-module
description: >-
  Writes a short Markdown explainer for one game module’s parent type (Buff,
  Blob, Hero, ArmorAbility, Spell, …) so engineers and product designers share
  the same picture. Explains the base/hub class, how children plug into it, and
  how it integrates with other modules (Char, items, blobs, UI, save, …). Not a
  catalogue or a deep-dive of one subclass. Use when the user asks to explain,
  document, or write an .md for a package, module, component, or directory such
  as buffs, blobs, hero, abilities, or spells.
---

# Explain a game module

The explainer is about the **parent** — the base or hub class that defines the
module. Children exist only to show kinds (“a [`FlavourBuff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/FlavourBuff.java) waits; [`Frost`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Frost.java) is one”).
Do not write an instance spec (no Frost attach/detach card).

A **module** is one Java package/directory that owns one kind of game object
(e.g. [`actors/buffs`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs),
[`actors/blobs`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs),
[`actors/hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero),
[`actors/hero/abilities`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities),
[`items/spells`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/spells)).

**Link every class, package, and file you mention** (first mention).

## Link paths

Links inside the generated explainer are **relative to the doc's own location**, so they
resolve in GitHub, VS Code preview, and any plain Markdown reader — not just an editor
that happens to anchor at the repo root. From `<module>/docs/<Parent>.md`, `../` is the
module directory:

| Target                            | In `actors/buffs/docs/Buff.md`         | Never                                          |
| --------------------------------- | -------------------------------------- | ---------------------------------------------- |
| Parent, same directory            | `../Buff.java`                         | `core/src/main/java/.../buffs/Buff.java`       |
| Sibling class in the same module  | `../FlavourBuff.java`                  | absolute or repo-root path                     |
| Class in another package          | `../../Char.java`                      | `core/src/main/java/.../actors/Char.java`      |
| A package (directory)             | `../../blobs`                          | `core/src/main/java/.../actors/blobs`          |
| Engine class in `SPD-classes`     | `../../../../../../../../../../SPD-classes/src/main/java/com/watabou/utils/Bundle.java` | a repo-root path |
| A file in another repo            | do not link — name it, or drop it      | a path that resolves from neither repo root    |

Check every link before finishing: from the doc's directory, the target must exist.
Deep `../` chains to `SPD-classes` are ugly but correct; if a chain is unreadable, name
the class in backticks without a link rather than writing a path that resolves nowhere.

Paths shown **in this skill file** are repo-root anchors to help you navigate the code.
They are not the format for the generated doc — do not copy their shape into it.

Words this skill uses:

| Term          | Meaning                                        | Example                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------- | ---------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Module        | The directory                                  | [`actors/buffs`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs)                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Parent / type | Base or hub class — **the subject of the doc** | [`Buff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Buff.java), [`Blob`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs/Blob.java), [`Hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java), [`ArmorAbility`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/ArmorAbility.java), [`Spell`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/spells/Spell.java) |
| Kind          | Immediate subtype of the parent                | [`FlavourBuff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/FlavourBuff.java), [`TargetedSpell`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/spells/TargetedSpell.java)                                                                                                                                                                                                                                                                                     |
| Child         | One concrete class — name it, don’t spec it    | [`Frost`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Frost.java), [`HeroicLeap`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/warrior/HeroicLeap.java)                                                                                                                                                                                                                                                                                      |

Write for **two readers**: engineer (API, clock, save) and designer (what the player sees, what a change to the _type_ feels like). Prefer tables and mermaid. Do not write essays.

## Output

Default path: **`<this-directory>/docs/<Parent>.md`** — filename matches the parent class. Nested packages are separate modules; an ancestor `docs/` does not count.

Example: [`actors/buffs/docs/Buff.md`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/docs/Buff.md). `hero/`, `hero/abilities/`, and `hero/abilities/warrior/` each get their own `docs/<Parent>.md`.

Create `docs/` in that package if it is missing. Use another path only if the user names one.

Do **not** list every child. Do **not** center the doc on a child. Extra pages for the same module also go in that `docs/` folder.

## Workflow

```
- [ ] 1. Identify module + parent
- [ ] 2. Read the parent (base/hub) fully
- [ ] 3. Skim children only to name kinds
- [ ] 4. Grep the parent’s API; group hits by other module
- [ ] 5. Write explainer (template below)
```

### 1. Identify

- Parent class (or hub, e.g. [`Hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java))
- How children are chosen (subclass folder, [`HeroClass`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/HeroClass.java) switch, item inventory)
- What this module is **not** (neighbor types that apply or query the parent)

Two shapes — both still parent-first:

How to aim the explainer:

| Shape      | When                                                                                                                    | Doc focus                                              |
| ---------- | ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| **Family** | Many subclasses of one type                                                                                             | Parent contract, verbs, kinds, **integrations**        |
| **Hub**    | One class + collaborators ([`Hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java)) | Hub job, extension points it exposes, **integrations** |

### 2. Read the parent

Read the base/hub class end to end ([`Buff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Buff.java), [`Blob`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs/Blob.java), [`ArmorAbility`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/ArmorAbility.java), [`Spell`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/spells/Spell.java), [`Hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java)). That file is the source of verbs, lifecycle, polarity, save, UI hooks.

Skim **kind** classes next ([`FlavourBuff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/FlavourBuff.java), [`CounterBuff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/CounterBuff.java), [`ShieldBuff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/ShieldBuff.java)) — enough to fill the kinds picker. Open a child only to confirm a kind exists (put its name in the Example column). Stop.

Discover apply APIs from the **parent** — do not assume `Buff.affect`. Blobs seed volume on a [`Level`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/Level.java); spells `onCast`; armor abilities `activate`.

### 3. Grep integrations (other modules)

Search for the parent’s public API and host hooks, not one child’s call sites. **Group hits by module** (Java package), one row per module — not per class.

How to label each integration row:

| Direction    | Meaning                              | Typical find                                                                                                                                                                                                                                                                                                  |
| ------------ | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **is-a**     | Parent extends / sits on their clock | `extends` [`Actor`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Actor.java)                                                                                                                                                                                                            |
| **hosts**    | Holds instances of the parent        | [`Char`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java)`.buffs`, [`Level`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/Level.java)`.blobs`                                                                                                              |
| **applies**  | Creates / starts the parent          | [`Buff.affect`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Buff.java), [`Blob.seed`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs/Blob.java), [`Spell.onCast`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/spells/Spell.java) |
| **queries**  | Branches on “is this on?”            | `buff(Class)`, `isImmune`, `volumeAt`                                                                                                                                                                                                                                                                         |
| **renders**  | Icon, sprite, log                    | [`BuffIndicator`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/BuffIndicator.java), `fx`, [`Messages`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/messages/Messages.java)                                                                                                  |
| **persists** | Save / load                          | [`Bundle`](SPD-classes/src/main/java/com/watabou/utils/Bundle.java)                                                                                                                                                                                                                                           |

Packages to scan (skip empty): [`actors`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors) ([`Char`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java), [`Hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java), [`Mob`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Mob.java)), [`actors/blobs`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs), [`actors/hero/abilities`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities), [`actors/hero/spells`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells), [`items`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items), [`plants`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/plants), [`levels`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels), [`ui`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui), [`sprites`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites), [`messages`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/messages), [`windows`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows), [`scenes`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes), [`heroechoes`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes), [`com.watabou.utils`](SPD-classes/src/main/java/com/watabou/utils) ([`Bundle`](SPD-classes/src/main/java/com/watabou/utils/Bundle.java)).

A module may have more than one direction ([`Hero`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java) **applies** and **queries**). Keep each row to the parent API — do not retell how potions or blobs work.

Stop after representative sites. Do not inventory every item.

### 4. Write — form follows content

The template is the parent. Children appear only as **kind examples**. Put a **one-line caption** immediately above every table (what the table answers). Same for mermaid. Link every class / package / file in those tables.

Which shape each section takes:

| Section        | Form                                                            | Prose?       |
| -------------- | --------------------------------------------------------------- | ------------ |
| Job            | One sentence + “not this” table                                 | One sentence |
| Lifecycle      | Mermaid sequence (source → **parent API** → host → clock → end) | No           |
| Verbs          | Table from the parent class                                     | No           |
| Kinds          | Picker: “if it only … → this subtype” + one child name          | No           |
| Integrations   | Table **and** mermaid of **other modules** (below)              | No           |
| Player vs code | Two-column: what the _type_ shows vs parent methods             | No           |
| Change safety  | Checklist of **parent** footguns                                | No           |

Leave out: child attach/detach specs, full catalogues (for statuses/items, point at [`combat-interactions.md`](../../../../hero-echoes/docs/features/echo-policy/arsenal/combat-interactions.md)), Echo-fight house rules unless asked, implementation walkthroughs.

## Template

When filling the template, every named type and package is a markdown link to its file or directory, written **relative to the doc's own location** (see [Link paths](#link-paths)).

```markdown
# <Module> — <Parent> explainer

<One sentence: what this parent is, on what host, on what clock.>

What this parent is, versus lookalikes in other modules:

| This | Not this |
| ---- | -------- |
| …    | …        |

## Lifecycle

How an instance starts, lives on the host clock, and ends:

\`\`\`mermaid
sequenceDiagram
participant Src as Item / blob / trap / UI
participant P as Parent
participant Host as Char / Level / Hero
Src->>P: <parent apply verb>
P->>Host: attach / seed / activate
Note over P,Host: clock / player action
P->>Host: detach / clear / spend
\`\`\`

## Verbs

How other code starts, extends, or strips this parent:

| Call | If already present | Effect |
| ---- | ------------------ | ------ |

## Kinds

How children specialize the parent (name one child; do not spec it):

| If it… | Kind (subtype of parent) | e.g. |
| ------ | ------------------------ | ---- |

## Integrations

Which other modules talk to this parent, and in which direction:

| Module | Direction | Parent hook |
| ------ | --------- | ----------- |

Same map as the table:

\`\`\`mermaid
flowchart LR
subgraph apply [Apply]
Items
Blobs
end
subgraph host [Host / clock]
Char
Actor
end
subgraph show [Present / save]
UI
Bundle
end
Items --> P[Parent]
Blobs --> P
P --> Char
Actor --- P
P --> UI
P --> Bundle
\`\`\`

Replace node names with this parent’s real modules. Omit empty subgraphs.

## Player vs code

What the player sees versus the parent method that produces it:

| Player | Parent API |
| ------ | ---------- |

## Change without surprises

- [ ] … (parent rules only: stacking verbs, immunity, clock, save identity)
```

## Do not

- Spec a child (no [`Frost`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Frost.java) / [`HeroicLeap`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/warrior/HeroicLeap.java) lifecycle card)
- Catalogue every class in the directory
- Grep one child’s applicators and call that the module
- Mix modules **as full explainers** (do not retell how blobs work). One integration row is the blob relationship.
- Use long “why” essays; if a section needs a paragraph, it wanted a diagram
- Mention a class or package as plain text when a path exists — **link it**
- Write a link that does not resolve from the doc's own directory (repo-root paths, cross-repo paths)

Density sample (parent = [`Buff`](core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Buff.java)): [examples.md](examples.md)
