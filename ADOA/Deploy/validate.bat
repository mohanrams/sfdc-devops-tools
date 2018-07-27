@echo off

set statusTxt=temp
<nul set /p statusTxt=Start Deploy .....
:: Update with the path to the Salesforce Repository
set WORKSPACE=
:: Update with the branch to be validated
set GIT_BRANCH=
:: Update with the Sandbox name
set SANDBOX=
:: The possible values are:
:: Main_Package to validate the main package
:: Destructive to validate the destructive changes package
set PackageToValidate=Main_Package

ant -DSandbox=%SANDBOX% -DPackageToValidate="%PackageToValidate%" startFullValidationProcess
echo DONE
