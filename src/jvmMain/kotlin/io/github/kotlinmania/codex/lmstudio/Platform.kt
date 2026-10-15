package io.github.kotlinmania.codex.lmstudio

import kotlinx.io.IOException
import java.io.File

internal actual fun platformFamily(): LmstudioPlatformFamily =
    if (
        System
            .getProperty("os.name")
            .orEmpty()
            .lowercase()
            .contains("win")
    ) {
        LmstudioPlatformFamily.WINDOWS
    } else {
        LmstudioPlatformFamily.OTHER
    }

internal actual fun envVar(name: String): String? = System.getenv(name)

internal actual fun pathExists(path: String): Boolean = File(path).exists()

internal actual fun isExecutable(path: String): Boolean =
    File(path).let { it.isFile && it.canExecute() }

internal actual fun runLmsGet(lms: String, model: String): Int =
    try {
        ProcessBuilder(lms, "get", "--yes", model)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor()
    } catch (e: Throwable) {
        throw IOException("Failed to execute '$lms get --yes $model': ${e.message}")
    }

internal actual fun printToStderr(line: String) {
    System.err.println(line)
}
