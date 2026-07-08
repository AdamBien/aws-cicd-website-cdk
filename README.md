# AWS CDK Static Website Infrastructure

Two-region AWS CDK application deploying production-ready static websites with CloudFront CDN and automated CI/CD. ACM certificates run in `us-east-1` (CloudFront requirement); the content bucket and pipeline run in `eu-central-1`.

## Features

- CloudFront distribution with S3 origin secured via Origin Access Control (OAC)
- Optimized cache policy (`CACHING_OPTIMIZED`) for static assets
- Automatic CloudFront cache invalidation on every pipeline run
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

- `appName`: `aws-cicd-website-cdk` (derived in `CDKApp` as `aws-{shortName}-cdk`, where `shortName` is `cicd-website`)
- `normalizedDomain`: Domain name with dots replaced by hyphens (e.g., example-com)
- `stackType`: certificate, cloudfront, or codepipeline

Example stack names for domain `example.com`:
- `aws-cicd-website-cdk-example-com-certificate` (us-east-1)
- `aws-cicd-website-cdk-example-com-cloudfront` (eu-central-1)
- `aws-cicd-website-cdk-example-com-codepipeline` (eu-central-1)

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

# Existing Route53 public hosted zone for the domain.
# Required only when external.dns.provider=false (domain registered in Route53):
# the stack reuses this zone instead of creating a duplicate. Find the Z... id in
# the Route53 console. Leave blank when external.dns.provider=true — the stack
# creates a new zone in that case.
hosted.zone.id=Z0123456789ABCDEFGHIJ

# External DNS provider (default: false)
# Set to true if using Hover, GoDaddy, etc.
external.dns.provider=false

# Alternative apex domain served by the same distribution (optional).
# Both properties must be set together, and the alternative domain must have its own
# existing Route53 public hosted zone (external providers are not supported for
# alternatives). See "Two apex domains, one distribution" below.
domain.alternative.name=example.org
alternative.hosted.zone.id=Z9876543210ZYXWVUTSR

# GitHub integration
# Reference an existing connection ARN (the stack does not create it). Create and
# authorize the connection once in the console first (see "GitHub Connection" below).
codestar.connection.arn=arn:aws:codestar-connections:region:account:connection/xxx
git.owner=your-github-username
git.repository=your-repo-name
git.branch=main

