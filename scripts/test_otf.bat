@echo off
ranflood.exe flood start on-the-fly C:\Users\User\ranFloodTest\test
timeout /t 2 /nobreak
ranflood.exe flood list
set /p ID='dammi id'
rem echo %ID%
ranflood.exe flood stop on-the-fly %ID%
pause
