# AWS CDK Static Website Infrastructure

Multi-region AWS CDK application deploying production-ready static websites with CloudFront CDN and automated CI/CD.

## Features

- CloudFront distribution with optimized S3 origin and OAI
- ACM certificates in us-east-1 for CloudFront compatibility
- CodePipeline/CodeBuild CI/CD with GitHub CodeStar integration
- Multi-domain support with domain-specific configurations
- External DNS provider support (Hover, GoDaddy)

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

## Stack Naming Convention

CloudFormation stacks follow the pattern: `{appName}-{normalizedDomain}-{stackType}`

- `appName`: cicd-website
- `normalizedDomain`: Domain name with dots replaced by hyphens (e.g., example-com)
- `stackType`: certificate, cloudfront, or codepipeline

Example stack names for domain `example.com`:
- `cicd-website-example-com-certificate` (us-east-1)
- `cicd-website-example-com-cloudfront` (target region)
- `cicd-website-example-com-codepipeline` (target region)

## Configuration

Configuration files are loaded in this order (later sources override earlier ones):
1. `~/.aws-cicd-website-cdk/{domain}.properties` (global, domain-specific)
2. `./app.properties` (local, overwrites global)
3. System properties (highest priority)

Example paths for domain `example.com`:
- `~/.aws-cicd-website-cdk/example.com.properties`
- `./app.properties`

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
3. Add CNAME validation record from us-east-1 ACM to your DNS provider
4. Wait for certificate validation to complete
5. Add CloudFront distribution CNAME/A records to your DNS provider

### Using environment variable:
```bash
DOMAIN=example.com cdk deploy
```

## External DNS Provider Setup

When using providers like Hover, the CDK automatically creates:
- Route53 hosted zone for your domain
- A and AAAA alias records pointing to CloudFront

**Manual step required**: Add the ACM certificate validation CNAME record
- The certificate validation CNAME is created in us-east-1 region
- Find the validation record in AWS Certificate Manager console (us-east-1)
- Add this CNAME record to your external DNS provider within 72 hours
- Keep the record permanently for automatic certificate renewal

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