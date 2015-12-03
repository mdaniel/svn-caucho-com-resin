/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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
using System.Collections.Specialized;
using System.Text;

namespace Caucho
{
  public class ResinArgs
  {
    public bool IsVerbose { get; private set; }
    public bool IsService { get; private set; }

    //used with -install / -remove.
    //value of true indicates that the process is started
    //from resin.exe with elevated privileges
    public String JavaExe { get; private set; }
    public String JavaHome { get; private set; }
    public String ResinArguments { get; private set; }
    public String ServiceName { get; private set; }
    public String ResinHome { get; private set; }
    public String ResinRoot { get; private set; }
    public String DisplayName { get; private set; }
    public String Command { get; private set; }
    public String ResinDataDir { get; private set; }
    public String Exe { get; private set; }
    public String Server { get; private set; }
    public String Conf { get; private set; }
    public String Log { get; private set; }
    public String JmxPort { get; private set; }
    public String DebugPort { get; private set; }
    public String WatchDogPort { get; private set; }
    public bool IsPreview { get; private set; }
    public StringCollection RawArgs { get; private set; }
    public bool DynamicServer { get; private set; }
    public bool ElasticServer { get; private set; }
    public String ElasticServerAddress { get; private set; }
    public String ElasticServerPort { get; private set; }
    public String Cluster { get; private set; }

    public ResinArgs(String cmd)
    {
      int resinIdx = cmd.IndexOf("resin.exe");
      if (resinIdx == -1)
        resinIdx = cmd.IndexOf("httpd.exe");

      if (resinIdx > 0 && cmd[0] == '"')
        Exe = cmd.Substring(1, resinIdx - 1 + 9);
      else if (resinIdx > 0)
        Exe = cmd.Substring(0, resinIdx + 9);

      StringCollection arguments = new StringCollection();
      if (Exe != null)
        arguments.Add(Exe);

      StringBuilder builder = new StringBuilder();

      int startIdx = 0;
      if (resinIdx > 0)
        startIdx = resinIdx + 9;
      for (; startIdx < cmd.Length; startIdx++)
      {
        if (cmd[startIdx] == ' ')
          break;
      }

      bool quoted = false;
      for (int i = startIdx; i < cmd.Length; i++)
      {
        char c = cmd[i];
        if ('"' == c && !quoted)
        {
          quoted = true;
        }
        else if ('"' == c && quoted)
        {
          if (builder.Length > 0)
            arguments.Add(builder.ToString());

          builder = new StringBuilder();
          quoted = false;
        }
        else if (' ' == c && quoted)
        {
          builder.Append(c);
        }
        else if (' ' == c)
        {
          arguments.Add(builder.ToString());
          builder = new StringBuilder();

        }
        else if (builder != null)
        {
          builder.Append(c);
        }
        else
        {
          builder.Append(c);
        }
      }

      parse(arguments);
    }

    public ResinArgs(String[] args)
    {
      StringCollection arguments = new StringCollection();
      arguments.AddRange(args);

      parse(arguments);
    }

