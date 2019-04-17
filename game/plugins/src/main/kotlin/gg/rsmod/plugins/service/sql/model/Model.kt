package gg.rsmod.plugins.service.sql.model

import java.sql.*
import java.util.Properties

object Model {

    internal var conn: Connection? = null

    fun executeMySQLQuery(sql: String): ResultSet? {
        var stmt: Statement? = null
        var resultset: ResultSet? = null

        try {
            stmt = conn!!.createStatement()
            resultset = stmt!!.executeQuery(sql)
            if (stmt.execute(sql)) {
                resultset = stmt.resultSet
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            if (resultset != null) {
                try {
                    resultset.close()
                } catch (sqlEx: SQLException) {
                }
                resultset = null
            }
            if (stmt != null) {
                try {
                    stmt.close()
                } catch (sqlEx: SQLException) {
                }
                stmt = null
            }
        }

        return resultset
    }

    fun destroyConnection() {
        if (conn != null) {
            try {
                conn!!.close()
            } catch (sqlEx: SQLException) {
            }
            conn = null
        }
    }

    fun startConnection(user: String, paswd: String, host: String, port: Int, driver: String) {
        val connectionProps = Properties()

        connectionProps.put("user", user)
        connectionProps.put("password", paswd)

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance()
            conn = DriverManager.getConnection("jdbc:$driver://$host:$port/", connectionProps)
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}