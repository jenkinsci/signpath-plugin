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
        if(installSignPathModule)
            portablePowerShell.uninstallSignPathModule();
        portablePowerShell.close();
    }

    public String getPowerShellExecutable() {
        return portablePowerShell.getPowerShellExecutable();
    }
}
