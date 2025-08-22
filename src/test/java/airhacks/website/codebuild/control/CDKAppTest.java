package airhacks.website.codebuild.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import airhacks.website.Configuration;
import airhacks.website.certificate.boundary.DomainCertificateStack;
import airhacks.website.cloudfront.boundary.CloudFrontStack;
import software.amazon.awscdk.App;

public class CDKAppTest {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void stacks() throws IOException {
        App app = new App();
        var domain = "airhacks.live";
        var entries = Configuration.domainEntries(domain, "test-app");
        var certificateConfiguration = Configuration.certificate(domain);
        var domainStack = new DomainCertificateStack(app, entries);
        var actual = JSON.valueToTree(app.synth().getStackArtifact(domainStack.getArtifactId()).getTemplate());
        assertThat(actual.get("Resources")).isNotEmpty();

        var cloudFrontStack = new CloudFrontStack(app, entries,certificateConfiguration);
        actual = JSON.valueToTree(app.synth().getStackArtifact(cloudFrontStack.getArtifactId()).getTemplate());
        assertThat(actual.get("Resources")).isNotEmpty();
    }
}
