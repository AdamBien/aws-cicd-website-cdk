package airhacks.website.s3.control;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketAccessControl;
import software.amazon.awscdk.services.s3.ObjectOwnership;
import software.constructs.Construct;

public interface Buckets {

    static Bucket createWebsiteBucket(Construct scope,String domainName) {
        return Bucket.Builder.create(scope, "StaticWebSiteBucket")
                .bucketName(domainName)
                .objectOwnership(ObjectOwnership.BUCKET_OWNER_ENFORCED)
                .accessControl(BucketAccessControl.BUCKET_OWNER_FULL_CONTROL)
                .publicReadAccess(false)
                .removalPolicy(RemovalPolicy.RETAIN)
                .build();
    }
    
    static Bucket createPrivateBucket(Construct scope) {
        return  Bucket.Builder.create(scope, "PrivateBucket")
        .accessControl(BucketAccessControl.PRIVATE)
        .build();

    }
    
}
