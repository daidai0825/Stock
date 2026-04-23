@REM Apache Maven Wrapper for Windows
@REM ==============================================================================
@REM Licensed under the Apache License, Version 2.0 (the "License")
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM      https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ==============================================================================

@echo off
setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

@REM Read maven wrapper properties
set MAVEN_HOME_DIR=%APP_HOME%\.mvn\wrapper
set MAVEN_WRAPPER_PROPERTIES=%MAVEN_HOME_DIR%\maven-wrapper.properties

@REM Load distribution URL from properties
setlocal enableextensions enabledelayedexpansion
for /f "tokens=1,* delims==" %%A in (%MAVEN_WRAPPER_PROPERTIES%) do (
    if "%%A"=="distributionUrl" set distributionUrl=%%B
)

@REM Set default if not found
if "%distributionUrl%"=="" set distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip

@REM Create .mvn/wrapper directory if it doesn't exist
if not exist "%MAVEN_HOME_DIR%" mkdir "%MAVEN_HOME_DIR%"

@REM Check if Maven is already extracted
if not exist "%MAVEN_HOME_DIR%\apache-maven-3.9.9\bin\mvn.cmd" (
    echo Downloading Maven from %distributionUrl% ...
    set MAVEN_DIST=%MAVEN_HOME_DIR%\maven-dist.zip

    @REM Download using powershell
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%distributionUrl%', '%MAVEN_DIST%')"

    if errorlevel 1 (
        echo Error: Failed to download Maven
        exit /b 1
    )

    echo Extracting Maven distribution ...
    powershell -Command "Expand-Archive -Path '%MAVEN_DIST%' -DestinationPath '%MAVEN_HOME_DIR%'"

    if errorlevel 1 (
        echo Error: Failed to extract Maven
        exit /b 1
    )

    del "%MAVEN_DIST%"
    echo Maven distribution extracted successfully.
)

@REM Find Maven executable
set MVN_EXEC=%MAVEN_HOME_DIR%\apache-maven-3.9.9\bin\mvn.cmd

if not exist "%MVN_EXEC%" (
    @REM Try system mvn as fallback
    for /f "tokens=*" %%i in ('where mvn 2^>nul') do set MVN_EXEC=%%i
)

if exist "%MVN_EXEC%" (
    call "%MVN_EXEC%" %*
) else (
    echo Error: Maven executable not found. Please check your installation.
    exit /b 1
)

endlocal
