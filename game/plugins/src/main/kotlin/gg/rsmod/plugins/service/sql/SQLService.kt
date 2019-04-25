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
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.game.model.timer.TimerKey
import gg.rsmod.game.service.serializer.PlayerLoadResult
import gg.rsmod.game.service.serializer.PlayerSerializerService
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.plugins.service.sql.controllers.LoadController
import gg.rsmod.plugins.service.sql.controllers.SaveController
import gg.rsmod.plugins.service.sql.models.*
import gg.rsmod.plugins.service.sql.serializers.SQLSerializer
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

class SQLService : PlayerSerializerService()
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
        Database.connect("jdbc:$driverHost://$host:$port/$dbname?useSSL=false", driver = driver, user = user, password = pswd)

        // Create tables if not yet created
        transaction {
            mutableListOf(
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

        /*val loadResponse = PlayerLoadController().loadPlayer(client, client.world, request)

        when(loadResponse) {

            /*
             * [PlayerLoadController.loadPlayer] will return [PlayerLoadResult.NEW_ACCOUNT]
             * If the first query for retrieving the username does not have any rows.
             */

            PlayerLoadResult.NEW_ACCOUNT -> {
                configureNewPlayer(client, request)
                client.uid = PlayerSaveController().createPlayer(client, client.world)

                logger.info { "${client.loginUsername} has been created and saved successfully." }

                // Added try catch in-case there is an error not being caught
                try { PlayerSaveController().savePlayer(client, client.world) }
                catch (e: Exception) { logger.info { e.message } }

                logger.info { "${client.loginUsername} has saved successfully." }
            }

            /*
             * Allows for debugging or logging on any other [PlayerLoadResult]
             * response from the [PlayerLoadController.loadPlayer]
             */

            // Account loaded successfully
            PlayerLoadResult.LOAD_ACCOUNT -> {}

            // Account existed but xtea keys do not match
            PlayerLoadResult.INVALID_RECONNECTION -> {
                log("${client.loginUsername} failed to re-identify with xtea keys.")
            }

            // Account existed but credentials did not match
            PlayerLoadResult.INVALID_CREDENTIALS -> {}

            // Account existed but was malformed
            PlayerLoadResult.MALFORMED -> {
                log("${client.loginUsername} failed to load player save.")
            }
        }

        logger.info { "${client.loginUsername} is now being forwarded to the [LoginService]." }

        return loadResponse*/

        val serialize:SQLSerializer = LoadController().loadPlayer(client)
                ?: return configureNewAccount(client, request)

        val previousXteas = IntArray(4)

        previousXteas[0] = serialize.player[PlayerModel.xteaKeyOne]
        previousXteas[1] = serialize.player[PlayerModel.xteaKeyTwo]
        previousXteas[2] = serialize.player[PlayerModel.xteaKeyThree]
        previousXteas[3] = serialize.player[PlayerModel.xteaKeyFour]

        if (!request.reconnecting) {
            if (!BCrypt.checkpw(request.password, serialize.player[PlayerModel.hash])) {
                return PlayerLoadResult.INVALID_CREDENTIALS
            }
        } else {
            if (!Arrays.equals(previousXteas, request.xteaKeys)) {
                return PlayerLoadResult.INVALID_RECONNECTION
            }
        }

        // Load player into client

        client.uid = PlayerUID(serialize.player[PlayerModel.id])
        client.passwordHash = serialize.player[PlayerModel.hash]
        client.username = serialize.player[PlayerModel.displayName]
        client.runEnergy = serialize.player[PlayerModel.runEnergy].toDouble()
        client.privilege = client.world.privileges.get(serialize.player[PlayerModel.privilege]) ?: Privilege.DEFAULT
        client.tile = Tile(serialize.player[PlayerModel.x], serialize.player[PlayerModel.height], serialize.player[PlayerModel.z])
        client.interfaces.displayMode = DisplayMode.values.firstOrNull { it.id == serialize.player[PlayerModel.displayMode] } ?: DisplayMode.FIXED

        // Load skills into client

        serialize.skillModels.forEach { skill ->
            client.getSkills().setXp(skill[SkillModel.skill], skill[SkillModel.xp].toDouble())
            client.getSkills().setCurrentLevel(skill[SkillModel.skill], skill[SkillModel.lvl])
        }

        // Load item containers into client

        serialize.itemContainers.forEach { container ->
            val key = client.world.plugins.containerKeys.firstOrNull { other -> other.name == container.container[ItemContainerModel.name] }
                    ?: return@forEach

            val cont = if (client.containers.containsKey(key)) client.containers[key] else {
                client.containers[key] = ItemContainer(client.world.definitions, key)
                client.containers[key]
            } ?: return@forEach

            container.items.forEach { item ->
                val i = Item(item.item[ItemModel.itemId], item.item[ItemModel.amount])
                item.attributes.forEach { attr ->
                    //i.attr[attr[ItemAttributeModel.key]] = attr[ItemAttributeModel.value]
                }
                cont[item.item[ItemModel.index]] = i
            }
        }

        // Load attributes into client

        serialize.attributeModels.forEach { attr ->
            client.attr[AttributeKey<Any>(attr[AttributesModel.key])] = attr[AttributesModel.value]
        }

        // Load timers into client

        serialize.timerModels.forEach { timer ->
            var time = timer[TimerModel.timeLeft]
            val tick = timer[TimerModel.tickOffline]
            if(tick) {
                val elapsed = System.currentTimeMillis() - timer[TimerModel.currentMs]
                val ticks = (elapsed / client.world.gameContext.cycleTime).toInt()
                time -= ticks
            }

            client.timers[TimerKey(timer[TimerModel.identifier], tick)] = Math.max(0, time)
        }

        // Load varps into client

        serialize.varpModels.forEach { varp ->
            client.varps.setState(varp[VarpModel.varpId], varp[VarpModel.state])
        }

        return PlayerLoadResult.LOAD_ACCOUNT
    }

    private fun configureNewAccount(client: Client, request: LoginRequest): PlayerLoadResult {
        configureNewPlayer(client, request)
        SaveController().createPlayer(client, client.world)
        return PlayerLoadResult.NEW_ACCOUNT
    }

    override fun saveClientData(client: Client): Boolean {
        /*PlayerSaveController().savePlayer(client, client.world)
        return true*/

        return SaveController().savePlayer(client)
    }

    private fun log(message: String) {
        logger.info { message }
    }

    companion object: KLogging()
}