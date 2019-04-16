package gg.rsmod.plugins.service.sql

import gg.rsmod.game.Server
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.service.Service
import gg.rsmod.game.service.serializer.PlayerLoadResult
import gg.rsmod.game.service.serializer.PlayerSerializerService
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.util.ServerProperties

class SQLService : Service, PlayerSerializerService()
{
    private var mysql = false

    override fun initSerializer(server: Server, world: World, serviceProperties: ServerProperties) {
        mysql = serviceProperties.getOrDefault("enabled", false)
    }

    override fun loadClientData(client: Client, request: LoginRequest): PlayerLoadResult {
        return PlayerLoadResult.LOAD_ACCOUNT
    }

    override fun saveClientData(client: Client): Boolean {
        return true
    }

    override fun postLoad(server: Server, world: World) {
    }

    override fun bindNet(server: Server, world: World) {
    }

    override fun terminate(server: Server, world: World) {
    }
}