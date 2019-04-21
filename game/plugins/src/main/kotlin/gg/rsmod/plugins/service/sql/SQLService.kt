package gg.rsmod.plugins.service.sql

import gg.rsmod.game.Server
import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.model.interf.DisplayMode
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.game.service.Service
import gg.rsmod.game.service.serializer.PlayerLoadResult
import gg.rsmod.game.service.serializer.PlayerSerializerService
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.plugins.service.sql.model.ItemContainerModel
import gg.rsmod.plugins.service.sql.model.ItemModel
import gg.rsmod.plugins.service.sql.model.PlayerModel
import gg.rsmod.plugins.service.sql.model.SkillModel
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
    private var mysql = false

    override fun initSerializer(server: Server, world: World, serviceProperties: ServerProperties) {
        // Ensures the SQL is enabled / disabled
        mysql = serviceProperties.getOrDefault("enabled", false)

        if(!mysql) {
            return initSerializer(server, world, serviceProperties)
        }

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

        // TODO("Create rest of SQL models")

        // Create tables if not yet created
        transaction {
            mutableListOf<Table>(PlayerModel, SkillModel, ItemContainerModel, ItemModel).forEach {
                table -> if(!table.exists()) {
                    SchemaUtils.create(table)
                    log("${table.tableName} did not exist. Created successfully...")
                }
            }
        }
    }

    private fun log(message: String) {
        logger.info { message }
    }

    override fun loadClientData(client: Client, request: LoginRequest): PlayerLoadResult {
        if(!mysql) {
            return loadClientData(client, request)
        }

        var query: Query? = null

        transaction {
            query = (PlayerModel innerJoin SkillModel).select {
                PlayerModel.username eq client.loginUsername
            }
        }

        if(!query!!.empty()) {
            val world = client.world

            // Tests login validation
            if (!request.reconnecting) {
                /*
                 * If the [request] is not a [LoginRequest.reconnecting] request, we have to
                 * verify the password is correct.
                 */
                if (!BCrypt.checkpw(request.password, query!!.first()[PlayerModel.hash])) {
                    return PlayerLoadResult.INVALID_CREDENTIALS
                }
            } else {
                /*
                 * If the [request] is a [LoginRequest.reconnecting] request, we
                 * verify that the login xteas match from our previous session.
                 */
                val previousXteas = IntArray(4)

                previousXteas[0] = query!!.first()[PlayerModel.xteaKeyOne]
                previousXteas[1] = query!!.first()[PlayerModel.xteaKeyTwo]
                previousXteas[2] = query!!.first()[PlayerModel.xteaKeyThree]
                previousXteas[3] = query!!.first()[PlayerModel.xteaKeyFour]

                if (!Arrays.equals(previousXteas, request.xteaKeys)) {
                    return PlayerLoadResult.INVALID_RECONNECTION
                }
            }

            // Load player details

            client.loginUsername = query!!.first()[PlayerModel.username]
            client.uid = PlayerUID(query!!.first()[PlayerModel.id])
            client.username = query!!.first()[PlayerModel.displayName]
            client.passwordHash = query!!.first()[PlayerModel.hash]
            client.tile = Tile(query!!.first()[PlayerModel.x], query!!.first()[PlayerModel.z], query!!.first()[PlayerModel.height])
            client.privilege = world.privileges.get(query!!.first()[PlayerModel.privilege]) ?: Privilege.DEFAULT
            client.runEnergy = query!!.first()[PlayerModel.runEnergy].toDouble()
            client.interfaces.displayMode = DisplayMode.values.firstOrNull { it.id == query!!.first()[PlayerModel.displayMode] } ?: DisplayMode.FIXED

            // Load player skills
            query!!.forEach {
                client.getSkills().setXp(it[SkillModel.skill], it[SkillModel.xp].toDouble())
                client.getSkills().setCurrentLevel(it[SkillModel.skill], it[SkillModel.lvl])
            }

            // Load player items
            var containerInventoryQuery: Query? = null
            var containerBankQuery: Query? = null

            transaction {
                containerInventoryQuery = (ItemContainerModel innerJoin ItemModel).select {
                    ItemContainerModel.playerId eq query!!.first()[PlayerModel.id]
                    ItemContainerModel.name eq "inventory"
                }

                containerBankQuery = (ItemContainerModel innerJoin ItemModel).select {
                    ItemContainerModel.playerId eq query!!.first()[PlayerModel.id]
                    ItemContainerModel.name eq "bank"
                }
            }

            val itemContainers = mutableListOf<Query?>(containerInventoryQuery, containerBankQuery)

            itemContainers.forEach {
                // TODO("Make containerKeys plugin public")
            }

            // TODO("Load rest of SQL data")

            return PlayerLoadResult.LOAD_ACCOUNT
        } else {
            configureNewPlayer(client, request)
            client.uid = createPlayer(client, client.world)
            saveClientData(client)
            return PlayerLoadResult.NEW_ACCOUNT
        }
    }

    override fun saveClientData(client: Client): Boolean {
        if(!mysql) {
            return saveClientData(client)
        }

        transaction {

            // Update  player model
            PlayerModel.update ({
                PlayerModel.id eq client.uuid.toInt()
            }) {
                it[PlayerModel.username] = client.loginUsername
                it[PlayerModel.hash] = client.passwordHash
                it[PlayerModel.xteaKeyOne] = client.currentXteaKeys[0]
                it[PlayerModel.xteaKeyTwo] = client.currentXteaKeys[1]
                it[PlayerModel.xteaKeyThree] = client.currentXteaKeys[2]
                it[PlayerModel.xteaKeyFour] = client.currentXteaKeys[3]
                it[PlayerModel.displayName] = client.username
                it[PlayerModel.x] = client.tile.x
                it[PlayerModel.height] = client.tile.height
                it[PlayerModel.z] = client.tile.z
                it[PlayerModel.privilege] = client.privilege.id
                it[PlayerModel.runEnergy] = client.runEnergy.toFloat()
                it[PlayerModel.displayMode] = client.interfaces.displayMode.id
            }

            // Update player skills
            SkillModel.select {
                SkillModel.playerId eq client.uuid.toInt()
            }.forEach { skill ->
                SkillModel.update({
                    SkillModel.playerId eq client.uuid.toInt()
                    SkillModel.skill eq skill[SkillModel.skill]
                }) {
                    it[SkillModel.lvl] = client.getSkills().getCurrentLevel(skill[SkillModel.skill])
                    it[SkillModel.xp] = client.getSkills().getCurrentXp(skill[SkillModel.skill]).toFloat()
                }
            }

            // TODO("Update rest of client data")

        }

        return saveClientData(client)
    }

    private fun createPlayer(client: Client, world: World): PlayerUID {
        return PlayerUID(PlayerModel.insert {
            it[username] = client.loginUsername
            it[displayName] = client.loginUsername
            it[hash] = client.passwordHash
            it[x] = world.gameContext.home.x
            it[height] = world.gameContext.home.height
            it[z] = world.gameContext.home.z
        } get PlayerModel.id)
    }

    override fun postLoad(server: Server, world: World) {
    }

    override fun bindNet(server: Server, world: World) {
    }

    override fun terminate(server: Server, world: World) {

    }

    companion object: KLogging()
}