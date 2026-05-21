// port-lint: source src/client.rs (tests)
package io.github.kotlinmania.codex.lmstudio

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val CODEX_SANDBOX_NETWORK_DISABLED_ENV_VAR: String =
    "CODEX_SANDBOX_NETWORK_DISABLED"

private fun networkDisabled(): Boolean =
    envVar(CODEX_SANDBOX_NETWORK_DISABLED_ENV_VAR) != null

class ClientTest {
    @Test
    fun testFetchModelsHappyPath() = runTest {
        if (networkDisabled()) return@runTest

        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/models", request.url.encodedPath)
            respond(
                content = """{"data":[{"id":"openai/gpt-oss-20b"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = LMStudioClient.fromHostRootForTesting(HttpClient(engine), "http://example.test")
        val models = client.fetchModels()
        assertTrue(models.contains("openai/gpt-oss-20b"))
    }

    @Test
    fun testFetchModelsNoDataArray() = runTest {
        if (networkDisabled()) return@runTest

        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = LMStudioClient.fromHostRootForTesting(HttpClient(engine), "http://example.test")
        val err = assertFailsWith<IOException> { client.fetchModels() }
        assertTrue(err.message?.contains("No 'data' array in response") == true)
    }

    @Test
    fun testFetchModelsServerError() = runTest {
        if (networkDisabled()) return@runTest

        val engine = MockEngine {
            respondError(HttpStatusCode.InternalServerError)
        }
        val client = LMStudioClient.fromHostRootForTesting(HttpClient(engine), "http://example.test")
        val err = assertFailsWith<IOException> { client.fetchModels() }
        assertTrue(err.message?.contains("Failed to fetch models: 500") == true)
    }

    @Test
    fun testCheckServerHappyPath() = runTest {
        if (networkDisabled()) return@runTest

        val engine = MockEngine { respondOk() }
        val client = LMStudioClient.fromHostRootForTesting(HttpClient(engine), "http://example.test")
        client.checkServer()
    }

    @Test
    fun testCheckServerError() = runTest {
        if (networkDisabled()) return@runTest

        val engine = MockEngine {
            respondError(HttpStatusCode.NotFound)
        }
        val client = LMStudioClient.fromHostRootForTesting(HttpClient(engine), "http://example.test")
        val err = assertFailsWith<IOException> { client.checkServer() }
        assertTrue(err.message?.contains("Server returned error: 404") == true)
    }

    @Test
    fun testFindLms() {
        try {
            LMStudioClient.findLms()
        } catch (e: IOException) {
            assertTrue(e.message?.contains("LM Studio not found") == true)
        }
    }

    @Test
    fun testFindLmsWithMockHome() {
        try {
            LMStudioClient.findLmsWithHomeDir("/test/home")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("LM Studio not found") == true)
        }
    }

    @Test
    fun testFromHostRoot() {
        val a = LMStudioClient.fromHostRootForTesting(
            HttpClient(MockEngine { respondOk() }),
            "http://localhost:1234",
        )
        assertEquals("http://localhost:1234", a.baseUrl)

        val b = LMStudioClient.fromHostRootForTesting(
            HttpClient(MockEngine { respondOk() }),
            "https://example.com:8080/api",
        )
        assertEquals("https://example.com:8080/api", b.baseUrl)
    }
}
