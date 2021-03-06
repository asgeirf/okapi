cd ../../
call mvn clean
if ERRORLEVEL 1 goto end
call mvn install
if ERRORLEVEL 1 goto end

cd deployment/maven
call ant -f build_okapi-lib.xml
if ERRORLEVEL 1 goto end

call ant -f build_okapi-apps.xml -Dplatform=win32-x86
if ERRORLEVEL 1 goto end

call ant -f build_okapi-plugins.xml
if ERRORLEVEL 1 goto end

call ant -f build_omegat-plugins.xml
if ERRORLEVEL 1 goto end

cd ../../applications/integration-tests
call mvn clean integration-test

:end
pause