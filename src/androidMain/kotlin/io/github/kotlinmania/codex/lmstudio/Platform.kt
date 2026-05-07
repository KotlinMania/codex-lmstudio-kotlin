// port-lint: ignore (platform support for src/client.rs)
package io.github.kotlinmania.codex.lmstudio

import kotlinx.io.IOException

internal actual fun platformFamily(): LmstudioPlatformFamily = LmstudioPlatformFamily.OTHER

internal actual fun envVar(name: String): String? = null

internal actual fun pathExists(path: String): Boolean = false

internal actual fun isExecutable(path: String): Boolean = false

internal actual fun runLmsGet(lms: String, model: String): Int {
    throw IOException("Failed to execute '$lms get --yes $model'")
}

internal actual fun printToStderr(line: String) {
    println(line)
}
