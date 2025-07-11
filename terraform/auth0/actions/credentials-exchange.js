exports.onExecuteCredentialsExchange = async (event, api) => {
  try {
    const roles = event.client.metadata?.roles;
    console.log(`Setting roles claim: ${roles}`);
    const namespace = "https://zenobiapay.com/";
    api.accessToken.setCustomClaim(`${namespace}roles`, roles);

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
