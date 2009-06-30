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
using System.Collections;
using System.Collections.Generic;
using System.IO;
using Microsoft.Win32;
using System.Windows.Forms;
using System.Runtime.InteropServices;
using System.Text;
using System.ServiceProcess;
using System.Diagnostics;
using System.DirectoryServices;

namespace Caucho
{
  public class Setup
  {
    [DllImport("kernel32.dll", CharSet = CharSet.Auto)]
    public static extern int GetLongPathName(
      [MarshalAs(UnmanagedType.LPTStr)]
      string path,
      [MarshalAs(UnmanagedType.LPTStr)]
      StringBuilder longPath,
      int longPathLength
     );
    
    private static String REG_APACHE_2_2 = "Software\\Apache Software Foundation\\Apache";
    private static String REG_APACHE_2 = "Software\\Apache Group\\Apache";
    private static String REG_SERVICES = "SYSTEM\\CurrentControlSet\\Services";
    
    private String _resinHome;
    private String _apacheHome;
    private String _iisScripts;
    private ArrayList _apacheHomeSet;
    
    
    public String ResinHome {
      get {return _resinHome;}
      set {_resinHome = value;}
    }
    
    public String ApacheHome {
      get {return _apacheHome;}
      set {_apacheHome = value;}
    }
    
    public ArrayList ApacheHomeSet{
      get {return _apacheHomeSet;}
    }
    
    public String IISScripts{
      get {return _iisScripts;}
    }
    
    public Setup()
    {
      String path =  System.Reflection.Assembly.GetExecutingAssembly().Location;
      this.ResinHome = Util.GetResinHome(null, path);
      
      this._apacheHomeSet = new DirSet();
      FindApache(_apacheHomeSet);
      
      _iisScripts = FindIIS();
    }
    
    public bool IsValidResinHome(String dir) {
      return File.Exists(dir + "\\win32\\isapi_srun.dll");
    }
    
    public bool IsValidApacheHome(String dir) {
      return File.Exists(dir + "\\conf\\httpd.conf");
    }
    
    public void FindApache(ArrayList homes) {
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
    
    public void FindApacheInProgramFiles(ArrayList homes) {
      String programFiles 
        = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
      
      String[] groupDirs = Directory.GetDirectories(programFiles, "Apache*");
      
      foreach (String groupDir in groupDirs) {
        String[] testDirs = Directory.GetDirectories(groupDir, "*");
        foreach (String testDir in testDirs){
          if (File.Exists(testDir + "\\bin\\Apache.exe") || File.Exists(testDir + "\\bin\\httpd.exe")) {
            homes.Add(Util.GetCanonicalPath(testDir));
          }
        }
      }
    }
    
    public String FindApacheInRegistry(RegistryKey registryKey, String location) {
      
      RegistryKey apacheKey = registryKey.OpenSubKey(location);
      
      String result = null;
      
      if (apacheKey != null) {
        foreach (String name in apacheKey.GetSubKeyNames()) {
          RegistryKey key = apacheKey.OpenSubKey(name);
          
          String testRoot = (String)key.GetValue("ServerRoot");
          if (testRoot != null && ! "".Equals(testRoot))
            result = testRoot;
        }
      }
      
      if (result != null && result.IndexOf('~')!=-1) {
        StringBuilder builder = new StringBuilder(256);
        GetLongPathName(result, builder, builder.Capacity);
        
        result = builder.ToString();
      }
      
      return result;
    }
    
    public String GetApacheVersion(String apacheHome) {
      ProcessStartInfo startInfo = new ProcessStartInfo();
      
      if (File.Exists(apacheHome + "\\bin\\apache.exe"))
        startInfo.FileName = apacheHome + "\\bin\\apache.exe";
      else if (File.Exists(apacheHome + "\\bin\\httpd.exe"))
        startInfo.FileName = apacheHome + "\\bin\\httpd.exe";
      else
        throw new ApplicationException(String.Format("Can not find apache.exe or httpd.exe in {0}\\bin", apacheHome));
      
      startInfo.RedirectStandardError = true;
      startInfo.RedirectStandardOutput = true;
      startInfo.Arguments = "-v";
      startInfo.UseShellExecute = false;

      StringBuilder error = new StringBuilder();
      String version = null;
      String versionString = null;
      
      Process process = Process.Start(startInfo);
      process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e) {
        if (e.Data != null)
          error.Append(e.Data).Append('\n');
      };
      process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e) {
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
      
      process.BeginErrorReadLine();
      process.BeginOutputReadLine();
      while(! (process.HasExited)) {
        //Vista needs more time
        process.WaitForExit(3000);
      }
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
    
    public ConfigureInfo SetupApache(String resinHome, String apacheHome) {
      
      ConfigureInfo configureInfo = new ConfigureInfo();
      
      String apacheVersion = GetApacheVersion(apacheHome);
      
      String httpdConfData = null;
      
      String httpdConfFile = apacheHome + "\\conf\\httpd.conf";
      StreamReader httpdConfFileReader = null;
      try {
        httpdConfFileReader = new StreamReader(httpdConfFile);
        httpdConfData = httpdConfFileReader.ReadToEnd();
      } catch (Exception e) {
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
              ! IsCommentedOut(line)){
            loadModCauchoLine = lineCounter;
          }
        }
        
        if (line.IndexOf("<IfModule") != -1 &&
            line.IndexOf("mod_caucho.c") != -1 &&
            ! IsCommentedOut(line)) {
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
        while ((line = httpdConfReader.ReadLine())!= null) {
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
        } catch (Exception e) {
          throw e;
        } finally {
          if (httpdConfWriter != null)
            httpdConfWriter.Close();
        }
        
        configureInfo.Status = ConfigureInfo.SETUP_OK;
      } else {
        configureInfo.Status = ConfigureInfo.SETUP_ALREADY;
      }
      
      return configureInfo;
    }
    
