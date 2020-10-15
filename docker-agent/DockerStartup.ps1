$env:PATH = $env:PATH + ';C:\MinGit\cmd\;C:\MinGit\cmd'
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::User)

C:\\openjdk\\bin\\java.exe -jar "C:\\agent.jar" -jnlpUrl "http://172.18.1.57:8080/jenkins/computer/agent01/slave-agent.jnlp" -workDir "C:\temp\jenkins"