# Certificate validation (optional)
# Not required for Route53 (Flow A) — the ACM console "Create records in Route 53"
# button creates the validation CNAME and the deployment waits until the cert is issued.
# Only set these if you want Route53 to hand-build the validation CNAME instead;
# ACM emits the values during certificate creation (us-east-1 console).
cert.validation.record.name=_xxx.example.com
cert.validation.domain.name=_xxx.acm-validations.aws.
```

## Deployment

The domain is always passed via CDK context. Because the app synthesizes three stacks, deploy them with `--all` (a bare `cdk deploy` errors asking you to name a stack):
```bash
cdk deploy --all --context domain=example.com
```
`--all` honors the inter-stack dependencies described in Flow A below. To target a single stack, name it (wildcards work), e.g. `cdk deploy 'aws-cicd-website-cdk-example-com-*' --context domain=example.com`.

### Flow A — Route53 manages DNS (registrar at AWS or NS delegated):
1. `external.dns.provider=false` (default); set `hosted.zone.id` to the domain's existing Route53 zone, leave `cert.validation.*` blank
2. `cdk deploy --all --context domain=example.com` — the certificate stack enters `PENDING_VALIDATION` and the deployment waits
3. In the ACM console (us-east-1), open the certificate and click **"Create records in Route 53"** — ACM writes the validation CNAME into the hosted zone automatically
4. ACM validates and the deployment **continues on its own** through the cloudfront and codepipeline stacks — no redeploy and no manual `cert.validation.*` values required

The three stacks and their dependencies (CDK orders them automatically from the references in `airhacks.CDKApp`):

- `…-certificate` (`DomainCertificateStack`, us-east-1) — produces the ACM certificate; CloudFront requires a us-east-1 certificate.
- `…-cloudfront` (`CloudFrontStack`, eu-central-1) — consumes the certificate (cross-region reference) and creates the S3 bucket, the distribution, and the CloudFront alias records. The hosted zone is resolved by the DNS model: with `external.dns.provider=false` it **references the existing zone** via `hosted.zone.id` (`HostedZone.fromHostedZoneAttributes`); with `external.dns.provider=true` it **creates a new zone** so its NS records can be delegated at the external registrar.
- `…-codepipeline` (`CodePipelineStack`, eu-central-1) — consumes the website bucket and the distribution from the CloudFront stack.

**Validation timing.** The certificate stack uses `CertificateValidation.fromDns()` and enters `PENDING_VALIDATION`. The simplest way to clear it is the ACM console **"Create records in Route 53"** button (Flow A step 3), which writes the validation CNAME into the hosted zone for you; the in-progress `cdk deploy` then validates and continues without intervention. The `cert.validation.record.name` / `cert.validation.domain.name` properties are only consumed by the optional hand-built CNAME in `Route53.setupAliasRecord` — leave them blank to let the console (or ACM auto-validation) manage the record instead.

### Flow B — DNS stays with an external provider (Hover, GoDaddy, ...):
Use this when the apex/www records remain at your provider and only the ACM validation CNAME is added there.
1. Set `external.dns.provider=true`
2. `cdk deploy --all --context domain=example.com` — deployment blocks waiting for cert validation
3. Copy the validation CNAME from ACM (us-east-1 console) into your DNS provider — see [Provider-specific notes](#provider-specific-notes) below for Hover
4. Wait for ACM to validate
5. Add A/AAAA (or CNAME) records at your provider pointing to the CloudFront distribution domain (printed as `CloudFrontDistributionDomainNameOutput`)

### Flow C — External registrar, DNS delegated to Route53:
Use this when you keep the registrar (Hover, GoDaddy, ...) but want Route53 to be authoritative for DNS.
1. Set `external.dns.provider=true`
2. `cdk deploy --all --context domain=example.com` — deployment blocks waiting for cert validation
3. Copy the validation CNAME from ACM (us-east-1 console) into your provider's DNS *or* into the new Route53 hosted zone (the zone is created by `CloudFrontStack` in eu-central-1; Route53 itself is global) — see [Provider-specific notes](#provider-specific-notes) below for Hover
4. Wait for ACM to validate
5. Read the four NS records from the new Route53 hosted zone and configure them at your registrar — DNS propagation up to 48h
6. Keep the validation CNAME permanently for automatic certificate renewal

## Two apex domains, one distribution

To serve a second, unrelated domain (e.g. `example.org` alongside `example.com`) from the same website, set `domain.alternative.name` and `alternative.hosted.zone.id` in the primary domain's properties file and deploy as usual (`cdk deploy --all --context domain=example.com` — stack names stay keyed to the primary domain). This changes three things:

- **Certificate**: the SANs become `example.com`, `*.example.com`, `example.org`, `*.example.org`. Changing SANs *replaces* the ACM certificate, so the first deploy after adding the alternative re-enters `PENDING_VALIDATION` — click **"Create records in Route 53"** in the ACM console once and all names validate together (see [Validation timing](#deployment)). CloudFront only switches after validation, so the primary domain keeps serving.
- **CloudFront**: both apex names are added as alternate domain names on the same distribution.
- **Route53**: A/AAAA alias records are created in *each* domain's hosted zone, all pointing to the same distribution.

Both properties must be set together; the alternative domain must be Route53-managed with an existing public hosted zone (`external.dns.provider` applies to the primary domain only). Leaving both blank keeps the exact single-domain behavior.

**Adding an alternative to an already-deployed domain.** The certificate ARN crosses regions via CDK's export writer, which rejects a changed value under a stable export name (`Some exports have changed!`). The certificate construct id is therefore derived from the domain set: a SAN change replaces the certificate under a new logical id, turning the export into a removed+added pair. The *removed* export must not be tagged in-use, so before the first deploy after adding (or changing) alternatives on a live deployment, delete the old export parameter in the CloudFront region:

```bash
aws ssm delete-parameter --region eu-central-1 \
  --name "$(aws ssm get-parameters-by-path --region eu-central-1 \
      --path '/cdk/exports/aws-cicd-website-cdk-<normalized-domain>-cloudfront/' \
      --query 'Parameters[0].Name' --output text)"
