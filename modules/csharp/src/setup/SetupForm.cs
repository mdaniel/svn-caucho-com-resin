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
using System.Collections.Generic;
using System.Collections;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.IO;

namespace Caucho
{
  public partial class SetupForm : Form
  {
    private enum MODE { NEW, EXISTING, NONE };

    private Setup _setup;
    private String _createService = "New Service ...";
    private ArrayList _userJdks = new ArrayList();
    private Environment.SpecialFolder _none;
    private Resin _resin;
    private ResinService _resinService;
    private ResinConf _resinConf;
    private MODE _mode;
    private String _resinRoot;
    private String _resinLog;
    private String _resinConfFile;
    private String _javaHome;

    public SetupForm(Setup setup)
    {
      _setup = setup;
      _resin = _setup.Resin;

      InitializeComponent();
      _none = _folderDlg.RootFolder;


      _resinCmbBox.BeginUpdate();
      _resinCmbBox.DataSource = _setup.GetResinList();
      _resinCmbBox.SelectedItem = _resin;
      _resinCmbBox.EndUpdate();

      ResinSelectectionCommitted(null, null);

      _mode = MODE.NONE;
    }

    private void SelectResinBtnClick(object sender, EventArgs e)
    {
      String resinHome = Util.GetResinHome(null, System.Reflection.Assembly.GetExecutingAssembly().Location);

      bool select = true;

      while (select) {
        if (resinHome == null || "".Equals(resinHome))
          _folderDlg.RootFolder = Environment.SpecialFolder.MyComputer;
        else
          _folderDlg.SelectedPath = resinHome;

        if (_folderDlg.ShowDialog() == DialogResult.OK) {
          resinHome = _folderDlg.SelectedPath;
          //
          if (Util.IsResinHome(resinHome)) {
            SelectResin(resinHome);
            select = false;
          } else {
            String caption = "Incorrect Resin Home";
            String message = "Resin Home must contain lib\\resin.jar";

            if (MessageBox.Show(message, caption, MessageBoxButtons.RetryCancel) == DialogResult.Cancel)
              select = false;
          }
        } else {
          select = false;
        }
      }
    }

    private void SelectResin(String resinHome)
    {
      _resinCmbBox.BeginUpdate();
      _resin = _setup.SelectResin(resinHome);
      _resinCmbBox.DataSource = _setup.GetResinList();
      _resinCmbBox.SelectedItem = _resin;
      _resinCmbBox.EndUpdate();

      ResinSelectectionCommitted(null, null);
    }

    private void ResinSelectectionCommitted(object sender, EventArgs e)
    {
      _resin = (Resin)_resinCmbBox.SelectedItem;

      if (_resin != null) {
        IList<ResinService> services = _setup.GetResinServices(_resin);
        ArrayList items = new ArrayList();

        items.Add(_createService);
        items.AddRange((ICollection)services);

        _servicesCmbBox.BeginUpdate();
        _servicesCmbBox.DataSource = items;

        if (services.Count == 0) {
          _servicesCmbBox.SelectedIndex = 0;
        } else if (services.Count > 0) {
          _servicesCmbBox.SelectedIndex = -1;
          StringBuilder builder = new StringBuilder("Select Service: [");
          for (int i = 0; i < services.Count; i++) {
            ResinService service = services[i];
            builder.Append(service.ServiceName);
            if (i + 1 < services.Count)
              builder.Append(", ");
          }
          builder.Append(']');

          _servicesCmbBox.Text = builder.ToString();
          Console.WriteLine(_servicesCmbBox.Text);
        }
        _servicesCmbBox.EndUpdate();
      }
    }

