/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */
using System;
using System.Reflection;
using System.Collections;
using System.Text;
using System.IO;
using Microsoft.Win32;
using System.Diagnostics;
using System.Windows.Forms;
using System.ServiceProcess;
using System.Threading;
using System.Configuration.Install;
using System.Runtime.Serialization.Formatters.Binary;

namespace Caucho
{
  public class Resin: ServiceBase
  {
    private static String HKEY_JRE = "Software\\JavaSoft\\Java Runtime Environment";
    private static String HKEY_JDK = "Software\\JavaSoft\\Java Development Kit";
    private static String CAUCHO_APP_DATA = "Caucho Technology\\Resin";
    
    private static String USAGE = @"usage: {0} [flags] [console | status | start | stop | restart | kill | shutdown]
  -h                                     : this help
  -verbose                               : information on launching java
  -java_home <dir>                       : sets the JAVA_HOME
  -java_exe <path>                       : path to java executable
  -classpath <dir>                       : java classpath
  -Jxxx                                  : JVM arg xxx
  -J-Dfoo=bar                            : Set JVM variable
  -Xxxx                                  : JVM -X parameter
  -install                               : install as NT service
  -install-as <name>                     : install as a named NT service
  -remove                                : remove as NT service
  -remove-as <name>                      : remove as a named NT service
  -user <name>                           : specify username for NT service
  -password <pwd>                        : specify password for NT
  -resin_home <dir>                      : home of Resin
  -root-directory <dir>                  : select a root directory
  -log-directory  <dir>                  : select a logging directory
  -dynamic-server <cluster:address:port> : initialize a dynamic server
  -watchdog-port  <port>                 : override the watchdog-port
  -server <id>                           : select a <server> to run
  -conf <resin.conf>                     : alternate configuration file";
    
    private static String REQUIRE_COMMAND_MSG = @"Resin requires a command:
  console - start Resin in console mode
  status - watchdog status
  start - start a Resin server
  stop - stop a Resin server
  restart - restart a Resin server
  kill - force a kill of a Resin server
  shutdown - shutdown the watchdog";

    private bool _verbose;
    private bool _nojit;
    private bool _service = false;
    private bool _install = false;
    private bool _uninstall = false;
    private bool _standalone = false;
    private bool _help = false;
    
    private String _user;
    private String _password;
    private String _javaExe;
    private String _javaHome;
    private String _jvmArgs;
    private String _resinArgs;
    private String _cp;
    private String _envCp;
    private String _resinHome;
    private String _rootDirectory;
    private String _displayName;
    private String _passToServiceArgs;
    private String _command;
    
    private Process _process;
    
