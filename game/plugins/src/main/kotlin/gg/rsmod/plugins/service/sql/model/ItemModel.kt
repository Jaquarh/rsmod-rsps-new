package gg.rsmod.plugins.service.sql.model

import org.jetbrains.exposed.sql.*

object ItemModel : Table("Items") {
    val id = integer("id").autoIncrement().primaryKey()
    val containerId = (integer("container_id") references ItemContainerModel.id).nullable()
    val index = integer("index").default(0)
    val attr = varchar("attr", 60).nullable()
    val itemId = integer("item_id")
    val amount = integer("amount").default(1)
}