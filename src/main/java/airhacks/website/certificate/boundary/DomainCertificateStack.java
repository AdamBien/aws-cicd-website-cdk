package airhacks.website.certificate.boundary;

import java.util.List;

import airhacks.website.Stacks;
import airhacks.website.Configuration.DomainEntriesConfiguration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.constructs.Construct;

public class DomainCertificateStack extends Stack {

    Certificate certificate;

    public DomainCertificateStack(Construct scope, DomainEntriesConfiguration configuration) {
        super(scope, configuration.appNameWithDomain() + "-certificate", Stacks.US_EAST_1); 
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
                .domainName(domainName)
                .subjectAlternativeNames(List.of(domainName, wildcardDomain))
                .certificateName(domainName)
                .validation(CertificateValidation.fromDns())
                .build();
    }

    public Certificate getCertificate() {
        return this.certificate;
    }

}