    private void parse(StringCollection arguments)
    {
      RawArgs = arguments;
      StringBuilder resinArgs = new StringBuilder();

      int argsIdx = 1;
      while (argsIdx < arguments.Count)
      {
        if ("-install".Equals(arguments[argsIdx])
            || "--install".Equals(arguments[argsIdx])
            || "-install-as".Equals(arguments[argsIdx])
            || "--install-as".Equals(arguments[argsIdx]))
        {
          Console.WriteLine("Please use setup.exe to install the service.");

          Environment.Exit(1);
        }
        else if ("-remove".Equals(arguments[argsIdx])
            || "--remove".Equals(arguments[argsIdx])
            || "-remove-as".Equals(arguments[argsIdx])
            || "--remove-as".Equals(arguments[argsIdx]))
        {
          Console.WriteLine("Please use setup.exe to remove a service.");

          Environment.Exit(1);
        }
        else if ("-verbose".Equals(arguments[argsIdx]))
        {
          IsVerbose = true;
          argsIdx++;
          resinArgs.Append(" -verbose ");
        }
        else if (arguments[argsIdx].StartsWith("-J")
                 || arguments[argsIdx].StartsWith("-D")
                 || arguments[argsIdx].StartsWith("-X"))
        {
          resinArgs.Append(' ').Append(arguments[argsIdx++]).Append(' ');
        }
        else if ("-java_home".Equals(arguments[argsIdx])
                   || "-java-home".Equals(arguments[argsIdx]))
        {
          JavaHome = arguments[argsIdx + 1];

          argsIdx += 2;
        }
        else if ("-java_exe".Equals(arguments[argsIdx])
                 || "-java-exe".Equals(arguments[argsIdx]))
        {
          JavaExe = arguments[argsIdx + 1];

          argsIdx += 2;
        }

        else if ("-resin_home".Equals(arguments[argsIdx])
                 || "-resin-home".Equals(arguments[argsIdx])
                 || "--resin-home".Equals(arguments[argsIdx]))
        {
          ResinHome = arguments[argsIdx + 1];

          argsIdx += 2;
        }
        else if ("-server-root".Equals(arguments[argsIdx])
                 || "-server_root".Equals(arguments[argsIdx])
                 || "--root-directory".Equals(arguments[argsIdx])
                 || "-root-directory".Equals(arguments[argsIdx]))
        {
          ResinRoot = arguments[argsIdx + 1];

          argsIdx += 2;
        }
        else if ("-service".Equals(arguments[argsIdx]))
        {
          IsService = true;

          argsIdx++;
        }
        else if ("-console".Equals(arguments[argsIdx]))
        {
          Command = "console";

          argsIdx++;
        }
        else if ("-standalone".Equals(arguments[argsIdx]))
        {
          argsIdx++;
        }
        else if ("-server".Equals(arguments[argsIdx])
                 || "--server".Equals(arguments[argsIdx]))
        {
          Server = arguments[argsIdx + 1];

          argsIdx += 2;
        }
        else if ("-dynamic-server".Equals(arguments[argsIdx])
                 || "--dynamic-server".Equals(arguments[argsIdx]))
        {
          argsIdx += 2;
        }
        else if ("-join-cluster".Equals(arguments[argsIdx])
                  || "--join-cluster".Equals(arguments[argsIdx])
                  || "-cluster".Equals(arguments[argsIdx]) 
                  || "--cluster".Equals(arguments[argsIdx]))
        {
          DynamicServer = true;
          ElasticServer = true;
          Cluster = arguments[argsIdx + 1];

          argsIdx += 2;
        }
        else if ("-elastic-server".Equals(arguments[argsIdx])
                 || "--elastic-server".Equals(arguments[argsIdx])) {
          ElasticServer = true;
          argsIdx++;
        } 
        else if ("--elastic-server-address".Equals(arguments[argsIdx]) 
                 || "-elastic-server-address".Equals(arguments[argsIdx])) {
          ElasticServerAddress = arguments[argsIdx + 1];
          argsIdx += 2;
        } 
        else if ("--elastic-server-port".Equals(arguments[argsIdx])
                   || "-elastic-server-port".Equals(arguments[argsIdx])) {
          ElasticServerPort = arguments[argsIdx + 1];
          argsIdx += 2;
        }
        else if ("-conf".Equals(arguments[argsIdx])
                 || "--conf".Equals(arguments[argsIdx]))
        {
          Conf = arguments[argsIdx + 1];

          resinArgs.Append("-conf \"").Append(Conf).Append("\" ");

          argsIdx += 2;
        }
        else if ("-log-directory".Equals(arguments[argsIdx])
                 || "--log-directory".Equals(arguments[argsIdx]))
        {
          Log = arguments[argsIdx + 1];

          resinArgs.Append("-log-directory \"").Append(Log).Append("\" ");

          argsIdx += 2;
        }
        else if ("-jmx-port".Equals(arguments[argsIdx])
                 || "--jmx-port".Equals(arguments[argsIdx]))
        {
          JmxPort = arguments[argsIdx + 1];

          resinArgs.Append("-jmx-port ").Append(JmxPort).Append(' ');

          argsIdx += 2;
        }
        else if ("-debug-port".Equals(arguments[argsIdx])
                 || "--debug-port".Equals(arguments[argsIdx]))
        {
          DebugPort = arguments[argsIdx + 1];

          resinArgs.Append("-debug-port ").Append(DebugPort).Append(' ');

          argsIdx += 2;
        }
        else if ("-watchdog-port".Equals(arguments[argsIdx])
                 || "--watchdog-port".Equals(arguments[argsIdx]))
        {
          WatchDogPort = arguments[argsIdx + 1];

          resinArgs.Append("-watchdog-port ").Append(WatchDogPort).Append(' ');

          argsIdx += 2;
        }
        else if ("-name".Equals(arguments[argsIdx]) && IsService)
        {
          ServiceName = arguments[argsIdx + 1];

          argsIdx += 2;
        }
        else if ("-name".Equals(arguments[argsIdx]) && ! IsService)
        {
          resinArgs.Append(" -name \"").Append(arguments[argsIdx + 1]).Append("\" ");

          argsIdx += 2;
        }
        else if ("-preview".Equals(arguments[argsIdx])
                 || "--preview".Equals(arguments[argsIdx]))
        {
          IsPreview = true;
          argsIdx++;
        }
        else if (Command == null
                 && ("-h".Equals(arguments[argsIdx])
                     || "config-cat".Equals(arguments[argsIdx])
                     || "config-deploy".Equals(arguments[argsIdx])
                     || "config-ls".Equals(arguments[argsIdx])
                     || "config-undeploy".Equals(arguments[argsIdx])
                     || "console".Equals(arguments[argsIdx])
                     || "deploy".Equals(arguments[argsIdx])
                     || "deploy-copy".Equals(arguments[argsIdx])
                     || "deploy-list".Equals(arguments[argsIdx])
                     || "disable".Equals(arguments[argsIdx])
                     || "disable-soft".Equals(arguments[argsIdx])
                     || "enable".Equals(arguments[argsIdx])
                     || "gui".Equals(arguments[argsIdx])
                     || "heap-dump".Equals(arguments[argsIdx])
                     || "jmx-call".Equals(arguments[argsIdx])
                     || "jmx-dump".Equals(arguments[argsIdx])
                     || "jmx-list".Equals(arguments[argsIdx])
                     || "jmx-set".Equals(arguments[argsIdx])
                     || "jspc".Equals(arguments[argsIdx])
                     || "kill".Equals(arguments[argsIdx])
                     || "license-add".Equals(arguments[argsIdx])
                     || "list-restarts".Equals(arguments[argsIdx])
                     || "log-level".Equals(arguments[argsIdx])
                     || "passsword-encrypt".Equals(arguments[argsIdx])
                     || "passsword-generate".Equals(arguments[argsIdx])
                     || "pdf-report".Equals(arguments[argsIdx])
                     || "profile".Equals(arguments[argsIdx])
                     || "restart".Equals(arguments[argsIdx])
                     || "scoreboard".Equals(arguments[argsIdx])
                     || "shutdown".Equals(arguments[argsIdx])
                     || "start".Equals(arguments[argsIdx])
                     || "start-all".Equals(arguments[argsIdx])
                     || "start-with-foreground".Equals(arguments[argsIdx])
                     || "status".Equals(arguments[argsIdx])
                     || "stop".Equals(arguments[argsIdx])
                     || "thread-dump".Equals(arguments[argsIdx])
                     || "undeploy".Equals(arguments[argsIdx])
                     || "watchdog".Equals(arguments[argsIdx])
                     || "web-app-deploy".Equals(arguments[argsIdx])
                     || "web-app-restart".Equals(arguments[argsIdx])
                     || "web-app-start".Equals(arguments[argsIdx])
                     || "web-app-stop".Equals(arguments[argsIdx])
                     || "web-app-undeploy".Equals(arguments[argsIdx])
                     || "help".Equals(arguments[argsIdx])
                     || "version".Equals(arguments[argsIdx])
                     //
                     || "copy".Equals(arguments[argsIdx])
                     || "list".Equals(arguments[argsIdx])
                     || "deploy-start".Equals(arguments[argsIdx])
                     || "deploy-stop".Equals(arguments[argsIdx])
                     || "deploy-restart".Equals(arguments[argsIdx])
                     || "generate-password".Equals(arguments[argsIdx])
                     || "start-webapp".Equals(arguments[argsIdx])
                     || "stop-webapp".Equals(arguments[argsIdx])
                     || "restart-webapp".Equals(arguments[argsIdx])
                    )
                 )
        {
          Command = arguments[argsIdx];

          argsIdx++;
        }
        else
        {
          resinArgs.Append(' ').Append(arguments[argsIdx++]).Append(' ');
        }
      }

      ResinArguments = resinArgs.ToString();
    }
  }
}