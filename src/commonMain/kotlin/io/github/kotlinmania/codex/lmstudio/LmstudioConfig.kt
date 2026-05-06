// port-lint: ignore (project-local shim for codex_core::config::Config)
package io.github.kotlinmania.codex.lmstudio

/**
 * Minimal configuration surface required by [LMStudioClient.tryFromProvider]
 * and [ensureOssReady]. Stands in for the slice of `codex_core::config::Config`
 * the upstream `codex-rs/lmstudio` crate uses, namely the OSS provider's
 * base URL plus the chosen model identifier.
 *
 * Downstream callers (e.g. codex-kotlin) implement this against their own
 * `Config.modelProviders[LMSTUDIO_OSS_PROVIDER_ID]` lookup.
 */
public interface LmstudioConfig {
    public val baseUrl: String?
    public val model: String
}
