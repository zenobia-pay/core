package com.zenobiapay.rds.util

import com.zenobiapay.rds.di.RdsModule
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types

@Singleton
class RdsWrapper @Inject constructor(
    @Named(RdsModule.Companion.PG_JDBC_URL) private val jdbcUrl: String,
    @Named(RdsModule.Companion.PG_USER) private val username: String,
    @Named(RdsModule.Companion.PG_PASSWORD) private val password: String
) {
    init {
        // Load the PostgreSQL JDBC driver
        try {
            Class.forName("org.postgresql.Driver")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("PostgreSQL JDBC driver not found", e)
        }
    }

    /**
     * Creates a new database connection
     */
    fun getConnection(): Connection {
        return try {
            DriverManager.getConnection(jdbcUrl, username, password)
        } catch (e: SQLException) {
            throw SQLException("Failed to connect to database: ${e.message}", e)
        }
    }

    /**
     * Executes a query and processes the results with the provided function
     */
    fun <T> executeQuery(sql: String, parameters: List<Any> = emptyList(), resultMapper: (ResultSet) -> T): List<T> {
        val results = mutableListOf<T>()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                // Set parameters
                parameters.forEachIndexed { index, param ->
                    setParameter(statement, index + 1, param)
                }

                // Execute query and map results
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        results.add(resultMapper(resultSet))
                    }
                }
            }
        }

        return results
    }

    /**
     * Executes an update statement (INSERT, UPDATE, DELETE)
     * @return Number of rows affected
     */
    fun executeUpdate(sql: String, parameters: List<Any> = emptyList()): Int {
        getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                // Set parameters
                parameters.forEachIndexed { index, param ->
                    setParameter(statement, index + 1, param)
                }

                // Execute update
                return statement.executeUpdate()
            }
        }
    }

    /**
     * Executes an insert statement and returns the generated keys
     */
    fun <T> executeInsertAndGetKeys(sql: String, parameters: List<Any> = emptyList(), keyMapper: (ResultSet) -> T): T? {
        getConnection().use { connection ->
            connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS).use { statement ->
                // Set parameters
                parameters.forEachIndexed { index, param ->
                    setParameter(statement, index + 1, param)
                }

                // Execute insert
                statement.executeUpdate()

                // Get generated keys
                statement.generatedKeys.use { keys ->
                    return if (keys.next()) {
                        keyMapper(keys)
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * Executes a transaction with multiple SQL statements
     */
    fun <T> executeTransaction(block: (Connection) -> T): T {
        getConnection().use { connection ->
            try {
                connection.autoCommit = false
                val result = block(connection)
                connection.commit()
                return result
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    /**
     * Helper method to set parameters of different types in a PreparedStatement
     */
    private fun setParameter(statement: PreparedStatement, index: Int, value: Any) {
        when (value) {
            is String -> statement.setString(index, value)
            is Int -> statement.setInt(index, value)
            is Long -> statement.setLong(index, value)
            is Double -> statement.setDouble(index, value)
            is Boolean -> statement.setBoolean(index, value)
            is ByteArray -> statement.setBytes(index, value)
            is Date -> statement.setDate(index, value)
            is Timestamp -> statement.setTimestamp(index, value)
            is java.util.Date -> statement.setTimestamp(index, Timestamp(value.time))
            null -> statement.setNull(index, Types.NULL)
            else -> statement.setObject(index, value)
        }
    }
}