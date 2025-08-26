package airhacks.website.certificate.boundary;

import airhacks.website.Stacks;
import airhacks.website.Configuration.EntriesConfiguration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.constructs.Construct;

public class DomainCertificateStack extends Stack {

    Certificate certificate;

    public DomainCertificateStack(Construct scope, EntriesConfiguration configuration) {
        super(scope, configuration.appName() + "-certificate", Stacks.US_EAST_1); 
        this.certificate = this.createCertificate(configuration.domainName());
    }

    /**
     * https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html
     * @param domainName
     * @return
     */
    Certificate createCertificate(String domainName) {
        var wildcardDomain = "*."+domainName;
        return Certificate.Builder.create(this, "DnsValidatedCertificate")
                .domainName(wildcardDomain)
                .certificateName(wildcardDomain)
                .validation(CertificateValidation.fromDns())
                .build();
    }

    public Certificate getCertificate() {
        return this.certificate;
    }

}
