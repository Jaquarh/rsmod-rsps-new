package gg.rsmod.plugins.service.restapi.controllers

import com.google.gson.JsonArray
import gg.rsmod.game.model.World
import gg.rsmod.plugins.service.restapi.auth.Auth
import spark.Request
import spark.Response

abstract class Controller(req: Request, resp: Response, auth: Boolean) {
    protected var authState = true

    init {
        if(auth) {
            val xAuth = req.headers("X-AUTH") ?: ""

            if(xAuth.isNullOrEmpty()) {
                // X-AUTH was not present in request headers
                authState = false
            }

            if(!Auth.auth(xAuth)) {
                // X-AUTH was not valid or has expired
                authState = false
            }
        }
    }

    abstract fun init(world: World): JsonArray

    fun deploy(): String {
        return Auth.build()
    }
}