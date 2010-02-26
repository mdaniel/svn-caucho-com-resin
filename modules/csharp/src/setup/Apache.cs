/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;
using Microsoft.Win32;



namespace Caucho
{
  class Apache
  {
    private static String REG_APACHE_2_2 = "Software\\Apache Software Foundation\\Apache";
    private static String REG_APACHE_2 = "Software\\Apache Group\\Apache";

    public static void FindApache(ArrayList homes)
    {
      String apacheHome = null;

      apacheHome = FindApacheInRegistry(Registry.LocalMachine, REG_APACHE_2_2);

      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      apacheHome = FindApacheInRegistry(Registry.CurrentUser, REG_APACHE_2_2);

      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      apacheHome = FindApacheInRegistry(Registry.LocalMachine, REG_APACHE_2);

      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      apacheHome = FindApacheInRegistry(Registry.CurrentUser, REG_APACHE_2);
      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      FindApacheInProgramFiles(homes);
    }

    public static void FindApacheInProgramFiles(ArrayList homes)
    {
      String programFiles
        = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);

      String[] groupDirs = Directory.GetDirectories(programFiles, "Apache*");

      foreach (String groupDir in groupDirs) {
        String[] testDirs = Directory.GetDirectories(groupDir, "*");
        foreach (String testDir in testDirs) {
          if (File.Exists(testDir + "\\bin\\Apache.exe") || File.Exists(testDir + "\\bin\\httpd.exe")) {
            homes.Add(Util.GetCanonicalPath(testDir));
          }
        }
      }
    }

    public static String FindApacheInRegistry(RegistryKey registryKey, String location)
    {
      RegistryKey apacheKey = registryKey.OpenSubKey(location);

      String result = null;

      if (apacheKey != null) {
        foreach (String name in apacheKey.GetSubKeyNames()) {
          RegistryKey key = apacheKey.OpenSubKey(name);

          String testRoot = (String)key.GetValue("ServerRoot");
          if (testRoot != null && !"".Equals(testRoot))
            result = testRoot;
        }
      }

      if (result != null && result.IndexOf('~') != -1) {
        StringBuilder builder = new StringBuilder(256);
        Interop.GetLongPathName(result, builder, builder.Capacity);

        result = builder.ToString();
      }

      return result;
    }

