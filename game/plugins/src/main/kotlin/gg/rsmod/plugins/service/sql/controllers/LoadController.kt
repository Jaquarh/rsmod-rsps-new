package gg.rsmod.plugins.service.sql.controllers

import gg.rsmod.game.model.entity.Client
import gg.rsmod.plugins.service.sql.models.*
import gg.rsmod.plugins.service.sql.serializers.Item
import gg.rsmod.plugins.service.sql.serializers.ItemContainer
import gg.rsmod.plugins.service.sql.serializers.SQLSerializer
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class LoadController: Controller() {
    fun loadPlayer(client: Client): SQLSerializer? {

        var serialize: SQLSerializer? = null

        transaction {
            /*
             * Load the player
             */
            val player = PlayerModel.select {
                PlayerModel.username eq client.loginUsername
            }.firstOrNull() ?: return@transaction

            /*
             * Load items, item attributes and item containers for the player
             */

            val containers: MutableList<ItemContainer> = mutableListOf()

            ItemContainerModel.select {
                ItemContainerModel.playerId eq player[PlayerModel.id]
            }.forEach { container ->
                val items: MutableList<Item> = mutableListOf()

                ItemModel.select {
                    ItemModel.containerId eq container[ItemContainerModel.id]
                }.forEach { item ->

                    val attributes: MutableList<ResultRow> = mutableListOf()

                    ItemAttributeModel.select {
                        ItemAttributeModel.itemId eq item[ItemModel.id]
                    }.forEach { itemAttr ->
                        attributes.add(itemAttr)
                    }

                    items.add(Item(item, attributes))
                }

                containers.add(ItemContainer(items, container))
            }

            /*
             * Load attributes for player
             */

            val attributes: MutableList<ResultRow> = mutableListOf()

            AttributesModel.select {
                AttributesModel.playerId eq player[PlayerModel.id]
            }.forEach {
                attributes.add(it)
            }

            /*
             * Load skills for player
             */

            val skills: MutableList<ResultRow> = mutableListOf()

            SkillModel.select {
                SkillModel.playerId eq player[PlayerModel.id]
            }.forEach {
                skills.add(it)
            }

            /*
             * Load timers for player
             */

            val timers: MutableList<ResultRow> = mutableListOf()

            TimerModel.select {
                TimerModel.playerId eq player[PlayerModel.id]
            }.forEach {
                timers.add(it)
            }

            /*
             * Load varps for player
             */

            val varps: MutableList<ResultRow> = mutableListOf()

            VarpModel.select {
                VarpModel.playerId eq player[PlayerModel.id]
            }.forEach {
                varps.add(it)
            }

            /*
             * Create a [SQLSerializer] of all the data
             */

            serialize = SQLSerializer(player, containers, skills, attributes, timers, varps)
        }

        return serialize
    }
}