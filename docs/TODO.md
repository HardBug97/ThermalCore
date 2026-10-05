# TODO — ThermalCore

Current, outstanding work only. See [progress-log.md](progress-log.md) for what's
already done and why.

## Next up

The plan for all four repos is `../CoFHCore/docs/port-plan.md`; this repo's steps are §4 (Phase 0,
per repo), §5 A.2/A.3 (1.21.1) and §6 B.10 (26.1.2).

Phase A (1.21.1) is **code-complete** on branch `1.21.1`: `./gradlew build` is clean,
`verify_runserver.sh` reaches `Done`, and `runData` runs and matches the committed output
(2026-09-22). This repo builds against whatever branch `../CoFHCore` has checked out, so
**put CoFHCore on `1.21.1` to build or run it**. CoFHCore's working branch is `26.1.2` now.

1. **Joel's `runClient` pass** (port plan §A.4). It is the one Phase A exit criterion left, and
   everything client-side is unverified. See `../CoFHCore/docs/TODO.md` for the checklist and
   for `MouseHandlerMixin`, a specific suspect.
2. **Phase B (B.10) waits for CoFHCore's 26.1.2 port** (B.0-B.5 done there, ~895 errors left: recipes, client, mixins).
   **Before starting B.10, read `../CoFHCore/docs/TODO.md`'s "B.10 inherits from B.3/B.4/B.5"
   notes.** They list what changes here: the persistence bridge, capability registration on the new
   transfer API, armour and tool material records, the `ENCHANTABLE` component, the new
   `neighborChanged` without a neighbour position (TD's ducts), and more. The shapes are in
   `../CoFHCore/docs/api-notes-26.1.2.md`.
   Nothing to do here until CoFHCore compiles on 26.1.2. When it does, branch `26.1.2` from
   `1.21.1`, switch the data run back to `clientData()`, and **regenerate `src/main/generated`
   rather than hand-migrating it** (see the progress log's runData entry for why).

## Inbox

- **24 `c:` tags are referenced but defined by nobody.** Not a NeoForge convention, not emitted
  by this repo's datagen: `c:dusts/{lead,silver,tin}`,
  `c:gears/{bronze,constantan,electrum,lead,nickel,silver,tin}`,
  `c:ingots/{electrum,invar,lead,silver,tin}`, `c:nuggets/{lead,tin}`,
  `c:plates/{constantan,electrum,invar,lead,silver}`, `c:gems/{ruby,sapphire}`. These are the
  alloys and metals Thermal Foundation used to add and other mods may still provide — the
  recipes using them are compat recipes and were already dead without those mods on 1.20.4, so
  this is pre-existing, not something the sweep caused. Decide per tag whether to define it
  under `data/c/tags/item/` or drop the recipe. (The mod's own `c:slag`, `c:tar`, `c:bitumen`,
  `c:coal_coke`, `c:rosin`, `c:sawdust`, `c:coins/*`, `c:plates/*`, `c:gears/*` etc. *are*
  emitted under `src/main/generated/data/c/tags/` — those are fine.)
- **When adding or checking a `c:` tag, use two oracles, not memory**: `Tags.java` in
  `neoforge-21.1.251-sources.jar` for the constants, and the `data/c/tags/**.json` that
  `neoforge-21.1.251.jar` itself ships for what is actually populated. Some conventions are not
  literal `tag("...")` calls — `c:dyes/<colour>` and `c:dyed/<colour>` come from NeoForge's
  `DyeColor` patch — so grepping `Tags.java` alone gives false "does not exist" answers.
  `c:slimeballs` still works but is deprecated in favour of `c:slime_balls`.
- ~~`guidebook.json` result shape~~ fixed 2026-10-05: result sets the `patchouli:book`
  component. Original note: **`data/thermal/recipe/tools/guidebook.json` is still on the pre-1.20.5 recipe shape** — a
  `"nbt"` string in `result` and `{"item": …}` ingredients. Needs `components` /
  `DataComponents` when recipes are done (A.2). Its condition type was fixed to
  `neoforge:mod_loaded` during the sweep (it was `forge:mod_loaded` — a condition, not a tag,
  so it is `neoforge:`, not `c:`).
- **`settings.gradle` dropped a `mixingradle` `resolutionStrategy` and the Sponge/Parchment
  plugin repos.** Neither plugin was applied and this repo has no mixins — re-add if that
  changes.
