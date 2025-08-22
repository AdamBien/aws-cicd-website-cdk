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
        
        var domain = fetchDomain(app);
        var certificateConfiguration = Configuration.certificate(domain);
        var entries = Configuration.domainEntries(domain, appName);
        var buildConfiguration = Configuration.build(domain);
        Tags.of(app).add("environment", "production");
        Tags.of(app).add("domain", entries.domainName());
        Tags.of(app).add("application", appName);

        var domainCertificate = new DomainCertificateStack(app, entries);
        var certificate = domainCertificate.getCertificate();
        var extendedEntries = entries.withCertificate(certificate);
        var cloudfront = new CloudFrontStack(app, extendedEntries,certificateConfiguration);
        var websiteBucket = cloudfront.getWebsiteBucket();
        new CodeBuildStack(app, appName, websiteBucket,buildConfiguration);
        app.synth();
    }
    
    static String fetchDomain(App app) {
        var domain = (String) app.getNode().tryGetContext("domain");
                if (domain != null && !domain.isBlank()) {
            System.out.println("Deploying for domain: " + domain);
        } else {
            System.out.println("No domain specified, using default configuration");
        }

        return domain;
    }
}
