package com.zenobiapay.itemmetadata.transform

import com.zenobiapay.rds.model.ItemMetadataSchema
import java.util.UUID

class ShopifySchemaTransformer: ItemSchemaTransformer {
    override fun transform(itemId: String, merchantId: String, metadata: Map<String, Any>): List<ItemMetadataSchema> {
        val quantity = metadata["quantity"] as Int
        return (1..quantity).map {
            ItemMetadataSchema(
                itemId = UUID.fromString(itemId),
                merchantId = merchantId,
                metadata = metadata,
                name = metadata["title"] as String,
                productId = null,
                brandId = null,
                tags = listOf(metadata["variant"] as String)
            )
        }
    }
}