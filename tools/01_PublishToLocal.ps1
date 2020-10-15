$ModuleName="SignPath"
$SourcePath="C:\Development\signpath.application\src\Applications.Api\wwwroot\Tools\"
$ModulePath = "$PSScriptRoot\$ModuleName\"

Remove-Item -Path $ModulePath -Force -Recurse -ErrorAction Ignore
New-Item -Path $ModulePath -ItemType Directory

Copy-Item "$($SourcePath)$($ModuleName).psd1" -Destination "$($ModulePath)"
Copy-Item "$($SourcePath)$($ModuleName).psm1" -Destination "$($ModulePath)"

Publish-Module -Path $ModulePath -Repository LocalPowershellRepository