    private Resin(String[] args)
    {
      _displayName = "Resin Web Server";
      StringBuilder jvmArgs = new StringBuilder();
      StringBuilder resinArgs = new StringBuilder();
      StringBuilder passToServiceArgs = new StringBuilder();
      
      int argsIdx = 1;
      while (argsIdx < args.Length) {
        if ("-verbose".Equals(args[argsIdx])) {
          _verbose = true;
          
          argsIdx++;
        } else if (args[argsIdx].StartsWith("-J")){
          argsIdx++;
        } else if (args[argsIdx].StartsWith("-D")) {
          jvmArgs.Append(' ').Append(args[argsIdx]);
          
          argsIdx++;
        } else if (args[argsIdx].StartsWith("-X")){
          jvmArgs.Append(' ').Append(args[argsIdx]);
          
          argsIdx++;
        } else if (args[argsIdx].StartsWith("-E")){
          String envVar = args[argsIdx];
          int equalsIdx;
          if ((equalsIdx = envVar.IndexOf('=')) > -1) {
            String variable = envVar.Substring(2, equalsIdx -2);
            String val = envVar.Substring(equalsIdx +1);
            Environment.SetEnvironmentVariable(variable, val, EnvironmentVariableTarget.Process);
          } else {
            String variable = envVar.Substring(2);
            Environment.SetEnvironmentVariable(variable,"", EnvironmentVariableTarget.Process);
          }
          
          argsIdx++;
        } else if ("-user".Equals(args[argsIdx])) {
          _user = args[argsIdx + 1];
          
          if (! _user.StartsWith(".\\") && _user.IndexOf('\\') < 0)
            _user = ".\\" + _user;
          
          argsIdx += 2;
        } else if ("-password".Equals(args[argsIdx])) {
          _password = args[argsIdx + 1];
          
          argsIdx += 2;
        } else if ("-name".Equals(args[argsIdx])) {
          ServiceName = args[argsIdx + 1];
          
          argsIdx += 2;
        } else if ("-display-name".Equals(args[argsIdx])) {
          _displayName = args[argsIdx + 1];
          
          argsIdx += 2;
        } else if ("-install".Equals(args[argsIdx])) {
          _install = true;
          _service = false;
          
          argsIdx++;
        } else if ("-install-as".Equals(args[argsIdx]) ||
                   "-install_as".Equals(args[argsIdx])) {
          _install = true;
          _service = false;
          
          ServiceName = args[argsIdx + 1];
          
          argsIdx += 2;
        } else if ("-remove".Equals(args[argsIdx])) {
          _uninstall = true;
          _service = false;
          
          argsIdx++;
        } else if ("-remove-as".Equals(args[argsIdx]) ||
                   "-remove_as".Equals(args[argsIdx])) {
          _uninstall = true;
          _service = false;
          
          ServiceName = args[argsIdx + 1];
          
          argsIdx += 2;
        } else if("-java_home".Equals(args[argsIdx]) ||
                  "-java-home".Equals(args[argsIdx])) {
          _javaHome = args[argsIdx + 1];
          
          passToServiceArgs.Append("-java_home ").Append(_javaHome).Append(' ');
          
          argsIdx += 2;
        } else if ("-java_exe".Equals(args[argsIdx]) ||
                   "-java-exe".Equals(args[argsIdx])) {
          _javaExe = args[argsIdx + 1];
          
          passToServiceArgs.Append("-java_exe ").Append(_javaExe).Append(' ');
          
          argsIdx += 2;
        } else if ("-msjava".Equals(args[argsIdx])) {
          //msJava = true; XXX no longer supported
          
          argsIdx++;
        } else if ("-resin_home".Equals(args[argsIdx]) ||
                   "-resin-home".Equals(args[argsIdx]) ||
                   "--resin-home".Equals(args[argsIdx])) {
          _resinHome = args[argsIdx + 1];
          
          passToServiceArgs.Append("-resin_home ").Append(_resinHome).Append(' ');
          
          argsIdx += 2;
        } else if ("-server-root".Equals(args[argsIdx]) ||
                   "-server_root".Equals(args[argsIdx]) ||
                   "--root-directory".Equals(args[argsIdx]) ||
                   "-root-directory".Equals(args[argsIdx])) {
          _rootDirectory = args[argsIdx + 1];
          
          passToServiceArgs.Append("-server-root ").Append(_rootDirectory);
          
          argsIdx += 2;
        } else if ("-classpath".Equals(args[argsIdx]) ||
                   "-cp".Equals(args[argsIdx])) {
          _cp += args[argsIdx + 1];
          
          argsIdx += 2;
        } else if ("-env-classpath".Equals(args[argsIdx])) {
          _envCp = args[argsIdx + 1];
          
          argsIdx += 2;
        } else if ("-stdout".Equals(args[argsIdx])) {
          //stdOutFile = args[argsIdx + 1]; XXX not supported
          
          argsIdx += 2;
        } else if ("-stderr".Equals(args[argsIdx])) {
          //stdErrFile = args[argsIdx + 1]; XXX not supported
          
          argsIdx += 2;
        } else if ("-jvm-log".Equals(args[argsIdx])) {
          //jvmFile = args[argsIdx + 1]; XXX not supported
          
          argsIdx += 2;
        } else if ("-main".Equals(args[argsIdx])) {
          //main = args[argsIdx + 1]; XXX not supported - was used with jview
          
          argsIdx += 2;
        } else if("-help".Equals(args[argsIdx]) ||
                  "-h".Equals(args[argsIdx])) {
          _help = true;
          
          argsIdx++;
        } else if ("-service".Equals(args[argsIdx])) {
          _service = true;
          
          argsIdx++;
        } else if ("-console".Equals(args[argsIdx])) {
          //make -console be a command
          _command = "console";
          
          argsIdx++;
        } else if("-nojit".Equals(args[argsIdx])) {
          _nojit = true;
          
          argsIdx++;
        } else if("-standalone".Equals(args[argsIdx])) {
          _standalone = true;
          
          argsIdx++;
        } else if("-e".Equals(args[argsIdx]) ||
                  "-compile".Equals(args[argsIdx])) {
          argsIdx++;
        } else if ("console".Equals(args[argsIdx]) ||
                   "status".Equals(args[argsIdx])  ||
                   "start".Equals(args[argsIdx])   ||
                   "stop".Equals(args[argsIdx])    ||
                   "restart".Equals(args[argsIdx]) ||
                   "kill".Equals(args[argsIdx]) ||
                   "shutdown".Equals(args[argsIdx])) {
          _command = args[argsIdx];

          _standalone = true;
          
          argsIdx++;
        } else {
          resinArgs.Append(' ').Append(args[argsIdx++]);
        }
      }

      if (_command == null &&
          (! _install) &&
          (! _uninstall)&&
          (! _service)) {
        Info(REQUIRE_COMMAND_MSG);
        
        Environment.Exit(-1);
      }
      
      if (ServiceName == null || "".Equals(ServiceName))
        ServiceName = "Resin";
      
      _jvmArgs = jvmArgs.ToString();
      _resinArgs = resinArgs.ToString();
      _passToServiceArgs = passToServiceArgs.ToString();
    }
    
