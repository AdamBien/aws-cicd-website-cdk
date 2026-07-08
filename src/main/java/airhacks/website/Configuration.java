package airhacks.website;

import java.util.List;
import java.util.stream.Stream;

import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.configuration.control.ZCfg;
import software.amazon.awscdk.services.certificatemanager.Certificate;

public interface Configuration {

    public record AlternativeDomain(String domainName, String hostedZoneId) {
    }

    public record DomainEntriesConfiguration(String appName, String domainName, String hostedZoneId,
            List<AlternativeDomain> alternativeDomains, Certificate certificate) {
        public DomainEntriesConfiguration withCertificate(Certificate certificate) {
            return new DomainEntriesConfiguration(this.appName, this.domainName, this.hostedZoneId,
                    this.alternativeDomains, certificate);
        }

        public List<String> allDomainNames() {
            return Stream.concat(Stream.of(this.domainName),
                    this.alternativeDomains.stream().map(AlternativeDomain::domainName)).toList();
        }

        public String appNameWithDomain(String suffix) {
            var appNameWithDomain = appNameWithDomain();
            return "%s-%s".formatted(appNameWithDomain,suffix);
        }

        public String appNameWithDomain() {
            var normalizedDomainName = domainName.replace(".", "-").trim();
            return "%s-%s".formatted(this.appName, normalizedDomainName);
        }
    }

    public record BuildConfiguration(String codeStarConnectionARN,
            String owner,
            String repository, String branch,
            GitRepository gitRepository) {
    }

    public record CertificateValidationConfiguration(String recordName, String domainName,
            boolean externalDnsProvider) {
    }

    static DomainEntriesConfiguration domainEntries(String appName) {
        var domainName = ZCfg.string("domain.name");
        var hostedZoneId = ZCfg.string("hosted.zone.id");
        var alternativeDomains = alternativeDomains(domainName);
        return new DomainEntriesConfiguration(appName, domainName, hostedZoneId, alternativeDomains, null);
    }

    /**
     * An alternative apex domain served by the same distribution. Both properties are optional,
     * but a name without a zone id cannot get alias records — fail fast at synth instead.
     */
    static List<AlternativeDomain> alternativeDomains(String primaryDomainName) {
        var name = ZCfg.string("domain.alternative.name", null);
        var hostedZoneId = ZCfg.string("alternative.hosted.zone.id", null);
        if (!isPresent(name) && !isPresent(hostedZoneId)) {
            return List.of();
        }
        if (!isPresent(name) || !isPresent(hostedZoneId)) {
            throw new IllegalStateException(
                    "domain.alternative.name and alternative.hosted.zone.id must be set together");
        }
        if (name.equals(primaryDomainName)) {
            throw new IllegalStateException("domain.alternative.name must differ from domain.name");
        }
        return List.of(new AlternativeDomain(name, hostedZoneId));
    }

    static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    static BuildConfiguration build() {
        var codeStarConnectionARN = ZCfg.string("codestar.connection.arn",
                "arn:aws:codestar-connections:");
        var owner = ZCfg.string( "git.owner");
        var repository = ZCfg.string( "git.repository");
        var branch = ZCfg.string( "git.branch", "main");
        var gitRepository = new GitRepository(owner, repository, branch);
        return new BuildConfiguration(codeStarConnectionARN, owner, repository, branch, gitRepository);
    }

    static CertificateValidationConfiguration certificate() {
        var recordName = ZCfg.string( "cert.validation.record.name", null);
        var domainName = ZCfg.string( "cert.validation.domain.name", null);
        var externalDnsProvider = Boolean
                .parseBoolean(ZCfg.string( "external.dns.provider", "false"));
        return new CertificateValidationConfiguration(recordName, domainName, externalDnsProvider);
    }
}
