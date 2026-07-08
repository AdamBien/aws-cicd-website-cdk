package airhacks.website.cloudfront.boundary;

import airhacks.website.Stacks;
import airhacks.website.Configuration.CertificateValidationConfiguration;
import airhacks.website.Configuration.DomainEntriesConfiguration;
import airhacks.website.route53.control.Route53;
import airhacks.website.s3.control.Buckets;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.cloudfront.AllowedMethods;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CachePolicy;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.IOrigin;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

public class CloudFrontStack extends Stack {

        static String stackName = "cloudfront";

        Distribution distribution;
        Bucket websiteBucket;

        public CloudFrontStack(Construct scope, DomainEntriesConfiguration configuration,CertificateValidationConfiguration certificateConfiguration) {
                super(scope, configuration.appNameWithDomain(stackName),Stacks.EU_CENTRAL_1);

                this.websiteBucket = Buckets.createWebsiteBucket(this, configuration.domainName());
                var s3Origin = createS3Origin(websiteBucket);
                this.distribution = this.createCloudFrontDistribution(configuration, s3Origin);
                Route53.setupAliasRecord(this, this.distribution, configuration.domainName(),configuration.hostedZoneId(),certificateConfiguration);
                Route53.setupAlternativeAliasRecords(this, this.distribution, configuration.alternativeDomains());
                Tags.of(websiteBucket).add("component", "bucket for static assets");
                Tags.of(websiteBucket).add("domain", configuration.domainName());
                CfnOutput.Builder.create(this, "CloudFrontDistributionDomainNameOutput").value(this.distribution.getDistributionDomainName()).build();
        }

        /**
         * Origin Access Control (OAC) is the AWS-recommended successor to Origin Access Identity (OAI)
         * for restricting S3 origin access to CloudFront. CDK creates the OAC and the bucket policy automatically.
         *
         * @see <a href="https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html">CloudFront: Restricting access to an S3 origin</a>
         * @see <a href="https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_cloudfront_origins.S3BucketOrigin.html">CDK API: S3BucketOrigin.withOriginAccessControl</a>
         */
        static IOrigin createS3Origin(Bucket websiteBucket) {
                return S3BucketOrigin.withOriginAccessControl(websiteBucket);
        }

        /**
         * Cache policy impact on {@code index.html}:
         * <ul>
         *   <li>{@link CachePolicy#CACHING_OPTIMIZED} (used here) — {@code index.html} is cached at the edge for up to 1 day; updates require an invalidation.</li>
         *   <li>{@link CachePolicy#CACHING_OPTIMIZED_FOR_UNCOMPRESSED_OBJECTS} — same, but skips gzip/brotli negotiation.</li>
         *   <li>{@link CachePolicy#CACHING_DISABLED} — every request for {@code index.html} hits S3.</li>
         *   <li>{@link CachePolicy#USE_ORIGIN_CACHE_CONTROL_HEADERS} — TTL of {@code index.html} follows the {@code Cache-Control} header set on the S3 object.</li>
         * </ul>
         *
         * @see <a href="https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-managed-cache-policies.html">CloudFront: Using managed cache policies</a>
         */
        Distribution createCloudFrontDistribution(DomainEntriesConfiguration entries,
                        IOrigin s3Origin) {
                var certificate = entries.certificate();
                return Distribution.Builder
                                .create(this, "CloudFrontDistribution")
                                .domainNames(entries.allDomainNames())
                                .certificate(certificate)
                                .defaultRootObject("index.html")
                                .defaultBehavior(BehaviorOptions.builder()
                                                .allowedMethods(AllowedMethods.ALLOW_ALL)
                                                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                                                .cachePolicy(CachePolicy.CACHING_OPTIMIZED)
                                                .origin(s3Origin)
                                                .build())
                                .build();
        }

        public Distribution getDistribution() {
                return this.distribution;
        }

        public Bucket getWebsiteBucket() {
                return websiteBucket;
        }

}
