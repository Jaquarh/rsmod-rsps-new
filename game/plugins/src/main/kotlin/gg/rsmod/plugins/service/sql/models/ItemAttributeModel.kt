package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object ItemAttributeModel : Table("ItemAttributes") {
    val id = integer("id").autoIncrement().primaryKey()
    val itemId = (integer("item_id") references ItemModel.id).nullable()
    val key = varchar("key", 1024)
    val value = varchar("value", 1024)
}