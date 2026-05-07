// port-lint: ignore (platform support for src/client.rs)
@file:OptIn(ExperimentalForeignApi::class)

package io.github.kotlinmania.codex.lmstudio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.F_OK
import platform.posix.X_OK
import platform.posix.access
import platform.posix.getenv

internal actual fun platformFamily(): LmstudioPlatformFamily = LmstudioPlatformFamily.OTHER

internal actual fun envVar(name: String): String? = getenv(name)?.toKString()

internal actual fun pathExists(path: String): Boolean = access(path, F_OK) == 0

internal actual fun isExecutable(path: String): Boolean = access(path, X_OK) == 0

internal actual fun runLmsGet(lms: String, model: String): Int {
    throw IOException("Failed to execute '$lms get --yes $model'")
}

internal actual fun printToStderr(line: String) {
    println(line)
}
