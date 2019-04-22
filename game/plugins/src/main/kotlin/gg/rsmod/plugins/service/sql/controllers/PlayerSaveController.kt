package gg.rsmod.plugins.service.sql.controllers

import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.model.priv.Privilege
import gg.rsmod.plugins.service.sql.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerSaveController : Controller() {
    fun savePlayer(client: Client, world: World) {
        transaction {

            // Update  player model
            PlayerModel.update ({
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

            // Update player skills
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

            // Save item containers
            getContainers(client).forEach { container ->
                val containerKey = ItemContainerModel.select {
                    ItemContainerModel.playerId eq Integer.parseInt(client.uid.value.toString())
                    ItemContainerModel.name eq container.name
                }.first()[ItemContainerModel.id]

                container.items.forEach { item ->
                    ItemModel.update({
                        ItemModel.containerId eq containerKey
                    }) {
                        it[index] = item.key
                        it[itemId] = item.value.id
                        it[amount] = item.value.amount

                        // Store item attributes
                        item.value.attr.forEach { attr ->
                            ItemAttributeModel.update({
                                ItemAttributeModel.itemId eq item.value.id
                                ItemAttributeModel.playerId eq Integer.parseInt(client.uid.value.toString())
                                ItemAttributeModel.key eq attr.key.name
                            }) { upd ->
                                upd[value] = attr.value.toString()
                            }
                        }
                    }
                }
            }

            // Save attributes
            client.attr.toPersistentMap().forEach { attr ->
                AttributesModel.update({
                    AttributesModel.playerId eq Integer.parseInt(client.uid.value.toString())
                    AttributesModel.key eq attr.key
                }) {
                    it[value] = attr.value.toString()
                }
            }

            // Save timers
            client.timers.toPersistentTimers().forEach { timer ->
                if(timer.identifier != null) {
                    TimerModel.update({
                        TimerModel.playerId eq Integer.parseInt(client.uid.value.toString())
                        TimerModel.identifier eq timer.identifier.toString()
                    }) {
                        it[currentMs] = timer.currentMs
                        it[tickOffline] = timer.tickOffline
                        it[timeLeft] = timer.timeLeft
                    }
                }
            }

            // Save varps
            client.varps.getAll().forEach { varp ->
                VarpModel.update({
                    VarpModel.varpId eq varp.id
                    VarpModel.playerId eq Integer.parseInt(client.uid.value.toString())
                }) {
                    it[state] = varp.state
                }
            }
        }
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
         */

        transaction {

            // Since there are 22 skills

            for(i in 0..22) {
                SkillModel.insert {
                    it[this.skill] = i
                    it[this.lvl] = 1
                    it[this.xp] = 0.toFloat()
                }
            }

        }

        return PlayerUID(player)
    }
}