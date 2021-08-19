package io.jenkins.plugins.signpath.TestUtils;

import hudson.Functions;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.DefaultPowerShellExecutor;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellCommand;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class PortablePowerShell implements Closeable {

    private final File extractionDirectory;
    private final String moduleDirectory;
    private final String powerShellExecutable;

    public static PortablePowerShell setup() throws IOException, ArchiveException {
        if(Functions.isWindows()) {
            return setupForWindows();
        } else {
            return setupForLinux();
        }
    }

    private static PortablePowerShell setupForWindows() throws IOException, ArchiveException {
        try (InputStream resource = PortablePowerShell.class.getClassLoader().getResourceAsStream("PowerShell-7.1.4-win-x64.zip");
             ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(resource))
        {
            return setup(input, "pwsh.exe");
        }
    }

    private static PortablePowerShell setupForLinux() throws IOException {
        try (InputStream resource = PortablePowerShell.class.getClassLoader().getResourceAsStream("powershell-7.1.4-linux-x64.tar.gz");
             InputStream buffered = new BufferedInputStream(resource);
             InputStream gz = new GzipCompressorInputStream(buffered);
             ArchiveInputStream input = new TarArchiveInputStream(gz))
        {
            PortablePowerShell portablePowerShell = setup(input, "pwsh");

            Path executable = new File(portablePowerShell.powerShellExecutable).toPath();
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(executable);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(new File(portablePowerShell.powerShellExecutable).toPath(), permissions);

            return portablePowerShell;
        }
    }

    private static PortablePowerShell setup(ArchiveInputStream input, String executableName) throws IOException {
        File extractionDirectory = Files.createTempDirectory(PortablePowerShell.class.getName()).toFile();

        ArchiveEntry entry = input.getNextEntry();

        while(entry != null) {
            File entryDestination = new File(extractionDirectory, entry.getName());
            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                entryDestination.getParentFile().mkdirs();
                try (OutputStream out = new FileOutputStream(entryDestination)) {
                    IOUtils.copy(input, out);
                }
            }

            entry = input.getNextEntry();
        }

        return new PortablePowerShell(extractionDirectory, executableName);
    }

    private PortablePowerShell(File directory, String executableName) throws IOException {
        this.extractionDirectory = directory;
        this.powerShellExecutable = new File(directory, executableName).getCanonicalPath();
        this.moduleDirectory = new File(directory, "Modules").getCanonicalPath();
    }

    public void installSignPathModule() {
        runPowerShellCommand("Find-Module SignPath -Repository PSGallery | Save-Module -Path '" + moduleDirectory + "'", 20);
    }

    private void runPowerShellCommand(String command, int timeoutInSeconds) {
        DefaultPowerShellExecutor executor = new DefaultPowerShellExecutor(powerShellExecutable, System.out);
        PowerShellExecutionResult result = executor.execute(new PowerShellCommand(command), timeoutInSeconds);
        Assert.assertFalse(result.getHasError());
    }

    @Override
    public void close() {
        try {
            FileUtils.deleteDirectory(extractionDirectory);
        } catch (IOException e) {
            System.err.format("Could not delete temporary directory '%s'.%n", extractionDirectory.toString());
        }
    }

    public String getPowerShellExecutable() {
        return powerShellExecutable;
    }
}
