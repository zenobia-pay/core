exports.onExecutePostLogin = async (event, api) => {
  try {
    const emailVerified = event.user.email_verified;
    if (!emailVerified) {
      console.log("Access denied due to email not being verified")
      api.access.deny('Access denied', 'Please verify your email before logging in.');
    }

    console.log(`Setting email claim: ${event.user.email}`)
    api.idToken.setCustomClaim("email", event.user.email)
    api.accessToken.setCustomClaim("email", event.user.email)
    const userRole = event.user.app_metadata?.role;
    if (userRole) {
      console.log(`Found user role ${userRole}, adding to claims`);
      api.idToken.setCustomClaim("role", userRole);
      api.accessToken.setCustomClaim("role", userRole);
    } else {
      console.log("No user role found, skipping adding to claim");
    }
  } catch (err) {
    console.log("Got err: " + err);
  }
};
