# Embedded Markdown Manual

Imported from https://github.com/North-Western-Development/MarkdownManual,
branch `1.21.1`, commit `b1d6b4a5a87091b0a31aa7476c216ced088787ea`.
Upstream: https://github.com/MightyPirates/MarkdownManual.

The original MIT license and asset dedication are retained in
[LICENSE-MarkdownManual](../LICENSE-MarkdownManual). The implementation lives
under `src/main/java/li/cil/oc2/manual`; packages are relocated to avoid collisions
with a separately installed Markdown Manual mod. Assets retain their original
namespace. OC2R owns initialization on both loaders; no external Markdown Manual
or Architectury installation is required.

## Compatibility

`ManualGraphics` adapts GUI extraction, bitmap text, item rendering, tooltips and
scissor coordinates between 1.21.1 and 26.x. The document parser, inline formatting,
provider matching, localized page lookup, redirects and navigation
history remain in the embedded implementation. Screen access reuses OC2R's
`RenderCompat` for the 26.2 GUI owner change.

Manual registries are client-local and retain deferred, memoized factories. In
particular, tab item stacks must not be created during registry/client setup or
at the 26.x title screen: item component defaults are bound when joining a world.
NeoForge queues initialization through client setup; Fabric initializes through
its client entry point.

The manual license is packaged by both loaders. A separately installed Markdown
Manual mod is neither required nor used by OC2R.

The missing OC2R monospace atlas is restored from the existing bundled Monocraft
font. `scripts/generate-manual-font.py` reproduces it with Pillow; the original
font's OFL license remains in `assets/oc2r/fonts/LICENSE`. A resource regression
test checks the atlas dimensions and every printable ASCII glyph.

## Verified targets (2026-10-18)

| Target | Unit tests | Server GameTests | Manual in a real world |
| --- | ---: | ---: | --- |
| NeoForge 1.21.1 | 502 | 35 | Passed |
| NeoForge 26.1.2 | 503 | 37 | Passed; all 43 Russian pages also resolved and laid out without fallback |
| NeoForge 26.2 | 503 | 37 | Passed |
| Fabric 1.21.1 | — | 53 | Passed |

All listed tests are passing. The 26.x server counts each include one vanilla
dummy test. Client checks cover the real manual item, tabs, a link, back,
scrolling, formatting, item/block/tag previews and textures. Screenshots and
result files are under `build/reports/manual-qa/`; the Russian run has ten
screenshots under `26.1.2-ru/`.

The final four JARs contain the embedded implementation, Russian resources and
license, with no temporary QA classes/entrypoints or external manual dependency.
See [the English/Russian content audit](MANUAL_CONTENT_AUDIT.md) for corrected
information and inspection boundaries. This does not close the unrelated
[remaining Stage 3 work](roadmap/stage3-acceptance.md).

## Code examples

Inline code uses single backticks. Multiline examples use a fence of at least
three backticks on its own line, optionally followed by a language such as `lua`
or `sh`. A closing fence must contain at least as many backticks as the opener.
An unclosed fence extends to the end of the page. Up to three spaces before the
opener are allowed and the same indentation is removed from its content.

The language is an authoring label, not a syntax-highlighting mode. Code stays
literal: headings, links, underscores, asterisks and inner backticks are not
parsed. Empty lines and indentation remain visible, with tabs using four-column stops;
long lines wrap within the
manual instead of extending beyond the page. Existing inline code is retained.

English and Russian scripting, networking and robotics examples distinguish
commands run in the Linux shell from code entered in Lua. The examples omit
prompt characters so those characters are not copied as part of the command.

## Guidebook research (2026-10-18)

Primary references inspected for this revision:

- [GuideME supported Markdown](https://guideme.appliedenergistics.org/authoring/markdown/)
  and [page authoring](https://guideme.appliedenergistics.org/authoring/) describe
  Markdown pages, item links and embedded images/recipes.
- [Patchouli page types](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/page-types/)
  separate text, crafting and visual pages.
- [Modonomicon introduction](https://klikli-dev.github.io/modonomicon/docs/intro/)
  describes index/graph navigation and text, item, recipe and image page types.

The relevant improvement here is clearer command examples alongside existing
linked pages, tabs and live item previews. A graph interface, search engine,
recipe renderer or replacement guidebook dependency is outside this revision.
The embedded renderer supports its documented subset, not all CommonMark or
any of those guidebooks' custom tags.