    public String FindApacheServiceName(String apacheHome) {
      String apacheHomeLower = apacheHome.ToLower();
      String result = null;
      RegistryKey services = Registry.LocalMachine.OpenSubKey(REG_SERVICES);
      foreach (String name in services.GetSubKeyNames()) {
        Console.WriteLine("Service: " + name);
        RegistryKey key = services.OpenSubKey(name);
        String imagePath = (String)key.GetValue("ImagePath");
        if (imagePath != null && !"".Equals(imagePath)){
          imagePath = imagePath.ToLower();
          if(imagePath.IndexOf(apacheHomeLower) != -1) {
            result = name;
            break;
          }
        }
        key.Close();
      }
      
      services.Close();
      return result;
    }
    
    public String BackupHttpConf(String httpdConfFile) {
      String backUpFile = httpdConfFile + ".bak";
      
      bool backedUp = false;
      int i = 0;
      do {
        if (! File.Exists(backUpFile)) {
          File.Copy(httpdConfFile, backUpFile);
          backedUp = true;
        } else {
          backUpFile = httpdConfFile + ".bak-" + i++;
        }
      } while (! backedUp && i < 100);
      
      if (! backedUp)
        throw new ApplicationException("Can not make back up copy of the file");
      
      return backUpFile;
    }
    
    public ConfigureInfo RemoveApache(String apacheHome) {
      
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
              ! IsCommentedOut(line)) {
            resinRemoved = true;
          } else if (line.IndexOf("IfModule") != -1 &&
                     line.IndexOf("mod_caucho.c") != -1 &&
                     ! IsCommentedOut(line)) {
            inCauchoIfModule = true;
            resinRemoved = true;
          } else if (inCauchoIfModule &&
                     line.IndexOf("/IfModule") != -1){
            inCauchoIfModule = false;
          } else if (inCauchoIfModule) {
          } else {
            buffer.WriteLine(line);
          }
        }
        
