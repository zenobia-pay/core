package com.zenobiapay.itemmetadata.transform

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.itemmetadata.di.METADATA_TRANSFORMER_LAMBDA_NAME
import com.zenobiapay.rds.model.ItemMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

class ItemSchemaTransformer @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val lambdaClient: LambdaClient,
    @Named(METADATA_TRANSFORMER_LAMBDA_NAME) private val metadataTransformerLambdaName: String,
) {
    fun transform(metadata: Map<String, Any>): List<ItemMetadata> {
        try {
            if (metadata.isEmpty()) {
                logger.info { "Metadata is empty, skipping transformation" }
                return emptyList()
            }
            val metadataJson = objectMapper.writeValueAsString(metadata)
            val invokeRequest = InvokeRequest.builder()
                .functionName(metadataTransformerLambdaName)
                .payload(SdkBytes.fromString(metadataJson, StandardCharsets.UTF_8))
                .build()
            val response = lambdaClient.invoke(invokeRequest)

            // Check for errors
            if (response.functionError() != null) {
                val errorMessage = String(response.payload().asByteArray(), StandardCharsets.UTF_8)
                logger.error { "Error invoking metadata-transformer Lambda: $errorMessage" }
                throw RuntimeException("Failed to transform metadata: $errorMessage")
            }

            // Parse the response and extract the transformedMetadata key
            val responseString = String(response.payload().asByteArray(), StandardCharsets.UTF_8)
            logger.info { "Got returned item ${responseString}" }
            val responseJson = objectMapper.readTree(responseString)
            val transformedMetadataJson = responseJson.get("body")?.let { objectMapper.readTree(it.asText()) }?.get("transformedMetadata")?.toString()
                ?: throw RuntimeException("Response does not contain 'body.transformedMetadata' key")
            
            // Read the ItemMetadataSchema from the transformedMetadata
            val itemsMetadata = objectMapper.readValue<ItemMetadata>(transformedMetadataJson, ItemMetadata::class.java)
            
            logger.info { "Successfully transformed metadata into ${itemsMetadata.size} items" }
            return listOf(itemsMetadata)
        } catch (e: Exception) {
            logger.error(e) { "Failed to transform metadata" }
            throw RuntimeException("Failed to transform metadata", e)
        }
    }
}