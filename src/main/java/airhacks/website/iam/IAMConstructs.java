package airhacks.website.iam;

import java.util.List;

import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.iam.CanonicalUserPrincipal;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;

public interface IAMConstructs {

        static PolicyStatement createReadPolicy(Bucket bucket, OriginAccessIdentity oai) {
                return PolicyStatement.Builder.create()
                                .sid("UIBucketCloudFrontReadPolicy")
                                .actions(List.of("s3:GetObject"))
                                .resources(List.of(bucket.arnForObjects("*")))
                                .principals(
                                                List.of(new CanonicalUserPrincipal(oai
                                                                .getCloudFrontOriginAccessIdentityS3CanonicalUserId())))
                                .build();

        }
}
