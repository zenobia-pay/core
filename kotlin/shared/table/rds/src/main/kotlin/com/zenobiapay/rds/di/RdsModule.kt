package com.zenobiapay.rds.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.rds.model.MetadataTableSecret
import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class RdsModule {

    companion object {
        // Named constants for dependency injection
        const val PG_HOST = "PG_HOST"
        const val PG_PORT = "PG_PORT"
        const val PG_DATABASE = "PG_DATABASE"
        const val PG_USER = "PG_USER"
        const val PG_PASSWORD = "PG_PASSWORD"
        const val PG_JDBC_URL = "PG_JDBC_URL"

        // Environment variable names
        private const val ENV_METADATA_DB_PROXY_ENDPOINT = "METADATA_DB_PROXY_ENDPOINT"
        private const val ENV_METADATA_DB_NAME = "METADATA_DB_NAME"
        private const val ENV_METADATA_DB_SECRET_ARN = "METADATA_DB_SECRET_ARN"
        
        // Default values
        private const val DEFAULT_PG_PORT = "5432"
        private const val DEFAULT_PG_USER = "dbadmin"
    }

    @Provides
    @Named(PG_HOST)
    fun providePgHost(): String {
        return System.getenv(ENV_METADATA_DB_PROXY_ENDPOINT)!!
    }

    @Provides
    @Named(PG_PORT)
    fun providePgPort(): String {
        return DEFAULT_PG_PORT
    }

    @Provides
    @Named(PG_DATABASE)
    fun providePgDatabase(): String {
        return System.getenv(ENV_METADATA_DB_NAME)!!
    }

    @Provides
    @Named(PG_USER)
    fun providePgUser(): String {
        return DEFAULT_PG_USER
    }

    @Provides
    @Named(PG_PASSWORD)
    fun providePgPassword(objectMapper: ObjectMapper, secretsManager: SecretsManagerClient): String {
        val secretArn = System.getenv(ENV_METADATA_DB_SECRET_ARN)!!

        val secretValue = secretsManager.getSecretValue {
            it.secretId(secretArn)
        }
        return objectMapper.readValue(secretValue.secretString(), MetadataTableSecret::class.java).password
    }

    @Provides
    @Named(PG_JDBC_URL)
    fun provideJdbcUrl(
        @Named(PG_HOST) host: String,
        @Named(PG_PORT) port: String,
        @Named(PG_DATABASE) database: String
    ): String {
        return "jdbc:postgresql://$host:$port/$database"
    }
}