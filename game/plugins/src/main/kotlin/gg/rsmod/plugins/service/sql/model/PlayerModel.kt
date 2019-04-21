package gg.rsmod.plugins.service.sql.model

import org.jetbrains.exposed.sql.*

object PlayerModel : Table("Players") {
    val id = integer("id").autoIncrement().primaryKey()
    val username = varchar("username", 60).uniqueIndex()
    val displayName = varchar("display_name", 60).uniqueIndex()
    val x = integer("position_x")
    val height = integer("position_y")
    val z = integer("position_z")
    val privilege = integer("privilege").default(1)
    val runEnergy = float("run_energy").default(0.toFloat())
    val displayMode = integer("display_mode").default(0)
    val hash = varchar("password_hash", 60)
    val xteaKeyOne = integer("xtea_one").nullable()
    val xteaKeyTwo = integer("xtea_two").nullable()
    val xteaKeyThree = integer("xtea_three").nullable()
    val xteaKeyFour = integer("xtea_four").nullable()
}