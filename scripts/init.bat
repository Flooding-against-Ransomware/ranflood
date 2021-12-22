@echo off

set rfPathFolder=%1

if [%1]==[] (echo "init.bat <Ranflood folder>"
pause
goto:eof
)

set RANFLOOD="C:/Program Files/Ranflood"
set RANFLOOD_nq=%RANFLOOD:"=%
set JDK="C:\Program Files\Java\jdk-11.0.12\bin"

if exist %RANFLOOD% (rmdir /s /q %RANFLOOD%
)

mkdir %RANFLOOD%
robocopy %rfPathFolder% %RANFLOOD% > nul
mkdir %RANFLOOD%/ranflood_testsite

(
echo [OnTheFlyFlooder]
echo Signature_DB = %RANFLOOD_nq%/ranflood_testsite/signatures.db
echo [ShadowCopyFlooder]
echo ArchiveDatabase = %RANFLOOD_nq%/ranflood_testsite/archives.db
echo ArchiveRoot = %RANFLOOD_nq%/ranflood_testsite/archives
echo [ZMQ_JSON_Server]
echo address = tcp://localhost:7890 
) > %RANFLOOD%/settings.ini






