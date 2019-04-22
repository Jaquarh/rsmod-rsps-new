package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object ItemContainerModel : Table("ItemContainer"){
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("name", 60)
    val playerId = (integer("player_id") references PlayerModel.id).nullable()
}