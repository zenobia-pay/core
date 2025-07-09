/**
 * Handler that will be called during the execution of a PostLogin flow.
 *
 * @param {Event} event - Details about the user and the context in which they are logging in.
 * @param {PostLoginAPI} api - Interface whose methods can be used to change the behavior of the login.
 */
exports.onExecutePostLogin = async (event, api) => {
  try {
    const isAdminApp = event.client && event.client.metadata && event.client.metadata.role === "ADMIN";
    
    if (isAdminApp) {
      const hasUserRole = event.client.metadata?.role == "ADMIN";
      
      if (!hasUserRole) {
        console.log(`Admin access denied for user without ADMIN role: ${event.user.email}`);
        api.access.deny("Access denied. You must have ADMIN role to access this application.");
        return;
      }
      
      const email = event.user.email;
      if (!email || !email.endsWith('@zenobiapay.com')) {
        console.log(`Admin access denied for non-zenobiapay.com email: ${email}`);
        api.access.deny("Access denied. Admin access requires a zenobiapay.com email address.");
        return;
      }
      
      // Check if MFA (2FA) was completed during this authentication
      if (!event.authentication?.methods.some(method => method.name === "mfa")) {
        console.log(`Admin access denied for user without MFA: ${email}`);
        // Trigger MFA challenge
        api.multifactor.enable("any", { allowRememberBrowser: false });
        return;
      }
      
      console.log(`Admin access granted for user with MFA: ${email}`);
    }
  } catch (err) {
    console.log(`Got error during admin authorization: ${err}`);
  }
};
