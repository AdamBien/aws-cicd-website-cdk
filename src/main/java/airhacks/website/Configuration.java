package airhacks.website;

import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.configuration.control.ConfigurationLoader;
import software.amazon.awscdk.services.certificatemanager.Certificate;

public interface Configuration {

    public record EntriesConfiguration(String appName, String domainName, Certificate certificate) {
        public EntriesConfiguration withCertificate(Certificate certificate) {
            return new EntriesConfiguration(this.appName, this.domainName, certificate);
        }

        public String appNameWithDomain(){
            return "%s-%s".formatted(this.appName,domainName);
        }
    }
    
    public record BuildConfiguration(String codeStarConnectionARN, String owner, 
                                    String repository, String branch, 
                                    GitRepository gitRepository) {
    }
    
    public record CertificateValidationConfiguration(String recordName, String domainName, boolean externalDnsProvider) {
    }

    static EntriesConfiguration domainEntries(String domain, String appName) {
        var properties = ConfigurationLoader.loadConfigurationForDomain(domain);
        var domainName = ConfigurationLoader.getProperty(properties, "domain.name", "");
        return new EntriesConfiguration(appName, domainName, null);
    }
    
    static BuildConfiguration build(String domain) {
        var properties = ConfigurationLoader.loadConfigurationForDomain(domain);
        var codeStarConnectionARN = ConfigurationLoader.getProperty(properties, "codestar.connection.arn", "arn:aws:codestar-connections:");
        var owner = ConfigurationLoader.getProperty(properties, "git.owner", "");
        var repository = ConfigurationLoader.getProperty(properties, "git.repository", "");
        var branch = ConfigurationLoader.getProperty(properties, "git.branch", "main");
        var gitRepository = new GitRepository(owner, repository, branch);
        return new BuildConfiguration(codeStarConnectionARN, owner, repository, branch, gitRepository);
    }
    
    static CertificateValidationConfiguration certificate(String domain) {
        var properties = ConfigurationLoader.loadConfigurationForDomain(domain);
        var recordName = ConfigurationLoader.getProperty(properties, "cert.validation.record.name", null);
        var domainName = ConfigurationLoader.getProperty(properties, "cert.validation.domain.name", null);
        var externalDnsProvider = Boolean.parseBoolean(ConfigurationLoader.getProperty(properties, "external.dns.provider", "false"));
        return new CertificateValidationConfiguration(recordName, domainName, externalDnsProvider);
    }
}
