FROM docker.int.rubicon-it.com/default-docker/dotnet/core/sdk:3.1.402-nanoserver-1909

SHELL ["pwsh", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]

ENV JAVA_HOME C:\\openjdk
ENV INTERNAL_OPENJDK_INSTALLER_PATH C:\\openjdk.zip
ENV INTERNAL_MINGIT_INSTALLER_PATH C:\\mingit.zip

COPY "openjdk-15_windows-x64_bin.zip" ${INTERNAL_OPENJDK_INSTALLER_PATH}
COPY "MinGit-2.12.2.2-64-bit.zip" ${INTERNAL_MINGIT_INSTALLER_PATH}
COPY "agent.jar" C:\\agent.jar

COPY DockerStartup.ps1 .

RUN Write-Host 'Extracting OpenJDK...'; \
New-Item -ItemType Directory -Path $env:JAVA_HOME | Out-Null; \
Expand-Archive $env:INTERNAL_OPENJDK_INSTALLER_PATH -DestinationPath "C:\Temp\openjdk"; \
Move-Item "C:\Temp\openjdk\jdk-15\*" $env:JAVA_HOME; \
Remove-Item -Recurse "C:\Temp\openjdk"; \
Write-Host 'Removing OpenJDK Zip...'; \
Remove-Item $env:INTERNAL_OPENJDK_INSTALLER_PATH -Force; \
Write-Host 'OpenJDK installation complete.'

RUN Write-Host 'Extracting MinGit...'; \
Expand-Archive $env:INTERNAL_MINGIT_INSTALLER_PATH -DestinationPath C:\MinGit; \
Write-Host 'Removing MinGit Zip...'; \
Remove-Item $env:INTERNAL_MINGIT_INSTALLER_PATH -Force; \
Write-Host 'MinGit installation complete.'

ENV PORT 50923
EXPOSE ${Port}

ENTRYPOINT [ "pwsh", "-File", "DockerStartup.ps1"]