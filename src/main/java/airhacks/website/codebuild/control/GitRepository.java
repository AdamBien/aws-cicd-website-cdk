package airhacks.website.codebuild.control;

import airhacks.website.Configuration;

public record GitRepository(String owner,String repository,String branch) {

    public static GitRepository create(){
        return new GitRepository(Configuration.owner, Configuration.repository, Configuration.branch);
    }
}
