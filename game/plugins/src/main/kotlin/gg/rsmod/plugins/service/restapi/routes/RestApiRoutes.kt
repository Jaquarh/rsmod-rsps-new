package gg.rsmod.plugins.service.restapi.routes

import com.google.gson.Gson
import gg.rsmod.game.model.World
import gg.rsmod.plugins.service.restapi.controllers.CommandsController
import gg.rsmod.plugins.service.restapi.controllers.OnlinePlayersController
import spark.Spark.*

class RestApiRoutes {
    fun init(world: World, auth: Boolean) {

        get("/api/players") {
            req, res -> Gson().toJson(OnlinePlayersController(req, res, false).init(world))
        }

        /*get("/item/:id/:amount/:player") {
            req, res -> Gson().toJson(CommandsController(req, res, auth).init(world))
        }*/
    }
}