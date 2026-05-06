# Claude Code Project Instructions — codex-lmstudio-kotlin

## Project Overview

This is **codex-lmstudio-kotlin**, a line-by-line port of the
[`codex-rs/lmstudio/`](https://github.com/openai/codex/tree/main/codex-rs/lmstudio)
crate from the openai/codex repository. The crate provides a small
async client for the LM Studio OSS provider used by `codex-cli`.

The Kotlin port lives in
`src/commonMain/kotlin/io/github/kotlinmania/codex/lmstudio/`. The
upstream Rust source it was translated from is **not tracked** in this
repo — fetch it into `tmp/codex-lmstudio/` by running
`./tools/fetch-rust-source.sh` after cloning. CI does this automatically
as a setup step.

## Translator's mindset

This is a translation project, not a software-engineering project. While porting a file, you are
the Kotlin author of the same document a Rust author wrote. Architecture, optimization, design
critique, drift measurement — all later. While translating, the only job is the translation.

The discipline:

1. **Read the whole upstream file before you type.** A line-by-line port composes only when you
   know how the file ends. If the file is too long to read in one sitting, split your turn into
   "read the file" and "write the file" — never start typing on a file you've only half-read.

2. **One Rust file → one Kotlin file. Always.** No splitting one `.rs` across several `.kt`. No
   merging several `.rs` into one `.kt`. The 1:1 mapping is the contract; everything downstream
   (ast_distance, port-lint headers, code review) assumes it. If a `.rs` is genuinely too big for
   one Kotlin file, that's a sign you're in `mod.rs`-equivalent territory and the upstream itself
   is a re-export — verify, don't split.

3. **Translate top to bottom in upstream order.** Preserve the declaration order. Don't reorder
   for "logical flow" — the upstream's order *is* the logical flow. The reader who already knows
   the Rust file should be able to scroll the Kotlin file and find every item in the same place.

4. **Comments are content.** License header, module-level doc, every `///` block, every inline
   `//` note, every upstream `// TODO`/`// FIXME` — all translate. Rust syntax inside doc comments
   gets rewritten to Kotlin equivalents (`Vec<T>` → `List<T>`, `Self::foo()` → `foo()`, lifetimes
   dropped, `cfg(test)` and `#[derive(...)]` lifted into prose). You are translating a *document*,
   not just the code.

5. **When a Rust idiom has no Kotlin analog, apply the mapping rule and move on.** `Box<T>`,
   `Arc<T>`, `Cell<T>`, `RefCell<T>`, `Rc<T>`, lifetimes, `PhantomData`, `mem::forget`,
   `drop_in_place`, `Pin`, `MaybeUninit`, `dyn Trait` — all collapse per the mapping table.
   Don't relitigate. A proc-macro becomes a builder/runtime API, not nothing. An upstream Rust
   crate with no KMP equivalent becomes a *separate Kotlin port*, not a `// TODO` placeholder.
   Pay the snowball cost upfront — the next consumer will thank you.

6. **Don't measure mid-port.** ast_distance, FnSim, similarity reports — useful *after* a file is
   done, useless *during*. Mid-translation measurement is procrastination dressed as rigor. Run
   the tools when a file lands or when a port phase wraps, not while you're choosing between
   `Result<T>` and `T?`.

7. **Don't optimize the translation.** "This Kotlin shape would be simpler" is the wrong
   thought. The upstream shape is the spec. If a faithful translation produces a function that
   takes a parameter you'd never write in Kotlin from scratch, take it. Optimization is a
   separate, named pass after parity is reached — never blended into the translation.

8. **Don't re-architect mid-port.** "This whole module would be cleaner if..." — write the
   thought on a sticky note, throw the sticky note away, finish the file. The current architecture
   is the upstream's architecture. Earn the right to redesign by first reaching parity.

9. **Compile errors during translation are normal and expected.** A bottom-of-tree file compiles
   when its deps are ported, not before. Don't pause to "make it compile" mid-port — that pulls
   you into stub-shaped fixes that you'll have to undo. Climb the dep tree bottom-up; the leaves
   compile first, then their parents, then everything compiles together at the end.

10. **Bottom-up always.** Port dependencies before consumers. If `state.rs` uses `EvalException`,
    port `eval_exception.rs` first. If `eval_exception.rs` uses `Error`/`WithDiagnostic`/`CallStack`,
    port those first. The order isn't optional; trying to port top-down produces a tree of stubs
    that all need replacing.

11. **Hard files are not skippable.** logos-codegen, lalrpop's table generator, an annotate-snippets
    equivalent — when you hit one, port it. Skipping leaves a `// TODO`-shaped hole that grows
    every time another consumer needs it. The snowball is the whole point: each hard port done
    makes the next port easier, because the dep is now in Kotlin.

12. **Warnings are real, but `@Suppress` is never the answer.** `UNUSED_PARAMETER` on a callback
    helper means the function shape doesn't fit Kotlin — restructure the signature, don't suppress.
    `UNCHECKED_CAST` means the type system is missing an invariant — encode it. Every warning is
    either a real bug or a translation choice that needs revisiting; treat them as compile errors.

13. **Stop at file boundaries, not function boundaries.** After every completed file, exhale,
    commit, move on. Don't pause mid-function to second-guess a choice. The whole-file context
    is what makes individual choices coherent.

14. **Doc-port discipline applies even when the upstream doc is awkward.** If the upstream
    author wrote a tortured English sentence in a doc comment, translate the tortured sentence.
    Don't smooth it. Don't paraphrase. Their doc is the contract for the Kotlin doc.

15. **The cheat detector is your friend.** If `ast_distance` forces your file's score to 0
    because you left snake_case identifiers or `pub` keywords in Kotlin comments, take it as a
    literal instruction: rewrite those comments to be Kotlin-native. Rust syntax in Kotlin source
    — code or comments — is the cheat we're catching.

The sticky-note version: **"Read the file. Translate it. Don't think about anything else."**

## Critical Workflows

### 1. Read AGENTS.md first

Every translation rule, idiom mapping, and naming convention is in
[AGENTS.md](./AGENTS.md). Don't translate a file without reading it.

### 2. Port-Lint headers (REQUIRED)

Every Kotlin file MUST start with:

```kotlin
// port-lint: source src/<file>.rs
package io.github.kotlinmania.codex.lmstudio
```

The `// port-lint:` line points at the upstream Rust path relative to
`tmp/codex-lmstudio/`.

### 3. Quality verification

After porting a file:

```bash
./gradlew compileKotlinMacosArm64
./gradlew macosArm64Test
```

The runtime gate is the ported tests passing — `./gradlew test` running
green across targets. Compilation alone is not the gate.

## STRICT RULES — Translation, Not Engineering

### This is a translation project.

When you encounter a compile error, the fix is ALMOST ALWAYS in the Rust
source. Don't invent solutions to make the Kotlin compiler happy. Read
the corresponding Rust file and translate faithfully. The two narrow
exceptions, both still faithful:

1. Async runtime: Rust uses `tokio` (`#[tokio::test]`, `tokio::process`).
   Kotlin uses `kotlinx-coroutines` (`runTest`, suspend functions).
2. HTTP client: Rust uses `reqwest`. Kotlin uses `Ktor`. The semantics
   stay the same; only the API surface differs.

### No code stubs. Period.

No empty bodies, no `TODO()`, no `error("not implemented")`,
no `unimplemented!()`-equivalent placeholders. If you can't fully
translate a file, don't create it.

### No JVM-only APIs

`commonMain` is the default. `nativeMain` for native-specific shims (e.g.
`platform.posix.getenv`). `jsMain`/`wasmJsMain` for web targets. Never
import `kotlin.jvm.*`, `java.*`, or `javax.*` anywhere.

### No typealias re-export shims

If Rust has `pub use foo::Bar`, find the callers and point them at the
canonical Kotlin location of `Bar` rather than re-exporting through a
typealias.

### No @Suppress

`allWarningsAsErrors` is on. Fix the cause.

## Translation Mappings (short reference)

| Rust | Kotlin |
| --- | --- |
| `async fn` | `suspend fun` |
| `tokio::test` | `runTest { … }` (kotlinx-coroutines-test) |
| `reqwest::Client` | `io.ktor.client.HttpClient` |
| `serde_json::Value` | `kotlinx.serialization.json.JsonElement` |
| `serde_json::json!({…})` | `buildJsonObject { put(…) }` |
| `std::env::var(name)` | `platform.posix.getenv(name)?.toKString()` (nativeMain) |
| `std::process::Command` | suspend wrapper around `kotlinx-io` / platform `posix_spawn` |
| `Result<T, io::Error>` | `Result<T>` returning `T` or throwing `IOException` |

For HTTP responses:

```kotlin
val response: HttpResponse = client.get(url)
if (response.status.isSuccess()) { /* ... */ } else { /* ... */ }
```

## Trait default methods with `where` clauses

Rust trait default methods gated by `where T: SomeBound` translate to
Kotlin **extension functions whose own generic type parameter carries
the bound** — never tighten the interface, never add a runtime
`is Comparable<*>` cast, never make the method abstract just to dodge
the issue. Concrete subtypes specialise by declaring a same-named
member (no `override` keyword); Kotlin resolves to the member for the
concrete static receiver type and to the extension for the interface
type, exactly mirroring Rust's per-impl override of a trait default.
When the bound lives on a *class* parameter rather than a trait method,
fall back to the `Comparator<in K>` field pattern with a
comparator-or-natural dispatch helper. See
[AGENTS.md](./AGENTS.md) §"Trait default methods with `where` clauses"
for the worked recipe and rationale.

## TODO Policy

**DO NOT add TODO comments.** No exceptions. If you can't translate
something, don't commit a stub — leave it unwritten and track the gap
in NEXT_ACTIONS.md or via ast_distance.

## Commit Messages

Follow Sydney's style:
- No AI branding or attribution
- Clear, descriptive messages about the technical changes
- No emoji unless requested
- No "Co-Authored-By" lines

## Re-exports from upstream `mod.rs` files

When an upstream Rust `mod.rs` is **only re-exporting** something that actually lives elsewhere
(`pub use <crate-path>::<Name>;`, often under a different name), do **not** preserve that
re-export shape in Kotlin as a "central alias" API. Do not write a `typealias` for the
re-exported name. The existing `Forbidden` rule against "Re-export typealias files at root
packages" is enforced through this procedure.

Workflow:

1. **Identify what the `mod.rs` is re-exporting and the name it's exported as.** Record both
   the original symbol's fully-qualified upstream path and the (possibly different) re-export
   name.

2. **Find callers — Rust-side first, then Kotlin-side.** Many `*-kotlin` repos are
   bootstrap-only (`tmp/` cloned, little or no Kotlin ported yet), so the deterministic source
   of truth is the Rust import graph, not the Kotlin source. Grepping the Kotlin tree first
   will silently miss every caller whose port hasn't started.

   a. **Rust-side (deterministic, primary).** Build or query a graph (graphml or an equivalent
      JSON index) of every `use` statement and every `pub use` re-export across all
      `tmp/<crate>/**/*.rs` files in the workspace, keyed by symbol path. Every Rust crate that
      does `use <reexport-crate>::<reexport-path>::<Name>` — directly, or via a transitive
      `pub use` chain — is a future Kotlin caller. For each importer, drill into the Rust
      source to find the specific call sites: `<Name>(…)`, `: <Name>`, `<Name>::method`,
      `impl <Name> for …`, pattern matches, trait bounds, generics. Record the Rust path of
      each call site so that when that crate is later ported to Kotlin, the translation lands
      on the upstream symbol from day one and never on the re-export.

   b. **Kotlin-side (live ports, secondary).** Repos that have already produced Kotlin source
      need migration *now*. Search `*-kotlin/src/**/*.kt` for:
      - direct imports: `import <reexport-package>.<Name>`
      - wildcard imports of the re-export package, when `<Name>` is used in the file body
      - fully-qualified inline references

   The Rust pass catches callers whose Kotlin doesn't exist yet; the Kotlin pass catches
   callers already ported. Both must run.

3. **Rewrite each live Kotlin caller to reference the upstream/original symbol directly.** If
   the caller still needs to write `<Name>` unchanged, use Kotlin aliasing:
   `import <upstream-fully-qualified-name> as <Name>`. Never bridge with a Kotlin `typealias`.
   For Rust-side findings whose Kotlin counterpart hasn't been written yet, no edit is made
   now — instead, the call sites are recorded as a porting hint for whoever lands the Kotlin
   translation later.

4. **Keep `Mod.kt` (or the equivalent file for that package) as a tracking file.** It carries
   the translated upstream module-level comments and a literal-quoted reference to each
   upstream `pub use` line (e.g. `// pub use crate::lib::result::Result;`). Each time a caller
   is migrated off the re-export, append the caller's absolute path under a
   `// Callers migrated:` ledger in `Mod.kt`. Append, never delete. Once all callers are
   migrated, the `typealias` (if any) is removed; the tracking file remains as the ledger of
   the migration.

   Also record the **Rust-side projected callers** (crates with `tmp/` that import the
   re-export but haven't been ported yet) under a `// Projected callers (Rust):` block in the
   same file, so future porters see the migration target before they ever introduce a new
   caller pointing at the re-export.

Reference example: `/Volumes/stuff/Projects/kotlinmania/serde-kotlin/tmp/serde/serde_core/src/private/mod.rs`
re-exports `Result` from `crate::lib::result`. The Kotlin tracking file lives at
`/Volumes/stuff/Projects/kotlinmania/serde-kotlin/src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Mod.kt`.
A caller that previously did `import io.github.kotlinmania.serde.core.private.Result` is
rewritten to `import kotlin.Result as Result` (or just removes the import and relies on the
auto-imported `kotlin.Result`).
