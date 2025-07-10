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
    
    const roles = event.authorization?.roles || [];
    console.log(`User has the following roles: ${JSON.stringify(roles)}`);
    
    let returnedRoles = roles;
    if (roles.includes('Admin')) {
      if (event.client.name !== "Zenobia Admin") {
        console.log("Skipping admin role for non-admin client");
        returnedRoles = roles.filter(role => role !== 'Admin');
      }
    }

    console.log(`Setting roles claim: ${JSON.stringify(returnedRoles)}`);
    
    api.idToken.setCustomClaim("roles", returnedRoles);
    api.accessToken.setCustomClaim("roles", returnedRoles);

    // Setting for backwards compatibility
    const userRole = event.user.app_metadata?.role;
    if (userRole) {
      // Deny access if the role is ADMIN and this is not the admin client
      if (userRole === "ADMIN" && event.client.name !== "Zenobia Admin") {
        console.log("Access denied: ADMIN users must use the admin client");
        api.access.deny('ADMIN_REQUIRES_ADMIN_CLIENT');
        return;
      }
      
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
