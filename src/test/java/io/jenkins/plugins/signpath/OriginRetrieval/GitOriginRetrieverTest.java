package io.jenkins.plugins.signpath.OriginRetrieval;

import hudson.model.Run;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.OriginNotRetrievableException;
import io.jenkins.plugins.signpath.TestUtils.BuildDataDomainObjectMother;
import io.jenkins.plugins.signpath.TestUtils.Some;
import io.jenkins.plugins.signpath.TestUtils.TemporaryFileUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class GitOriginRetrieverTest {

    private GitOriginRetriever sut;

    @Mock
    ConfigFileProvider configFileProvider;

    @Mock
    Run<?, ?> run;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BuildData buildData;
    private int buildNumber;
    private String buildUrl;
    private String repositoryUrl;

    @Before
    public void setup() {
        String jenkinsRootUrl = Some.url();
        String jobUrl = Some.urlFragment();
        buildUrl = StringUtils.strip(jenkinsRootUrl, "/") + "/" + StringUtils.strip(jobUrl, "/");
        repositoryUrl = Some.stringNonEmpty();

        // hard-coded to avoid conflicts with other build numbers in the tests
        buildNumber = 99;
        buildData = new BuildData(Some.stringNonEmpty());
        buildData.addRemoteUrl(repositoryUrl);

        when(run.getUrl()).thenReturn(jobUrl);
        when(run.getNumber()).thenReturn(buildNumber);
        when(run.getAction(BuildData.class)).thenReturn(buildData);

        sut = new GitOriginRetriever(configFileProvider, run, jenkinsRootUrl);
    }

    @Theory
    public void retrieveOrigin() throws IOException, OriginNotRetrievableException {
        String commitId = Some.sha1Hash();
        String branchId = Some.sha1Hash();
        String branchName = Some.stringNonEmpty();
        byte[] jobConfigXmlContent = Some.bytes();

        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(101));
        buildData.saveBuild(BuildDataDomainObjectMother.createBuild(buildNumber, commitId, BuildDataDomainObjectMother.createBranch(branchId, branchName)));
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(102));

        TemporaryFile jobConfigTemporaryFile = TemporaryFileUtil.create(jobConfigXmlContent);
        when(configFileProvider.retrieveBuildConfigFile()).thenReturn(jobConfigTemporaryFile.getFile());

        // ACT
        SigningRequestOriginModel result = sut.retrieveOrigin();
        // This close is important to make sure that retrieveOrigin copies the contents into a new temporary file
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

    @DataPoints("allBranchNames")
    public static String[][] allBranchNames() {
        return new String[][]{
                new String[]{"develop", "develop"},
                new String[]{"refs/remotes/origin/master", "master"},
                new String[]{"refs/remotes/something/develop", "develop"},
                new String[]{"refs/remotes/origin/feature/SIGN-3415", "feature/SIGN-3415"}
        };
    }

    @Theory
    public void retrieveOrigin_withRefBranchName(@FromDataPoints("allBranchNames") String[] branchNames) throws IOException, OriginNotRetrievableException {
        String actualBranchName = branchNames[0];
        String expectedBranchName = branchNames[1];
        buildData.saveBuild(BuildDataDomainObjectMother.createBuild(buildNumber, Some.sha1Hash(), BuildDataDomainObjectMother.createBranch(Some.sha1Hash(), actualBranchName)));

        TemporaryFile jobConfigTemporaryFile = TemporaryFileUtil.create(Some.bytes());
        when(configFileProvider.retrieveBuildConfigFile()).thenReturn(jobConfigTemporaryFile.getFile());

        // ACT
        SigningRequestOriginModel result = sut.retrieveOrigin();

        // ASSERT
        assertEquals(expectedBranchName, result.getRepositoryMetadata().getBranchName());
    }

    @Theory
    public void retrieveOrigin_noMatchingBuildNumber_throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(buildNumber + 1));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveOrigin();

        // ASSERT
        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("No builds with build number '%d' found.", buildNumber));
    }

    @Theory
    public void retrieveOrigin_multipleMatchingBuildNumbers_throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(buildNumber));
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(buildNumber));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveOrigin();

        // ASSERT
        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("2 builds with build number '%d' found. This is not supported.", buildNumber));
    }

    @Theory
    public void retrieveOrigin_multipleBranches_throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.createBuild(buildNumber, Some.sha1Hash(), BuildDataDomainObjectMother.createRandomBranch(), BuildDataDomainObjectMother.createRandomBranch()));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveOrigin();

        // ASSERT
        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("2 builds with build number '%d' found. This is not supported.", buildNumber));
    }

    @Theory
    public void retrieveOrigin_noRemoteUrls_throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(buildNumber));
        buildData.remoteUrls.clear();

        // ACT
        ThrowingRunnable act = () -> sut.retrieveOrigin();

        // ASSERT
        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("No remote URLs for build with build number '%d' found.", buildNumber));
    }

    @Theory
    public void retrieveOrigin_multipleRemoteUrls_throws() {
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(buildNumber));
        buildData.addRemoteUrl(Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.retrieveOrigin();

        // ASSERT
        Throwable ex = assertThrows(OriginNotRetrievableException.class, act);
        assertEquals(ex.getMessage(), String.format("2 remote URLs for build with build number '%d' found. This is not supported.", buildNumber));
    }
}
