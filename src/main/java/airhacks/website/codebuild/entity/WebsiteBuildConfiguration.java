package airhacks.website.codebuild.entity;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.services.codebuild.BuildSpec;

public interface WebsiteBuildConfiguration {

    static BuildSpec createBuildSpec(String domainName) {
        var spec = Map.of("version", "0.2",
                "phases", phases(domainName));
        return BuildSpec.fromObject(spec);
    }

    static Map<String, Object> phases(String domainName) {
        return Map.of("build", Map.of("commands", commands(domainName)));
    }

    static List<String> commands(String domainName) {
        return """
                echo "publishing static assets to %1$s"
                aws s3 rm s3://%1$s --recursive
                echo "emptying bucket ${appname}"
                aws s3 cp . s3://%1$s --recursive
                """
                .formatted(domainName)
                .lines()
                .map(String::trim)
                .toList();
    }

}
