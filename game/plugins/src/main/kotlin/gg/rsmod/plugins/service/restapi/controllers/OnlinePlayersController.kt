package gg.rsmod.plugins.service.restapi.controllers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.rsmod.game.model.World
import spark.Request
import spark.Response

class OnlinePlayersController(req: Request, resp: Response, auth: Boolean) : Controller(req, resp, auth) {

    private val auth = auth

    override fun init(world: World): JsonArray {

        val arr = JsonArray()
        val obj = JsonObject()

        obj.addProperty("count", world.players.count())
        arr.add(obj)

        val players = JsonArray()

        world.players.forEach { player ->
            val pObj = JsonObject()
            pObj.addProperty("username", player.username)
            pObj.addProperty("privilege", player.privilege.id)
            pObj.addProperty("gameMode", player.gameMode)
            players.add(pObj)
        }

        arr.add(players)

        return arr
    }
}