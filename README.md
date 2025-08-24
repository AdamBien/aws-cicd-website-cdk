# AWS CDK Static Website Infrastructure

AWS CDK application for deploying static websites with CloudFront, S3, and CI/CD pipeline.

## Features

- CloudFront distribution with S3 origin
- ACM certificate with DNS validation  
- CodeBuild CI/CD pipeline with GitHub integration
- Support for external DNS providers (Hover, GoDaddy, etc.)
- Domain-specific configuration files

## Prerequisites

- AWS CLI configured with credentials
- AWS CDK CLI installed (`npm install -g aws-cdk`)
- Java 21+
- Maven
- AWS account bootstrapped for CDK (see Bootstrap section)

## CDK Bootstrap

Before first deployment, bootstrap your AWS account for CDK. This creates the necessary resources (S3 bucket, IAM roles) for CDK deployments.

**Important**: This project creates certificates for CloudFront, which must be in the us-east-1 region. Always bootstrap us-east-1 regardless of your primary region.

### Bootstrap for CloudFront (requires us-east-1):
```bash
# Bootstrap both your main region and us-east-1
cdk bootstrap aws://ACCOUNT-NUMBER/us-east-1
cdk bootstrap aws://ACCOUNT-NUMBER/eu-central-1
```


### Check bootstrap status:
```bash
aws cloudformation describe-stacks --stack-name CDKToolkit --region us-east-1
```

## Configuration

Create a configuration file in one of these locations:
- `~/.aws-website-cdk/configuration.properties` (user directory)
- `./configuration.properties` (project directory)

For domain-specific configurations:
- `~/.aws-website-cdk/configuration-example.com.properties`
- `./configuration-example.com.properties`

### Configuration Properties

```properties
# Domain configuration
domain.name=example.com

# External DNS provider (default: false)
# Set to true if using Hover, GoDaddy, etc.
external.dns.provider=false

# GitHub integration
codestar.connection.arn=arn:aws:codestar-connections:region:account:connection/xxx
git.owner=your-github-username
git.repository=your-repo-name
git.branch=main

# Certificate validation (for external DNS)
cert.validation.record.name=_xxx.example.com
cert.validation.domain.name=_xxx.acm-validations.aws.
```

## Deployment

### Standard deployment (Route53 DNS):
```bash
cdk deploy --context domain=example.com
```

### With external DNS provider:
1. Set `external.dns.provider=true` in configuration
2. Deploy: `cdk deploy --context domain=example.com`
3. Add CNAME validation record to your DNS provider
4. Wait for certificate validation to complete
5. Add CloudFront distribution CNAME/A records to your DNS provider

### Using environment variable:
```bash
DOMAIN=example.com cdk deploy
```

## External DNS Provider Setup

When using providers like Hover:

1. **Certificate Validation**: Add the ACM validation CNAME record
   - Find records in AWS Certificate Manager console
   - Add to your DNS provider within 72 hours
   - Keep the record for automatic renewal

2. **CloudFront Distribution**: After deployment completes
   - Add A/AAAA records or CNAME pointing to CloudFront distribution
   - Configure www subdomain if needed

## Project Structure

- `CDKApp` - Main application entry point
- `Configuration` - Configuration management
- `DomainCertificateStack` - ACM certificate creation (us-east-1)
- `CloudFrontStack` - CloudFront distribution and S3 bucket
- `CodeBuildStack` - CI/CD pipeline for automatic deployments

## Scripts

- `buildAndDeploy.sh` - Build and deploy the CDK application
- `buildAndDeployDontAsk.sh` - Deploy without confirmation
- `destroy.sh` - Tear down all stacks

## Notes

- **CloudFront Certificates**: ACM certificates used with CloudFront distributions must be created in the us-east-1 region. The `DomainCertificateStack` automatically deploys to us-east-1 for this reason
- Stack deployment waits for DNS validation when using external providers
- Configuration files in user directory take precedence over project directory
- The certificate stack is deployed separately in us-east-1 while other resources can be in your preferred region