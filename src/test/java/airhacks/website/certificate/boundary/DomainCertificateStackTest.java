package airhacks.website.certificate.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import airhacks.website.Configuration.AlternativeDomain;
import airhacks.website.Configuration.DomainEntriesConfiguration;

public class DomainCertificateStackTest {

    static DomainEntriesConfiguration configuration(List<AlternativeDomain> alternatives, String version) {
        return new DomainEntriesConfiguration("app", "example.com", "Z1", alternatives, version, null);
    }

    @Test
    public void historicalIdWithoutAlternativesAndVersion() {
        assertThat(DomainCertificateStack.certificateConstructId(configuration(List.of(), null)))
                .isEqualTo("DnsValidatedCertificate");
        assertThat(DomainCertificateStack.certificateConstructId(configuration(List.of(), " ")))
                .isEqualTo("DnsValidatedCertificate");
    }

    @Test
    public void versionReplacesCertificate() {
        assertThat(DomainCertificateStack.certificateConstructId(configuration(List.of(), "2")))
                .isEqualTo("DnsValidatedCertificate-v2");
        assertThat(DomainCertificateStack.certificateConstructId(configuration(List.of(), "2026-11-17")))
                .isEqualTo("DnsValidatedCertificate-v2026-11-17");
    }

    @Test
    public void versionCombinesWithAlternatives() {
        var alternatives = List.of(new AlternativeDomain("example.org", "Z2"));
        assertThat(DomainCertificateStack.certificateConstructId(configuration(alternatives, null)))
                .isEqualTo("DnsValidatedCertificate-example-org");
        assertThat(DomainCertificateStack.certificateConstructId(configuration(alternatives, "2")))
                .isEqualTo("DnsValidatedCertificate-example-org-v2");
    }
}
