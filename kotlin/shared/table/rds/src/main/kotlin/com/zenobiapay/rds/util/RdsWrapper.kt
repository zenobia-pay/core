package com.zenobiapay.rds.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.rds.di.RdsModule
import com.zenobiapay.rds.model.ItemMetadata
import com.zenobiapay.rds.model.RdsItemMetadataSchema
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
            logger.info { "Attempting to connect to $jdbcUrl, username=$username" }
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
            is Map<*, *> -> {
                // Convert map to JSON string and set as string parameter for JSONB
                val jsonString = objectMapper.writeValueAsString(value)
                statement.setString(index, jsonString)
            }
            null -> statement.setNull(index, Types.NULL)
            else -> statement.setObject(index, value)
        }
    }

    fun getItem(itemId: UUID): RdsItemMetadataSchema? {
        val query = "SELECT * FROM items WHERE id = ?"
        val items = executeQuery(query, listOf(itemId)) { resultSet ->
            val id = resultSet.getObject("id", UUID::class.java)
            val name = resultSet.getString("name")
            val merchantId = resultSet.getString("merchant_id")
            val brandName = resultSet.getString("brand_name")
            val size = resultSet.getString("size")
            val color = resultSet.getString("color")
            val material = resultSet.getString("material")
            val year = resultSet.getString("year")
            // TODO: add metadata

            RdsItemMetadataSchema(
                itemId = id,
                merchantId = merchantId,
                itemMetadata = ItemMetadata(
                    name = name,
                    brandName = brandName,
                    size = size,
                    color = color,
                    material = material,
                    year = year,
                    imageUrls = null,
                ),
                rawMetadata = null,
                imageS3ObjectKeys = resultSet.getArray("image_keys")?.let { array -> (array.array as? Array<*>)?.mapNotNull { it as? String } },
            )
        }
        
        return items.firstOrNull()
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
        transferMetadata: Map<String, Any>?,
        itemsMetadata: List<RdsItemMetadataSchema>?
    ) {
        logger.info { "Storing transfer and ${itemsMetadata?.size} items metadata for transfer ID: $transferId" }
        val creationTime = Timestamp(System.currentTimeMillis())
        
        return executeTransaction { connection ->
            val itemIds = mutableListOf<UUID>()
            
            // Process each item metadata
            itemsMetadata?.forEach { rdsItemMetadata ->
                val itemMetadata = rdsItemMetadata.itemMetadata
                // Insert item using executeInsertAndGetKeys
                val sql = "INSERT INTO items (id, name, merchant_id, brand_name, size, color, material, year, creation_time, metadata) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                
                val params = listOf<Any?>(
                    rdsItemMetadata.itemId,
                    itemMetadata.name,
                    merchantId,
                    itemMetadata.brandName,
                    itemMetadata.size,
                    itemMetadata.color,
                    itemMetadata.material,
                    itemMetadata.year,
                    creationTime,
                    rdsItemMetadata.rawMetadata,
                    merchantId,
                    itemMetadata.name,
                    itemMetadata.brandName,
                    itemMetadata.size,
                    itemMetadata.color,
                    itemMetadata.material,
                    itemMetadata.year,
                    creationTime
                )
                
                val insertedItemId = executeInsertAndGetKeys(sql, params) { rs ->
                    rs.getObject("id", UUID::class.java)
                }
                
                if (insertedItemId != null) {
                    itemIds.add(insertedItemId)
                } else {
                    throw SQLException("Failed to insert or update item metadata for item ID: ${rdsItemMetadata.itemId}")
                }
            }
            
            // Insert transfer with item IDs using executeInsertAndGetKeys
            val transferMetadataJson = objectMapper.writeValueAsString(transferMetadata)
            val transferSql = "INSERT INTO transfers (id, item_ids, metadata) VALUES (?, ?, ?::jsonb) ON CONFLICT (id) DO UPDATE SET item_ids = ?, metadata = ?::jsonb RETURNING id"

            if (transferMetadata != null) {
                // We need to create the array in the connection context
                connection.prepareStatement(transferSql, PreparedStatement.RETURN_GENERATED_KEYS).use { statement ->
                    statement.setObject(1, UUID.fromString(transferId))
                    statement.setArray(2, connection.createArrayOf("uuid", itemIds.toTypedArray()))
                    statement.setString(3, transferMetadataJson)
                    statement.setArray(4, connection.createArrayOf("uuid", itemIds.toTypedArray()))
                    statement.setString(5, transferMetadataJson)

                    statement.executeUpdate()

                    statement.generatedKeys.use { keys ->
                        if (keys.next()) {
                            return@executeTransaction
                        } else {
                            throw SQLException("Failed to insert or update transfer metadata for transfer ID: $transferId")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Updates ownership information for all items in a transfer
     * @param transferId The UUID of the transfer
     * @param ownerId The ID of the new owner
     * @param ownershipTime The timestamp when ownership was transferred
     * @return Number of items updated
     */
    fun updateTransferOwnership(
        transferId: String,
        ownerId: String,
        ownershipTime: Timestamp
    ): Int {
        logger.info { "Updating ownership for transfer ID: $transferId to owner: $ownerId at time: $ownershipTime" }
        
        return executeTransaction { connection ->
            // First, get the item IDs associated with this transfer
            val getItemIdsSql = "SELECT item_ids FROM transfers WHERE id = ?"
            val itemIds = mutableListOf<UUID>()
            
            connection.prepareStatement(getItemIdsSql).use { statement ->
                statement.setObject(1, transferId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        val itemIdsArray = resultSet.getArray("item_ids")
                        if (itemIdsArray != null) {
                            val itemIdsObjects = itemIdsArray.array as Array<*>
                            itemIdsObjects.forEach { itemIdObj ->
                                if (itemIdObj is UUID) {
                                    itemIds.add(itemIdObj)
                                }
                            }
                        }
                    } else {
                        logger.warn { "No transfer found with ID: $transferId" }
                        return@executeTransaction 0
                    }
                }
            }
            
            if (itemIds.isEmpty()) {
                logger.warn { "No items found for transfer ID: $transferId" }
                return@executeTransaction 0
            }
            
            // Update ownership for all items
            val updateSql = "UPDATE items SET owner = ?, ownership_time = ? WHERE id = ANY(?)"
            
            connection.prepareStatement(updateSql).use { statement ->
                statement.setString(1, ownerId)
                statement.setTimestamp(2, ownershipTime)
                statement.setArray(3, connection.createArrayOf("uuid", itemIds.toTypedArray()))
                
                val updatedRows = statement.executeUpdate()
                logger.info { "Updated ownership for $updatedRows items in transfer ID: $transferId" }
                return@executeTransaction updatedRows
            }
        }
    }
    
    /**
     * Lists all items owned by a specific user
     * @param ownerId The ID of the owner
     * @return List of items owned by the user
     */
    fun listItemsByOwnerId(ownerId: String): List<RdsItemMetadataSchema> {
        logger.info { "Listing items for owner ID: $ownerId" }
        val query = "SELECT * FROM items WHERE owner = ?"
        
        return executeQuery(query, listOf(ownerId)) { resultSet ->
            val id = resultSet.getObject("id", UUID::class.java)
            val name = resultSet.getString("name")
            val merchantId = resultSet.getString("merchant_id")
            val brandName = resultSet.getString("brand_name")
            val size = resultSet.getString("size")
            val color = resultSet.getString("color")
            val material = resultSet.getString("material")
            val year = resultSet.getString("year")
            
            RdsItemMetadataSchema(
                itemId = id,
                merchantId = merchantId,
                itemMetadata = ItemMetadata(
                    name = name,
                    brandName = brandName,
                    size = size,
                    color = color,
                    material = material,
                    year = year,
                    imageUrls = null,
                ),
                rawMetadata = null,
                imageS3ObjectKeys = resultSet.getArray("image_keys")?.let { array -> (array.array as? Array<*>)?.mapNotNull { it as? String } },
            )
        }
    }
}