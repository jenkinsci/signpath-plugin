package io.jenkins.plugins.signpath.TestUtils;

import hudson.model.Result;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import org.eclipse.jgit.lib.ObjectId;

import java.util.Arrays;

public class BuildDataDomainObjectMother {

    public static Build createRandomBuild(int buildNumber) {
        String commitId = Some.sha1Hash();
        Branch[] branches = new Branch[] { createRandomBranch() };
        return createBuild(buildNumber, commitId, branches);
    }

    public static Build createBuild(int buildNumber, String commitId, Branch... branches) {
        Result buildResult = Result.SUCCESS;
        Revision revision = new Revision(ObjectId.fromString(commitId), Arrays.asList(branches));
        return new Build(revision, buildNumber, buildResult);
    }

    public static Branch createRandomBranch() {
        return new Branch(Some.stringNonEmpty(), ObjectId.fromString(Some.sha1Hash()));
    }

    public static Branch createBranch(String branchId, String branchName) {
        return new Branch(branchName, ObjectId.fromString(branchId));
    }
}