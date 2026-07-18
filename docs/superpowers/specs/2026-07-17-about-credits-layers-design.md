# Design: About Credits Layers (IATDB + Shattered)

**Date:** 2026-07-17  
**Approved:** yes

## Goal

About screen shows a distinct **I am the Dungeon Boss** credit layer above a restored **Shattered Pixel Dungeon / Evan** layer, without removing GPLv3-required upstream credits.

## Layers (top → bottom)

1. **IATDB** — title `I am the Dungeon Boss`; body `Developed by: _Marwan Elzainy_\nBased on Shattered Pixel Dungeon`; no link; distinct highlight color; reuse an existing neutral icon until custom art
2. **Shattered PD** — title `Shattered Pixel Dungeon`; Evan Debenham; ShatteredPixel.com; Aleks / Celesti / Lumine under this layer (SHPX color)
3. **Pixel Dungeon** (Watabou) → libGDX → Transifex → Freesound — unchanged

## Out of scope

- Custom IATDB avatar art
- Website / GitHub link on IATDB block
- Locale strings for About (hardcoded English blocks match existing AboutScene style)
