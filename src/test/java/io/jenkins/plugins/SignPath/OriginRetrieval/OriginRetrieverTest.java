package io.jenkins.plugins.SignPath.OriginRetrieval;

import com.google.common.base.CharMatcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class OriginRetrieverTest {

    private OriginRetriever sut;

    @Mock
    IConfigFileProvider configFileProvider;

    @Mock
    Run run;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setup(){
        sut = new OriginRetriever(configFileProvider);
    }

    @Test
    public void retrieveForBuild() throws IOException {
        String repositoryUrl = Some.stringNonEmpty();
        String sourceControlManagementType = Some.stringNonEmpty();
        String commitId = Some.sha1Hash();
        String branchId = Some.sha1Hash();
        String branchName = Some.stringNonEmpty();
        String jenkinsRootUrl = Some.url();
        String jobUrl = Some.urlFragment();
        String buildUrl = CharMatcher.is('/').trimFrom(jenkinsRootUrl) + "/" + CharMatcher.is('/').trimFrom(jobUrl);
        byte[] jobConfigXmlContent = Some.bytes();

        // hard-coded to avoid conflicts with other build numbers
        Integer buildNumber = 99;

        BuildData buildData = new BuildData(sourceControlManagementType);
        buildData.addRemoteUrl(repositoryUrl);
        buildData.saveBuild(CreateRandomBuild(101));
        buildData.saveBuild(CreateBuild(buildNumber, commitId, CreateBranch(branchId, branchName)));
        buildData.saveBuild(CreateRandomBuild(102));

        when(run.getUrl()).thenReturn(jobUrl);
        when(run.getNumber()).thenReturn(buildNumber);
        when(run.getAction(BuildData.class)).thenReturn(buildData);

        TemporaryFile jobConfigTemporaryFile = TemporaryFileUtil.create(jobConfigXmlContent);
        when(configFileProvider.retrieveBuildConfigFile(run)).thenReturn(jobConfigTemporaryFile.getFile());

        // ACT
        SigningRequestOriginSubmitModel result = sut.retrieveForBuild(jenkinsRootUrl, run);
        jobConfigTemporaryFile.close();

        // ASSERT
        assertEquals(repositoryUrl, result.getRepositoryMetadata().getRepositoryUrl());
        assertEquals(sourceControlManagementType, result.getRepositoryMetadata().getSourceControlManagementType());
        assertEquals(branchName, result.getRepositoryMetadata().getBranchName());
        assertEquals(commitId, result.getRepositoryMetadata().getCommitId());

        assertEquals(buildUrl, result.getBuildUrl());

        try (TemporaryFile buildSettingsFile = result.getBuildSettingsFile()) {
            assertArrayEquals(jobConfigXmlContent, TemporaryFileUtil.getContent(buildSettingsFile));
        }
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
