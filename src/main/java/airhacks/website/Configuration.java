package airhacks.website;

import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.configuration.control.ConfigurationLoader;
import software.amazon.awscdk.services.certificatemanager.Certificate;

public interface Configuration {

    public record Entries(String appName,String domainName,Certificate certificate,
                         String codeStarConnectionARN, String owner, String repository, 
                         String branch, GitRepository gitRepository,
                         String certValidationRecordName, String certValidationDomainName){
        public Entries withCertificate(Certificate certificate){
            return new Entries(this.appName, this.domainName, certificate,
                    this.codeStarConnectionARN, this.owner, this.repository,
                    this.branch, this.gitRepository, this.certValidationRecordName,
                    this.certValidationDomainName);
        }
    }

    static Entries create(String domain, String appName){
        
        var properties = ConfigurationLoader.loadConfigurationForDomain(domain);
        
        var domainName = ConfigurationLoader.getProperty(properties, "domain.name", "");
        var codeStarConnectionARN = ConfigurationLoader.getProperty(properties, "codestar.connection.arn", "arn:aws:codestar-connections:");
        var owner = ConfigurationLoader.getProperty(properties, "git.owner", "");
        var repository = ConfigurationLoader.getProperty(properties, "git.repository", "");
        var branch = ConfigurationLoader.getProperty(properties, "git.branch", "main");
        var gitRepository = GitRepository.create();
        var certValidationRecordName = ConfigurationLoader.getProperty(properties, "cert.validation.record.name", null);
        var certValidationDomainName = ConfigurationLoader.getProperty(properties, "cert.validation.domain.name", null);
        
        return new Entries(appName, domainName, null, codeStarConnectionARN, owner, 
                          repository, branch, gitRepository, certValidationRecordName, 
                          certValidationDomainName);
    }
}
