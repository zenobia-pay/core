package com.zenobiapay.model.api.transfer

import com.zenobiapay.model.api.ApiResponse

data class CreateTransferRequestResponse(val transferRequestId: String, val debtorId: String): ApiResponse
