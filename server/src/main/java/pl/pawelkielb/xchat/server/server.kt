package pl.pawelkielb.xchat.server

import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import pl.pawelkielb.xchat.server.dagger.AppComponent
import pl.pawelkielb.xchat.server.dagger.DaggerAppComponent
import pl.pawelkielb.xchat.server.routes.*
import java.io.File
import org.eclipse.jetty.server.Server as Jetty


class Server(
    port: Int = defaultPort,
    rootServlet: RootServlet,
    v1Resource: V1Resource,
    channelsResource: ChannelsResource,
    messagesResource: MessagesResource,
    filesResource: FilesResource,
    fileResource: FileResource
) {
    companion object {
        const val defaultPort = 8080
    }

    private val jetty = Jetty(port).also {
        it.handler = ServletContextHandler().apply {
            contextPath = "/"

            addServlet(ServletHolder(rootServlet), "/")

            // add jersey
            run {
                val resourceConfig = ResourceConfig().apply {
                    register(v1Resource)
                    register(channelsResource)
                    register(messagesResource)
                    register(filesResource)
                    register(fileResource)
                    register(MultiPartFeature::class.java)
                    register(IllegalArgumentExceptionMapper::class.java)
                }
                addServlet(ServletHolder(ServletContainer(resourceConfig)), "/v1/*")
            }
        }
    }

    fun start() {
        jetty.start()
    }

    fun stop() {
        jetty.stop()
    }
}

@JvmOverloads
fun Server(port: Int = Server.defaultPort, databaseRoot: File): Server {
    val component: AppComponent = DaggerAppComponent.factory().create(databaseRoot)
    val applicationContext = component.applicationContext()
    with(applicationContext) {
        return Server(port)
    }
}
