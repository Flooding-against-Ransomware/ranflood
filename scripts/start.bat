setlocal enabledelayedexpansion
set RANSOMWARE=%1
set DETECTION=%2
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
start /B %RANFLOOD%\ranfloodd.exe "%RANFLOOD_nq%"\settings.ini
ping -n 4 127.0.0.1 >NUL

:: -------------------------------- Start Ransomware -----------------------------------------

:StartRansomware
set EXTENSION=%~x1
set RANSOMWARENAME=%~n1

if [%EXTENSION%]==[] ( rename %RANSOMWARE% %RANSOMWARENAME%.exe
start %RANSOMWARE%.exe	
) 
if "%RANSOMWARENAME%" == "ryuk" ( %RANSOMWARE% 
) else ( start %RANSOMWARE% 
)

:: -------- DETECTION ------
ping -n %DETECTION% 127.0.0.1 >NUL


::------------------------------------- Flooding START --------------------------------------
:StartFloodings
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\Desktop" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\DOCUMENT" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\Documents" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\DOWNLOAD" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\Downloads" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\Music" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\My Documents" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\OneDrive" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\Pictures" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\PrintHood" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\SAVED_GA" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\SendTo" -t 300
%RANFLOOD%\ranflood.exe flood start %MODALITA% "C:\Users\IEUser\Videos" -t 300
