package chpapantonis

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import chpapantonis.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

const val API_VERSION = "/v1"
fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureRouting()
}