    private void ServiceSelectionChanged(object sender, EventArgs e)
    {

      if (_createService.Equals(_servicesCmbBox.SelectedItem)) {
        _resinService = null;
        _mode = MODE.NEW;
      } else if (_servicesCmbBox.SelectedItem is ResinService) {
        _mode = MODE.EXISTING;
        _resinService = (ResinService)_servicesCmbBox.SelectedItem;
      } else {
        _mode = MODE.NONE;
      }
      bool enabled = MODE.NEW.Equals(_mode);
      _resinConfTxtBox.Enabled = enabled;
      _selectResinConfBtn.Enabled = enabled;
      _serverCmbBox.Enabled = enabled;
      _serviceNameTxtBox.Enabled = enabled;
      _serviceUserCmbBox.Enabled = enabled;
      _servicePassTxtBox.Enabled = enabled;
      enabled = !MODE.NONE.Equals(_mode);
      _javaHomeCmbBox.Enabled = enabled;
      _selectJavaHomeBtn.Enabled = enabled;
      _resinRootTxtBox.Enabled = enabled;
      _selectResinRootBtn.Enabled = enabled;
      _logDirTxtBox.Enabled = enabled;
      _selectLogDirBtn.Enabled = enabled;
      _previewCmbBox.Enabled = enabled;
      _jmxPortTxtBox.Enabled = enabled;
      _debugPortTxtBox.Enabled = enabled;
      _extraParamsTxbBox.Enabled = enabled;
      _watchdogPortTxtBox.Enabled = enabled;

      UpdateDetails();
    }

