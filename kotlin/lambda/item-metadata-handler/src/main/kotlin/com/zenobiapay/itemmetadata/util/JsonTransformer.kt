package com.zenobiapay.itemmetadata.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.zenobiapay.itemmetadata.di.METADATA_TRANSFORMER_LAMBDA_NAME
import com.zenobiapay.rds.model.ItemMetadataSchema
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.model.LambdaResponse
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

