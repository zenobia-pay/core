package com.zenobiapay.transfermetadata.transform

import com.zenobiapay.rds.model.ItemMetadataSchema

class ShopifySchemaTransformer: ItemSchemaTransformer {
    override fun transform(merchantId: String, metadata: Map<String, Any>): List<ItemMetadataSchema> {
        val quantity = metadata["quantity"] as Int
        return (1..quantity).map {
            ItemMetadataSchema(
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