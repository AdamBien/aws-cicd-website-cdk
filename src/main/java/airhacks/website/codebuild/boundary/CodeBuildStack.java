package airhacks.website.codebuild.boundary;


import java.util.List;

import airhacks.website.Configuration.BuildConfiguration;
import airhacks.website.Configuration.DomainEntriesConfiguration;
import airhacks.website.codebuild.control.BucketAccessingBuild;
import airhacks.website.codebuild.control.GitRepository;
import airhacks.website.codebuild.entity.WebsiteBuildConfiguration;
import airhacks.website.s3.control.Buckets;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.codebuild.IBuildImage;
import software.amazon.awscdk.services.codebuild.IProject;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.IAction;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionType;
import software.amazon.awscdk.services.codepipeline.actions.CodeStarConnectionsSourceAction;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

public class CodeBuildStack extends Stack{

    public static IBuildImage BUILD_IMAGE = LinuxBuildImage.STANDARD_7_0;
    static Artifact SOURCE_OUTPUT = Artifact.artifact("source");

    public CodeBuildStack(Construct scope, DomainEntriesConfiguration domainConfiguration,IBucket websiteBucket,BuildConfiguration buildConfiguration) {
                super(scope, domainConfiguration.appNameWithDomain() + "-codepipeline");
        var appName = domainConfiguration.appName();
        var logGroup = createLogGroup(appName);
        var artifactBucket = Buckets.createPrivateBucket(this);
        var domainName = websiteBucket.getBucketName();
        var buildSpec = WebsiteBuildConfiguration.createBuildSpec(domainName);
        var buildStack = new BucketAccessingBuild(this, appName, artifactBucket,websiteBucket, logGroup,buildSpec);
                
        var buildProject = buildStack.getPipelineProject();
        var pipeline = Pipeline.Builder.create(this, appName + "Pipeline")
                .crossAccountKeys(false)
                .artifactBucket(artifactBucket)
                .pipelineName(appName)
                .build();
        pipeline.addStage(createStage("github-checkout",
                List.of(createGithubConnection(buildConfiguration.codeStarConnectionARN(),
                        buildConfiguration.gitRepository()))));

        var devActions = List.of(
                createCodeBuildActionWithOutput(buildProject, SOURCE_OUTPUT, "build",
                        CodeBuildActionType.BUILD, 1));
        pipeline.addStage(createStage("DEV", devActions));

        CfnOutput.Builder.create(this, "PipelineOutput").value(pipeline.getPipelineArn()).build();
    }





    IAction createCodeBuildActionWithOutput(IProject project, Artifact input, String actionName,
            CodeBuildActionType actionType, int runOrder) {
        return CodeBuildAction.Builder.create()
                .project(project)
                .input(input)
                .actionName(actionName)
                .input(SOURCE_OUTPUT)
                .runOrder(runOrder)
                .type(actionType)
                .build();
    }

    StageOptions createStage(String stageName, List<IAction> actions) {
        return StageOptions.builder()
                .stageName(stageName)
                .actions(actions)
                .build();
    }

    CodeStarConnectionsSourceAction createGithubConnection(String codestarConnectionARN,
            GitRepository gitRepository) {
        return CodeStarConnectionsSourceAction.Builder.create()
                .actionName("checkout-from-github")
                .branch(gitRepository.branch())
                .repo(gitRepository.repository())
                .owner(gitRepository.owner())
                .output(SOURCE_OUTPUT)
                .connectionArn(codestarConnectionARN)
                .build();
    }

    LogGroup createLogGroup(String project) {
        return LogGroup.Builder.create(this, "BuildLogGroup")
                .logGroupName("/codepipeline/" + project + "/build")
                .retention(RetentionDays.FIVE_DAYS)
                .build();

    }    
    
}
