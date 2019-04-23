package gg.rsmod.plugins.service.sql.controllers

import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.plugins.service.sql.models.*
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerSaveController : Controller() {
    fun savePlayer(client: Client, world: World) {
        transaction {

            /*
             * Update [PlayerModel] for player
             *
             * We know that all the player data already exist in the DB
             * since they was created on [PlayerLoadResponse.NEW_ACCOUNT]
             */
            PlayerModel.update({
                PlayerModel.id eq Integer.parseInt(client.uid.value.toString())
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

            log("${client.loginUsername}:PlayerModel been updated.")

            /*
             * Update [SkillModel] for player
             *
             * We know that all the skills already exist in the DB
             * since they was created on [PlayerLoadResponse.NEW_ACCOUNT]
             */
            SkillModel.select {
                SkillModel.playerId eq Integer.parseInt(client.uid.value.toString())
            }.forEach { skill ->
                SkillModel.update({
                    SkillModel.playerId eq Integer.parseInt(client.uid.value.toString())
                    SkillModel.skill eq skill[SkillModel.skill]
                }) {
                    it[lvl] = client.getSkills().getCurrentLevel(skill[SkillModel.skill])
                    it[xp] = client.getSkills().getCurrentXp(skill[SkillModel.skill]).toFloat()
                }
            }

            log("${client.loginUsername}:SkillModel been updated.")

            /*
             * Update [ItemModel] (has many) for player
             *
             * Since the items are not created by default
             * the index may not yet exist for the [ItemModel]
             * thus we need to first check it exists before updating it.
             * If it does not exist, we need to insert a new entry.
             */
        }

        getContainers(client).forEach { container ->
            val containerKey = transaction {
                ItemContainerModel.select {
                    ItemContainerModel.playerId eq Integer.parseInt(client.uid.value.toString())
                    ItemContainerModel.name eq container.name
                }.firstOrNull()
            }?.get(ItemContainerModel.id) ?: transaction {
                ItemContainerModel.insert {
                    it[this.name] = container.name
                    it[this.playerId] = Integer.parseInt(client.uid.value.toString())
                } get ItemContainerModel.id
            }

            /*
             * With the new or existing container key
             * we can now start inserting or updating the [ItemModel]
             * based on whether the index of the [Item] exists or not.
             */

            container.items.forEach { item ->
                val itemKey = transaction {
                    ItemModel.select {
                        ItemModel.containerId eq containerKey
                        ItemModel.index eq item.key
                    }.firstOrNull()
                }

                if (itemKey != null) {
                    transaction {
                        ItemModel.update({
                            ItemModel.index eq itemKey[ItemModel.index]
                            ItemModel.containerId eq containerKey
                        }) {
                            it[itemId] = item.value.id
                            it[amount] = item.value.amount
                        }
                    }
                } else {
                    transaction {
                        ItemModel.insert {
                            it[index] = item.key
                            it[itemId] = item.value.id
                            it[amount] = item.value.amount
                        }
                    }
                }

                /*
                 * We need to ensure the [ItemAttributeModel] exists
                 * before updating it. If it doesn't exist, we can
                 * insert a new row.
                 */

                // Store item attributes
                item.value.attr.forEach { attr ->
                    val attrKey = transaction {
                        ItemAttributeModel.select {
                            ItemAttributeModel.itemId eq item.value.id
                            ItemAttributeModel.playerId eq Integer.parseInt(client.uid.value.toString())
                            ItemAttributeModel.key eq attr.key.name
                        }.firstOrNull()
                    }

                    if (attrKey != null) {
                        transaction {
                            ItemAttributeModel.update({
                                ItemAttributeModel.id eq attrKey[ItemAttributeModel.id]
                            }) { upd ->
                                upd[value] = attr.value.toString()
                            }
                        }
                    } else {
                        transaction {
                            ItemAttributeModel.insert {
                                it[this.itemId] = item.value.id
                                it[this.key] = attr.key.name
                                it[this.playerId] = Integer.parseInt(client.uid.value.toString())
                                it[this.value] = attr.value.toString()
                            }
                        }
                    }

                    log("${client.loginUsername}:ItemModel been updated.")
                }
            }
        }

        log("${client.loginUsername}:ItemContainerModel been updated.")

        /*
        * Update [AttriubtesModel] (has many) for player
        *
        * Since [AttributesModel] is not inserted by default
        * we must check it exists before updating.
        * If it doesn't exist, we insert a new row.
        */
        client.attr.toPersistentMap().forEach { attr ->
            val attrKey = transaction {
                AttributesModel.select {
                    AttributesModel.playerId eq Integer.parseInt(client.uid.value.toString())
                    AttributesModel.key eq attr.key
                }.firstOrNull()
            }

            if(attrKey != null) {
                transaction {
                    AttributesModel.update({
                        AttributesModel.id eq attrKey[AttributesModel.id]
                    }) {
                        it[value] = attr.value.toString()
                    }
                }
            } else {
                transaction {
                    AttributesModel.insert {
                        it[this.key] = attr.key
                        it[this.playerId] = Integer.parseInt(client.uid.value.toString())
                        it[this.value] = attr.value.toString()
                    }
                }
            }
        }

        log("${client.loginUsername}:AttributeModel been updated.")

        /*
        * Update [TimerModel] (has many) for player
        *
        * Since [TimerModel] is not inserted by default
        * we must check it exists before updating.
        * If it doesn't exist, we insert a new row.
        */
        client.timers.toPersistentTimers().forEach { timer ->
            if(timer.identifier != null) {

                val timerKey = transaction {
                    TimerModel.select {
                        TimerModel.playerId eq Integer.parseInt(client.uid.value.toString())
                        TimerModel.identifier eq timer.identifier.toString()
                    }.firstOrNull()
                }

                if(timerKey != null) {
                    transaction {
                        TimerModel.update({
                            TimerModel.id eq timerKey[TimerModel.id]
                        }) {
                            it[currentMs] = timer.currentMs
                            it[tickOffline] = timer.tickOffline
                            it[timeLeft] = timer.timeLeft
                        }
                    }
                } else {
                    transaction {
                        TimerModel.insert {
                            it[this.currentMs] = timer.currentMs
                            it[this.tickOffline] = timer.tickOffline
                            it[this.timeLeft] = timer.timeLeft
                            it[this.playerId] = Integer.parseInt(client.uid.value.toString())
                            it[this.identifier] = timer.identifier.toString()
                        }
                    }
                }
            }
        }

        log("${client.loginUsername}:TimerModel been updated.")

        /*
        * Update [VarpModel] (has many) for player
        *
        * Since [VarpModel] is not inserted by default
        * we must check it exists before updating.
        * If it doesn't exist, we insert a new row.
        */
        client.varps.getAll().forEach { varp ->

            val varpKey = transaction {
                VarpModel.select {
                    VarpModel.varpId eq varp.id
                    VarpModel.playerId eq Integer.parseInt(client.uid.value.toString())
                }.firstOrNull()
            }

            if(varpKey != null) {
                transaction {
                    VarpModel.update({
                        VarpModel.id eq varpKey[VarpModel.id]
                    }) {
                        it[state] = varp.state
                    }
                }
            } else {
                transaction {
                    VarpModel.insert {
                        it[this.state] = varp.state
                        it[this.playerId] = Integer.parseInt(client.uid.value.toString())
                        it[this.varpId] = varp.id
                    }
                }
            }
        }

        log("${client.loginUsername}:VarpModel been updated.")
    }

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

    private fun log(str: String) = logger.info { str }

    companion object: KLogging()
}