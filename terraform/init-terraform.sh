# Need to run aws sso login with the deployment account to use
terraform init \
  -backend-config="bucket=zenobia-terraform-bucket" \
  -backend-config="key=envs/beta/terraform.tfstate" \
  -backend-config="region=us-east-1" \
  -reconfigure

echo "Call terraform apply to deploy to beta!"
