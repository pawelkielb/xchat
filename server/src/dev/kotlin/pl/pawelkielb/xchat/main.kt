package pl.pawelkielb.xchat

import kotlinx.coroutines.coroutineScope
import java.io.File
import java.net.URLClassLoader
import kotlin.concurrent.timer


suspend fun main(): Unit = coroutineScope {
    var server: Any? = null
    var serverClass: Class<*>? = null
    var lastWatch = "null"

    timer(period = 100) {
        val currentWatch = File("build/watch").readText()
        if (currentWatch != lastWatch) {
            lastWatch = currentWatch

            // restart server
            if (server != null) {
                println("Restarting the server...")
                // server.stop()
                serverClass!!.declaredMethods.find { it.name == "stop" }!!.invoke(server)
            }

            val classLoader = URLClassLoader(
                listOf(
                    "build/classes/kotlin/main",
                    "build/classes/java/main",
                    "../shared/build/classes/java/main"
                ).map { File(it).toURI().toURL() }.toTypedArray()
            )

            serverClass = classLoader.loadClass("pl.pawelkielb.xchat.server.Server")
            val serverKt = classLoader.loadClass("pl.pawelkielb.xchat.server.ServerKt")

            server = serverKt
                .getDeclaredMethod("Server", File::class.java)
                .invoke(null, File("../test_server"))

            // server.start()
            serverClass!!.declaredMethods.find { it.name == "start" }!!.invoke(server)
        }
    }
}
