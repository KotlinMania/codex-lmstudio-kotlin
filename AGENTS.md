# codex-lmstudio-kotlin — Agent Guidelines

Line-by-line Kotlin Multiplatform port of the `codex-rs/lmstudio/` crate
from the [openai/codex](https://github.com/openai/codex) repository. This
crate provides the LM Studio OSS provider client used by `codex-cli` to
verify reachability of a local LM Studio server, list local models,
load a model, and trigger downloads via the `lms` CLI.

The upstream Rust source is **not tracked** in this repo — run
`./tools/fetch-rust-source.sh` once after cloning to populate
`tmp/codex-lmstudio/`. That fetched tree is the only authority on what
each function should do; never edit it to make a port easier.

## General Porting Principles

### 1. Line-by-line transliteration

- Maintain file structure and organization from the Rust source.
- Translate functions in the same order they appear upstream.
- Preserve every comment, inline note, and safety/panic doc section —
  translate the language conventions to KDoc but keep the intent
  verbatim. Translate any Rust syntax inside doc strings (e.g. `Vec<T>`,
  `Option<&str>`, `Self::foo()`) to its Kotlin equivalent.
- **NO PORTING NOTES**: Do not add comments explaining Kotlin
  workarounds, "Rust vs Kotlin" rationale, or any other porting
  narratives to the source code.
- **NO RUST IN COMMENTS**: Never leave untranslated Rust code snippets
  or snake_case identifiers in the Kotlin KDocs.
- A missing function is preferable to a stub. If you can't translate
  something, leave the slot empty and track it explicitly rather than
  committing a fake implementation.

### 2. Provenance markers (REQUIRED)

Every ported `.kt` file must start with:

```kotlin
// port-lint: source src/<file>.rs
package io.github.kotlinmania.codex.lmstudio
```

The path on the `// port-lint:` line is relative to `tmp/codex-lmstudio/`
(e.g. `src/client.rs`). Without this header, `port-lint` analysis
cannot track provenance.

### 3. License compatibility

Upstream `codex-rs` is dual-licensed Apache-2.0 OR MIT. This Kotlin port
preserves that. Don't add code under any other license without surfacing
it for review. NOTICE at the project root carries long-form attribution.

### 4. No JVM-only APIs in shared code

This is Kotlin Multiplatform with native, JS, Wasm targets. Don't import
`kotlin.jvm.*`, `java.*`, or `javax.*` from `commonMain`. Use kotlinx
libraries (kotlinx-coroutines, kotlinx-serialization, kotlinx-io,
kotlinx-datetime) and Ktor for HTTP only while the corresponding Rust-crate
Kotlin ports do not expose a usable API.

### 5. No `@Suppress`, no warnings-as-errors bypass

`allWarningsAsErrors` is on. Fix the cause; do not silence with
`@Suppress`. If the cause is upstream behavior the Kotlin compiler
cannot validate, ask first.

### 6. mod.rs → no Mod.kt

If upstream `mod.rs` is pure reexport glue (`pub mod foo; pub use foo::Bar;`),
drop it and rewire callers to import the canonical defining package
directly. If it carries real implementation, re-home that into properly
named files (`Lib.kt`, `Client.kt`).

## Trait default methods with `where` clauses → method-level Kotlin generic bounds

Rust traits routinely declare a default method whose body only typechecks
when the type parameter satisfies a stricter bound:

```rust
pub trait RangeBounds<T> {
    fn start_bound(&self) -> Bound<&T>;
    fn end_bound(&self) -> Bound<&T>;

    fn is_empty(&self) -> bool
    where T: PartialOrd,
    { /* default body uses < */ }
}
```

The trait stays unconstrained; the *method* picks up the bound via its
own `where` clause. Kotlin has no per-method `where` on an interface
member. Three obvious mappings fail:

1. **Tighten the interface to `<T : Comparable<T>>`.** Breaks every
   caller that holds the unbounded interface type.
2. **Make the method abstract on the interface.** Forces every concrete
   impl to invent a body and pile on `override` boilerplate, even when
   the Rust counterpart inherits the default unchanged.
3. **Runtime cast helper** — `if (left is Comparable<*> ...) ... else throw IllegalStateException(...)`.
   Compile-time bounds become runtime crashes; the cheat detector flags
   this and zeros the file's score.

### The faithful pattern

Translate the default to a Kotlin **extension function whose own type
parameter carries the bound**:

```kotlin
interface RangeBounds<T> {
    fun startBound(): Bound<T>
    fun endBound(): Bound<T>
}

fun <T : Comparable<T>> RangeBounds<T>.isEmpty(): Boolean { /* default body */ }
```

Concrete impls that want to specialise the default supply a same-named
**member function**. Kotlin resolves `range.isEmpty()` to the member
when the static receiver type is the concrete class and to the
extension when it is the interface — exactly mirroring Rust's
"default method, per-impl override". No `override` keyword on the
member; there is nothing on the interface to override.

Recipe:

1. Interface keeps only the methods declared without where-clauses.
2. Each default-method-with-where-clause becomes a Kotlin extension
   whose own type-parameter bound mirrors the where-clause.
3. Concrete subtypes specialise by declaring a same-named member.
4. Callers holding the unbounded interface type cannot invoke the
   comparison-using methods — correct, Rust would reject the same
   call without the bound.

### Pair with the dual-overload pattern when both paths are needed

When a function has to work in both the comparator-aware and natural-order
paths, expose two overloads — the unbounded one takes the comparator
explicitly, the bounded one is sugar:

```kotlin
internal fun <Q> Tree.search(key: Q, compare: (Stored, Q) -> Int): Hit { /* heavy */ }

internal fun <Q : Comparable<Q>> Tree.search(key: Q): Hit
    where Stored : Comparable<Q> =
    search(key) { stored, query -> stored.compareTo(query) }
```

Heavy lifting in the comparator overload; natural-order overload is a
one-line delegation. The canonical implementation lives in
[`btree-kotlin`](../btree-kotlin/) `Search.kt::searchTree` /
`searchNode` / `findLowerBoundEdge` / `findUpperBoundEdge` and
`Navigate.kt::searchTreeForBifurcation` / `lowerBound` / `upperBound`.

### Why this is faithful, not engineering

- Interface mirrors Rust's trait declaration shape exactly.
- Extension's bound mirrors Rust's `where` clause exactly.
- Concrete-class members shadow the extension exactly the way Rust
  inherent-impl methods override a trait default.
- "Unbounded callers can't use these methods" mirrors Rust's
  compile-time rejection without the bound.
- No runtime casts, no `IllegalStateException`, no `is Comparable<*>`.

### When you cannot apply this

When the bound is on a *class* type parameter (e.g. `impl<K: Ord> Map<K, V>`),
Kotlin has no method-level analog — class type parameters bind for the
whole class. Use the `Comparator<in K>` field pattern with a
`compareKeys(a, b)` dispatch helper that prefers the supplied
comparator and falls back to a `Comparable<K>`-based path. The fallback
is the design contract, not a translation hack.

## What this crate exposes

From `src/lib.rs`:

- `DEFAULT_OSS_MODEL` — the default OSS model identifier
- `ensure_oss_ready(config)` — verify reachability and load the
  configured model

From `src/client.rs`:

- `LMStudioClient` — async client wrapping a Ktor HTTP client and the
  base URL of the LM Studio server. Methods: `try_from_provider`,
  `check_server`, `load_model`, `fetch_models`, `download_model`,
  `find_lms` (and `find_lms_with_home_dir`).

The Rust crate's tests are network-dependent and use `wiremock` plus an
env-gated sandbox-disabled check (`CODEX_SANDBOX_NETWORK_DISABLED_ENV_VAR`).
The Kotlin port should mirror those tests with `ktor-client-mock` in
`commonTest` and respect the same env-var skip.

## External dependencies

Always review both `tmp/codex-lmstudio/Cargo.toml` and `PORT_REPORT.md`
before changing dependencies. `PORT_REPORT.md` captures cross-repo Rust
`use` edges; `Cargo.toml` captures direct crate dependencies that may need
their own Kotlin ports even when they are not listed in the outbound table.

The upstream Rust crate currently depends on:

- `codex-core`
- `reqwest`
- `serde_json`
- `tokio`
- `tracing`
- `which`
- `wiremock` for tests

The current Kotlin source may use direct Kotlin Multiplatform equivalents
only as a bootstrap bridge while those `*-kotlin` repos are scaffold-only or
missing the specific surface this crate needs. Do not wire a sibling port
just because the directory exists; if it contains only `.gitkeep` files or
does not expose the needed Rust-shaped symbols, depending on it would just
move the stub boundary into another repo.

When a dependency port becomes real and published, migrate this repo to that
port in the same change that verifies the full CI matrix. The expected
migrations are:

- `reqwest` → `io.github.kotlinmania:reqwest-kotlin` for `Client`,
  request builders, responses, status handling, timeouts, and JSON response
  decoding.
- `serde_json` → `io.github.kotlinmania:serde-json-kotlin` for JSON values,
  object construction, parsing, and typed extraction.
- `tokio` → `io.github.kotlinmania:tokio-kotlin` for background task spawn
  and process execution if that port provides the needed runtime/process
  surface; otherwise keep using the established coroutine mapping until it
  does.
- `tracing` → `io.github.kotlinmania:tracing-kotlin` for `info`/`warn`
  events; remove local no-op/drop behavior once a portable tracing facade
  exists.
- `which` → `io.github.kotlinmania:which-kotlin` for locating `lms` on
  `PATH`; remove the local `PATH` scanner once the port supports the same
  targets.
- `wiremock` → `io.github.kotlinmania:wiremock-kotlin` for tests once it can
  replace the current Ktor mock coverage.

Prefer published Maven artifacts for these ports. If a port is only available
as a local checkout, do not silently add an ad hoc composite build; make the
workflow shape explicit and verify that publish still has a clean dependency
story.

This port depends on a small surface from the still-monolithic
`codex-kotlin` project:

- `Config.modelProviders[LMSTUDIO_OSS_PROVIDER_ID].baseUrl`
- `CODEX_SANDBOX_NETWORK_DISABLED_ENV_VAR` (env var name for the
  test-skip check)

Until `codex-kotlin` publishes these as a Maven artifact, the porter
should define a minimal local interface (e.g. `LmstudioConfig` with
just the `baseUrl: String` field) and let downstream consumers wire
the real `Config` to it. Do **not** vendor unrelated codex-core types
to satisfy the dependency.

## Verification

Per the [kotlinmania `AGENTS.md`](../AGENTS.md), the gate is **runtime
equivalence to the Rust source**, not structural similarity scores.

Concretely, after porting a file:

```bash
./gradlew compileKotlinMacosArm64
./gradlew macosArm64Test
```

Compilation is a precondition for the gate, not the gate itself. The
gate is the ported tests passing against the same fixtures the Rust
tests use.

## Commit Messages

- No AI branding, no "Co-Authored-By" lines
- Clear, descriptive messages about what changed and why
- No emoji unless requested
