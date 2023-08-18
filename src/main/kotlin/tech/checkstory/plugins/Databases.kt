package tech.checkstory.plugins

import com.typesafe.config.Config
import java.sql.Connection
import java.sql.DriverManager

fun connectToPostgres(embedded: Boolean, config: Config): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        DriverManager.getConnection("jdbc:h2:~/h2db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MYSQL", "root", "")
    } else {
        val url = config.getString("postgres.url")!!
        val user = config.getString("postgres.user")!!
        val password = config.getString("postgres.password")!!

        require(password.isNotEmpty()) {
            "Postgres password is empty, which means some configuration was incorrect"
        }
        DriverManager.getConnection(url, user, password)
    }
}
