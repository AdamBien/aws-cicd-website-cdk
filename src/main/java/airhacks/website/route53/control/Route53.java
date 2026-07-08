package airhacks.website.route53.control;

import java.util.List;

import airhacks.website.Configuration.AlternativeDomain;
import airhacks.website.Configuration.CertificateValidationConfiguration;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.AaaaRecord;
import software.amazon.awscdk.services.route53.CnameRecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.constructs.Construct;

public interface Route53 {

        static void setupAliasRecord(Construct scope, Distribution distribution, String domainName,
                        String hostedZoneId, CertificateValidationConfiguration certificateConfiguration) {

                var hostedZone = resolveHostedZone(scope, domainName, hostedZoneId,
                                certificateConfiguration.externalDnsProvider());
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

        /**
         * Alias records for alternative apex domains served by the same distribution.
         * Alternatives always require an existing Route53 public hosted zone
         * ({@code alternative.hosted.zone.id}) — {@code external.dns.provider} is a
         * primary-domain concern and no zone is ever created here. The construct ids of the
         * primary domain stay unsuffixed, so existing deployments are unaffected. No
         * validation CNAME either: the ACM console "Create records in Route 53" button
         * covers all certificate names.
         */
        static void setupAlternativeAliasRecords(Construct scope, Distribution distribution,
                        List<AlternativeDomain> alternativeDomains) {
                var cloudFrontTarget = new CloudFrontTarget(distribution);
                alternativeDomains.forEach(alternative -> {
                        var suffix = alternative.domainName().replace(".", "-");
                        var hostedZone = HostedZone.fromHostedZoneAttributes(scope, "HostedZone-" + suffix,
                                        HostedZoneAttributes.builder()
                                                        .hostedZoneId(alternative.hostedZoneId())
                                                        .zoneName(alternative.domainName())
                                                        .build());
                        AaaaRecord.Builder.create(scope, "AliasRecord-" + suffix)
                                        .zone(hostedZone)
                                        .target(RecordTarget.fromAlias(cloudFrontTarget))
                                        .build();
                        ARecord.Builder.create(scope, "IPv4AliasRecord-" + suffix)
                                        .zone(hostedZone)
                                        .target(RecordTarget.fromAlias(cloudFrontTarget))
                                        .build();
                });
        }

        /**
         * Resolves the hosted zone according to the DNS ownership model:
         * <ul>
         *   <li>{@code externalDnsProvider == false} — the domain is registered in Route53, so an
         *       authoritative zone already exists. Reuse it via {@code hosted.zone.id}; creating a
         *       second zone would be ignored by DNS.</li>
         *   <li>{@code externalDnsProvider == true} — the registrar is external. Create a new zone so
         *       its NS records can be configured at the registrar (Route53 delegation).</li>
         * </ul>
         */
        static IHostedZone resolveHostedZone(Construct scope, String domainName, String hostedZoneId,
                        boolean externalDnsProvider) {
                if (externalDnsProvider) {
                        return HostedZone.Builder.create(scope, "HostedZone")
                                        .zoneName(domainName)
                                        .comment("zone for external domain")
                                        .build();
                }
                return HostedZone.fromHostedZoneAttributes(scope, "HostedZone",
                                HostedZoneAttributes.builder()
                                                .hostedZoneId(hostedZoneId)
                                                .zoneName(domainName)
                                                .build());
        }

        static boolean hasValidationRecord(CertificateValidationConfiguration configuration) {
                return isPresent(configuration.recordName()) && isPresent(configuration.domainName());
        }

        static boolean isPresent(String value) {
                return value != null && !value.isBlank();
        }
}
