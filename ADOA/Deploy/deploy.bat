@echo off

set statusTxt=temp
<nul set /p statusTxt=Start Deploy .....
echo 
:: Update with the path to the Salesforce Repository
set WORKSPACE=
:: Update with the branch to be deployed
set GIT_BRANCH=
:: Update with the Sandbox name
set SANDBOX=
:: The possible values are:
:: Main_Package to deploy the main package
:: Destructive to deploy the destructive changes package
set PackageToValidate=Main_Package

ant -DSandbox=%SANDBOX% -DPackageToValidate="%PackageToValidate%"
echo %var%
echo DONE
