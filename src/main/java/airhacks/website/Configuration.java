package airhacks.website;

import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.configuration.control.ConfigurationLoader;
import software.amazon.awscdk.services.certificatemanager.Certificate;

public interface Configuration {

    public record Entries(String appName, String domainName, Certificate certificate) {
        public Entries withCertificate(Certificate certificate) {
            return new Entries(this.appName, this.domainName, certificate);
        }
    }
    
    public record BuildConfiguration(String codeStarConnectionARN, String owner, 
                                    String repository, String branch, 
                                    GitRepository gitRepository) {
    }
    
    public record CertificateValidation(String recordName, String domainName) {
    }

    static Entries domainEntries(String domain, String appName) {
        var properties = ConfigurationLoader.loadConfigurationForDomain(domain);
        var domainName = ConfigurationLoader.getProperty(properties, "domain.name", "");
        return new Entries(appName, domainName, null);
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
    
    static CertificateValidation certificate(String domain) {
        var properties = ConfigurationLoader.loadConfigurationForDomain(domain);
        var recordName = ConfigurationLoader.getProperty(properties, "cert.validation.record.name", null);
        var domainName = ConfigurationLoader.getProperty(properties, "cert.validation.domain.name", null);
        return new CertificateValidation(recordName, domainName);
    }
}