```

Then deploy as usual. Expect harmless `DELETE_FAILED` retries on the old certificate during cleanup (it is still attached to the distribution until the CloudFront stack switches); delete it in the ACM console afterwards if it was orphaned.

## GitHub Connection

The pipeline pulls the website repository through an AWS CodeConnections (formerly CodeStar Connections) connection. The stack does **not** create this connection — it only references an existing one by ARN (`codestar.connection.arn` → `CodeStarConnectionsSourceAction.connectionArn`). You create and authorize the connection yourself; a freshly created one is **PENDING** until you complete the GitHub handshake, and the pipeline's source stage fails while it stays pending.

1. Create the connection in the console (Developer Tools → Settings → Connections) — it starts **PENDING** — and put its ARN in `codestar.connection.arn`.
2. Open the connection and click **Update pending connection** — complete the GitHub handshake and install/authorize the AWS Connector GitHub App on the account/org that owns the website repo (`git.owner`/`git.repository`).
3. With the connection **Available**, `cdk deploy` wires its ARN into the pipeline and the source stage can fetch the repository.

The handshake is a one-time step per connection; subsequent deploys and runs reuse it.

## Cache Invalidation

The `CACHING_OPTIMIZED` policy caches `index.html` and other assets at CloudFront edges for up to 1 day by default, so freshly published files would not be visible until the TTL expires. To keep deployments instantly visible, the CodeBuild publishing stage runs an invalidation as the last build command:

```
aws cloudfront create-invalidation --distribution-id <ID> --paths "/*"
```

The distribution ID is wired in from `CloudFrontStack` via `WebsiteBuildConfiguration.createBuildSpec(domainName, distributionId)`, and `PublishingStage` grants the CodeBuild role least-privilege `cloudfront:CreateInvalidation` permission scoped to this distribution (`distribution.grantCreateInvalidation(serviceRole)`).

Cost note: AWS includes 1,000 invalidation paths per month at no charge. `"/*"` counts as a single path, so even daily deploys stay well inside the free tier.

## Provider-specific notes

### Hover
When pasting the ACM validation CNAME into Hover:
- **Hostname / "CNAME name"**: use the left-most part relative to your domain. Hover stores the hostname only, not the FQDN — e.g. ACM emits `_abc123.example.com.`, enter `_abc123`.
- **Target / Value**: drop the trailing dot. Hover rejects the FQDN form — e.g. `_xyz.acm-validations.aws.` → `_xyz.acm-validations.aws`.

## Project Structure

- `airhacks.CDKApp` — entry point, wires the three stacks
- `airhacks.website.Configuration` — typed configuration records
- `airhacks.website.Stacks` — region-pinned `StackProps` (us-east-1, eu-central-1)
- `airhacks.website.certificate.boundary.DomainCertificateStack` — ACM certificate (us-east-1)
- `airhacks.website.cloudfront.boundary.CloudFrontStack` — distribution + S3 origin (OAC)
- `airhacks.website.codebuild.boundary.CodePipelineStack` — CodePipeline + CodeBuild publishing stage
- `airhacks.website.route53.control.Route53` — alias records; reuses the existing hosted zone (`external.dns.provider=false`) or creates one (`true`)
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