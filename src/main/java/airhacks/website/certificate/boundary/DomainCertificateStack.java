package airhacks.website.certificate.boundary;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import airhacks.website.Stacks;
import airhacks.website.Configuration.AlternativeDomain;
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
        return Certificate.Builder.create(this, certificateConstructId(configuration))
                .domainName(domainName)
                .subjectAlternativeNames(subjectAlternativeNames)
                .certificateName(domainName)
                .validation(CertificateValidation.fromDns())
                .build();
    }

    /**
     * The construct id is derived from the domain set and the optional certificate.version:
     * a change to either replaces the certificate under a NEW logical id, so the cross-region
     * export to the CloudFront stack is removed+added instead of changed in place — the CDK
     * export writer rejects a changed value under a stable export name
     * ("Some exports have changed!").
     *
     * CloudFormation never renews a certificate — ACM does, and only while the validation
     * CNAME exists. When renewal failed and the certificate expired, bumping
     * certificate.version is the way to get a fresh one (see README, "Replacing an expired
     * certificate"). Without alternatives and without a version the historical id is kept:
     * existing single-domain deployments stay untouched.
     */
    static String certificateConstructId(DomainEntriesConfiguration configuration) {
        var alternativeDomains = configuration.alternativeDomains();
        var id = "DnsValidatedCertificate";
        if (!alternativeDomains.isEmpty()) {
            id = alternativeDomains.stream()
                    .map(AlternativeDomain::domainName)
                    .map(name -> name.replace(".", "-"))
                    .collect(Collectors.joining("-", id + "-", ""));
        }
        if (configuration.hasCertificateVersion()) {
            id = "%s-v%s".formatted(id, configuration.certificateVersion().trim());
        }
        return id;
    }

    public Certificate getCertificate() {
        return this.certificate;
    }

}