    private void UpdateDetails()
    {
      _resinRoot = null;
      _resinLog = null;
      _resinConfFile = null;
      _resinConf = null;
      if (_resinService != null)
        _resinConfFile = _resinService.Conf;

      if (_resinConfFile == null)
        _resinConfFile = _setup.GetResinConfFile(_resin);

      if (_resinConfFile != null && !"".Equals(_resinConfFile) && !Util.IsAbsolutePath(_resinConfFile))
        _resinConfFile = _resin.Home + @"\" + _resinConfFile;

      if (_resinConfFile != null) {
        _resinConfTxtBox.Text = _resinConfFile;
        _resinConf = _setup.GetResinConf(_resinConfFile);
      } else
        _resinConfTxtBox.Text = "Please specify configuration file";

      _resinRoot = null;
      if (_resinService != null)
        _resinRoot = _resinService.Root;

      if (_resinRoot == null || "".Equals(_resinRoot))
        _resinRoot = _resinConf.getRootDirectory();

      if (_resinRoot != null && !"".Equals(_resinRoot) && !Util.IsAbsolutePath(_resinRoot))
        _resinRoot = Util.GetCanonicalPath(_resin.Home + @"\" + _resinRoot);

      if (_resinRoot == null || "".Equals(_resinRoot))
        _resinRoot = _resin.Home;

      _resinRootTxtBox.Text = _resinRoot;

      _resinLog = null;
      if (_resinService != null)
        _resinLog = _resinService.LogDirectory;

      if (_resinLog == null || "".Equals(_resinLog))
        _resinLog = "log";

      if (!Util.IsAbsolutePath(_resinLog))
        _resinLog = _resinRoot + @"\log";

      _logDirTxtBox.Text = _resinLog;

      if (_resinService != null && _resinService.IsPreview)
        _previewCmbBox.SelectedItem = "Yes";
      else
        _previewCmbBox.SelectedItem = "No";

      String javaHome = null;
      if (_resinService != null)
        javaHome = _resinService.JavaHome;
      UpdateJavaHomes(javaHome);

      UpdateServices();

      UpdateJmxAndDebugPorts();
    }

    private void UpdateServices()
    {
      IList servers = _resinConf.getServerIds();
      _serverCmbBox.BeginUpdate();
      _serverCmbBox.DataSource = null;
      _serverCmbBox.DataSource = servers;
      _serverCmbBox.EndUpdate();

      _serviceUserCmbBox.DataSource = _setup.GetUsers();

      if (_resinService != null)
        _serviceNameTxtBox.Text = _resinService.ServiceName;
      else if (servers.Count > 0) {
        String cluster = ((ResinConfServer)servers[0]).Cluster;
        String id = ((ResinConfServer)servers[0]).ID;
        if ("".Equals(id))
          _serviceNameTxtBox.Text = "Resin";
        else
          _serviceNameTxtBox.Text = "Resin-" + id;
      }
    }

    private void UpdateJmxAndDebugPorts()
    {
      String jmxPort = null;
      String debugPort = null;
      String watchDogPort = null;

      if (_resinService != null) {
        if (_resinService.JmxPort > 0)
          jmxPort = _resinService.JmxPort.ToString();

        if (_resinService.DebugPort > 0)
          debugPort = _resinService.DebugPort.ToString();

        if (_resinService.WatchdogPort > 0)
          watchDogPort = _resinService.WatchdogPort.ToString();
      }

      ResinConfServer server = null;
      if ((jmxPort == null || debugPort == null) && _serverCmbBox.SelectedItem is ResinConfServer)
        server = (ResinConfServer)_serverCmbBox.SelectedItem;

      if (jmxPort == null && server != null)
        jmxPort = _resinConf.GetJmxPort(server.Cluster, server.ID);

      if (jmxPort == null)
        jmxPort = "Not Specified";

      if (debugPort == null && server != null)
        debugPort = _resinConf.GetDebugPort(server.Cluster, server.ID);

      if (debugPort == null)
        debugPort = "Not Specified";

      if (watchDogPort == null && server != null)
        watchDogPort = _resinConf.GetWatchDogPort(server.Cluster, server.ID);

      if (watchDogPort == null)
        watchDogPort = "Not Specified";

      _jmxPortTxtBox.Text = jmxPort;
      _debugPortTxtBox.Text = debugPort;
      _watchdogPortTxtBox.Text = watchDogPort;
    }

    public void ResinConfFileChanged()
    {
      UpdateServices();
      UpdateJmxAndDebugPorts();
    }

    private void SelectJavaHome(object sender, EventArgs e)
    {
      bool select = true;
      String javaHome;
      while (select) {
        _folderDlg.RootFolder = Environment.SpecialFolder.ProgramFiles;

        if (_folderDlg.ShowDialog() == DialogResult.OK) {
          javaHome = _folderDlg.SelectedPath;
          if (Util.IsValidJavaHome(javaHome)) {
            _userJdks.Add(javaHome);

            UpdateJavaHomes(javaHome);
            select = false;
          } else {
            String caption = "Incorrect Java Home";
            String message = @"Java Home must contain bin\java.exe";

            if (MessageBox.Show(message, caption, MessageBoxButtons.RetryCancel) == DialogResult.Cancel)
              select = false;
          }
        } else {
          select = false;
        }
      }
    }

    private void UpdateJavaHomes(String javaHome)
    {
      ArrayList jdks = new ArrayList();
      jdks.AddRange(_userJdks);

      IList foundJdks = Util.FindJava();

      if (foundJdks.Count > 0)
        jdks.Add("Default: [" + foundJdks[0] + "]");

      foreach (String jdk in foundJdks) {
        if (!jdks.Contains(jdk)) {
          jdks.Add(jdk);
        }
      }

      if (javaHome == null && jdks.Count > 0) {
        javaHome = jdks[0].ToString();
      }

      _javaHomeCmbBox.BeginUpdate();
      _javaHomeCmbBox.DataSource = jdks;
      if (javaHome == null)
        _javaHomeCmbBox.SelectedIndex = -1;
      else
        _javaHomeCmbBox.SelectedItem = javaHome;

      _javaHome = (String)_javaHomeCmbBox.SelectedItem;

      _javaHomeCmbBox.EndUpdate();
    }

    private void SelectResinRoot(object sender, EventArgs e)
    {
      _folderDlg.RootFolder = _none;
      _folderDlg.SelectedPath = _resin.Home;
      if (DialogResult.OK.Equals(_folderDlg.ShowDialog())) {
        String resinRoot = _folderDlg.SelectedPath;
        _resinRootTxtBox.Text = resinRoot;
        ResinRootTxtBoxValidating(null, null);
      }
    }

    private void SelectResinConf(object sender, EventArgs e)
    {

      int lastSlashIdx = _resinConfFile.LastIndexOf('\\');
      if (lastSlashIdx != -1) {
        _fileDlg.InitialDirectory = _resinConfFile.Substring(0, lastSlashIdx);
        _fileDlg.FileName = _resinConfFile.Substring(lastSlashIdx + 1, _resinConfFile.Length - lastSlashIdx - 1);
      }

      if (DialogResult.OK.Equals(_fileDlg.ShowDialog())) {
        _resinConfFile = _fileDlg.FileName;
        _resinConfTxtBox.Text = _resinConfFile;
        _resinConf = _setup.GetResinConf(_resinConfFile);
        ResinConfFileChanged();
      }
    }

    private void ServerSelectionChanged(object sender, EventArgs e)
    {
      UpdateJmxAndDebugPorts();
    }

    private void SelectLogDirectory(object sender, EventArgs e)
    {
      _folderDlg.RootFolder = _none;
      String log = _resinLog;

      while (!Directory.Exists(log)) {
        log = log.Substring(0, log.LastIndexOf('\\'));
      }

      _folderDlg.SelectedPath = log;

      if (DialogResult.OK.Equals(_folderDlg.ShowDialog())) {
        _resinLog = _folderDlg.SelectedPath;
        _logDirTxtBox.Text = _resinLog;
      }
    }

    private void ResinCmbBoxValidating(object sender, CancelEventArgs e)
    {
      String resinHome = _resinCmbBox.Text;
      if (Util.IsResinHome(resinHome)) {
        SelectResin(resinHome);
      } else {
        String caption = "Incorrect Resin Home";
        String message = @"Resin Home must contain lib\resin.jar";
        MessageBox.Show(message, caption, MessageBoxButtons.OK);
        _resinCmbBox.Text = _resinCmbBox.SelectedItem.ToString();
        e.Cancel = true;
      }
    }

    private void JavaHomeCmbBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      if (_javaHomeCmbBox.Text.StartsWith("Default: [")
        && _javaHomeCmbBox.SelectionLength < _javaHomeCmbBox.Text.Length)
        e.Handled = true;

      if (e.KeyChar == 27)
        _javaHomeCmbBox.Text = _javaHomeCmbBox.SelectedItem.ToString();
    }