    private int Execute() {
      if (_help) {
        Usage(ServiceName);
        
        return 0;
      }
      
      _resinHome = Util.GetResinHome(_resinHome, System.Reflection.Assembly.GetExecutingAssembly().Location);

      if (_resinHome == null) {
        Error("Can't find RESIN_HOME", null);
        
        return 1;
      }
      
      if (_rootDirectory == null)
        _rootDirectory = _resinHome;

      _javaHome = GetJavaHome(_resinHome, _javaHome);
      
      _cp = GetClasspath(_cp, _resinHome, _javaHome, _envCp);
      
      if (_javaExe == null && _javaHome != null)
        _javaExe = GetJavaExe(_javaHome);
      
      if (_javaExe == null)
        _javaExe = "java.exe";

      try {
        Directory.SetCurrentDirectory(_rootDirectory);
      } catch (Exception e) {
        Error(String.Format("Can't change dir to {0} due to: {1}", _rootDirectory, e), e);
        
        return 1;
      }
      
      Environment.SetEnvironmentVariable("CLASSPATH", _cp);
      Environment.SetEnvironmentVariable("PATH",
                                         String.Format("{0};{1}\\win32;{1}\\win64;\\openssl\\bin",
                                                       Environment.GetEnvironmentVariable("PATH"),
                                                       _resinHome));

      if (_install) {
        try {
          InstallService();
        } catch (Exception e) {
          Error(e.Message, e);
          
          return 1;
        }
      } else if (_uninstall) {
        try {
          UninstallService();
        } catch (Exception e) {
          Error(e.Message, e);
          
          return 1;
        }
      } else if (_service) {
        ServiceBase.Run(new ServiceBase[]{this});
      } else if (_standalone) {
        if (StartResin()) {
          Join();
          
          if (_process != null)
            return _process.ExitCode;
        }
        
        return 1;
      } else {
        if (StartResin()) {
          ResinWindow window = new ResinWindow(this, _displayName);
          window.Show();
          Application.Run();
        }
        
        return 1;
      }
      
      return 0;
    }
    
