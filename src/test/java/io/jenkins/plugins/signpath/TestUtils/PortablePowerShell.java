package io.jenkins.plugins.signpath.TestUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class PortablePowerShell implements Closeable {

    private final File directory;
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

        System.out.println(extractionDirectory.toString());

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

    private PortablePowerShell(File directory, String executableName) {
        this.directory = directory;
        this.powerShellExecutable = new File(directory, executableName).toString();
    }

    public void installSignPathModule() {
        runPowerShellCommand("Install-Module SignPath -Scope CurrentUser -Repository PSGallery", 20);
    }

    public void uninstallSignPathModule () {
        runPowerShellCommand("Uninstall-Module SignPath -Force", 10);
    }

    private void runPowerShellCommand(String command, int timeoutInSeconds) {
        DefaultPowerShellExecutor executor = new DefaultPowerShellExecutor(powerShellExecutable, System.out);
        PowerShellExecutionResult result = executor.execute(new PowerShellCommand(command), timeoutInSeconds);
        Assert.assertFalse(result.getHasError());
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void close() {
        //noinspection ResultOfMethodCallIgnored
        directory.delete();
    }

    public String getPowerShellExecutable() {
        return powerShellExecutable;
    }
}
