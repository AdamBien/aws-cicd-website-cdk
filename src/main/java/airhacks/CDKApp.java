package airhacks;

import airhacks.website.Configuration;
import airhacks.website.certificate.boundary.DomainCertificateStack;
import airhacks.website.cloudfront.boundary.CloudFrontStack;
import airhacks.website.codebuild.boundary.CodeBuildStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Tags;

public class CDKApp {

    public static void main(final String[] args) {

        var app = new App();
        var appName = "aws-website-cdk";
        var entries = Configuration.create(app, appName);
        Tags.of(app).add("environment", "production");
        Tags.of(app).add("domain", "production");
        Tags.of(app).add("application", appName);

        var domainCertificate = new DomainCertificateStack(app, entries);
        var certificate = domainCertificate.getCertificate();
        var extendedEntries = entries.withCertificate(certificate);
        var cloudfront = new CloudFrontStack(app, extendedEntries);
        var websiteBucket = cloudfront.getWebsiteBucket();
        new CodeBuildStack(app, appName, websiteBucket);
        app.synth();
    }
}