    public String GetApacheVersion(String apacheHome)
    {
      Process process = new Process();

      if (File.Exists(apacheHome + "\\bin\\apache.exe"))
        process.StartInfo.FileName = apacheHome + "\\bin\\apache.exe";
      else if (File.Exists(apacheHome + "\\bin\\httpd.exe"))
        process.StartInfo.FileName = apacheHome + "\\bin\\httpd.exe";
      else
        throw new ApplicationException(String.Format("Can not find apache.exe or httpd.exe in {0}\\bin", apacheHome));

      process.StartInfo.RedirectStandardError = true;
      process.StartInfo.RedirectStandardOutput = true;
      process.StartInfo.Arguments = "-v";
      process.StartInfo.UseShellExecute = false;

      StringBuilder error = new StringBuilder();
      String version = null;
      String versionString = null;

      process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e)
      {
        if (e.Data != null)
          error.Append(e.Data).Append('\n');
      };
      process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e)
      {
        if (e.Data == null)
          return;

        String test = e.Data.ToLower();
        if (test.IndexOf("version") != -1) {
          versionString = e.Data;

          if (test.IndexOf("2.2") != -1)
            version = "2.2";
          else if (test.IndexOf("2.0") != -1)
            version = "2.0";
        }
      };

      process.Start();

      process.BeginOutputReadLine();
      process.BeginErrorReadLine();

      process.WaitForExit();

      process.CancelErrorRead();
      process.CancelOutputRead();

      process.Close();

      if (version != null)
        return version;

      if (error.Length > 0)
        throw new ApplicationException("Unable to determine version of Apache due to error: " + error.ToString());
      else if (version != null)
        throw new ApplicationException("Unsupported Apache Version: " + versionString);
      else
        throw new ApplicationException("Unable to determine version of Apache");
    }

    public bool IsValidApacheHome(String dir)
    {
      return File.Exists(dir + "\\conf\\httpd.conf");
    }

    private static bool IsCommentedOut(String line)
    {
      foreach (char c in line) {
        switch (c) {
          case ' ': break;
          case '\t': break;
          case '#': return true;
          default: return false;
        }
      }

      return false;
    }

    public String BackupHttpConf(String httpdConfFile)
    {
      String backUpFile = httpdConfFile + ".bak";

      bool backedUp = false;
      int i = 0;
      do {
        if (!File.Exists(backUpFile)) {
          File.Copy(httpdConfFile, backUpFile);
          backedUp = true;
        } else {
          backUpFile = httpdConfFile + ".bak-" + i++;
        }
      } while (!backedUp && i < 100);

      if (!backedUp)
        throw new ApplicationException("Can not make back up copy of the file");

      return backUpFile;
    }

    public ConfigureInfo SetupApache(String resinHome, String apacheHome)
    {

      ConfigureInfo configureInfo = new ConfigureInfo();

      String apacheVersion = GetApacheVersion(apacheHome);

      String httpdConfData = null;

      String httpdConfFile = apacheHome + "\\conf\\httpd.conf";
      StreamReader httpdConfFileReader = null;
      try {
        httpdConfFileReader = new StreamReader(httpdConfFile);
        httpdConfData = httpdConfFileReader.ReadToEnd();
      }
      catch (Exception e) {
        throw e;
      }
      finally {
        if (httpdConfFileReader != null)
          httpdConfFileReader.Close();
      }

      StringReader httpdConfReader = new StringReader(httpdConfData);

      int lineCounter = 0;
      int lastLoadModuleLine = 0;
      int loadModCauchoLine = -1;
      int ifModuleCaucho = -1;
      String line;
      while ((line = httpdConfReader.ReadLine()) != null) {
        if (line.IndexOf("LoadModule") != -1) {
          lastLoadModuleLine = lineCounter;

          if ((line.IndexOf("mod_caucho.dll") != -1) &&
              !IsCommentedOut(line)) {
            loadModCauchoLine = lineCounter;
          }
        }

        if (line.IndexOf("<IfModule") != -1 &&
            line.IndexOf("mod_caucho.c") != -1 &&
            !IsCommentedOut(line)) {
          ifModuleCaucho = lineCounter;
        }

        lineCounter++;
      }
      httpdConfReader.Close();

      if (ifModuleCaucho == -1 || loadModCauchoLine == -1) {

        configureInfo.BackUpFile = BackupHttpConf(httpdConfFile);

        httpdConfReader = new StringReader(httpdConfData);
        StringWriter buffer = new StringWriter();
        lineCounter = 0;
        //
        while ((line = httpdConfReader.ReadLine()) != null) {
          buffer.WriteLine(line);

          if (lineCounter == lastLoadModuleLine &&
              loadModCauchoLine == -1) {
            buffer.WriteLine(String.Format("LoadModule caucho_module \"{0}/win32/{1}/mod_caucho.dll\"", resinHome.Replace('\\', '/'), "apache-" + apacheVersion));
          }

          lineCounter++;
        }

        if (ifModuleCaucho == -1) {
          buffer.WriteLine("<IfModule mod_caucho.c>");
          buffer.WriteLine("  ResinConfigServer localhost 6800");
          buffer.WriteLine("  CauchoStatus yes");
          buffer.WriteLine("</IfModule>");
        }

        buffer.Flush();

        StreamWriter httpdConfWriter = null;

        try {
          httpdConfWriter = new StreamWriter(httpdConfFile);
          httpdConfWriter.Write(buffer.ToString());
          httpdConfWriter.Flush();
        }
        catch (Exception e) {
          throw e;
        }
        finally {
          if (httpdConfWriter != null)
            httpdConfWriter.Close();
        }

        configureInfo.Status = ConfigureInfo.SETUP_OK;
      } else {
        configureInfo.Status = ConfigureInfo.SETUP_ALREADY;
      }

      return configureInfo;
    }

    public ConfigureInfo RemoveApache(String apacheHome)
    {
      ConfigureInfo configInfo = new ConfigureInfo();

      String httpdConfFile = apacheHome + "\\conf\\httpd.conf";

      StreamReader httpdConfReader = null;
      StringWriter buffer = new StringWriter();
      bool resinRemoved = false;
      try {

        httpdConfReader = new StreamReader(httpdConfFile);
        String line = null;
        bool inCauchoIfModule = false;
        while ((line = httpdConfReader.ReadLine()) != null) {
          if (line.IndexOf("LoadModule") != -1 &&
              line.IndexOf("mod_caucho.dll") != -1 &&
              !IsCommentedOut(line)) {
            resinRemoved = true;
          } else if (line.IndexOf("IfModule") != -1 &&
                     line.IndexOf("mod_caucho.c") != -1 &&
                     !IsCommentedOut(line)) {
            inCauchoIfModule = true;
            resinRemoved = true;
          } else if (inCauchoIfModule &&
                     line.IndexOf("/IfModule") != -1) {
            inCauchoIfModule = false;
          } else if (inCauchoIfModule) {
          } else {
            buffer.WriteLine(line);
          }
        }

        buffer.Flush();
      }
      catch (Exception e) {
        throw e;
      }
      finally {
        if (httpdConfReader != null)
          httpdConfReader.Close();
      }

      if (!resinRemoved) {
        configInfo.Status = ConfigureInfo.REMOVED_ALREADY;

        return configInfo;
      }

      configInfo.BackUpFile = BackupHttpConf(httpdConfFile);
      StreamWriter httpdConfWriter = null;
      try {
        httpdConfWriter = new StreamWriter(httpdConfFile);
        httpdConfWriter.Write(buffer.ToString());
        httpdConfWriter.Flush();
      }
      catch (Exception e) {
        throw e;
      }
      finally {
        if (httpdConfWriter != null)
          httpdConfWriter.Close();
      }

      configInfo.Status = ConfigureInfo.REMOVED_OK;

      return configInfo;
    }

    public String FindApacheServiceName(String apacheHome)
    {
      String apacheHomeLower = apacheHome.ToLower();
      String result = null;
      RegistryKey services = Registry.LocalMachine.OpenSubKey(Setup.REG_SERVICES);
      foreach (String name in services.GetSubKeyNames()) {
        Console.WriteLine("Service: " + name);
        RegistryKey key = services.OpenSubKey(name);
        String imagePath = (String)key.GetValue("ImagePath");
        if (imagePath != null && !"".Equals(imagePath)) {
          imagePath = imagePath.ToLower();
          if (imagePath.IndexOf(apacheHomeLower) != -1) {
            result = name;
            break;
          }
        }
        key.Close();
      }

      services.Close();
      return result;
    }
  }
}
