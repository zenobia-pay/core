package com.zenobiapay.table.di

import com.zenobiapay.di.SAM_LOCAL
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI
import javax.inject.Named

const val BANK_TABLE_NAME = "BANK_TABLE_NAME"
const val TRANSFER_TABLE_NAME = "TRANSFER_TABLE_NAME"
const val USER_TABLE_NAME = "USER_TABLE_NAME"

@Module
class TableModule {
    @Provides
    @Named(BANK_TABLE_NAME)
    fun provideBankTableName(): String = System.getenv("BANK_TABLE_NAME")!!

    @Provides
    @Named(TRANSFER_TABLE_NAME)
    fun provideTransferTableName(): String = System.getenv("TRANSFER_TABLE_NAME")

    @Provides
    @Named(USER_TABLE_NAME)
    fun provideUserTableName(): String = System.getenv("USER_TABLE_NAME")

    @Provides
    fun provideDynamoDbClient(@Named(SAM_LOCAL) samLocal: String?): DynamoDbClient {
        val builder = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
        if (samLocal != null) {
            builder.endpointOverride(URI("http://dynamodb-local:8000"))
        }
        return builder.build()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(dynamoDbClient: DynamoDbClient) = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build()
}
