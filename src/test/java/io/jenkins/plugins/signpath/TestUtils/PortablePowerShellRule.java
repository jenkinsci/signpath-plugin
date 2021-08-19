package io.jenkins.plugins.signpath.TestUtils;

import org.junit.rules.ExternalResource;

public class PortablePowerShellRule extends ExternalResource {
    private final boolean installSignPathModule;
    private PortablePowerShell portablePowerShell;

    public PortablePowerShellRule(boolean importPowerShellModule) {
        this.installSignPathModule = importPowerShellModule;
    }

    @Override
    protected void before() throws Throwable {
        portablePowerShell = PortablePowerShell.setup();
        if(installSignPathModule)
            portablePowerShell.installSignPathModule();
    }

    @Override
    protected void after() {
        // no need to uninstall the PowerShell module since it is installed in the directory that is removed in close
        portablePowerShell.close();
    }

    public String getPowerShellExecutable() {
        return portablePowerShell.getPowerShellExecutable();
    }
}
