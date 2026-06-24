package airhacks.website.route53.control;

import airhacks.website.Configuration.CertificateValidationConfiguration;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.AaaaRecord;
import software.amazon.awscdk.services.route53.CnameRecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.constructs.Construct;

public interface Route53 {

        static void setupAliasRecord(Construct scope, Distribution distribution, String domainName,
                        CertificateValidationConfiguration certificateConfiguration) {

                var hostedZone = HostedZone.Builder.create(scope, "HostedZone")
                                .zoneName(domainName)
                                .comment("zone for external domain")
                                .build();
                var cloudFrontTarget = new CloudFrontTarget(distribution);
                AaaaRecord.Builder.create(scope, "AliasRecord")
                                .zone(hostedZone)
                                .target(RecordTarget.fromAlias(cloudFrontTarget))
                                .build();
                ARecord.Builder.create(scope, "IPv4AliasRecord")
                                .zone(hostedZone)
                                .target(RecordTarget.fromAlias(cloudFrontTarget))
                                .build();
                //The external DNS provider must maintain validation records.
                //On the first deploy the ACM-emitted values are still unknown, so skip the
                //record until cert.validation.record.name / cert.validation.domain.name are set.
                if (!certificateConfiguration.externalDnsProvider() && hasValidationRecord(certificateConfiguration)) {
                        CnameRecord.Builder.create(scope, "CertValidation")
                                        .zone(hostedZone)
                                        .comment("requested from ACM for validation")
                                        .recordName(certificateConfiguration.recordName())
                                        .domainName(certificateConfiguration.domainName())
                                        .ttl(Duration.minutes(5))
                                        .build();
                }
        }

        static boolean hasValidationRecord(CertificateValidationConfiguration configuration) {
                return isPresent(configuration.recordName()) && isPresent(configuration.domainName());
        }

        static boolean isPresent(String value) {
                return value != null && !value.isBlank();
        }
}
