package com.zenobiapay.rds.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.rds.di.RdsModule
import com.zenobiapay.rds.model.ItemMetadataSchema
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types
import java.util.UUID

private val logger = KotlinLogging.logger {}

class RdsWrapper @Inject constructor(
    @Named(RdsModule.Companion.PG_JDBC_URL) private val jdbcUrl: String,
    @Named(RdsModule.Companion.PG_USER) private val username: String,
    @Named(RdsModule.Companion.PG_PASSWORD) private val password: String,
    private val objectMapper: ObjectMapper
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
    fun <T> executeInsertAndGetKeys(sql: String, parameters: List<Any?> = emptyList(), keyMapper: (ResultSet) -> T): T? {
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
    private fun setParameter(statement: PreparedStatement, index: Int, value: Any?) {
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
            is UUID -> statement.setObject(index, value)
            null -> statement.setNull(index, Types.NULL)
            else -> statement.setObject(index, value)
        }
    }

    /**
     * Stores transfer and item metadata in a single transaction
     * @param transferId The UUID of the transfer
     * @param transferMetadata JSON metadata for the transfer
     * @param itemsMetadata List of item metadata objects
     * @return The UUID of the inserted transfer record
     */
    fun storeTransferAndItemsMetadata(
        transferId: String,
        merchantId: String,
        transferMetadata: Map<String, Any>,
        itemsMetadata: List<ItemMetadataSchema>
    ): UUID {
        logger.info { "Storing transfer and ${itemsMetadata.size} items metadata for transfer ID: $transferId" }
        
        return executeTransaction { connection ->
            val itemIds = mutableListOf<UUID>()
            
            // Process each item metadata
            itemsMetadata.forEach { itemMetadata ->
                val metadataJson = objectMapper.writeValueAsString(itemMetadata.metadata)
                // Insert item using executeInsertAndGetKeys
                val sql = "INSERT INTO items (id, merchant_id, product_id, brand_id, metadata) VALUES (?, ?, ?, ?, ?::jsonb) ON CONFLICT (id) DO UPDATE SET merchant_id = ?, product_id = ?, brand_id = ?, metadata = ?::jsonb RETURNING id"
                
                val params = listOf<Any?>(
                    itemMetadata.itemId,
                    merchantId, 
                    itemMetadata.productId,
                    itemMetadata.brandId,
                    metadataJson,
                    merchantId,
                    itemMetadata.productId,
                    itemMetadata.brandId,
                    metadataJson
                )
                
                val insertedItemId = executeInsertAndGetKeys(sql, params) { rs ->
                    rs.getObject("id", UUID::class.java)
                }
                
                if (insertedItemId != null) {
                    itemIds.add(insertedItemId)
                } else {
                    throw SQLException("Failed to insert or update item metadata for item ID: ${itemMetadata.itemId}")
                }
            }
            
            // Insert transfer with item IDs using executeInsertAndGetKeys
            val transferMetadataJson = objectMapper.writeValueAsString(transferMetadata)
            val transferSql = "INSERT INTO transfers (id, item_ids, metadata) VALUES (?, ?, ?::jsonb) ON CONFLICT (id) DO UPDATE SET item_ids = ?, metadata = ?::jsonb RETURNING id"
            
            // We need to create the array in the connection context
            connection.prepareStatement(transferSql, PreparedStatement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setObject(1, transferId)
                statement.setArray(2, connection.createArrayOf("uuid", itemIds.toTypedArray()))
                statement.setString(3, transferMetadataJson)
                statement.setArray(4, connection.createArrayOf("uuid", itemIds.toTypedArray()))
                statement.setString(5, transferMetadataJson)

                statement.executeUpdate()
                
                statement.generatedKeys.use { keys ->
                    if (keys.next()) {
                        return@executeTransaction keys.getObject("id", UUID::class.java)
                    } else {
                        throw SQLException("Failed to insert or update transfer metadata for transfer ID: $transferId")
                    }
                }
            }
        }
    }
}