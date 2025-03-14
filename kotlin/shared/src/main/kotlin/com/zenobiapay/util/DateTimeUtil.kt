package com.zenobiapay.util

import java.time.LocalDate
import java.time.ZoneOffset

fun getUtcDate() = LocalDate.now(ZoneOffset.UTC)
