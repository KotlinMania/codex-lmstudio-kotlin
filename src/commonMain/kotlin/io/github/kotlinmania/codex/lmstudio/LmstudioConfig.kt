// port-lint: ignore (project-local config surface)
package io.github.kotlinmania.codex.lmstudio

/**
 * Minimal configuration surface required by [LMStudioClient.tryFromProvider]
 * and [ensureOssReady]: the OSS provider's base URL plus the chosen model
 * identifier.
 *
 * Downstream callers implement this against their own model-providers map,
 * looking up the LM Studio OSS provider entry.
 */
public interface LmstudioConfig {
    public val baseUrl: String?
    public val model: String
}
