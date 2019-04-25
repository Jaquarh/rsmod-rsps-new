package gg.rsmod.plugins.service.sql.controllers

import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.plugins.service.sql.models.*
import gg.rsmod.plugins.service.sql.serializers.SQLSerializer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet

class SaveController : Controller() {
    fun createPlayer(client: Client, world: World): PlayerUID {

        /*
         * Generate the [PlayerModel]
         */

        val player = transaction {
            PlayerModel.insert {
                it[this.username] = client.loginUsername
                it[this.displayName] = client.username
                it[this.displayMode] = client.interfaces.displayMode.id
                it[this.hash] = client.passwordHash
                it[this.height] = world.gameContext.home.height
                it[this.x] = world.gameContext.home.x
                it[this.z] = world.gameContext.home.z
                it[this.privilege] = Privilege.DEFAULT.id
                it[this.xteaKeyOne] = client.currentXteaKeys[0]
                it[this.xteaKeyTwo] = client.currentXteaKeys[1]
                it[this.xteaKeyThree] = client.currentXteaKeys[2]
                it[this.xteaKeyFour] = client.currentXteaKeys[3]
            } get PlayerModel.id
        }

        /*
         * Generate the players [SkillModel]
         * Generates the players [ItemContainerModel]
         */

        transaction {

            // Since there are n skills

            for(i in 0 until client.getSkills().maxSkills) {
                SkillModel.insert {
                    it[this.skill] = i
                    it[this.lvl] = 1
                    it[this.xp] = 0.toFloat()
                    it[this.playerId] = player
                }
            }

            // Lets build the generic inventory

            ItemContainerModel.insert {
                it[this.name] = "inventory"
                it[this.playerId] = player
            }
        }

        return PlayerUID(player)
    }

