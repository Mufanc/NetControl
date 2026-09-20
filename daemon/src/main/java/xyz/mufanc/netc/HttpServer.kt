package xyz.mufanc.netc

import android.system.Os
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.request.queryString
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

@Serializable
private data class ErrorResponse(val error: String)

internal class HttpServer(
    directory: File,
    private val mApps: Apps,
    private val mFirewall: Firewall,
) {
    private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mWebroot = File(directory, "webroot").canonicalFile
    private val mToken = UUID.randomUUID().toString().replace("-", "")
    private val mServer = mScope.embeddedServer(CIO, host = "127.0.0.1", port = 0) {
        install(ContentNegotiation) {
            json()
        }

        intercept(ApplicationCallPipeline.Call) {
            if (!accept(call)) return@intercept

            proceed()
        }

        routes()
    }.start(wait = false)
    private val mLocalPort = runBlocking { mServer.engine.resolvedConnectors().single().port }
    private val mOrigin = "http://127.0.0.1:$mLocalPort"
    private val mEndpoint = File(directory, "endpoint")

    init {
        check(mWebroot.isDirectory) { "Missing webroot" }

        mEndpoint.writeText("$mOrigin/$mToken/\n")
        Os.chmod(mEndpoint.path, 0x180)
    }

    private fun Application.routes() {
        routing {
            route("/$mToken") {
                get("/api/apps") {
                    call.handleApi {
                        respond(mApps.list())
                    }
                }

                get("/api/blacklist") {
                    call.handleApi {
                        respond(mFirewall.blockedAppIds())
                    }
                }

                put("/api/blacklist/{appid}") {
                    call.handleApi {
                        val appid = call.parameters["appid"]?.toInt() ?: throw IllegalArgumentException()

                        require(appid in 0..99999)
                        mFirewall.setBlocked(appid, true)

                        respond(HttpStatusCode.NoContent)
                    }
                }

                delete("/api/blacklist/{appid}") {
                    call.handleApi {
                        val appid = call.parameters["appid"]?.toInt() ?: throw IllegalArgumentException()

                        require(appid in 0..99999)
                        mFirewall.setBlocked(appid, false)

                        respond(HttpStatusCode.NoContent)
                    }
                }

                get("/api/apps/{appid}/icon") {
                    call.handleApi {
                        val appid = call.parameters["appid"]?.toInt() ?: throw IllegalArgumentException()

                        require(appid in 0..99999)

                        val icon = mApps.icon(appid)

                        if (icon == null) {
                            respond(HttpStatusCode.NotFound)
                        } else {
                            respondBytes(icon, ContentType.Image.PNG)
                        }
                    }
                }

                staticFiles("/", mWebroot, index = "management.html")
            }
        }
    }

    private fun accept(call: ApplicationCall): Boolean {
        val request = call.request

        return request.queryString().isEmpty() &&
            request.host() == "127.0.0.1" && request.port() == mLocalPort &&
            request.headers[HttpHeaders.Origin]?.let { it == mOrigin } != false
    }

    private suspend fun ApplicationCall.handleApi(block: suspend ApplicationCall.() -> Unit) {
        try {
            block()
        } catch (err: IllegalArgumentException) {
            respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request"))
        } catch (err: Exception) {
            System.err.println("API failed: ${err.javaClass.simpleName}: ${err.message}")
            respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Operation failed; check daemon log"),
            )
        }
    }
}
