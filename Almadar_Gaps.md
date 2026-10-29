<!-- Gap ledger for this repo: the source of truth for its open gaps. -->
# orbital-shell-android — open gaps

Every open gap this repo owns lives here. This file is the source of truth; the monorepo's `docs/Almadar_Gaps.md` only rolls it up.

- **One entry per gap:** `- **<code>** — <what is wrong and where>. <owning package> [mechanical|architectural] — <evidence, prevention rung>`.
- **Codes:** new gaps use this repo's prefix `G-ORBITAL-SHELL-ANDROID-`. Take the "Next code" below, then bump it in the same edit. Codes are never reused or renamed.
- **Close by deleting.** Remove the entry in the same commit as the fix.

Next code: `G-ORBITAL-SHELL-ANDROID-002`

- **G-ORBITAL-SHELL-ANDROID-001** — The generated `renderers/compose/renderers/molecules/avl_glyph.kt` still maps the retired operators `ref`, `deref`, `swap`, `watch`, `atomic` to glyph categories (lines ~332-348), and its generator (almadar-pattern-sync) is archived, so it cannot be regenerated [owner-decision]. Owner: whoever owns native renderer generation after the fold. The entries are dead (no program can produce those effects). Prevention rung: (3) a pin test that the glyph table names only operators the installed table declares. Root cause: periphery.
