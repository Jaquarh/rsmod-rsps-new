package gg.rsmod.plugins.service.restapi.controllers

import gg.rsmod.game.model.World
import gg.rsmod.util.ServerProperties
import mu.KLogging

abstract class Controller {
    abstract fun init(world: World, routeProperties: ServerProperties)

    companion object: KLogging()
}