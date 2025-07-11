exports.onExecutePostLogin = async (event, api) => {
  try {
    const emailVerified = event.user.email_verified;
    if (!emailVerified) {
      console.log("Access denied due to email not being verified")
      api.access.deny('EMAIL_NOT_VERIFIED');
    }
    console.log(`Setting email claim: ${event.user.email}`)
    api.idToken.setCustomClaim("email", event.user.email)
    api.accessToken.setCustomClaim("email", event.user.email)
    
    const roles = event.user.app_metadata?.roles || [];
    console.log(`User has the following roles: ${roles}`);
    
    let returnedRoles = roles.split(",");
    if (roles.includes('ADMIN')) {
      if (event.client.name !== "Zenobia Admin") {
        console.log("Skipping admin role for non-admin client");
        returnedRoles = roles.split(",").filter(role => role !== 'ADMIN');
      }
    }

    console.log(`Setting roles claim: ${returnedRoles.join(",")}`);
    const namespace = "https://zenobiapay.com/";
    api.idToken.setCustomClaim(`${namespace}roles`, returnedRoles.join(","));
    api.accessToken.setCustomClaim(`${namespace}roles`, returnedRoles.join(","));
  } catch (err) {
    console.log("Got err: " + err);
  }
};
