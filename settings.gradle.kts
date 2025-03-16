rootProject.name = "zenobiapay-backend"
include(
    "kotlin:shared",
    "kotlin:shared:api",
    "kotlin:shared:orum",
    "kotlin:shared:table:transfer",
    "kotlin:lambda:bank",
    "kotlin:lambda:transfer",
    "kotlin:lambda:user",
    "kotlin:lambda:payout",
    "kotlin:lambda:cognito",
    "kotlin:lambda:transfer-table-event"
)
