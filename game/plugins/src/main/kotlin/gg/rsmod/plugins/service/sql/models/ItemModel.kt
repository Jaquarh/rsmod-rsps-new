package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object ItemModel : Table("Items") {
    val id = integer("id").autoIncrement().primaryKey()
    val containerId = (integer("container_id") references ItemContainerModel.id).nullable()
    val itemId = integer("item_id")
    val amount = integer("amount").default(1)
    val index = integer("index")
}