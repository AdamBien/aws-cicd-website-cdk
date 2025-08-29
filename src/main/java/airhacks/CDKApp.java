package airhacks;

import airhacks.website.Configuration;
import airhacks.website.certificate.boundary.DomainCertificateStack;
import airhacks.website.cloudfront.boundary.CloudFrontStack;
import airhacks.website.codebuild.boundary.CodePipelineStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Tags;

public interface CDKApp {
    String name = "aws-cicd-website-cdk";

    static void main(String... args) {

        var app = new App();
        
        var domain = fetchDomain(app);
        var certificateConfiguration = Configuration.certificate(domain);
        var domainEntriesConfiguration = Configuration.domainEntries(domain, name);
        var buildConfiguration = Configuration.build(domain);
        Tags.of(app).add("environment", "production");
        Tags.of(app).add("domain", domainEntriesConfiguration.domainName());
        Tags.of(app).add("application", name);

        var domainCertificate = new DomainCertificateStack(app, domainEntriesConfiguration);
        var certificate = domainCertificate.getCertificate();
        var extendedEntries = domainEntriesConfiguration.withCertificate(certificate);
        var cloudfront = new CloudFrontStack(app, extendedEntries,certificateConfiguration);
        var websiteBucket = cloudfront.getWebsiteBucket();
        new CodePipelineStack(app, domainEntriesConfiguration, websiteBucket,buildConfiguration);
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
