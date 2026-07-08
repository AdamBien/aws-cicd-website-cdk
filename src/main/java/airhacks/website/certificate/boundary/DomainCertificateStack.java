package airhacks.website.certificate.boundary;

import java.util.stream.Stream;

import airhacks.website.Stacks;
import airhacks.website.Configuration.DomainEntriesConfiguration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.constructs.Construct;

public class DomainCertificateStack extends Stack {

    Certificate certificate;
    static String stackName = "certificate";
    public DomainCertificateStack(Construct scope, DomainEntriesConfiguration configuration) {
        super(scope, configuration.appNameWithDomain(stackName), Stacks.US_EAST_1);
        this.certificate = this.createCertificate(configuration);
    }

    /**
     * https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html
     *
     * The SAN list covers every configured domain plus its wildcard. Changing the SANs
     * (e.g. adding domain.alternative.name) REPLACES the certificate: the new one re-enters
     * PENDING_VALIDATION on the next deploy — validate all names via the ACM console
     * "Create records in Route 53" button. CloudFront switches only after validation,
     * so the primary domain keeps serving throughout.
     */
    Certificate createCertificate(DomainEntriesConfiguration configuration) {
        var domainName = configuration.domainName();
        var subjectAlternativeNames = configuration.allDomainNames().stream()
                .flatMap(name -> Stream.of(name, "*." + name))
                .toList();
        return Certificate.Builder.create(this, "DnsValidatedCertificate")
                .domainName(domainName)
                .subjectAlternativeNames(subjectAlternativeNames)
                .certificateName(domainName)
                .validation(CertificateValidation.fromDns())
                .build();
    }

    public Certificate getCertificate() {
        return this.certificate;
    }

}
