package gg.rsmod.plugins.service.sql

import gg.rsmod.game.Server
import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World
import gg.rsmod.game.model.attr.AttributeKey
import gg.rsmod.game.model.container.ItemContainer
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.model.interf.DisplayMode
import gg.rsmod.game.model.item.Item
import gg.rsmod.game.model.item.ItemAttribute
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.game.model.timer.TimerKey
import gg.rsmod.game.service.Service
import gg.rsmod.game.service.serializer.PlayerLoadResult
import gg.rsmod.game.service.serializer.PlayerSerializerService
import gg.rsmod.game.service.serializer.json.JsonPlayerSerializer
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.plugins.service.sql.controllers.PlayerLoadController
import gg.rsmod.plugins.service.sql.controllers.PlayerSaveController
import gg.rsmod.plugins.service.sql.models.*
import gg.rsmod.util.ServerProperties
import mu.KLogging

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*

/**
 * A [SQLService] implementation that decodes and encodes player
 * data in SQL.
 *
 * @author KyeT <okaydots@gmail.com>
 */

class SQLService : Service, PlayerSerializerService()
{

    override fun initSerializer(server: Server, world: World, serviceProperties: ServerProperties) {

        // Configuration
        val driver = serviceProperties.getOrDefault("driver", "com.mysql.jdbc.Driver")
        val port   = serviceProperties.getOrDefault("port", 3306)
        val host   = serviceProperties.getOrDefault("host", "127.0.0.1")
        val user   = serviceProperties.getOrDefault("user", "root")
        val pswd   = serviceProperties.getOrDefault("password", "")
        val dbname = serviceProperties.getOrDefault("dbname", "")

        var driverHost = ""

        // Host name for connecting based on configuration
        when(driver) {
            "com.mysql.jdbc.Driver" -> {
                driverHost = "mysql"
            }
            "org.postgresql.Driver" -> {
                driverHost = "postgresql"
            }
            "oracle.jdbc.OracleDriver" -> {
                driverHost = "jdbc:oracle:thin:@"
            }
            "com.microsoft.sqlserver.jdbc.SQLServerDriver" -> {
                driverHost = "sqlserver"
            }
        }

        // Start connection
        Database.connect("jdbc:$driverHost://$host:$port/$dbname", driver = driver, user = user, password = pswd)

        // Create tables if not yet created
        transaction {
            mutableListOf<Table>(
                PlayerModel,
                SkillModel,
                ItemContainerModel,
                ItemModel,
                AttributesModel,
                VarpModel,
                ItemAttributeModel,
                TimerModel
            ).forEach {
                table -> if(!table.exists()) {
                    SchemaUtils.create(table)
                    log("${table.tableName} did not exist. Created successfully...")
                }
            }
        }
    }

    override fun loadClientData(client: Client, request: LoginRequest): PlayerLoadResult {

        val loadResponse = PlayerLoadController().loadPlayer(client, client.world, request)

        when(loadResponse) {
            PlayerLoadResult.NEW_ACCOUNT -> {
                configureNewPlayer(client, request)
                client.uid = PlayerSaveController().createPlayer(client, client.world)
                PlayerSaveController().savePlayer(client, client.world)
            }
        }

        return loadResponse
    }

    override fun saveClientData(client: Client): Boolean {
        PlayerSaveController().savePlayer(client, client.world)
        return true
    }

    private fun log(message: String) {
        logger.info { message }
    }

    companion object: KLogging()
}