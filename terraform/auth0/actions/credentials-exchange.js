exports.onExecuteCredentialsExchange = async (event, api) => {
  try {
    const userRole = event.client.metadata?.role;
    if (userRole) {
      console.log(`Found user role ${userRole}, adding to claims`);
      api.idToken.setCustomClaim("role", userRole);
      api.accessToken.setCustomClaim("role", userRole);
    } else {
      console.log("No user role found, skipping adding to claim");
    }

    const m2mSub = event.client.metadata?.merchantSub;
    if (m2mSub) {
      console.log(`Found merchant sub ${m2mSub}, adding to claims`);
      api.idToken.setCustomClaim("m2mSub", m2mSub);
      api.accessToken.setCustomClaim("m2mSub", m2mSub);
    } else {
      console.log("No user role found, skipping adding to claim");
    }
  } catch (err) {
    console.log(`Got err ${err}`)
  }
}
