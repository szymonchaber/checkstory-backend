package tech.checkstory.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

private const val DIGITAL_OCEAN_CORE_COUNT = 2

// https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing#the-formula
private const val POOL_SIZE = DIGITAL_OCEAN_CORE_COUNT * 2 + 1


fun createHikariDataSource(
    url: String,
    driver: String
): HikariDataSource {
    return HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        maximumPoolSize = POOL_SIZE
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })
}

