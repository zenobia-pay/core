package com.zenobiapay.model.ddb.transfer

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.LocalDate

@DynamoDbBean
data class PayoutItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    @get:DynamoDbSortKey var sk: String = "",
    var amount: Int = 0,
    var data: PayoutData? = null
) {
    companion object {
        fun generatePk(merchantId: String) = "PAYOUT#m_$merchantId"
        fun generateSk(date: LocalDate) = date.toString() // yyyy-MM-dd
        fun generateSk(date: String) = date
    }
}

@DynamoDbBean
data class PayoutData(
    var merchantPaid: Boolean = false,
    var merchantAmount: Int? = null,
    var merchantPayoutId: PayoutId? = null,
    var feePaid: Boolean = false,
    var feeAmount: Int? = null,
    var feePayoutId: PayoutId? = null
)

@DynamoDbBean
data class PayoutId(
    var type: String = "Orum",
    var id: String? = null
)
