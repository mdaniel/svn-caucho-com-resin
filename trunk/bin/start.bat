@echo off
rem
rem Resin startup script which runs just like resin.exe but doesn't need
rem the "start" command
@echo on

%~dp0\..\resin %* start

