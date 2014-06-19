@echo off
rem
rem Resin startup script to start the command line
rem 
@echo on

java -jar %~dp0\..\lib\resin.jar %*

