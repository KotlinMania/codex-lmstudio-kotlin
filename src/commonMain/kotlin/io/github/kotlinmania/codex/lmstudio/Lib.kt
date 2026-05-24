// port-lint: source lib.rs
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
    } catch (e: Throwable) {
        // Not fatal; higher layers may still proceed and surface errors later.
        printToStderr("Failed to query local models from LM Studio: ${e.message ?: e::class.simpleName}.")
    }

    // Load the model in the background.
    GlobalScope.launch(Dispatchers.Default) {
        try {
            lmstudioClient.loadModel(model)
        } catch (e: Throwable) {
            printToStderr("Failed to load model $model: ${e.message ?: e::class.simpleName}")
        }
    }
}
