package com.zenobiapay.item.util

import com.zenobiapay.item.di.ItemModule.Companion.IMAGE_STORAGE_BUCKET_NAME
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import javax.inject.Singleton

@Singleton
class S3UrlGenerator @Inject constructor(
    private val s3Client: S3Client,
    @Named(IMAGE_STORAGE_BUCKET_NAME) private val bucketName: String
) {

    companion object {
        fun generateCustomerImagePrefix(itemId: String, sellJobId: String): String {
            return "item/$itemId/sellJob/$sellJobId"
        }
    }

    /**
     * List objects with a given prefix in the S3 bucket and generate presigned URLs for them
     * 
     * @param prefix The S3 object key prefix to list objects for
     * @param expirationMinutes Duration in minutes for which the presigned URLs are valid
     * @return List of presigned URLs for objects with the given prefix
     */
    fun generatePresignedUrlForS3Prefix(prefix: String, expirationMinutes: Long = 15): List<String> {
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .build()
        
        val response = s3Client.listObjectsV2(request)
        
        if (response.hasContents()) {
            val objectKeys = response.contents().map { it.key() }
            return generatePresignedUrls(objectKeys, expirationMinutes)
        }
        
        return emptyList()
    }
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
    
    /**
     * Generate a presigned PUT URL for uploading to an S3 object key
     * 
     * @param objectKey S3 object key
     * @param expirationMinutes Duration in minutes for which the presigned URL is valid
     * @return Presigned PUT URL as a string
     */
    fun generatePresignedPutUrl(objectKey: String, expirationMinutes: Long = 15): String {
        S3Presigner.create().use { presigner ->
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()
            
            val presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build()
            
            return presigner.presignPutObject(presignRequest).url().toString()
        }
    }
}
