package com.zenobiapay.item.util

import com.zenobiapay.item.di.ItemModule.Companion.IMAGE_STORAGE_BUCKET_NAME
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import javax.inject.Singleton

@Singleton
class S3UrlGenerator @Inject constructor(
    private val s3Client: S3Client,
    @Named(IMAGE_STORAGE_BUCKET_NAME) private val bucketName: String
) {
    /**
     * Generate presigned URLs for a list of S3 object keys
     * 
     * @param objectKeys List of S3 object keys
     * @param expirationMinutes Duration in minutes for which the presigned URL is valid (default: 15 minutes)
     * @return List of presigned URLs
     */
    fun generatePresignedUrls(objectKeys: List<String>?, expirationMinutes: Long = 15): List<String> {
        if (objectKeys.isNullOrEmpty()) {
            return emptyList()
        }
        
        return objectKeys.mapNotNull { objectKey ->
            try {
                generatePresignedUrl(objectKey, expirationMinutes)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Generate a presigned URL for an S3 object key
     * 
     * @param objectKey S3 object key
     * @param expirationMinutes Duration in minutes for which the presigned URL is valid
     * @return Presigned URL as a string
     */
    fun generatePresignedUrl(objectKey: String, expirationMinutes: Long = 15): String {
        S3Presigner.create().use { presigner ->
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()
            
            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build()
            
            return presigner.presignGetObject(presignRequest).url().toString()
        }
    }
}
