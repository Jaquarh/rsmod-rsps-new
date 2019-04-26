package gg.rsmod.plugins.service.restapi.routes

import gg.rsmod.plugins.service.restapi.data.*
import gg.rsmod.util.ServerProperties
import spark.Request
import spark.Response

class Router {
    companion object {
        var routes:MutableList<Route> = mutableListOf()

        fun get(uri: String, routeProperties: ServerProperties, callback: (res: Request, req: Response)->String) {
            routes.add(Route(uri, callback, RouteOptions.GET, routeProperties))
        }

        fun post(uri: String, routeProperties: ServerProperties, callback: (res: Request, req: Response)->String) {
            routes.add(Route(uri, callback, RouteOptions.POST, routeProperties))
        }

        fun put(uri: String, routeProperties: ServerProperties, callback: (res: Request, req: Response)->String) {
            routes.add(Route(uri, callback, RouteOptions.PUT, routeProperties))
        }

        fun delete(uri: String, routeProperties: ServerProperties, callback: (res: Request, req: Response)->String) {
            routes.add(Route(uri, callback, RouteOptions.DELETE, routeProperties))
        }

        fun validateAuth(req: Request, scopes: MutableList<AuthScopes>): AuthResponseData {
            val secret: String = req.headers("RSMOD-SECRET") ?: return AuthResponseData.MISSING_CREDENTIALS
            val clientId: String = req.headers("RSMOD-CLIENT-ID") ?: return AuthResponseData.MISSING_CREDENTIALS

            return AuthRoute.validate(AuthData(Integer.parseInt(clientId), secret, null), scopes)
        }

        fun generateAuth(scope: AuthScopes): AuthCreateData {
            return AuthRoute.create(scope)
        }

        fun bindRoutes() {
            routes.forEach {
                it.bind()
            }
        }
    }
}