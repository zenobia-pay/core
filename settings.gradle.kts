import org.gradle.kotlin.dsl.project

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "zenobiapay-backend"
include(
    "kotlin:shared",
    "kotlin:shared:api",
    "kotlin:shared:api:model",
    "kotlin:shared:orum",
    "kotlin:shared:plaid",
    "kotlin:shared:table",
    "kotlin:shared:webhook",
    "kotlin:shared:cryptography",
    "kotlin:shared:table:bank",
    "kotlin:shared:table:transfer",
    "kotlin:shared:table:user",
    "kotlin:lambda:bank-handler",
    "kotlin:lambda:transfer-handler",
    "kotlin:lambda:user-handler",
    "kotlin:lambda:webhook-handler",
    "kotlin:lambda:payout-handler",
    "kotlin:lambda:transfer-event-handler",
)