    private void InstallService() {
      Installer installer = InitInstaller();
      Hashtable installState = new Hashtable();
      installer.Install(installState);
      
      RegistryKey system = Registry.LocalMachine.OpenSubKey("System");
      RegistryKey currentControlSet = system.OpenSubKey("CurrentControlSet");
      RegistryKey servicesKey = currentControlSet.OpenSubKey("Services");
      RegistryKey serviceKey = servicesKey.OpenSubKey(ServiceName, true);
      
      StringBuilder builder = new StringBuilder((String)serviceKey.GetValue("ImagePath"));
      builder.Append(" -service -name ").Append(ServiceName).Append(' ');
      
      if (_passToServiceArgs.Length > 0)
        builder.Append(_passToServiceArgs).Append(' ');
      
      if (_jvmArgs.Length > 0)
        builder.Append(_jvmArgs).Append(' ');
      
      if (_resinArgs.Length > 0)
        builder.Append(_resinArgs).Append(' ');
      
      serviceKey.SetValue("ImagePath", builder.ToString());

      StoreState(installState, ServiceName);
      
      Info(String.Format("\nInstalled {0} as Windows Service", ServiceName));
    }
    
    private void UninstallService() {
      Hashtable state = LoadState(ServiceName);
      
      Installer installer = InitInstaller();
      
      installer.Uninstall(state);
      
      Info(String.Format("\nRemoved {0} as Windows Service", ServiceName));
    }
    
    private Installer InitInstaller() {
      TransactedInstaller txInst = new TransactedInstaller();
      txInst.Context = new InstallContext(null, new String[]{});
      txInst.Context.Parameters["assemblypath"] = System.Reflection.Assembly.GetExecutingAssembly().Location;
      
      ServiceProcessInstaller spInst = new ServiceProcessInstaller();
      if (_user != null) {
        spInst.Username = _user;
        spInst.Password = _password;
        spInst.Account = ServiceAccount.User;
      } else {
        spInst.Account = ServiceAccount.LocalSystem;
      }

      txInst.Installers.Add(spInst);

      ServiceInstaller srvInst = new ServiceInstaller();
      srvInst.ServiceName = ServiceName;
      srvInst.DisplayName = _displayName;
      srvInst.StartType = ServiceStartMode.Manual;

      txInst.Installers.Add(srvInst);

      return txInst;
    }
    private static String GetResinAppDataDir() {
      return Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + '\\' + CAUCHO_APP_DATA;
    }
    private static void StoreState(Hashtable state, String serviceName) {
      String dir = GetResinAppDataDir();
      if (! Directory.Exists(dir))
        Directory.CreateDirectory(dir);
      
      FileStream fs = new FileStream(dir + '\\' + serviceName + ".srv", FileMode.Create, FileAccess.Write);
      BinaryFormatter serializer = new BinaryFormatter();
      serializer.Serialize(fs, state);
      fs.Flush();
      fs.Close();
    }
    
    private static Hashtable LoadState(String serviceName) {
      String dir = GetResinAppDataDir();
      FileStream fs = new FileStream(dir + '\\' + serviceName + ".srv", FileMode.Open, FileAccess.Read);
      BinaryFormatter serializer = new BinaryFormatter();
      Hashtable state = (Hashtable)serializer.Deserialize(fs);
      fs.Close();
      
      return state;
    }
    
    public bool StartResin() {
      if (!_service && _process != null)
        return false;
      
      try {
        if (_service)
          ExecuteJava("start");
        else
          ExecuteJava(_command);
        
        return true;
      } catch (Exception e) {
        StringBuilder message = new StringBuilder("Unable to start application. Make sure java is in your path. Use option -verbose for more detail.\n");
        message.Append(e.ToString());
        
        Info(message.ToString());
        
        return false;
      }
    }
    
    public void StopResin() {
      if (_service) {
        Info("Stopping Resin");
        
        ExecuteJava("stop");
      } else {
        if (_process != null && ! _process.HasExited) {
          Info("Stopping Resin ", false);
          
          _process.Kill();
          
          while(! (_process.HasExited)) {
            Info(".", false);
            _process.WaitForExit(500);
          }
          
          Info(". done.");
        }
        
        _process = null;
      }
    }

