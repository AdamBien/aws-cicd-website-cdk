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

    Certificate createCertificate(String domainName) {
        return Certificate.Builder.create(this, "DnsValidatedCertificate")
                .domainName(domainName)
                .validation(CertificateValidation.fromDns())
                .build();
    }

    public Certificate getCertificate() {
        return this.certificate;
    }

}
