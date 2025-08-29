package airhacks.website.codebuild.control;

import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.CloudWatchLoggingOptions;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.LoggingOptions;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.logs.ILogGroup;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

public interface PublishingStage{


        public static PipelineProject create(Construct scope, String projectName,
                        IBucket sourceBucket, IBucket websiteBucket,ILogGroup logGroup, BuildSpec buildSpec) {
                var pipelineProject = PipelineProject.Builder
                                .create(scope, "PipelineProject")
                                .cache(Cache.none())
                                .buildSpec(buildSpec)
                                .projectName(projectName)
                                .logging(getLoggingOptions(logGroup))
                                .environment(BuildEnvironment.builder()
                                                .computeType(ComputeType.SMALL)
                                                .buildImage(LinuxBuildImage.STANDARD_7_0)
                                                .build())
                                .build();

                var serviceRole = pipelineProject.getRole();
                sourceBucket.grantReadWrite(serviceRole);
                websiteBucket.grantReadWrite(serviceRole);
                logGroup.grantWrite(serviceRole);
                return pipelineProject;
        }

        static LoggingOptions getLoggingOptions(ILogGroup logGroup) {
                return LoggingOptions.builder()
                                .cloudWatch(CloudWatchLoggingOptions.builder()
                                                .logGroup(logGroup)
                                                .enabled(true)
                                                .build())
                                .build();
        }

}
