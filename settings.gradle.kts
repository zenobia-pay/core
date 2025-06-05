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
    "kotlin:shared:table:rds",
    "kotlin:shared:table:bank",
    "kotlin:shared:table:transfer",
    "kotlin:shared:table:user",
    "kotlin:lambda:bank-handler",
    "kotlin:lambda:transfer-handler",
    "kotlin:lambda:item-handler",
    "kotlin:lambda:item-metadata-handler",
    "kotlin:lambda:user-handler",
    "kotlin:lambda:webhook-handler",
    "kotlin:lambda:payout-handler",
    "kotlin:lambda:transfer-event-handler",
)
include("kotlin:shared:table:credentials")
findProject(":kotlin:shared:table:credentials")?.name = "credentials"
include("kotlin:shared:events")
findProject(":kotlin:shared:events")?.name = "events"
include("kotlin:shared:metrics")
findProject(":kotlin:shared:metrics")?.name = "metrics"
include("kotlin:lambda:webhook-event-handler")
findProject(":kotlin:lambda:webhook-event-handler")?.name = "webhook-event-handler"
include("kotlin:shared:table:rds")
findProject(":kotlin:shared:table:rds")?.name = "rds"
include("kotlin:lambda:item-handler")
findProject(":kotlin:lambda:item-handler")?.name = "item-handler"
