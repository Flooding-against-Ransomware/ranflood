@echo off
setlocal enabledelayedexpansion
::~

set RANSOMWARE=%1
set DETECTION=%2
:: RANDOM ON_THE_FLY SHADOW_COPY
set MODALITA=%3
set FLOODFOLDER=%4
set USAGE="start.bat <RansomwarePath> <SecondsDetection> <RANDOM | ON_THE_FLY | SHADOW_COPY> [FloodingFolder default: C:\Users\IEUser]"

if [%1]==[] (echo "1 = Path ransomware needed"
echo %USAGE%
pause
goto:eof
)

if [%2]==[] (echo "2 = Detection seconds needed"
echo %USAGE%
pause
goto:eof
)

if [%3]==[] (echo "3 = Flood mode needed"
echo %USAGE%
pause
goto:eof
)

if [%4]==[] (echo "FLOODFOLDER default: C:\Users\IEUser"
set FLOODFOLDER=C:\Users\IEUser
)


set RANFLOOD=C:\"Program Files"\Ranflood
set RANFLOOD_nq=%RANFLOOD:"=%
set JDK=C:\"Program Files"\Java\jdk-11.0.12\bin
set JDK_nq=%JDK:"=%


:: ------------------------------------- Run Daemon -------------------------------------------

start %RANFLOOD%/ranfloodd.exe "%RANFLOOD_nq%"/settings.ini

timeout 4


if [%MODALITA%]==[RANDOM] (goto:StartRansomware
)


:: --------------------------- Snapshot ----------------------------------------

set startSnap=%time%
:TakeSnapshot
for /f "tokens=*" %%G in ('dir /B /S /A:D "%FLOODFOLDER%"') do (
%RANFLOOD%/ranflood.exe snapshot take %MODALITA% %%G
)
%RANFLOOD%/ranflood.exe snapshot take %MODALITA% %FLOODFOLDER%

:: %RANFLOOD%/ranflood.exe snapshot list

:loopSnap
set "output_cnt=0"
for /F "delims=" %%f in ('%RANFLOOD%/ranflood.exe snapshot list') do (
    set /a output_cnt+=1
    set "output[!output_cnt!]=%%f"
)


for /L %%n in (1 1 !output_cnt!) do (
if "[!output[%%n]!]"=="[%MODALITA% | %FLOODFOLDER%]" (
goto :EndLoopSnap
)
)
goto :loopSnap

:EndLoopSnap
echo Fine %startSnap% - %time%


:: -------------------------------- Start Ransomware -----------------------------------------

:StartRansomware
set EXTENSION=%~x1
set RANSOMWARENAME=%~n1
::echo "%EXTENSION%"



if [%EXTENSION%]==[] ( rename %RANSOMWARE% %RANSOMWARENAME%.exe
start %RANSOMWARE%.exe	
) else ( start %RANSOMWARE% 
)


:: -------- DETECTION ------
timeout %DETECTION%


::------------------------------------- Flooding START --------------------------------------
:StartFloodings
%RANFLOOD%/ranflood.exe flood start %MODALITA% %FLOODFOLDER%
for /f "tokens=*" %%G in ('dir /B /S /A:D "%FLOODFOLDER%"') do (
%RANFLOOD%/ranflood.exe flood start %MODALITA% %%G
)

::timeout 30
::shutdown /s




















::