package airhacks.website.cloudfront.boundary;

import java.util.List;

import airhacks.website.Stacks;
import airhacks.website.Configuration.CertificateValidationConfiguration;
import airhacks.website.Configuration.DomainEntriesConfiguration;
import airhacks.website.iam.IAMConstructs;
import airhacks.website.route53.control.Route53;
import airhacks.website.s3.control.Buckets;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.cloudfront.AllowedMethods;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CachePolicy;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

public class CloudFrontStack extends Stack {

        Distribution distribution;
        Bucket websiteBucket;

        public CloudFrontStack(Construct scope, DomainEntriesConfiguration configuration,CertificateValidationConfiguration certificateConfiguration) {
                super(scope, configuration.appNameWithDomain() + "-cloudfront",Stacks.EU_CENTRAL_1);

                this.websiteBucket = Buckets.createWebsiteBucket(this, configuration.domainName());
                var oai = new OriginAccessIdentity(this, "CloudFrontOriginAccessIdentity");
                var readBucketPolicy = IAMConstructs.createReadPolicy(websiteBucket, oai);
                websiteBucket.addToResourcePolicy(readBucketPolicy);
                var s3Origin = S3Origin.Builder
                                .create(websiteBucket)
                                .originAccessIdentity(oai)
                                .build();
                this.distribution = this.createCloudFrontDistribution(configuration, s3Origin);
                Route53.setupAliasRecord(this, this.distribution, configuration.domainName(),certificateConfiguration);
                Tags.of(websiteBucket).add("component", "bucket for static assets");
                Tags.of(websiteBucket).add("domain", configuration.domainName());
                CfnOutput.Builder.create(this, "CloudFrontDistributionDomainNameOutput").value(this.distribution.getDistributionDomainName()).build();
        }

        Distribution createCloudFrontDistribution(DomainEntriesConfiguration entries,
                        S3Origin s3Origin) {
                var domainName = entries.domainName();
                var certificate = entries.certificate();
                return Distribution.Builder
                                .create(this, "CloudFrontDistribution")
                                .domainNames(List.of(domainName))
                                .certificate(certificate)
                                .defaultRootObject("index.html")
                                .defaultBehavior(BehaviorOptions.builder()
                                                .allowedMethods(AllowedMethods.ALLOW_ALL)
                                                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                                                .cachePolicy(CachePolicy.CACHING_DISABLED)
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
