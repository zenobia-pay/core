package com.zenobiapay.transfermetadata.transform

import com.zenobiapay.rds.model.ItemMetadataSchema

interface ItemSchemaTransformer {
    fun transform(merchantId: String, metadata: Map<String, Any>): List<ItemMetadataSchema>
}