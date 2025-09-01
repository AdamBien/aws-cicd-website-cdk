package airhacks.website;

import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.configuration.control.ZCfg;
import software.amazon.awscdk.services.certificatemanager.Certificate;

public interface Configuration {

    public record DomainEntriesConfiguration(String appName, String domainName, Certificate certificate) {
        public DomainEntriesConfiguration withCertificate(Certificate certificate) {
            return new DomainEntriesConfiguration(this.appName, this.domainName, certificate);
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

    static DomainEntriesConfiguration domainEntries(String domain, String appName) {
        ZCfg.load(domain);
        var domainName = ZCfg.string("domain.name");
        return new DomainEntriesConfiguration(appName, domainName, null);
    }

    static BuildConfiguration build(String domainName) {
        ZCfg.load(domainName);
        var codeStarConnectionARN = ZCfg.string("codestar.connection.arn",
                "arn:aws:codestar-connections:");
        var owner = ZCfg.string( "git.owner");
        var repository = ZCfg.string( "git.repository");
        var branch = ZCfg.string( "git.branch", "main");
        var gitRepository = new GitRepository(owner, repository, branch);
        return new BuildConfiguration(codeStarConnectionARN, owner, repository, branch, gitRepository);
    }

    static CertificateValidationConfiguration certificate(String domain) {
        ZCfg.load(domain);
        var recordName = ZCfg.string( "cert.validation.record.name", null);
        var domainName = ZCfg.string( "cert.validation.domain.name", null);
        var externalDnsProvider = Boolean
                .parseBoolean(ZCfg.string( "external.dns.provider", "false"));
        return new CertificateValidationConfiguration(recordName, domainName, externalDnsProvider);
    }
}
