exports.onExecuteCredentialsExchange = async (event, api) => {
  try {
    const userRole = event.client.metadata?.role;
    if (userRole) {
      if (userRole === "ADMIN") {
        const email = event.user?.email;
        if (!email || !email.endsWith('@zenobiapay.com')) {
          console.log(`Admin access denied for non-zenobiapay.com email: ${email}`);
          api.access.deny('Admin role requires a zenobiapay.com email address');
          return;
        }
      
        // Check if MFA (2FA) was completed during this authentication
        if (!event.authentication?.methods.some(method => method.name === "mfa")) {
          console.log(`Admin access denied for user without MFA: ${email}, triggering challenge`);
          // Trigger MFA challenge
          api.multifactor.enable("any", { allowRememberBrowser: false });
          return;
        }
        console.log(`Admin access granted for zenobiapay.com email: ${email}`);
      }
      
      console.log(`Found user role ${userRole}, adding to claims`);
      api.accessToken.setCustomClaim("role", userRole);
    } else {
      console.log("No user role found, skipping adding to claim");
    }

    const m2mSub = event.client.metadata?.merchantSub;
    if (m2mSub) {
      console.log(`Found merchant sub ${m2mSub}, adding to claims`);
      api.accessToken.setCustomClaim("m2mSub", m2mSub);
    } else {
      console.log("No m2m sub found, skipping adding to claim");
    }
  } catch (err) {
    console.log(`Got err ${err}`)
  }
}
