package tech.checkstory.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun createHikariDataSource(
    url: String,
    driver: String
): HikariDataSource {
    return HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        maximumPoolSize = 21
        isAutoCommit = false
        transactionIsolation
        validate()
    })
}
