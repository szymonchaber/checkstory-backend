package tech.checkstory.plugins

import io.ktor.server.config.*
import java.sql.Connection
import java.sql.DriverManager

fun connectToPostgres(embedded: Boolean, config: ApplicationConfig): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        DriverManager.getConnection("jdbc:h2:~/h2db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MYSQL", "root", "")
    } else {
        val url = config.property("postgres.url").getString()
        val user = config.property("postgres.user").getString()
        val password = config.property("postgres.password").getString()

        require(password.isNotEmpty()) {
            "Postgres password is empty, which means some configuration was incorrect"
        }
        DriverManager.getConnection(url, user, password)
    }
}
