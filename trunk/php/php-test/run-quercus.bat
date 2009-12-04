@ECHO OFF

SET ARGS=
SET QUERCUS_CLASS=

FOR %%A IN (%*) DO (
  SET ARGS=%ARGS% %%A
)

SET QUERCUS_CLASS=com.caucho.quercus.QuercusEngine

IF defined RESIN_HOME java -cp %RESIN_HOME%\lib\resin.jar;%RESIN_HOME%\lib\resin-kernel.jar;%RESIN_HOME%\lib\quercus.jar;%PRO_HOME%\lib\pro.jar;%PRO_HOME%\lib\license.jar %QUERCUS_CLASS% %ARGS%
IF NOT DEFINED RESIN_HOME echo RESIN_HOME environment variable not set