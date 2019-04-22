package gg.rsmod.plugins.service.sql.controllers

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
import gg.rsmod.net.codec.login.LoginRequest
import gg.rsmod.plugins.service.sql.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class PlayerLoadController : Controller() {

    private var previousXteas: IntArray = IntArray(4)
    private var username: String? = null
    private var passwordHash: String? = null
    private var displayName: String?= null
    private var displayMode: DisplayMode? = null
    private var privilege: Privilege? = null
    private var tile: Tile? = null
    private var runEnergy: Double? = null
    private var uid: PlayerUID? = null

    fun loadPlayer(client: Client, world: World, request: LoginRequest): PlayerLoadResult {

        val player = transaction {
            PlayerModel.select {
                PlayerModel.username eq client.loginUsername
            }.firstOrNull()
        } ?: return PlayerLoadResult.NEW_ACCOUNT

        previousXteas[0] = player?.get(PlayerModel.xteaKeyOne)!!
        previousXteas[1] = player?.get(PlayerModel.xteaKeyTwo)!!
        previousXteas[2] = player?.get(PlayerModel.xteaKeyThree)!!
        previousXteas[3] = player?.get(PlayerModel.xteaKeyFour)!!
        uid = PlayerUID(player?.get(PlayerModel.id))
        username = player?.get(PlayerModel.username)
        passwordHash = player?.get(PlayerModel.hash)
        displayName = player?.get(PlayerModel.displayName)
        displayMode = DisplayMode.values.firstOrNull { it.id == player?.get(PlayerModel.displayMode) } ?: DisplayMode.FIXED
        privilege = world.privileges.get(player?.get(PlayerModel.privilege)!!)
        tile = Tile(player?.get(PlayerModel.x), player?.get(PlayerModel.height), player?.get(PlayerModel.z))
        runEnergy = player?.get(PlayerModel.runEnergy).toDouble()

        if (!request.reconnecting) {
            if (!BCrypt.checkpw(request.password, passwordHash)) {
                return PlayerLoadResult.INVALID_CREDENTIALS
            }
        } else {
            if (!Arrays.equals(previousXteas, request.xteaKeys)) {
                return PlayerLoadResult.INVALID_RECONNECTION
            }
        }

        client.uid = uid as PlayerUID
        client.username = displayName as String
        client.loginUsername = username as String
        client.passwordHash = passwordHash as String
        client.tile = tile as Tile
        client.privilege = privilege as Privilege
        client.runEnergy = runEnergy as Double
        client.interfaces.displayMode = displayMode as DisplayMode

        /*
         * Loading the player [Skill] information.
         *
         * Each [SkillModel] cross references the [PlayerModel]
         * We can loop through the rows and retrieve all the information.
         */

        transaction {
            SkillModel.select { SkillModel.playerId eq player?.get(PlayerModel.id) }
        }.forEach { skill ->
            client.getSkills().setXp(skill?.get(SkillModel.skill), skill?.get(SkillModel.xp).toDouble())
            client.getSkills().setCurrentLevel(skill?.get(SkillModel.skill), skill?.get(SkillModel.lvl))
        }

        /*
         * Loading the player [ItemContainer] information.
         *
         * Each [ItemModel] cross references the [ItemContainerModel]
         * Each [ItemContainerModel] cross references the [PlayerModel]
         * We can loop through all the containers, retrieve the client key
         * and save all the items to the client container.
         */

        transaction {
            ItemContainerModel.select { ItemContainerModel.playerId eq player?.get(PlayerModel.id) }
        }.forEach { container ->
            val key = world.plugins.containerKeys.firstOrNull { other -> other.name == container?.get(ItemContainerModel.name) }
                    ?: return@forEach

            val cont = if (client.containers.containsKey(key)) client.containers[key] else {
                client.containers[key] = ItemContainer(client.world.definitions, key)
                client.containers[key]
            }!!

            transaction {
                ItemModel.select { ItemModel.containerId eq container?.get(ItemContainerModel.id) }
            }.forEach { item ->
                cont[item?.get(ItemModel.index)] = Item(item?.get(ItemModel.itemId), item?.get(ItemModel.amount))
            }
        }

        /*
         * Loading the player [AttributesModel] information.
         *
         * Each [AttributesModel] cross references [PlayerModel]
         * We can loop through all of the rows, generate the [AttributeKey] and attach
         * The value to the client.
         */

        transaction {
            AttributesModel.select { AttributesModel.playerId eq player?.get(PlayerModel.id) }
        }.forEach { attribute ->
            val attr = AttributeKey<Any>(attribute?.get(AttributesModel.key))
            client.attr[attr] = attribute?.get(AttributesModel.value)
        }

        /*
         * Loading the player [TimerModel] information.
         *
         * Each [TimerModel] cross references [PlayerModel]
         * We can loop through all of the rows, generate the [TimerKey]
         * To store the values into the client.
         */

        transaction {
            TimerModel.select { TimerModel.playerId eq player?.get(PlayerModel.id) }
        }.forEach { timer ->
            var time = timer?.get(TimerModel.timeLeft)
            if (timer?.get(TimerModel.tickOffline)) {
                val elapsed = System.currentTimeMillis() - timer?.get(TimerModel.currentMs)
                val ticks = (elapsed / client.world.gameContext.cycleTime).toInt()
                time -= ticks
            }
            val key = TimerKey(timer?.get(TimerModel.identifier), timer?.get(TimerModel.tickOffline))
            client.timers[key] = Math.max(0, time)
        }

        /*
         * Loading the player [VarpModel] information.
         *
         * Each [VarpModel] cross references [PlayerModel]
         * We can loop through all the rows and assign the id
         * And values to the client.
         */

        transaction {
            VarpModel.select { VarpModel.playerId eq player?.get(PlayerModel.id) }
        }.forEach { varp ->
            client.varps.setState(varp?.get(VarpModel.varpId), varp?.get(VarpModel.state))
        }

        /*
         * Loading has completed successfully, return the
         * [PlayerLoadResult]
         */

        return PlayerLoadResult.LOAD_ACCOUNT
    }
}