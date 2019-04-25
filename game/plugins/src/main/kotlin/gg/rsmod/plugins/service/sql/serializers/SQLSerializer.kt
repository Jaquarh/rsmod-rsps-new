package gg.rsmod.plugins.service.sql.serializers

import org.jetbrains.exposed.sql.ResultRow

data class SQLSerializer(
        val player: ResultRow,
        val itemContainers: MutableList<ItemContainer>,
        val skillModels: MutableList<ResultRow>,
        val attributeModels: MutableList<ResultRow>,
        val timerModels: MutableList<ResultRow>,
        val varpModels:MutableList<ResultRow>
)

data class ItemContainer(
        val items: MutableList<Item>,
        val container: ResultRow
)

data class Item(
        val item: ResultRow,
        val attributes: MutableList<ResultRow>
)