package airhacks.website;

import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.configuration.control.ConfigurationLoader;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import java.util.Properties;

public interface Configuration {
    Properties PROPERTIES = ConfigurationLoader.CONFIGURATION;
    
    String domainName = ConfigurationLoader.getProperty("domain.name", "");
    String codeStarConnectionARN = ConfigurationLoader.getProperty("codestar.connection.arn", "arn:aws:codestar-connections:");
    String owner = ConfigurationLoader.getProperty("git.owner", "");
    String repository = ConfigurationLoader.getProperty("git.repository", "");
    String branch = ConfigurationLoader.getProperty("git.branch", "main");    
    GitRepository gitRepository = GitRepository.create();
    String CERT_VALIDATION_RECORD_NAME = ConfigurationLoader.getProperty("cert.validation.record.name", null);
    String CERT_VALIDATION_DOMAIN_NAME = ConfigurationLoader.getProperty("cert.validation.domain.name", null);    

    public record Entries(String appName,String domainName,Certificate certificate){
        public Entries withCertificate(Certificate certificate){
            return new Entries(this.appName, this.domainName, certificate);
        }
    }

    static Entries create(String appName){
        return new Entries(appName,domainName,null);
    }
}
