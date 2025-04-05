import org.gradle.kotlin.dsl.project

rootProject.name = "zenobiapay-backend"
include(
    "kotlin:shared",
    "kotlin:shared:api",
    "kotlin:shared:api:model",
    "kotlin:shared:orum",
    "kotlin:shared:plaid",
    "kotlin:shared:table",
    "kotlin:shared:cryptography",
    "kotlin:shared:table:bank",
    "kotlin:shared:table:transfer",
    "kotlin:shared:table:user",
    "kotlin:lambda:bank-handler",
    "kotlin:lambda:transfer-handler",
    "kotlin:lambda:user-handler",
    "kotlin:lambda:payout-handler",
    "kotlin:lambda:transfer-event-handler",
)