    fun savePlayer(client: Client): Boolean {

        val serialize:SQLSerializer = LoadController().loadPlayer(client) ?: return false

        transaction {

            /*
             * Save the player
             */
            PlayerModel.update ({
                PlayerModel.id eq serialize.player[PlayerModel.id]
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

            // Save the skills
            serialize.skillModels.forEach { skill ->
                SkillModel.update({
                    SkillModel.id eq skill[SkillModel.id]
                }) {
                    it[this.lvl] = client.getSkills().getCurrentLevel(skill[SkillModel.skill])
                    it[this.xp] = client.getSkills().getCurrentXp(skill[SkillModel.skill]).toFloat()
                }
            }

            /*
             * Save the item containers, item attributes and items
             */
            getContainers(client).forEach { container ->

                val dbContainer = serialize.itemContainers.firstOrNull {
                    c -> c.container[ItemContainerModel.name] == container.name
                }

                if(dbContainer != null) {
                    /*
                     * Use the container
                     */

                    log("${client.loginUsername} has ${container.items.keys.size} items in their ${container.name}.")

                    container.items.forEach { item ->
                        val dbItem = dbContainer.items.firstOrNull {
                            i -> i.item[ItemModel.itemId] == item.value.id
                        }

                        /*
                         * Update the item at that index
                         * Add the attributes
                         */
                        if(dbItem != null) {

                            log("${client.loginUsername} - [ITEM: ${item.value.id}] existed in the DB.")

                            ItemModel.update({
                                ItemModel.id eq dbItem.item[ItemModel.id]
                            }) {
                                it[this.itemId] = item.value.id
                                it[this.amount] = item.value.amount
                                it[this.index] = item.key
                            }

                            // TODO: remove unused item attrs

                            item.value.attr.forEach { itemAttr ->
                                val dbItemAttr = dbItem.attributes.firstOrNull {
                                    a ->
                                    a[ItemAttributeModel.key] == itemAttr.key.name
                                }

                                /*
                                 * Update the Attribute for that item
                                 */
                                if(dbItemAttr != null) {
                                    ItemAttributeModel.update({
                                        ItemAttributeModel.id eq dbItemAttr[ItemAttributeModel.id]
                                    }) {
                                       it[this.value] = itemAttr.value.toString()
                                    }
                                } else {
                                    ItemAttributeModel.insert {
                                        it[this.key] = itemAttr.key.name
                                        it[this.itemId] = dbItem.item[ItemModel.id]
                                        it[this.value] = itemAttr.value.toString()
                                    }
                                }
                            }

                            // Remove the item from container so
                            // At the end we are left with ones
                            // That need removing
                            dbContainer.items.remove(dbItem)

                        } else {
                            /*
                             * Insert the item at that index
                             */
                            log("${client.loginUsername} - [ITEM: ${item.value.id}] did not exist in the DB.")

                            val itemId = ItemModel.insert {
                                it[this.index] = item.key
                                it[this.containerId] = dbContainer.container[ItemContainerModel.id]
                                it[this.amount] = item.value.amount
                                it[this.itemId] = item.value.id
                            } get ItemModel.id

                            item.value.attr.forEach { itemAttr ->
                                ItemAttributeModel.insert {
                                    it[this.key] = itemAttr.key.name
                                    it[this.itemId] = itemId
                                    it[this.value] = itemAttr.value.toString()
                                }
                            }
                        }
                    }

                    log("${client.loginUsername} is now having all unused items removed for this container.")

                    // Remove all other items
                    dbContainer.items.forEach {
                        ItemModel.deleteWhere {
                            ItemModel.id eq it.item[ItemModel.id]
                        }
                        it.attributes.forEach { attr ->
                            ItemAttributeModel.deleteWhere {
                                ItemAttributeModel.id eq attr[ItemAttributeModel.id]
                            }
                        }
                    }
                } else {
                    /*
                     * Insert the container and all the items
                     */

                    log("${client.loginUsername} - [CONTAINER: ${container.name}] did not exist in the DB.")

                    val containerId = ItemContainerModel.insert {
                        it[this.playerId] = serialize.player[PlayerModel.id]
                        it[this.name] = container.name
                    } get ItemContainerModel.id

                    container.items.forEach { item ->
                        /*
                         * Insert the item at that index
                         */
                        val itemId = ItemModel.insert {
                            it[this.index] = item.key
                            it[this.containerId] = containerId
                            it[this.amount] = item.value.amount
                            it[this.itemId] = item.value.id
                        } get ItemModel.id

                        log("${client.loginUsername} - [ITEM: ${item.value.id}] has been inserted in the DB.")

                        item.value.attr.forEach { itemAttr ->
                            ItemAttributeModel.insert {
                                it[this.key] = itemAttr.key.name
                                it[this.itemId] = itemId
                                it[this.value] = itemAttr.value.toString()
                            }
                        }
                    }
                }
            }

            /*
             * Save player attributes
             */
            client.attr.toPersistentMap().forEach { attr ->
                // Over 2000 of these so we want to batch update / insert
                // Check if it exists in the DB
                val dbAttr = serialize.attributeModels.firstOrNull {
                    it[AttributesModel.key] == attr.key
                }

                if(dbAttr != null) {
                    // Update
                    AttributesModel.update({
                        AttributesModel.id eq dbAttr[AttributesModel.id]
                    }) {
                        it[this.value] = attr.value.toString()
                    }
                } else {
                    // Insert
                    AttributesModel.insert {
                        it[this.playerId] = serialize.player[PlayerModel.id]
                        it[this.key] = attr.key
                        it[this.value] = attr.value.toString()
                    }
                }
            }

            /*
             * Save player timers
             */

            client.timers.toPersistentTimers().forEach { timer ->
                val dbTimer = serialize.timerModels.firstOrNull {
                    timer.identifier == it[TimerModel.identifier]
                }

                if(dbTimer != null) {
                    // Remove so later we can loop through unused ones to delete
                    serialize.timerModels.remove(dbTimer)

                    // Update
                    TimerModel.update({
                        TimerModel.id eq dbTimer[TimerModel.id]
                    }) {
                        it[this.currentMs] = timer.currentMs
                        it[this.tickOffline] = timer.tickOffline
                        it[this.timeLeft] = timer.timeLeft
                    }
                } else {
                    // Insert
                    TimerModel.insert {
                        it[this.playerId] = serialize.player[PlayerModel.id]
                        it[this.currentMs] = timer.currentMs
                        it[this.tickOffline] = timer.tickOffline
                        it[this.timeLeft] = timer.timeLeft
                        it[this.identifier] = timer.identifier.toString()
                    }
                }
            }

            // Remove all unused ones
            serialize.timerModels.forEach { timer ->
                TimerModel.deleteWhere {
                    TimerModel.id eq timer[TimerModel.id]
                }
            }

            client.varps.getAll().forEach { varp ->
                val dbVarp = serialize.varpModels.firstOrNull {
                    it[VarpModel.varpId] == varp.id
                }

                if(dbVarp != null) {
                    // Update
                    VarpModel.update({
                        VarpModel.id eq dbVarp[VarpModel.id]
                    }) {
                        it[this.state] = varp.state
                    }

                    // Remove from models so we can delete unused ones later
                    serialize.varpModels.remove(dbVarp)
                } else {
                    // Insert
                    VarpModel.insert {
                        it[this.playerId] = serialize.player[PlayerModel.id]
                        it[this.varpId] = varp.id
                        it[this.state] = varp.state
                    }
                }
            }

            // Remove all unused varps
            serialize.varpModels.forEach {
                VarpModel.deleteWhere {
                    VarpModel.id eq it[VarpModel.id]
                }
            }

        }

        return true
    }
}