package com.zenobiapay.webhook.util

import org.apache.commons.net.util.SubnetUtils
import java.net.InetAddress
import java.net.URL

private val blockedSubnets = listOf(
    SubnetUtils("10.0.3.0/24"),          // Your private subnet 1
    SubnetUtils("10.0.4.0/24"),          // Your private subnet 2
    SubnetUtils("127.0.0.0/8"),          // Loopback
    SubnetUtils("169.254.169.254/32"),   // AWS metadata IP
    SubnetUtils("169.254.0.0/16"),       // Link-local
    SubnetUtils("fc00::/7"),             // IPv6 private
    SubnetUtils("fe80::/10"),            // IPv6 link-local
    SubnetUtils("::1/128")               // IPv6 loopback
).onEach { it.isInclusiveHostCount = true }

fun isValidWebhook(webhookUrl: String): Boolean {
    return try {
        val url = URL(webhookUrl)

        if (url.protocol != "https") return false
        val host = url.host

        // Block obvious local hosts
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return false

        val addresses = InetAddress.getAllByName(host)
        for (address in addresses) {
            if (isBlocked(address)) {
                return false
            }
        }

        true
    } catch (e: Exception) {
        false
    }
}

private fun isBlocked(ip: InetAddress): Boolean {
    val ipStr = ip.hostAddress
    return blockedSubnets.any { subnet ->
        try {
            subnet.info.isInRange(ipStr)
        } catch (e: Exception) {
            false
        }
    }
}
