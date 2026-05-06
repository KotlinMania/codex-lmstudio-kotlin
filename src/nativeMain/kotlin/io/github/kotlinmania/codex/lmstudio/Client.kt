// port-lint: source src/client.rs
@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package io.github.kotlinmania.codex.lmstudio

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.OsFamily
import kotlin.native.Platform
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import platform.posix.F_OK
import platform.posix.X_OK
import platform.posix.access
import platform.posix.fprintf
import platform.posix.getenv
import platform.posix.stderr
import platform.posix.system

private const val LMSTUDIO_CONNECTION_ERROR: String =
    "LM Studio is not responding. Install from https://lmstudio.ai/download and run 'lms server start'."

public class LMStudioClient internal constructor(
    private val client: HttpClient,
    internal val baseUrl: String,
) {
    public companion object {
        public suspend fun tryFromProvider(config: LmstudioConfig): LMStudioClient {
            val baseUrl = config.baseUrl
                ?: throw IOException("oss provider must have a base_url")

            val client = HttpClient(Curl) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 5_000
                }
            }

            val lmstudioClient = LMStudioClient(
                client = client,
                baseUrl = baseUrl,
            )
            lmstudioClient.checkServer()

            return lmstudioClient
        }

        /** Low-level constructor given a raw host root, e.g. "http://localhost:1234". */
        internal fun fromHostRoot(hostRoot: String): LMStudioClient {
            val client = HttpClient(Curl) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 5_000
                }
            }
            return LMStudioClient(
                client = client,
                baseUrl = hostRoot,
            )
        }

        // Find lms, checking fallback paths if not in PATH
        internal fun findLms(): String = findLmsWithHomeDir(null)

        internal fun findLmsWithHomeDir(homeDir: String?): String {
            // First try 'lms' in PATH
            if (whichLms() != null) {
                return "lms"
            }

            // Platform-specific fallback paths
            val home: String = when (homeDir) {
                null -> when (Platform.osFamily) {
                    OsFamily.WINDOWS -> getenv("USERPROFILE")?.toKString().orEmpty()
                    else -> getenv("HOME")?.toKString().orEmpty()
                }
                else -> homeDir
            }

            val fallbackPath: String = when (Platform.osFamily) {
                OsFamily.WINDOWS -> "$home/.lmstudio/bin/lms.exe"
                else -> "$home/.lmstudio/bin/lms"
            }

            return if (access(fallbackPath, F_OK) == 0) {
                fallbackPath
            } else {
                throw IOException(
                    "LM Studio not found. Please install LM Studio from https://lmstudio.ai/",
                )
            }
        }

        private fun whichLms(): String? {
            val pathEnv = getenv("PATH")?.toKString() ?: return null
            val separator = if (Platform.osFamily == OsFamily.WINDOWS) ';' else ':'
            val binaryName = if (Platform.osFamily == OsFamily.WINDOWS) "lms.exe" else "lms"
            for (dir in pathEnv.split(separator)) {
                if (dir.isEmpty()) continue
                val candidate = "$dir/$binaryName"
                if (access(candidate, X_OK) == 0) {
                    return candidate
                }
            }
            return null
        }
    }

    internal suspend fun checkServer() {
        val url = "${baseUrl.trimEnd('/')}/models"
        val response: HttpResponse? = runCatching { client.get(url) }.getOrNull()

        if (response != null) {
            if (response.status.isSuccess()) {
                return
            } else {
                throw IOException(
                    "Server returned error: ${response.status.value} $LMSTUDIO_CONNECTION_ERROR",
                )
            }
        } else {
            throw IOException(LMSTUDIO_CONNECTION_ERROR)
        }
    }

    // Load a model by sending an empty request with max_tokens 1
    public suspend fun loadModel(model: String) {
        val url = "${baseUrl.trimEnd('/')}/responses"

        val requestBody = buildJsonObject {
            put("model", model)
            put("input", "")
            put("max_output_tokens", 1)
        }

        val response: HttpResponse = try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
        } catch (e: Throwable) {
            throw IOException("Request failed: ${e.message ?: e::class.simpleName}")
        }

        if (response.status.isSuccess()) {
            // tracing::info!("Successfully loaded model '{model}'") — drop without a logger.
        } else {
            throw IOException("Failed to load model: ${response.status.value}")
        }
    }

    // Return the list of models available on the LM Studio server.
    public suspend fun fetchModels(): List<String> {
        val url = "${baseUrl.trimEnd('/')}/models"
        val response: HttpResponse = try {
            client.get(url)
        } catch (e: Throwable) {
            throw IOException("Request failed: ${e.message ?: e::class.simpleName}")
        }

        if (response.status.isSuccess()) {
            val text = response.bodyAsText()
            val element = try {
                Json.parseToJsonElement(text)
            } catch (e: Throwable) {
                throw IOException("JSON parse error: ${e.message ?: e::class.simpleName}")
            }
            val obj = element as? JsonObject
                ?: throw IOException("No 'data' array in response")
            val data = obj["data"] as? JsonArray
                ?: throw IOException("No 'data' array in response")
            return data.mapNotNull { model ->
                val modelObj = model as? JsonObject ?: return@mapNotNull null
                (modelObj["id"] as? JsonPrimitive)?.contentOrNull
            }
        } else {
            throw IOException("Failed to fetch models: ${response.status.value}")
        }
    }

    public fun downloadModel(model: String) {
        val lms = findLms()
        printToStderr("Downloading model: $model")

        val sanitizedModel = sanitizeShellArg(model)
        val sanitizedLms = sanitizeShellArg(lms)
        val command = if (Platform.osFamily == OsFamily.WINDOWS) {
            "$sanitizedLms get --yes $sanitizedModel 2> NUL"
        } else {
            "$sanitizedLms get --yes $sanitizedModel 2> /dev/null"
        }
        val raw = system(command)
        if (raw == -1) {
            throw IOException("Failed to execute '$lms get --yes $model'")
        }
        val exitCode = when (Platform.osFamily) {
            OsFamily.WINDOWS -> raw
            else -> (raw shr 8) and 0xff
        }
        if (exitCode != 0) {
            throw IOException("Model download failed with exit code: $exitCode")
        }
        // tracing::info!("Successfully downloaded model '{model}'") — drop without a logger.
    }
}

private fun printToStderr(line: String) {
    fprintf(stderr, "%s\n", line)
}

private fun sanitizeShellArg(arg: String): String {
    require(arg.all { ch ->
        ch.isLetterOrDigit() || ch in "._-/:\\"
    }) { "Refusing to spawn lms with arg containing shell metacharacters: $arg" }
    return arg
}
