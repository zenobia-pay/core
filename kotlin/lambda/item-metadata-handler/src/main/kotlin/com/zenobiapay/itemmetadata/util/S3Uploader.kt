package com.zenobiapay.itemmetadata.util

import com.zenobiapay.itemmetadata.di.ITEM_STORAGE_BUCKET_NAME
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import jakarta.inject.Named

private val logger = KotlinLogging.logger {}

@Singleton
class S3Uploader @Inject constructor(
    private val s3Client: S3Client,
    private val httpClient: OkHttpClient,
    @Named(ITEM_STORAGE_BUCKET_NAME) private val bucketName: String
) {
    /**
     * Downloads an image from a URL and uploads it to S3
     *
     * @param imageUrl The URL of the image to download
     * @param prefix Optional prefix for the S3 object key (e.g., "users/123/")
     * @return The S3 object key of the uploaded image or null if upload failed
     */
    fun uploadImageFromUrl(imageUrl: String, prefix: String = ""): String? {
        logger.info { "Downloading image from $imageUrl and uploading to S3 bucket $bucketName" }

        try {
            // Create a unique filename for the image
            val fileExtension = getFileExtensionFromUrl(imageUrl)
            val objectKey = "${prefix}/${UUID.randomUUID()}$fileExtension"
            logger.info { "Generated objectKey $objectKey" }
            
            // Download the image
            val request = Request.Builder()
                .url(imageUrl)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error { "Failed to download image from $imageUrl: ${response.code}" }
                    return null
                }

                response.body?.let { body ->
                    // Upload the image to S3
                    val contentType = response.header("Content-Type") ?: "application/octet-stream"
                    val putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType)
                        .build()

                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(body.bytes()))
                    logger.info { "Successfully uploaded image to S3: $objectKey" }
                    return objectKey
                }
            }

            logger.error { "Response body was null when downloading image from $imageUrl" }
            return null
        } catch (e: IOException) {
            logger.error(e) { "Error downloading or uploading image from $imageUrl" }
            return null
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error processing image from $imageUrl" }
            return null
        }
    }

    /**
     * Extracts the file extension from a URL, removing any query parameters
     */
    private fun getFileExtensionFromUrl(url: String): String {
        // Remove query parameters (anything after '?')
        val urlWithoutQuery = url.substringBefore('?')
        val lastPathSegment = urlWithoutQuery.substringAfterLast('/')
        return if (lastPathSegment.contains('.')) {
            ".${lastPathSegment.substringAfterLast('.')}"
        } else {
            // Default to .jpg if no extension is found
            ".jpg"
        }
    }

    /**
     * Generates the full S3 URL for an object
     *
     * @param objectKey The S3 object key
     * @param region The AWS region (default: us-east-1)
     * @return The full S3 URL
     */
    fun getS3Url(objectKey: String, region: String = "us-east-1"): String {
        return "https://$bucketName.s3.$region.amazonaws.com/$objectKey"
    }
    
    /**
     * Generates the full S3 URL for an object in a specific bucket
     *
     * @param customBucketName The S3 bucket name
     * @param objectKey The S3 object key
     * @param region The AWS region (default: us-east-1)
     * @return The full S3 URL
     */
    fun getS3UrlForBucket(customBucketName: String, objectKey: String, region: String = "us-east-1"): String {
        return "https://$customBucketName.s3.$region.amazonaws.com/$objectKey"
    }
}
