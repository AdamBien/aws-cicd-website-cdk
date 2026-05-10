# AWS CDK Static Website Infrastructure

Two-region AWS CDK application deploying production-ready static websites with CloudFront CDN and automated CI/CD. ACM certificates run in `us-east-1` (CloudFront requirement); the content bucket and pipeline run in `eu-central-1`.

## Features

- CloudFront distribution with S3 origin secured via Origin Access Control (OAC)
- Optimized cache policy (`CACHING_OPTIMIZED`) for static assets
- ACM certificates in us-east-1 for CloudFront compatibility
- CodePipeline + CodeBuild CI/CD with GitHub CodeStar Connections
- Domain-driven stack naming (one stack triple per domain)
- External DNS provider support (Hover, GoDaddy, etc.)

## Prerequisites

- AWS CLI configured with credentials
- AWS CDK CLI installed (`npm install -g aws-cdk`)
- Java 25+
- Maven
- AWS account bootstrapped for CDK (see Bootstrap section)

## CDK Bootstrap

Before first deployment, bootstrap your AWS account for CDK. This creates the necessary resources (S3 bucket, IAM roles) for CDK deployments.

**Important**: This project pins regions in `airhacks.website.Stacks` (`us-east-1` for the certificate stack, `eu-central-1` for CloudFront and the pipeline). Both regions must be bootstrapped.

### Bootstrap both regions:
```bash
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
- `cicd-website-example-com-cloudfront` (eu-central-1)
- `cicd-website-example-com-codepipeline` (eu-central-1)

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
# Leave blank on first deploy; ACM emits these values during certificate creation.
# Copy them from the AWS Certificate Manager console (us-east-1) and paste back, then redeploy.
cert.validation.record.name=_xxx.example.com
cert.validation.domain.name=_xxx.acm-validations.aws.
```

## Deployment

The domain is always passed via CDK context:
```bash
cdk deploy --context domain=example.com
```

### Flow A — Route53 manages DNS (registrar at AWS or NS delegated):
1. `external.dns.provider=false` (default)
2. `cdk deploy --context domain=example.com`
3. CDK creates the hosted zone, ACM validation CNAME, and CloudFront alias records automatically.

### Flow B — DNS stays with an external provider (Hover, GoDaddy, ...):
Use this when the apex/www records remain at your provider and only the ACM validation CNAME is added there.
1. Set `external.dns.provider=true`
2. `cdk deploy --context domain=example.com` — deployment blocks waiting for cert validation
3. Copy the validation CNAME from ACM (us-east-1 console) into your DNS provider
4. Wait for ACM to validate
5. Add A/AAAA (or CNAME) records at your provider pointing to the CloudFront distribution domain (printed as `CloudFrontDistributionDomainNameOutput`)

### Flow C — External registrar, DNS delegated to Route53:
Use this when you keep the registrar (Hover, GoDaddy, ...) but want Route53 to be authoritative for DNS.
1. Set `external.dns.provider=true`
2. `cdk deploy --context domain=example.com` — deployment blocks waiting for cert validation
3. Copy the validation CNAME from ACM (us-east-1 console) into your provider's DNS *or* into the new Route53 hosted zone (the zone is created by `CloudFrontStack` in eu-central-1; Route53 itself is global)
4. Wait for ACM to validate
5. Read the four NS records from the new Route53 hosted zone and configure them at your registrar — DNS propagation up to 48h
6. Keep the validation CNAME permanently for automatic certificate renewal

## Project Structure

- `airhacks.CDKApp` — entry point, wires the three stacks
- `airhacks.website.Configuration` — typed configuration records
- `airhacks.website.Stacks` — region-pinned `StackProps` (us-east-1, eu-central-1)
- `airhacks.website.certificate.boundary.DomainCertificateStack` — ACM certificate (us-east-1)
- `airhacks.website.cloudfront.boundary.CloudFrontStack` — distribution + S3 origin (OAC)
- `airhacks.website.codebuild.boundary.CodePipelineStack` — CodePipeline + CodeBuild publishing stage
- `airhacks.website.route53.control.Route53` — hosted zone and alias records
- `airhacks.website.s3.control.Buckets` — website and artifact bucket factories
- `airhacks.website.configuration.control.ZCfg` — properties cascade loader

## Scripts

- `buildAndDeploy.sh` - Build and deploy the CDK application
- `buildAndDeployDontAsk.sh` - Deploy without confirmation
- `destroy.sh` - Tear down all stacks

## Notes

- **CloudFront Certificates**: ACM certificates used with CloudFront must be in us-east-1. `DomainCertificateStack` is pinned to us-east-1 for this reason.
- The deployment blocks until the ACM certificate is validated. With an external DNS provider, add the validation CNAME promptly or the stack will time out.
- Configuration precedence: system properties > `./app.properties` > `~/.aws-cicd-website-cdk/{domain}.properties`.
- Regions are pinned in `airhacks.website.Stacks` — change there if you need a different content/pipeline region.