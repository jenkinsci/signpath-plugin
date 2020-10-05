package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.OriginNotRetrievableException;
import io.jenkins.plugins.SignPath.TestUtils.BuildDataDomainObjectMother;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class OriginRetrieverTest {

    private OriginRetriever sut;

    @Mock
    IConfigFileProvider configFileProvider;

    @Mock
    Run<?, ?> run;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BuildData buildData;
    private int buildNumber;
    private String buildUrl;
    private String repositoryUrl;

    @Before
    public void setup(){
        String jenkinsRootUrl = Some.url();
        String jobUrl = Some.urlFragment();
        buildUrl = StringUtils.strip(jenkinsRootUrl,"/") + "/" + StringUtils.strip(jobUrl, "/");
        repositoryUrl = Some.stringNonEmpty();

        // hard-coded to avoid conflicts with other build numbers
        buildNumber = 99;
        buildData = new BuildData(Some.stringNonEmpty());
        buildData.addRemoteUrl(repositoryUrl);

        when(run.getUrl()).thenReturn(jobUrl);
        when(run.getNumber()).thenReturn(buildNumber);
        when(run.getAction(BuildData.class)).thenReturn(buildData);
        sut = new OriginRetriever(configFileProvider, run, jenkinsRootUrl);
    }

    @Test
    public void retrieveOrigin() throws IOException, OriginNotRetrievableException {
        String commitId = Some.sha1Hash();
        String branchId = Some.sha1Hash();
        String branchName = Some.stringNonEmpty();
        byte[] jobConfigXmlContent = Some.bytes();

        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(101));
        buildData.saveBuild(BuildDataDomainObjectMother.CreateBuild(buildNumber, commitId, BuildDataDomainObjectMother.CreateBranch(branchId, branchName)));
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(102));

        TemporaryFile jobConfigTemporaryFile = TemporaryFileUtil.create(jobConfigXmlContent);
        when(configFileProvider.retrieveBuildConfigFile()).thenReturn(jobConfigTemporaryFile.getFile());

        // ACT
        SigningRequestOriginModel result = sut.retrieveOrigin();
        jobConfigTemporaryFile.close();

        // ASSERT
        assertEquals(repositoryUrl, result.getRepositoryMetadata().getRepositoryUrl());

        // we expect git hardcoded (no matter what the value is, as we only support git atm via git.BuildData)
        assertEquals("git", result.getRepositoryMetadata().getSourceControlManagementType());
        assertEquals(branchName, result.getRepositoryMetadata().getBranchName());
        assertEquals(commitId, result.getRepositoryMetadata().getCommitId());

        assertEquals(buildUrl, result.getBuildUrl());

        TemporaryFile buildSettingsFile = result.getBuildSettingsFile();
        assertArrayEquals(jobConfigXmlContent, TemporaryFileUtil.getContentAndDispose(buildSettingsFile));
    }

    @Test
    public void retrieveOrigin_NoMatchingBuildNumber_Throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(buildNumber + 1));

        ThrowingRunnable act = () -> sut.retrieveOrigin();

        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("No builds with build number '%d' found.", buildNumber));
    }

    @Test
    public void retrieveOrigin_MultipleMatchingBuildNumber_Throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(buildNumber));
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(buildNumber));

        ThrowingRunnable act = () -> sut.retrieveOrigin();

        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("2 builds with build number '%d' found. This is not supported.", buildNumber));
    }

    @Test
    public void retrieveOrigin_MultipleBranches_Throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.CreateBuild(buildNumber, Some.sha1Hash(), BuildDataDomainObjectMother.CreateRandomBranch(), BuildDataDomainObjectMother.CreateRandomBranch()));

        ThrowingRunnable act = () -> sut.retrieveOrigin();

        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("2 builds with build number '%d' found. This is not supported.", buildNumber));
    }

    @Test
    public void retrieveOrigin_NoRemoteUrls_Throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(buildNumber));
        buildData.remoteUrls.clear();

        ThrowingRunnable act = () -> sut.retrieveOrigin();

        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("No remote urls for build with build number '%d' found.", buildNumber));
    }

    @Test
    public void retrieveOrigin_MultipleRemoteUrls_Throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(buildNumber));
        buildData.addRemoteUrl(Some.stringNonEmpty());

        ThrowingRunnable act = () -> sut.retrieveOrigin();

        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("2 remote urls for build with build number '%d' found. This is not supported.", buildNumber));
    }
}
