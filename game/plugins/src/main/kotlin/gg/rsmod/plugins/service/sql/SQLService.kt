package gg.rsmod.plugins.service.sql

import com.google.gson.Gson
import gg.rsmod.game.Server
import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World
import gg.rsmod.game.model.attr.AttributeKey
import gg.rsmod.game.model.container.ItemContainer
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.model.interf.DisplayMode
import gg.rsmod.game.model.item.Item
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.game.service.Service
import gg.rsmod.game.service.serializer.PlayerLoadResult
import gg.rsmod.game.service.serializer.PlayerSerializerService
import gg.rsmod.game.service.serializer.json.JsonPlayerSerializer
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.plugins.service.sql.model.*
import gg.rsmod.util.ServerProperties
import mu.KLogging

import gg.rsmod.plugins.service.sql.model.*

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

        // TODO("Create timers model")
        // TODO("Create varps model")

        // Create tables if not yet created
        transaction {
            mutableListOf<Table>(
                PlayerModel,
                SkillModel,
                ItemContainerModel,
                ItemModel,
                AttributesModel,
                VarpModel
            ).forEach {
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

            itemContainers.forEach {q ->

                // Find key
                val key = world.plugins.containerKeys.firstOrNull { other -> other.name == q!!.first()[ItemContainerModel.name] } ?: return@forEach

                // Create container
                val container = if (client.containers.containsKey(key)) client.containers[key] else {
                    client.containers[key] = ItemContainer(client.world.definitions, key)
                    client.containers[key]
                }!!

                // Loop through items and add to container
                q!!.forEach {
                    container[it[ItemModel.index]] = Item(it[ItemModel.itemId], it[ItemModel.amount])
                }
            }

            // Load attributes

            var attrQuery: Query? = null

            transaction {
                attrQuery = AttributesModel.select{
                    AttributesModel.playerId eq client.uuid.toInt()
                }
            }

            attrQuery!!.forEach {
                val attribute = AttributeKey<Any>(it[AttributesModel.key])
                client.attr[attribute] = it[AttributesModel.value]
            }

            // TODO("Load timers")



            // Load varps
            var varpQuery: Query? = null

            transaction {
                varpQuery = VarpModel.select {
                    VarpModel.playerId eq client.uuid.toInt()
                }
            }

            varpQuery!!.forEach {
                client.varps.setState(it[VarpModel.varpId], it[VarpModel.state])
            }

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
                it[username] = client.loginUsername
                it[hash] = client.passwordHash
                it[xteaKeyOne] = client.currentXteaKeys[0]
                it[xteaKeyTwo] = client.currentXteaKeys[1]
                it[xteaKeyThree] = client.currentXteaKeys[2]
                it[xteaKeyFour] = client.currentXteaKeys[3]
                it[displayName] = client.username
                it[x] = client.tile.x
                it[height] = client.tile.height
                it[z] = client.tile.z
                it[privilege] = client.privilege.id
                it[runEnergy] = client.runEnergy.toFloat()
                it[displayMode] = client.interfaces.displayMode.id
            }

            // Update player skills
            SkillModel.select {
                SkillModel.playerId eq client.uuid.toInt()
            }.forEach { skill ->
                SkillModel.update({
                    SkillModel.playerId eq client.uuid.toInt()
                    SkillModel.skill eq skill[SkillModel.skill]
                }) {
                    it[lvl] = client.getSkills().getCurrentLevel(skill[SkillModel.skill])
                    it[xp] = client.getSkills().getCurrentXp(skill[SkillModel.skill]).toFloat()
                }
            }

            // Save item containers
            getContainers(client).forEach { container ->
                val containerKey = ItemContainerModel.select {
                    ItemContainerModel.playerId eq client.uuid.toInt()
                    ItemContainerModel.name eq container.name
                }.first()[ItemContainerModel.id]

                container.items.forEach { item ->
                    ItemModel.update({
                        ItemModel.containerId eq containerKey
                    }) {
                        it[index] = item.key
                        it[itemId] = item.value.id
                        it[amount] = item.value.amount
                        // TODO("Handle saving item attributes")
                        //it[attr] = item.value.getAttr().toString()
                    }
                }
            }

            // Save attributes
            client.attr.toPersistentMap().forEach { attr ->
                AttributesModel.update({
                    AttributesModel.playerId eq client.uuid.toInt()
                    AttributesModel.key eq attr.key
                }) {
                    // TODO("Handle saving attribute values of type [ANY]")
                    it[value] = Gson().toJson(attr.value).toString()
                }
            }

            // TODO("Save timers")

            // Save varps
            client.varps.getAll().forEach { varp ->
                VarpModel.update({
                    VarpModel.varpId eq varp.id
                    VarpModel.playerId eq client.uuid.toInt()
                }) {
                    it[state] = varp.state
                }
            }
        }

        return saveClientData(client)
    }

    private fun getContainers(client: Client): List<JsonPlayerSerializer.PersistentContainer> {
        val containers = mutableListOf<JsonPlayerSerializer.PersistentContainer>()

        client.containers.forEach { key, container ->
            if (!container.isEmpty) {
                containers.add(JsonPlayerSerializer.PersistentContainer(key.name, container.toMap()))
            }
        }

        return containers
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