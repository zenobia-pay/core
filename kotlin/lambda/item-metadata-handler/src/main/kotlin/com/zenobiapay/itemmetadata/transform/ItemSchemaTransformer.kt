package com.zenobiapay.itemmetadata.transform

import com.zenobiapay.rds.model.ItemMetadataSchema

interface ItemSchemaTransformer {
    fun transform(itemId: String, merchantId: String, metadata: Map<String, Any>): List<ItemMetadataSchema>
}