    private void ExecuteJava(String command) {
      if (_verbose) {
        StringBuilder info = new StringBuilder();
        
        info.Append("java        : ").Append(_javaExe).Append('\n');
        info.Append("JAVA_HOME   : ").Append(_javaHome).Append('\n');
        info.Append("RESIN_HOME  : ").Append(_resinHome).Append('\n');
        info.Append("SERVER_ROOT : ").Append(_rootDirectory).Append('\n');
        info.Append("CLASSPATH   : ").Append(_cp).Append('\n');
        info.Append("PATH        : ").Append(Environment.GetEnvironmentVariable("PATH"));
        
        Info(info.ToString());
      }
      
      ProcessStartInfo startInfo = new ProcessStartInfo();
      startInfo.FileName = _javaExe;
      StringBuilder arguments = new StringBuilder(_jvmArgs).Append(' ');
      
      if (_nojit)
        arguments.Append("-Djava.compiler=NONE ");
      
      arguments.Append("-Xrs -jar ");
      arguments.Append(_resinHome + "\\lib\\resin.jar ");
      arguments.Append("-resin-home ").Append(_resinHome).Append(' ');
      arguments.Append("-root-directory ").Append(_rootDirectory).Append(' ');
      arguments.Append(_resinArgs).Append(' ');
      
      if (command != null)
        arguments.Append(command);
      
      startInfo.Arguments = arguments.ToString();
      
      if (_verbose)
        Info("Using Command Line :" + _javaExe + ' ' + startInfo.Arguments);
      
      startInfo.UseShellExecute = false;
      startInfo.WorkingDirectory = _rootDirectory;
      
      if (_service) {
        startInfo.RedirectStandardError = true;
        startInfo.RedirectStandardOutput = true;
        
        Process process = null;
        try {
          process = Process.Start(startInfo);
        } catch (Exception e) {
          EventLog.WriteEntry(ServiceName, e.ToString(), EventLogEntryType.Error);
          
          return;
        }
        
        StringBuilder error = new StringBuilder();
        StringBuilder output = new StringBuilder();
        process.ErrorDataReceived += delegate(Object sendingProcess, DataReceivedEventArgs err) {
          error.Append(err.Data).Append('\n');
        };
        process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs err) {
          output.Append(err.Data).Append('\n');
        };
        process.BeginErrorReadLine();
        process.BeginOutputReadLine();
        
        while (! process.HasExited)
          process.WaitForExit(500);
        
        process.CancelErrorRead();
        process.CancelOutputRead();
        
        if (process.HasExited && process.ExitCode != 0) {
          StringBuilder messageBuilder = new StringBuilder("Error Executing Resin Using: ");
          messageBuilder.Append(startInfo.FileName).Append(' ').Append(startInfo.Arguments);
          
          if (output.Length > 0)
            messageBuilder.Append('\n').Append(output);
          
          if (error.Length > 0)
            messageBuilder.Append('\n').Append(error);
          
          String message = messageBuilder.ToString();
          EventLog.WriteEntry(ServiceName, message, EventLogEntryType.Error);
          
          throw new ApplicationException(message);
        }
      } else {
        _process = Process.Start(startInfo);
      }
    }
    
    protected override void OnStart(string[] args)
    {
      StartResin();
      base.OnStart(args);
    }

    protected override void OnStop()
    {
      StopResin();
      base.OnStop();
    }

    private void Join() {
      if (_process != null && ! _process.HasExited)
        _process.WaitForExit();
    }
    
    public void Error(String message, Exception e) {
      if (_service && EventLog != null)
        EventLog.WriteEntry(this.ServiceName, message, EventLogEntryType.Error);
      else
        Console.WriteLine(message);
    }
    
    private void Info(String message) {
      Info(message, true);
    }
    
    private void Info(String message, bool newLine) {
      if (_service && EventLog != null)
        EventLog.WriteEntry(this.ServiceName, message, EventLogEntryType.Information);
      else if (newLine)
        Console.WriteLine(message);
      else 
        Console.Write(message);
    }
    
    public static int Main(String []args) {
      Resin resin = new Resin(Environment.GetCommandLineArgs());
      return resin.Execute();
    }

    private static String GetJavaExe(String javaHome) {
      if (File.Exists(javaHome + "\\bin\\java.exe"))
        return javaHome + "\\bin\\java.exe";
      else if (File.Exists(javaHome + "\\jrockit.exe"))
        return javaHome + "\\jrockit.exe";
      else
        return null;
    }
    
