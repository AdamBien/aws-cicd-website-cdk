package airhacks.website.codebuild.control;

import airhacks.website.Configuration;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.CloudWatchLoggingOptions;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.LoggingOptions;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.logs.ILogGroup;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

public class BucketAccessingBuild extends NestedStack {

        PipelineProject pipelineProject;

        public BucketAccessingBuild(Construct scope, String projectName,
                        IBucket sourceBucket, IBucket websiteBucket,ILogGroup logGroup, BuildSpec buildSpec) {
                super(scope, "BucketAccessingBuild");
                this.getNode().addDependency(logGroup);
                this.pipelineProject = PipelineProject.Builder
                                .create(this, "PipelineProject")
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

                CfnOutput.Builder.create(this, "ServiceRoleARN").value(serviceRole.getRoleArn()).build();
        }

        LoggingOptions getLoggingOptions(ILogGroup logGroup) {
                return LoggingOptions.builder()
                                .cloudWatch(CloudWatchLoggingOptions.builder()
                                                .logGroup(logGroup)
                                                .enabled(true)
                                                .build())
                                .build();
        }

        public Project getPipelineProject() {
                return this.pipelineProject;
        }

}
