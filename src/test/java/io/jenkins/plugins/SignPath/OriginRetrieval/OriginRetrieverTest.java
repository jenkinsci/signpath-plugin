package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.*;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import jenkins.model.Jenkins;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class OriginRetrieverTest {

    private OriginRetriever sut;

    @Before
    public void setup(){
        sut = new OriginRetriever();
    }

    @Test
    public void retrieveForBuild() {
        String repositoryUrl = Some.stringNonEmpty();
        String sourceControlManagementType = Some.stringNonEmpty();
        String buildUrl = Some.stringNonEmpty();
        String commitId = Some.sha1Hash();
        String branchId = Some.sha1Hash();
        String branchName = Some.stringNonEmpty();
        Integer buildNumber = 99;

        BuildData buildData = new BuildData(sourceControlManagementType);
        buildData.addRemoteUrl(repositoryUrl);
        buildData.saveBuild(CreateRandomBuild(101));
        buildData.saveBuild(CreateBuild(buildNumber, commitId, CreateBranch(branchId, branchName)));
        buildData.saveBuild(CreateRandomBuild(102));

        // TODO SIGN-3326: Find out what run contains, try to fake it?
        // ACT
        SigningRequestOriginSubmitModel result = sut.retrieveForBuild(null);

        // ASSERT
        assertEquals(repositoryUrl, result.getRepositoryMetadata().getRepositoryUrl());
        assertEquals(sourceControlManagementType, result.getRepositoryMetadata().getSourceControlManagementType());
        assertEquals(branchName, result.getRepositoryMetadata().getBranchName());
        assertEquals(commitId, result.getRepositoryMetadata().getCommitId());

        assertEquals(buildUrl, result.getBuildUrl());
    }

    private Build CreateRandomBuild(int buildNumber){
        String branchName = Some.stringNonEmpty();
        String commitId = Some.sha1Hash();
        String branchId = Some.sha1Hash();
        return CreateBuild(buildNumber, commitId, CreateBranch(branchId, branchName));
    }

    private Build CreateBuild(int buildNumber, String commitId, Branch... branches){
        Result buildResult = Result.SUCCESS;
        Revision revision = new Revision(ObjectId.fromString(commitId), Arrays.asList(branches));
        return new Build(revision, buildNumber, buildResult);
    }

    private Branch CreateBranch(String branchId, String branchName){
        return new Branch(branchName, ObjectId.fromString(branchId));
    }
}
