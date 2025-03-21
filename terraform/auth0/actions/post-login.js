
const CUSTOMER_ROLE = "customer"

exports.onExecutePostLogin = async (event, api) => {
    const audience = 'https://zenobiapay.com';

    if (event.stats.logins_count === 1) { // Run only on initial login
        await registerUser(event, api, audience)
        console.log("Setting custom claims manually for initial login")
        api.idToken.setCustomClaim(`${audience}/role`, CUSTOMER_ROLE);
        api.accessToken.setCustomClaim(`${audience}/role`, CUSTOMER_ROLE);
    } else {
        const userRole = event.user.app_metadata?.role;
        if (userRole) {
            console.log(`Found user role ${userRole}, adding to claims`)
            api.idToken.setCustomClaim(`${audience}/role`, userRole);
            api.accessToken.setCustomClaim(`${audience}/role`, userRole);
        } else {
            console.log("No user role found, skipping adding to claim")
        }
    }
};

async function registerUser(event, api, audience) {
    console.log('Initial login. Running set up.')
    const axios = require('axios');
    const clientId = event.secrets.CLIENT_ID;
    const clientSecret = event.secrets.CLIENT_SECRET;
    const auth0Domain = event.secrets.AUTH0_DOMAIN;
    const tokenUrl = `https://${auth0Domain}/oauth/token`;
    const zenobiaEndpoint = event.secrets.ZENOBIA_ENDPOINT

    try {
        console.log('Adding user to customer role')
        api.user.setAppMetadata("role", CUSTOMER_ROLE);

        console.log(`Authenticating oauth to call /register-user using token url ${tokenUrl}`)
        const tokenResponse = await axios.post(tokenUrl, {
            client_id: clientId,
            client_secret: clientSecret,
            audience: audience,
            grant_type: 'client_credentials'
        }, {
            headers: { 'Content-Type': 'application/json' }
        });
        console.log("Got token response")

        const accessToken = tokenResponse.data.access_token;
        console.log(`Registering user using endpoint ${zenobiaEndpoint}`)

        await axios.post(`${zenobiaEndpoint}register-user`, {
            sub: event.user.user_id,
            email: event.user.email,
            firstName: event.user.given_name,
            lastName: event.user.family_name
        },
        {
            headers: {
                Authorization: "Bearer " + accessToken
            }
        }
        );
        console.log("Successfully called zenobia /register-user")
    } catch (err) {
        console.error('API call failed', err);
        // Optionally fail the registration or just log
    }
}
