package io.jenkins.plugins.SignPath.TestUtils;

import hudson.model.Result;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import org.eclipse.jgit.lib.ObjectId;

import java.util.Arrays;

public class BuildDataDomainObjectMother {
    public static Build CreateRandomBuild(int buildNumber) {
        String commitId = Some.sha1Hash();
        int branchCount = Some.integer(1, 2);
        Branch[] branches = new Branch[branchCount];
        for (int i = 0; i < branchCount; i++) {
            branches[i] = CreateRandomBranch();
        }

        return CreateBuild(buildNumber, commitId, branches);
    }

    public static Build CreateBuild(int buildNumber, String commitId, Branch... branches){
        Result buildResult = Result.SUCCESS;
        Revision revision = new Revision(ObjectId.fromString(commitId), Arrays.asList(branches));
        return new Build(revision, buildNumber, buildResult);
    }

    public static Branch CreateRandomBranch(){
        return new Branch(Some.stringNonEmpty(), ObjectId.fromString(Some.sha1Hash()));
    }

    public static Branch CreateBranch(String branchId, String branchName){
        return new Branch(branchName, ObjectId.fromString(branchId));
    }
}
