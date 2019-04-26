package gg.rsmod.plugins.service.restapi.routes

import gg.rsmod.plugins.service.restapi.data.RouteOptions
import gg.rsmod.util.ServerProperties
import spark.Request
import spark.Response
import spark.Spark.*

class Route(uri: String, callback: (res: Request, req: Response)->String, option: RouteOptions, routeProperties: ServerProperties) : RouteContainer() {

    private val uri: String = uri
    private val callback: (res: Request, req: Response) -> String = callback
    private val option: RouteOptions = option
    private val routeProperties: ServerProperties = routeProperties

    init {
        bind()
    }

    override fun bind() {
        when (option) {
            RouteOptions.GET -> {
                get(uri) { res, req ->
                    enableCORS(routeProperties)
                    callback.invoke(res, req)
                }
                log("GET")
            }
            RouteOptions.POST -> {
                post(uri) { res, req ->
                    enableCORS(routeProperties)
                    callback.invoke(res, req)
                }
                log("POST")
            }
            RouteOptions.PUT -> {
                put(uri) { res, req ->
                    enableCORS(routeProperties)
                    callback.invoke(res, req)
                }
                log("PUT")
            }
            RouteOptions.DELETE -> {
                delete(uri) { res, req ->
                    enableCORS(routeProperties)
                    callback.invoke(res, req)
                }
                log("DELETE")
            }
        }
    }


    private fun enableCORS(routeProperties: ServerProperties) {
        CorsRoute(routeProperties.getOrDefault("origin", "*"), routeProperties.getOrDefault("methods", "GET, POST, PUT, DELETE"), routeProperties.getOrDefault("headers", "Content-Type"))
    }

    private fun log(type: String) = logger.info { "Initiated API $type route listener for $uri..." }
}