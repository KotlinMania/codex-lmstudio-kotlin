// port-lint: source src/lib.rs
package io.github.kotlinmania.codex.lmstudio

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Default OSS model to use when `--oss` is passed without an explicit `-m`. */
public const val DEFAULT_OSS_MODEL: String = "openai/gpt-oss-20b"

/**
 * Prepare the local OSS environment when `--oss` is selected.
 *
 * - Ensures a local LM Studio server is reachable.
 * - Checks if the model exists locally and downloads it if missing.
 */
@OptIn(DelicateCoroutinesApi::class)
public suspend fun ensureOssReady(config: LmstudioConfig) {
    val model: String = config.model

    // Verify local LM Studio is reachable.
    val lmstudioClient = LMStudioClient.tryFromProvider(config)

    try {
        val models = lmstudioClient.fetchModels()
        if (models.none { it == model }) {
            lmstudioClient.downloadModel(model)
        }
    } catch (_: Throwable) {
        // Not fatal; higher layers may still proceed and surface errors later.
        // Upstream calls `tracing::warn!` here; with no portable logging
        // facade the closest faithful behavior is to discard the error.
    }

    // Load the model in the background.
    GlobalScope.launch(Dispatchers.Default) {
        try {
            lmstudioClient.loadModel(model)
        } catch (_: Throwable) {
            // Upstream `tracing::warn!`; discard without a logger.
        }
    }
}
