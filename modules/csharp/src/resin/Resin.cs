/*
 * Copyright (c) 1998-2016 Caucho Technology -- all rights reserved
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
using System.Runtime.Serialization.Formatters.Binary;
using System.Security.Principal;

namespace Caucho
{
  public class Resin : ServiceBase
  {
    private static String HKEY_JRE = @"Software\JavaSoft\Java Runtime Environment";
    private static String HKEY_JDK = @"Software\JavaSoft\Java Development Kit";
    private static String CAUCHO_APP_DATA = @"Caucho Technology\Resin";

    private String _javaExe;
    private String _javaHome;
    private String _resinHome;
    private String _rootDirectory;

    private Process _process;
    private ResinArgs ResinArgs;

    private Mutex _mutex;

    private Resin(ResinArgs args)
    {
      ResinArgs = args;

      _resinHome = ResinArgs.ResinHome;
      _rootDirectory = ResinArgs.ResinRoot;
      _javaHome = ResinArgs.JavaHome;

      //_mutex = new Mutex(false, @"Global\com.caucho.Resin." + _rootDirectory);
      //_mutex = new Mutex(false, @"Global\com.caucho.Resin");

      if (_mutex == null) {
          _mutex = new Mutex(false, @"Global\com.caucho.Resin." + _rootDirectory);
      }
    }

    public bool StartResin()
    {
      try
      {
        if (ResinArgs.IsService)
          ExecuteJava("start");
        else
          ExecuteJava(ResinArgs.Command);

        return true;
      } catch (ResinServiceException e)
      {
        throw e;
      } catch (Exception e)
      {
        StringBuilder message = new StringBuilder("Unable to start application. Make sure java is in your path. Use option -verbose for more detail.\n");
        message.Append(e.ToString());

        Info(message.ToString());

        return false;
      }
    }

    public void StopResin()
    {
      if (ResinArgs.IsService)
      {
        Info("Stopping Resin");
        ExecuteJava("stop");
      }
    }

    private int Execute()
    {
      _resinHome = Util.GetResinHome(_resinHome, System.Reflection.Assembly.GetExecutingAssembly().Location);

      if (_resinHome == null)
      {
        Error("Can't find RESIN_HOME", null);

        return 1;
      }

      if (_rootDirectory == null)
        _rootDirectory = _resinHome;

      _javaHome = GetJavaHome(_resinHome, _javaHome);


      if (_javaExe == null && _javaHome != null)
        _javaExe = GetJavaExe(_javaHome);

      if (_javaExe == null)
        _javaExe = "java.exe";

      System.Environment.SetEnvironmentVariable("JAVA_HOME", _javaHome);

      Environment.SetEnvironmentVariable("PATH",
                                         String.Format("{0};{1};\\openssl\\bin;.",
                                                       _javaHome + "\\bin",
                                                       Environment.GetEnvironmentVariable("PATH")));

      if (ResinArgs.IsService)
      {
        ServiceBase.Run(new ServiceBase[] { this });

        return 0;
      }
      else
      {
        if (StartResin())
        {
          Join();
          if (_process != null)
          {
            int exitCode = _process.ExitCode;
            _process.Dispose();
            return exitCode;
          }
        }

        return 0;
      }
    }

    private static String GetResinAppDataDir()
    {
      return Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + '\\' + CAUCHO_APP_DATA;
    }

    private void ExecuteJava(String command)
    {
      _mutex.WaitOne();
      try
      {
        ExecuteJavaImpl(command);
      }
      finally
      {
        _mutex.ReleaseMutex();
      }
    }

    private void ExecuteJavaImpl(String command)
    {
      if (ResinArgs.IsVerbose)
      {
        StringBuilder info = new StringBuilder();

        info.Append("java        : ").Append(_javaExe).Append('\n');
        info.Append("JAVA_HOME   : ").Append(_javaHome).Append('\n');
        info.Append("RESIN_HOME  : ").Append(_resinHome).Append('\n');
        info.Append("SERVER_ROOT : ").Append(_rootDirectory).Append('\n');
        info.Append("PATH        : ").Append(Environment.GetEnvironmentVariable("PATH"));

        Info(info.ToString());
      }

      ProcessStartInfo startInfo = new ProcessStartInfo();
      startInfo.FileName = _javaExe;

      StringBuilder arguments = new StringBuilder();

      arguments.Append("-Xrs -jar ");
      arguments.Append("\"" + _resinHome + "\\lib\\resin.jar\"");
      arguments.Append(" -resin-home \"").Append(_resinHome).Append("\" ");
      arguments.Append(" -root-directory \"").Append(_rootDirectory).Append("\" ");

      if ("".Equals(ResinArgs.Server))
        arguments.Append(" -server \"\"");
      else if (ResinArgs.Server != null)
        arguments.Append(" -server ").Append(ResinArgs.Server);

      if (ResinArgs.ElasticServer)
        arguments.Append(" --elastic-server ");

          /*
      else if (ResinArgs.DynamicServer != null)
        arguments.Append(" -dynamic-server ").Append(ResinArgs.DynamicServer);
        */
      if (command != null)
        arguments.Append(' ').Append(command);
      else if (ResinArgs.RawArgs.Count == 1)
        arguments.Append(' ').Append("gui");

      bool isStart = "start".Equals(command)
                     || "gui".Equals(command)
                     || "console".Equals(command);

      if (isStart && ResinArgs.Cluster != null)
        arguments.Append(" -cluster ").Append(ResinArgs.Cluster);

      if (isStart
        && ResinArgs.ElasticServer
        && ! String.IsNullOrEmpty(ResinArgs.ElasticServerAddress)) {
        arguments.Append(" --elastic-server-address ").Append(ResinArgs.ElasticServerAddress).Append(' ');
      }

      if (isStart
        && ResinArgs.ElasticServer
        && ! String.IsNullOrEmpty(ResinArgs.ElasticServerPort)) {
        arguments.Append(" --elastic-server-port ").Append(ResinArgs.ElasticServerPort).Append(' ');
      }

      arguments.Append(' ').Append(ResinArgs.ResinArguments);

      startInfo.Arguments = arguments.ToString();

      if (ResinArgs.IsVerbose)
        Info("Using Command Line: " + _javaExe + ' ' + startInfo.Arguments);

      startInfo.UseShellExecute = false;

      if (ResinArgs.IsService)
      {
        startInfo.RedirectStandardError = true;
        startInfo.RedirectStandardOutput = true;

        Process process = null;
        try
        {
          process = Process.Start(startInfo);
        } catch (Exception e)
        {
          Error(e.Message, e);

          return;
        }
 
        StringBuilder error = new StringBuilder();
        StringBuilder output = new StringBuilder();
        process.ErrorDataReceived += delegate(Object sendingProcess, DataReceivedEventArgs err)
        {
          error.Append(err.Data).Append('\n');
        };
        process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs err)
        {
          output.Append(err.Data).Append('\n');
        };
        process.BeginErrorReadLine();
        process.BeginOutputReadLine();

        while (!process.HasExited)
        {
            process.WaitForExit(500);
        }

        process.CancelErrorRead();
        process.CancelOutputRead();

        if (process.HasExited && process.ExitCode != 0)
        {
          StringBuilder messageBuilder = new StringBuilder("Error Executing Resin Using: ");
          messageBuilder.Append(startInfo.FileName).Append(' ').Append(startInfo.Arguments);

          if (output.Length > 0)
            messageBuilder.Append('\n').Append(output);

          if (error.Length > 0)
            messageBuilder.Append('\n').Append(error);

          String message = messageBuilder.ToString();

          Info(message, true);

          throw new ResinServiceException(message);
        }
      }
      else
      {
        _process = Process.Start(startInfo);
      }
    }

    protected override void OnStart(string[] args)
    {
      base.OnStart(args);

      Info("Service: " + ResinArgs.ServiceName);
      StartResin();
    }

    protected override void OnStop()
    {
      base.OnStop();

      StopResin();
    }

    private void Join()
    {
      if (_process != null && !_process.HasExited)
        _process.WaitForExit();
    }

    public void Error(String message, Exception e)
    {
      Error(message, e, null);
    }

    public void Error(String message, Exception e, TextWriter writer)
    {
      StringBuilder data = new StringBuilder(message);

      if (e != null)
        data.Append('\n').Append(e.ToString());

      if (writer != null)
        writer.WriteLine(data.ToString());
      else if (ResinArgs.IsService && EventLog != null)
      {
        EventLog.WriteEntry("Resin: " + ResinArgs.ServiceName, data.ToString(), EventLogEntryType.Error);
      }
      else
        Console.WriteLine(data.ToString());
    }

    private void Info(String message)
    {
      Info(message, null, true);
    }

    private void Info(String message, bool newLine)
    {
      Info(message, null, newLine);
    }

    private void Info(String message, TextWriter writer, bool newLine)
    {
      if (message.Length > 32000)
      {
        message = message.Substring(message.Length - 32000);
      }

      if (writer != null && newLine)
        writer.WriteLine(message);
      else if (writer != null && !newLine)
        writer.Write(message);
      else if (ResinArgs.IsService && EventLog != null)
      {
        EventLog.WriteEntry("Resin: " + ResinArgs.ServiceName, message, EventLogEntryType.Information);
      }
      else if (newLine)
        Console.WriteLine(message);
      else
        Console.Write(message);
    }

    public static int Main(String[] args)
    {
      ResinArgs resinArgs = new ResinArgs(Environment.GetCommandLineArgs());

      Resin resin = new Resin(resinArgs);

      return resin.Execute();
    }

    private static String GetJavaExe(String javaHome)
    {
      if (File.Exists(javaHome + @"\bin\java.exe"))
        return javaHome + @"\bin\java.exe";
      else if (File.Exists(javaHome + @"\jrockit.exe"))
        return javaHome + @"\jrockit.exe";
      else
        return null;
    }

    private static String FindJdkInRegistry(String key)
    {
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

    private static String FindPath(String exe)
    {
      String[] paths = Environment.GetEnvironmentVariable("PATH").Split(';');

      foreach (String path in paths)
      {
        String testPath;

        if (path.ToLower().EndsWith("bin") && File.Exists(testPath = path + "\\java.exe"))
          return testPath;
      }

      return null;
    }

    private static String GetJavaHome(String resinHome, String javaHome)
    {
      String path = null;

      if (javaHome != null)
      {
      }
      else if (Environment.GetEnvironmentVariable("JAVA_HOME") != null &&
                 !"".Equals(Environment.GetEnvironmentVariable("JAVA_HOME")))
      {
        javaHome = Environment.GetEnvironmentVariable("JAVA_HOME").Replace('/', '\\');
      }
      else if ((javaHome = FindJdkInRegistry(HKEY_JDK)) != null)
      {
      }
      else if ((javaHome = FindJdkInRegistry(HKEY_JRE)) != null)
      {
      }
      else if (File.Exists(resinHome + "\\jre\\bin\\java.exe"))
      {
        javaHome = resinHome + "\\jre";
      }
      else if ((path = FindPath("java.exe")) != null)
      {
        javaHome = Util.GetParent(path, 2);
      }

      if (javaHome == null && Directory.Exists("\\java\\lib"))
        javaHome = Directory.GetCurrentDirectory()[0] + ":\\java";

      if (javaHome == null && Directory.Exists("\\jre\\lib"))
        javaHome = Directory.GetCurrentDirectory()[0] + ":\\jre";

      if (javaHome == null)
      {
        String[] dirs = Directory.GetDirectories("\\", "jdk*");

        foreach (String dir in dirs)
        {
          if (File.Exists(dir + "\\bin\\java.exe"))
            javaHome = Directory.GetCurrentDirectory().Substring(0, 2) + dir;
        }

        String programFilesJava
          = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles)
          + "\\java";

        if (Directory.Exists(programFilesJava))
        {
          dirs = Directory.GetDirectories(programFilesJava, "jdk*");
          foreach (String dir in dirs)
          {
            if (File.Exists(dir + "\\bin\\java.exe"))
              javaHome = dir;
          }
        }
      }

      if (javaHome == null)
      {
        String[] dirs = Directory.GetDirectories("\\", "jre*");

        foreach (String dir in dirs)
        {
          if (File.Exists(dir + "\\bin\\java.exe"))
            javaHome = Directory.GetCurrentDirectory().Substring(0, 2) + dir;
        }
      }

      return javaHome;
    }
  }
}

class ResinServiceException : Exception
{
  public ResinServiceException(String message)
    : base(message)
  {
  }
}