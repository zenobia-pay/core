const { ManagementClient } = require("auth0");

const CUSTOMER_ROLE = "customer";
const MERCHANT_ROLE = "merchant";

exports.onExecutePostLogin = async (event, api) => {
  const audience = "https://dashboard.zenobiapay.com";
  const merchantClientId = event.secrets.merchantClientId;

  try {
    if (event.stats.logins_count === 1) {
      // Run only on initial login
      console.log("Initial login. Running set up.");
      const role =
        event.client.id === merchantClientId ? MERCHANT_ROLE : CUSTOMER_ROLE;
      console.log(`Setting app metadata ${role}`);
      api.user.setAppMetadata("role", role);

      // TODO: client id are both returning undefined, fix
      console.log(
        `Got role=${role} from clientId ${event.client.id}, expected merchant client id ${merchantClientId}`
      );

      await setupAuth0Configuration(event, role);
      await registerUser(event, api, audience, role);

      console.log("Setting custom claims manually for initial login");
      api.idToken.setCustomClaim("role", role);
      api.accessToken.setCustomClaim("role", role);
    } else {
      const userRole = event.user.app_metadata?.role;
      if (userRole) {
        console.log(`Found user role ${userRole}, adding to claims`);
        api.idToken.setCustomClaim("role", userRole);
        api.accessToken.setCustomClaim("role", userRole);
      } else {
        console.log("No user role found, skipping adding to claim");
      }
    }
  } catch (err) {
    console.log("Got err: " + err);
  }
};

async function setupAuth0Configuration(event, role) {
  const axios = require("axios");
  const auth0Domain = event.secrets.AUTH0_DOMAIN;
  const auth0ClientId = event.secrets.AUTH0_CLIENT_ID;
  const auth0Secret = event.secrets.AUTH0_CLIENT_SECRET;

  console.log("Fetching auth0 token");
  const managementApiToken = await axios
    .post(`https://${auth0Domain}/oauth/token`, {
      grant_type: "client_credentials",
      client_id: auth0ClientId,
      client_secret: auth0Secret,
      audience: `https://${auth0Domain}/api/v2/`,
    })
    .then((response) => response.data.access_token);
  setRole(event, role, managementApiToken);

  if (role == MERCHANT_ROLE) {
    const management = new ManagementClient({
      token: managementApiToken,
      domain: event.secrets.AUTH0_DOMAIN,
    });
    const org_id = await createOrganization(event, management);
    addUserToOrganization(event.user.user_id, org_id, management);
  }
}

async function setRole(event, role, managementApiToken) {
  console.log(`Adding user to role ${role}`);
  const axios = require("axios");
  const auth0Domain = event.secrets.AUTH0_DOMAIN;

  // Add role to user
  const userId = event.user.user_id; // Auth0 user ID
  const roleId = getRoleId(event, role);
  console.log(`Setting role ${role} with id ${roleId} for user ${userId}`);
  await axios.post(
    `https://${auth0Domain}/api/v2/users/${userId}/roles`,
    {
      roles: [roleId],
    },
    {
      headers: {
        Authorization: `Bearer ${managementApiToken}`,
      },
    }
  );
}

function getRoleId(event, roleName) {
  if (roleName == CUSTOMER_ROLE) {
    return event.secrets.CUSTOMER_ROLE_ID;
  } else if (roleName == MERCHANT_ROLE) {
    return event.secrets.MERCHANT_ROLE_ID;
  }
}

async function createOrganization(event, management) {
  console.log("Creating organization");

  try {
    // Create an organization
    const organization = await management.organizations.create({
      name: `org-${event.user.user_id.replace(/[^a-zA-Z0-9]/g, "_")}`,
      display_name: `${event.user.email} Organization`,
      metadata: {
        custom_field: "value",
      },
    });

    console.log("Organization created:", organization);
    return organization.data.id;
  } catch (error) {
    console.error("Error creating organization:", error);
  }
}

async function addUserToOrganization(user_id, org_id, management) {
  console.log(`Adding user ${user_id} to organization ${org_id}`);
  await management.organizations.addMembers(
    { id: org_id },
    { members: [user_id] }
  );
}

async function registerUser(event, api, audience) {
  const axios = require("axios");
  const clientId = event.secrets.CLIENT_ID;
  const clientSecret = event.secrets.CLIENT_SECRET;
  const auth0Domain = event.secrets.AUTH0_DOMAIN;
  const tokenUrl = `https://${auth0Domain}/oauth/token`;
  const zenobiaEndpoint = event.secrets.ZENOBIA_ENDPOINT;

  console.log(
    `Authenticating oauth to call /register-user using token url ${tokenUrl}`
  );
  const tokenResponse = await axios.post(
    tokenUrl,
    {
      client_id: clientId,
      client_secret: clientSecret,
      audience: audience,
      grant_type: "client_credentials",
    },
    {
      headers: { "Content-Type": "application/json" },
    }
  );
  console.log("Got token response");

  const accessToken = tokenResponse.data.access_token;
  console.log(`Registering user using endpoint ${zenobiaEndpoint}`);

  await axios.post(
    `${zenobiaEndpoint}register-user`,
    {
      sub: event.user.user_id,
      email: event.user.email,
      firstName: event.user.given_name,
      lastName: event.user.family_name,
    },
    {
      headers: {
        Authorization: "Bearer " + accessToken,
      },
    }
  );
  console.log("Successfully called zenobia /register-user");
}
