package gg.rsmod.plugins.service.restapi.controllers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.rsmod.game.model.World
import gg.rsmod.plugins.service.restapi.routes.*
import gg.rsmod.util.ServerProperties

class PlayersController(): Controller() {
    override fun init(world: World, routeProperties: ServerProperties) {
        Router.get(uri = "/players", routeProperties = routeProperties) { req, res ->
            val jsonBuilder: JsonObject = JsonObject()
            val playerArray: JsonArray = JsonArray()

            /*
             * Add Player Count
             */
            jsonBuilder.addProperty("count", world.players.count())

            /*
             * Add Player Data
             */
            world.players.forEach { player ->
                val playerObj = JsonObject()
                playerObj.addProperty("username", player.username)
                playerArray.add(playerObj)
            }

            jsonBuilder.add("players", playerArray)

            Gson().toJson(jsonBuilder)
        }
    }
}