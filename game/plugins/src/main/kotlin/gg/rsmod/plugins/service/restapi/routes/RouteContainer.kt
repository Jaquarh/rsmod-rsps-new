package gg.rsmod.plugins.service.restapi.routes

import mu.KLogging

abstract class RouteContainer {
    companion object: KLogging()

    abstract fun bind()
}