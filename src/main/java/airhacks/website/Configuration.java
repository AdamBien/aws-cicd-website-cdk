package airhacks.website;

import airhacks.website.codebuild.control.GitRepository;
import software.amazon.awscdk.services.certificatemanager.Certificate;

public interface Configuration {
    String domainName = "";
    String codeStarConnectionARN = "arn:aws:codestar-connections:";
    String owner       = "";
    String repository  = "";
    String branch      = "";    
    GitRepository gitRepository = GitRepository.create();
    String CERT_VALIDATION_RECORD_NAME = null;
    String CERT_VALIDATION_DOMAIN_NAME = null;    

    public record Entries(String appName,String domainName,Certificate certificate){
        public Entries withCertificate(Certificate certificate){
            return new Entries(this.appName, this.domainName, certificate);
        }
    }

    static Entries create(String appName){
        return new Entries(appName,domainName,null);
    }
}
