package com.zenobiapay.item.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.item.di.ItemModule.Companion.RESALE_SERVICE_ENDPOINT
import com.zenobiapay.item.di.ItemModule.Companion.RESALE_SIGNING_SECRET
import com.zenobiapay.rds.model.RdsItemMetadataSchema
import com.zenobiapay.rds.util.RdsWrapper
import com.zenobiapay.table.util.signHmacSha256
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val logger = KotlinLogging.logger {}

class ResaleUtil @Inject constructor(
    private val objectMapper: ObjectMapper,
    @Named(RESALE_SERVICE_ENDPOINT) private val resaleServiceEndpoint: String,
    @Named(RESALE_SIGNING_SECRET) private val resaleSigningSecret: String,
    private val rdsWrapper: RdsWrapper,
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    /**
     * Creates a listing on the Depop marketplace for the given item
     * @param item The item to list on Depop
     * @return true if the listing was created successfully, false otherwise
     */
    fun createDepopListing(
        item: RdsItemMetadataSchema,
        itemImageUrls: List<String>,
        price: Int,
        category: String,
        shippingAddress: String,
        condition: String,
    ): Boolean {
        try {
            logger.info { "Creating Depop listing for item ${item.itemId}" }
            
            // Build the listing payload based on the item metadata
            val listingPayload = mapOf(
                "title" to (item.itemMetadata.name),
                "description" to "Quality item from ${item.itemMetadata.brandName ?: "Unknown Brand"}. " +
                        "Size: ${item.itemMetadata.size ?: "Standard"}, " +
                        "Color: ${item.itemMetadata.color ?: "Various"}, " +
                        "Material: ${item.itemMetadata.material ?: "Unknown"}, " +
                        "Year: ${item.itemMetadata.year ?: "Unknown"}",
                "price" to price / 100.0,
                "category" to category,
                "brand" to (item.itemMetadata.brandName ?: "Unknown"),
                "size" to (item.itemMetadata.size ?: "Standard"),
                "condition" to condition,
                "photos" to (itemImageUrls),
                "shipping_address" to shippingAddress,
                "color" to (item.itemMetadata.color ?: "Various"),
                "age" to (item.itemMetadata.year ?: "Unknown"),
            )
            
            // Convert payload to JSON
            val jsonPayload = objectMapper.writeValueAsString(listingPayload)
            
            // Generate signature for the request
            val signature = signHmacSha256(jsonPayload, resaleSigningSecret)
            
            // Create HTTP request
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://$resaleServiceEndpoint/api/depop/listings"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()
            
            // Send the request
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val responseBody = objectMapper.readTree(response.body())
            val id = responseBody.get("id").asText()
            logger.info { "Setting job id as $id" }
            rdsWrapper.updateItemResaleJobId(item.itemId, id)

            // Check if the request was successful
            val isSuccess = response.statusCode() in 200..299
            if (isSuccess) {
                logger.info { "Successfully created Depop listing for item ${item.itemId}" }
            } else {
                logger.error { "Failed to create Depop listing for item ${item.itemId}. Status: ${response.statusCode()}, Body: ${response.body()}" }
            }
            
            return isSuccess
        } catch (e: Exception) {
            logger.error(e) { "Error creating Depop listing for item ${item.itemId}" }
            return false
        }
    }
}