    private void JavaHomeCmbBoxValidating(object sender, CancelEventArgs e)
    {
      String javaHome = _javaHomeCmbBox.Text;
      if (javaHome.StartsWith("Default: [")) {
      } else if (Util.IsValidJavaHome(javaHome)) {
      } else {
        String caption = "Incorrect Java Home";
        String message = @"Java Home must contain bin\java.exe";
        MessageBox.Show(message, caption, MessageBoxButtons.OK);
        _javaHomeCmbBox.Text = _javaHome;
        e.Cancel = true;
      }
    }

    private void ServicesCmbBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      e.Handled = true;
    }

    private void SetupFormClosing(object sender, FormClosingEventArgs e)
    {
      e.Cancel = false;
    }

    private void ResinRootTxtBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      if (e.KeyChar == 27) {
        _resinRootTxtBox.Text = _resinRoot;
        e.Handled = true;
      }
    }

    private void ResinRootTxtBoxValidating(object sender, CancelEventArgs e)
    {
      String resinRoot = null;

      if (Util.IsAbsolutePath(_resinRootTxtBox.Text)) {
        resinRoot = _resinRootTxtBox.Text;
      } else if (_resinConf.getRootDirectory() == null) {
        String path = Util.Canonicalize(_resinRootTxtBox.Text);
        resinRoot = _resin.Home + (path.StartsWith(@"\") ? "" : @"\") + path;
      }

      if (_resinLog.Equals(_resinRoot + @"\log")) {
        _resinLog = resinRoot + @"\log";
        _logDirTxtBox.Text = _resinLog;
      }

      _resinRoot = resinRoot;
      _resinRootTxtBox.Text = _resinRoot;
    }

    private void ServiceRefreshBtnClick(object sender, EventArgs e)
    {
      this.ServiceSelectionChanged(null, null);
    }

    private void LogDirTxtBoxValidating(object sender, CancelEventArgs e)
    {
      String log = _logDirTxtBox.Text;
      if (Util.IsAbsolutePath(log))
        _resinLog = log;
      else
        _resinLog = _resinRoot + @"\" + log;
    }

    private void ResinConfTxtBoxValidating(object sender, CancelEventArgs e)
    {
      String resinConfFile = _resinConfTxtBox.Text;

      if (!Util.IsAbsolutePath(resinConfFile))
        resinConfFile = Util.GetCanonicalPath(_resin.Home + @"\" + resinConfFile);

      if (File.Exists(resinConfFile)) {
        _resinConfFile = resinConfFile;
        _resinConf = _setup.GetResinConf(resinConfFile);
        ResinConfFileChanged();
      } else {
        String caption = "Incorrect Resin Conf File";
        String message = @"File '" + resinConfFile + "' does not exist";
        MessageBox.Show(message, caption, MessageBoxButtons.OK);
        _resinConfTxtBox.Text = _resinConfFile;
        e.Cancel = true;
      }
    }

    private void _resinConfTxtBox_KeyPress(object sender, KeyPressEventArgs e)
    {
      if (e.KeyChar == 27)
        _resinConfTxtBox.Text = _resinConfFile;
    }

    private void PreviewCmbBoxKeyPress(object sender, KeyPressEventArgs e)
    {
      char c = e.KeyChar;
      switch (c) {
        case 'y':
        case 'Y':
        case 't':
        case 'T': {
            e.Handled = true;
            _previewCmbBox.SelectedItem = "Yes";
            break;
          }
        case 'n':
        case 'N':
        case 'f':
        case 'F': {
            _previewCmbBox.SelectedItem = "No";
            e.Handled = true;
            break;
          }
        default: {
            e.Handled = true;
            break;
          }
      }
    }

    private void _previewCmbBox_KeyDown(object sender, KeyEventArgs e)
    {

      if (Keys.Left.Equals(e.KeyCode) && "Yes".Equals(_previewCmbBox.SelectedItem)) {
        _previewCmbBox.SelectedItem = "No";
        e.Handled = true;
      } else if (Keys.Right.Equals(e.KeyCode) && "No".Equals(_previewCmbBox.SelectedItem)) {
        _previewCmbBox.SelectedItem = "Yes";
        e.Handled = true;
      }

    }

    private void ServiceInstallBtnClick(object sender, EventArgs e)
    {
      if (MODE.NEW.Equals(_mode)) {
        ResinService resinService = new ResinService();
        resinService.Home = _resin.Home;
        resinService.Root = _resinRoot;
        resinService.Conf = _resinConfFile;
        if (!_javaHomeCmbBox.Text.StartsWith("Default: ["))
          resinService.JavaHome = _javaHome;
        resinService.IsPreview = "Yes".Equals(_previewCmbBox.Text);
        ResinConfServer server = null;
        if (_serverCmbBox.SelectedItem is ResinConfServer)
          server = (ResinConfServer)_serverCmbBox.SelectedItem;

        if (server != null) {
          resinService.Server = server.ID;
          if (!"Not Specified".Equals(_jmxPortTxtBox.Text)) {
            String jmxPort = _resinConf.GetJmxPort(server.Cluster, server.ID);
            if (!_jmxPortTxtBox.Text.Equals(jmxPort))
              resinService.JmxPort = int.Parse(jmxPort);
          }

          if (!"Not Specified".Equals(_debugPortTxtBox.Text)) {
            String debugPort = _resinConf.GetDebugPort(server.Cluster, server.ID);
            if (!_debugPortTxtBox.Text.Equals(debugPort))
              resinService.DebugPort = int.Parse(debugPort);
          }

          if (!"Not Specified".Equals(_watchdogPortTxtBox.Text)) {
            String watchDogPort = _resinConf.GetDebugPort(server.Cluster, server.ID);
          }
        } else {
          resinService.DynamicServer = _serverCmbBox.Text;
        }
        resinService.LogDirectory = _logDirTxtBox.Text;

        resinService.ExtraParams = _extraParams.Text;

        Console.WriteLine(resinService.GetCommandLine());
      }
    }

    private void label1_Click(object sender, EventArgs e)
    {

    }

    private void textBox1_TextChanged(object sender, EventArgs e)
    {

    }
  }
}
