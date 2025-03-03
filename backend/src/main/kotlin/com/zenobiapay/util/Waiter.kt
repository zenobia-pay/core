package com.zenobiapay.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

class WaiterFailedException(e: String): Exception(e)

fun <T> waitUntilCondition(
    timeout: Duration,
    sleep: Duration,
    successCondition: (T) -> Boolean,
    failCondition: (T) -> Boolean,
    block: () -> T
): T {
    return runBlocking {
        return@runBlocking withTimeout(timeout = timeout) {
            while (true) {
                val response = block()
                if (successCondition(response)) {
                    return@withTimeout response
                }

                if (failCondition(response)) {
                    break
                }
                delay(sleep)
            }
            throw WaiterFailedException("Fail condition matched")
        }
    }
}
