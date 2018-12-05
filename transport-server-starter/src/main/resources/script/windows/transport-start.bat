@echo off
rem Copyright (c) 2017 ~ 2025, the original author wangl.sir individual Inc,
rem All rights reserved. Contact us 983708408@qq.com
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem

setlocal enabledelayedexpansion

rem Title setting
TITLE Transport Server

rem Using pushd popd to set BASE_DIR to the absolute path
pushd %~dp0..\..
set BASE_DIR=%CD%
popd

rem
rem Global define.
rem
set MAIN_JAR_NAME=transport
set MAIN_CLASS=io.transport.Transport

set DATA_DIR=%BASE_DIR%
set CONF_DIR=%BASE_DIR%\conf
set LIBS_DIR=%BASE_DIR%\libs
rem Please note that the Java -cp loading is sequential. See: https://www.jianshu.com/p/23e0517d76f7
rem and https://docs.oracle.com/javase/8/docs/technotes/tools/unix/classpath.html
set APP_CLASSPATH=";%CONF_DIR%;%BASE_DIR%\libs\*"

IF NOT EXIST %LIBS_DIR%\%MAIN_JAR_NAME%.jar (
	echo "No transport.jar starter, or path error."
	rem For wait pause.
	pause
)

rem Which java to use
IF ["%JAVA_HOME%"] EQU [""] (
	set JAVA=java
) ELSE (
	set JAVA="%JAVA_HOME%/bin/java"
)

rem Memory options, detect OS architecture
wmic os get osarchitecture | find /i "32-bit" >nul 2>&1
IF NOT ERRORLEVEL 1 (
	rem 32-bit OS
	set HEAP_OPTS=-Xmx512M -Xms512M
) ELSE (
	rem 64-bit OS
	set HEAP_OPTS=-Xmx768M -Xms768M
)

rem JVM performance options
IF ["%JVM_PERFORMANCE_OPTS%"] EQU [""] (
	set JVM_PERFORMANCE_OPTS=-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC -Djava.awt.headless=true
)

rem JMX settings
IF ["%JMX_OPTS%"] EQU [""] (
	set JMX_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
)

rem JMX port to use
IF ["%JMX_PORT%"] NEQ [""] (
	set JMX_OPTS=%JMX_OPTS% -Dcom.sun.management.jmxremote.port=5005
)

rem Generic jvm settings you want to add
IF ["%JAVA_OPTS%"] EQU [""] (
	set JAVA_OPTS=-Dfile.encoding=UTF-8
)

rem Transporter application options settings
IF ["%APP_OPTS%"] EQU [""] (
	set APP_OPTS=%APP_OPTS% --spring.profiles.active=test --server.tomcat.basedir=%DATA_DIR%
)

set COMMAND=%JAVA% -server %HEAP_OPTS% %JVM_PERFORMANCE_OPTS% %JMX_PORT% %JAVA_OPTS% -cp %APP_CLASSPATH% %MAIN_CLASS% %APP_OPTS%
rem echo.
rem echo %COMMAND%
rem echo.

%COMMAND%

rem For wait pause.
pause