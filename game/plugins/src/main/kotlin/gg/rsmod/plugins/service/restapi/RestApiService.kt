package gg.rsmod.plugins.service.restapi

import gg.rsmod.game.Server
import gg.rsmod.game.model.World
import gg.rsmod.game.service.Service
import gg.rsmod.plugins.service.restapi.controllers.Controller
import gg.rsmod.plugins.service.restapi.routes.CorsRoute
import gg.rsmod.plugins.service.restapi.routes.Router
import gg.rsmod.util.ServerProperties
import mu.KLogger
import spark.Spark.stop

/**
 * A [RestApiService] to expose the Server cross-platform. Uses oAuth 2.0
 * to allow for sensitive routes.
 *
 * @author KyeT <okaydots@gmail.com>
 * @see "https://github.com/Kye-T/rsmod-restapi/README.md"
 */

class RestApiService : Service {
    override fun init(server: Server, world: World, serviceProperties: ServerProperties) {
        CorsRoute(serviceProperties.getOrDefault("origin", "*"), serviceProperties.getOrDefault("methods", "GET, POST, PUT, DELETE"), serviceProperties.getOrDefault("headers", "Content-Type"))

        (serviceProperties.get<ArrayList<Any>>("routes") ?: ArrayList()).forEach { r ->
            val values = r as LinkedHashMap<*, *>
            val className = values["class"] as String
            val clazz = Class.forName(className).asSubclass(Controller::class.java)!!
            val route = clazz.newInstance()

            val properties = hashMapOf<String, Any>()
            values.filterKeys { it != "class" }.forEach { key, value ->
                properties[key as String] = value
            }

            route.init(world, ServerProperties().loadMap(properties))
        }
    }

    override fun postLoad(server: Server, world: World) {
    }

    override fun bindNet(server: Server, world: World) {

    }

    override fun terminate(server: Server, world: World) {
        stop()
    }
}