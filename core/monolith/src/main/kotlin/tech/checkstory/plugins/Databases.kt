package tech.checkstory.plugins

import io.ktor.server.config.*
import java.sql.Connection
import java.sql.DriverManager

fun connectToPostgres(embedded: Boolean, config: ApplicationConfig): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        DriverManager.getConnection("jdbc:h2:~/h2db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MYSQL", "root", "")
    } else {
        val connectionString = config.property("postgres.connectionString").getString()
        require(connectionString.isNotEmpty()) {
            "Postgres connection string is empty, which means some configuration was incorrect"
        }
        DriverManager.getConnection(connectionString)
    }
}