        buffer.Flush();
      } catch (Exception e) {
        throw e;
      }
      finally {
        if (httpdConfReader != null)
          httpdConfReader.Close();
      }
      
      if (! resinRemoved ) {
        configInfo.Status = ConfigureInfo.REMOVED_ALREADY;
        
        return configInfo;
      }
      
      configInfo.BackUpFile = BackupHttpConf(httpdConfFile);
      StreamWriter httpdConfWriter = null;
      try {
        httpdConfWriter = new StreamWriter(httpdConfFile);
        httpdConfWriter.Write(buffer.ToString());
        httpdConfWriter.Flush();
      } catch (Exception e) {
        throw e;
      } finally {
        if (httpdConfWriter != null)
          httpdConfWriter.Close();
      }
      
      configInfo.Status = ConfigureInfo.REMOVED_OK;
      
      return configInfo;
    }
    
    public ConfigureInfo SetupIIS(String resinHome, String iisScripts) {
      ConfigureInfo configInfo = new ConfigureInfo();
      
      DirectoryEntry filters = new DirectoryEntry("IIS://localhost/W3SVC/Filters");
      DirectoryEntry resinFilter = null;
      
      foreach (DirectoryEntry entry in filters.Children) {
        if ("Resin".Equals(entry.Name)){
          resinFilter = entry;
        }
      }
      
      if (resinFilter == null)
        resinFilter = filters.Children.Add("Resin", "IIsFilter");
      
      resinFilter.Properties["FilterEnabled"][0] = true;
      resinFilter.Properties["FilterState"][0] = 4;
      resinFilter.Properties["KeyType"][0] = "IIsFilter";
      resinFilter.Properties["FilterPath"][0] = iisScripts + "\\isapi_srun.dll";
      resinFilter.Properties["FilterDescription"][0] = "isapi_srun Extension";
      
      PropertyValueCollection filterOrder = (PropertyValueCollection)filters.Properties["FilterLoadOrder"];
      String val = (String)filterOrder[0];
      
      if (! val.Contains("Resin,"))
        filterOrder[0] = "Resin," + val;
      
      resinFilter.CommitChanges();
      resinFilter.Close();
      filters.CommitChanges();
      filters.Close();
      
      try {
        CopyIsapiFilter(resinHome, iisScripts);
        configInfo.Status = ConfigureInfo.SETUP_OK;
      }
      catch (Exception e){
        configInfo.Status = ConfigureInfo.ISAPI_IO_ERROR;
        configInfo.Exception = e;
      }
      
      return configInfo;
    }
    
    public void CopyIsapiFilter(String resinHome, String iisScripts) {
      String filterPath = iisScripts + "\\isapi_srun.dll";
      if (File.Exists(filterPath))
        File.Delete(filterPath);
      
      File.Copy(resinHome + "\\win32\\isapi_srun.dll", filterPath);
    }
    
    public void RemoveIsapiFilter(String iisScripts) {
      String filterPath = iisScripts + "\\isapi_srun.dll";
      File.Delete(filterPath);
    }
    
    public void StopIIS(){
      ServiceController sc = new ServiceController("W3SVC");
      
      if (sc.Status == ServiceControllerStatus.Running) {
        sc.Stop();
        sc.WaitForStatus(ServiceControllerStatus.Stopped);
      }
      
      sc.Close();
    }
    
    public ConfigureInfo RemoveIIS(String iisScripts){
      ConfigureInfo configInfo = new ConfigureInfo();
      
      DirectoryEntry filters = new DirectoryEntry("IIS://localhost/W3SVC/Filters");
      DirectoryEntry resinFilter = null;
      
      foreach (DirectoryEntry entry in filters.Children) {
        if ("Resin".Equals(entry.Name)){
          resinFilter = entry;
        }
      }
      
      bool resinFound = false;
      if (resinFilter != null) {
        filters.Children.Remove(resinFilter);
        resinFound = true;
      }
      
      PropertyValueCollection filterOrder = (PropertyValueCollection)filters.Properties["FilterLoadOrder"];
      String val = (String)filterOrder[0];
      
      int index = val.IndexOf("Resin,");
      
      if (index != -1) {
        String newVal = val.Substring(0, index) + val.Substring(index + 6, val.Length - 6 - index);
        filterOrder[0] = newVal;
        resinFound = true;
      }
      
      filters.CommitChanges();
      filters.Close();

      try {
        String filterPath = iisScripts + "\\isapi_srun.dll";
        if (File.Exists(filterPath))
          File.Delete(filterPath);
        
        if (resinFound)
          configInfo.Status = ConfigureInfo.REMOVED_OK;
        else
          configInfo.Status = ConfigureInfo.REMOVED_ALREADY;
      } catch (Exception e){
        configInfo.Status = ConfigureInfo.ISAPI_IO_ERROR;
        configInfo.Exception = e;
      }
      
      return configInfo;
    }
    
    private String FindIIS() {
      String result = null;
      
      DirectoryEntry entry = null;
      try {
        entry = new DirectoryEntry("IIS://localhost/W3SVC/1/ROOT/scripts");
        
        if (entry.Properties != null) {
          Object val = entry.Properties["Path"];
          if (val != null && (val is PropertyValueCollection)) {
            PropertyValueCollection collection = (PropertyValueCollection) val;
            IEnumerator enumerator = collection.GetEnumerator();
            
            if (enumerator.MoveNext())
              result = (String) enumerator.Current;
          }
        }
      } catch (Exception e) {
        Console.Out.WriteLine(e.ToString());
      } finally {
        if (entry != null)
          entry.Close();
      }
      
      return result;
    }
    
    public void RestartIIS() {
      RestartService("W3SVC");
    }

    public void RestartService(String serviceName) {
      ServiceController sc = new ServiceController(serviceName);
      
      if (sc.Status == ServiceControllerStatus.Running) {
        sc.Stop();
        sc.WaitForStatus(ServiceControllerStatus.Stopped);
      }
      
      sc.Start();
      sc.WaitForStatus(ServiceControllerStatus.Running);
      
      sc.Close();
    }
    
    private static bool IsCommentedOut(String line) {
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
    
    
    [STAThread]
    public static void Main(String[] args) {
      SetupWindow w = new SetupWindow(new Setup());
      Application.Run(w);
    }
  }
  
  class DirSet : ArrayList {
    //perf doesn't matter
    public override int Add(object value)
    {
      int index = base.IndexOf(value);
      
      if (index != -1)
        return index;
      
      return base.Add(value);
    }
  }
  
  public class ConfigureInfo {
    public static int SETUP_ALREADY = 1;
    public static int SETUP_OK = 2;
    public static int ISAPI_IO_ERROR = 3;
    public static int REMOVED_OK = 4;
    public static int REMOVED_ALREADY = 5;
    
    private String _backupFile;
    private String _serviceName;
    private int _status;
    private Exception _exception;
    
    public String BackUpFile {
      set {_backupFile = value;}
      get {return _backupFile;}
    }
    
    public String ServiceName {
      set {_serviceName = value;}
      get {return _serviceName;}
    }
    
    public int Status {
      set {_status = value;}
      get {return _status;}
    }
    
    public Exception Exception {
      set {_exception = value;}
      get {return _exception;}
    }
  }
}
