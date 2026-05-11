package airhacks.website.codebuild.entity;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.services.codebuild.BuildSpec;

public interface WebsiteBuildConfiguration {

    static BuildSpec createBuildSpec(String domainName, String distributionId) {
        var spec = Map.of("version", "0.2",
                "phases", phases(domainName, distributionId));
        return BuildSpec.fromObject(spec);
    }

    static Map<String, Object> phases(String domainName, String distributionId) {
        return Map.of("build", Map.of("commands", commands(domainName, distributionId)));
    }

    static List<String> commands(String domainName, String distributionId) {
        return """
                echo "publishing static assets to %1$s"
                echo "emptying bucket ${appname}"
                aws s3 rm s3://%1$s --recursive
                aws s3 cp . s3://%1$s --recursive
                echo "invalidating CloudFront distribution %2$s"
                aws cloudfront create-invalidation --distribution-id %2$s --paths "/*"
                """
                .formatted(domainName, distributionId)
                .lines()
                .map(String::trim)
                .toList();
    }

}
