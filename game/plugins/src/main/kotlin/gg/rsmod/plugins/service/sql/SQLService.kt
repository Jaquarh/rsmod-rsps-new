package gg.rsmod.plugins.service.sql

import gg.rsmod.game.Server
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.service.Service
import gg.rsmod.game.service.serializer.PlayerLoadResult
import gg.rsmod.game.service.serializer.PlayerSerializerService
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.plugins.service.sql.model.Model
import gg.rsmod.util.ServerProperties
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class SQLService : Service, PlayerSerializerService()
{
    private var mysql = false
    private var conn = null

    override fun initSerializer(server: Server, world: World, serviceProperties: ServerProperties) {
        // Ensures the SQL is enabled / disabled
        mysql = serviceProperties.getOrDefault("enabled", false)

        if(!mysql) {
            // Ensures that the dir exists
            return initSerializer(server, world, serviceProperties)
        }

        // Configuration
        val driver = serviceProperties.getOrDefault("driver", "mysql")
        val port   = serviceProperties.getOrDefault("port", 3306)
        val host   = serviceProperties.getOrDefault("host", "127.0.0.1")
        val user   = serviceProperties.getOrDefault("user", "root")
        val pswd  = serviceProperties.getOrDefault("password", "")

        Model.startConnection(user, pswd, host, port, driver)
    }

    override fun loadClientData(client: Client, request: LoginRequest): PlayerLoadResult {
        if(!mysql) {
            return loadClientData(client, request)
        }

        val result = Model.executeMySQLQuery("SELECT * FROM test")
        while(result!!.next())
            println(result.getInt("id"))

        return loadClientData(client, request)
        //return PlayerLoadResult.LOAD_ACCOUNT
    }

    override fun saveClientData(client: Client): Boolean {
        if(!mysql) {
            return saveClientData(client)
        }

        return saveClientData(client)
        //return true
    }

    override fun postLoad(server: Server, world: World) {
    }

    override fun bindNet(server: Server, world: World) {
    }

    override fun terminate(server: Server, world: World) {
        Model.destroyConnection()
    }
}