    private static String GetClasspath(String cp, String resinHome, String javaHome, String envCp) {
      StringBuilder buffer = new StringBuilder();

      if(cp != null && ! "".Equals(cp))
        buffer.Append(cp).Append(';');

//      buffer.Append(resinHome + "\\classes;");
//      buffer.Append(resinHome + "\\lib\\resin.jar;");
      
      if (javaHome != null) {
        
        if (File.Exists(javaHome + "\\lib\\tools.jar"))
          buffer.Append(javaHome + "\\lib\\tools.jar;");
        
        if (File.Exists(javaHome + "\\jre\\lib\\rt.jar"))
          buffer.Append(javaHome + "\\jre\\lib\\rt.jar;");
      }
      
      //add zip files ommitted.
      
      if (envCp != null && !"".Equals (envCp)) {
        buffer.Append(envCp);
      }
      
      return buffer.ToString();
    }
    
    private static String FindJdkInRegistry(String key) {
      RegistryKey regKey
        = Registry.LocalMachine.OpenSubKey(key);
      
      if (regKey == null)
        return null;
      
      RegistryKey java = regKey.OpenSubKey("CurrentVersion");
      
      if (java == null)
        java = regKey.OpenSubKey("1.6");
      
      if (java == null)
        java = regKey.OpenSubKey("1.5");

      if (java == null)
        return null;
      
      String result = java.GetValue("JavaHome").ToString();
      
      java.Close();
      regKey.Close();
      
      return result;
    }
    
    private static String FindPath(String exe) {
      String[] paths = Environment.GetEnvironmentVariable("PATH").Split(';');
      
      foreach (String path in paths) {
        String testPath;
        
        if (path.ToLower().EndsWith("bin") && File.Exists(testPath = path + "\\java.exe"))
          return testPath;
      }
      
      return null;
    }
    
    private static String GetJavaHome(String resinHome, String javaHome) {
      String path = null;
      
      if (javaHome != null) {
      } else if (Environment.GetEnvironmentVariable("JAVA_HOME") != null &&
                 ! "".Equals(Environment.GetEnvironmentVariable("JAVA_HOME"))) {
        javaHome = Environment.GetEnvironmentVariable("JAVA_HOME").Replace('/', '\\');
      } else if ((javaHome = FindJdkInRegistry(HKEY_JDK)) != null) {
      } else if ((javaHome = FindJdkInRegistry(HKEY_JRE)) != null) {
      } else if (File.Exists(resinHome + "\\jre\\bin\\java.exe")) {
        javaHome = resinHome + "\\jre";
      } else if ((path = FindPath("java.exe")) != null) {
        javaHome = Util.GetParent(path, 2);
      }
      
      if (javaHome == null && Directory.Exists("\\java\\lib"))
        javaHome = Directory.GetCurrentDirectory()[0] + ":\\java";
      
      if (javaHome == null && Directory.Exists("\\jre\\lib"))
        javaHome = Directory.GetCurrentDirectory()[0] + ":\\jre";
      
      if (javaHome == null) {
        String[] dirs = Directory.GetDirectories("\\", "jdk*");
        
        foreach(String dir in dirs) {
          if (File.Exists(dir + "\\bin\\java.exe"))
            javaHome = Directory.GetCurrentDirectory().Substring(0, 2) + dir;
        }
        
        String programFilesJava
          = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles)
          + "\\java";
        
        if (Directory.Exists(programFilesJava)) {
          dirs = Directory.GetDirectories(programFilesJava, "jdk*");
          foreach (String dir in dirs) {
            if (File.Exists(dir + "\\bin\\java.exe"))
              javaHome = dir;
          }
        }
      }
      
      if (javaHome == null) {
        String[] dirs = Directory.GetDirectories("\\", "jre*");
        
        foreach(String dir in dirs) {
          if (File.Exists(dir + "\\bin\\java.exe"))
            javaHome = Directory.GetCurrentDirectory().Substring(0, 2) + dir;
        }
      }
      
      return javaHome;
    }
    
    private void Usage(String name) {
      Info(String.Format(USAGE, name));
    }
  }
}