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

namespace Caucho
{
  public partial class SetupForm : Form
  {
    private Setup _setup;
    private String _newService = "New Service ...";
    private ArrayList _userJdks = new ArrayList();
    private Environment.SpecialFolder _none;

    public SetupForm(Setup setup)
    {
      _setup = setup;

      InitializeComponent();
      _none = _folderDlg.RootFolder;

      _servicesCmbBox.DataSource = _setup.GetResinServices();

      _resinCmbBox.DataSource = _setup.GetResinList();
      _resinCmbBox.SelectedItem = _setup.Resin;
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
            _setup.SelectResin(resinHome);
            _resinCmbBox.DataSource = null;
            _resinCmbBox.DataSource = _setup.GetResinList();
            _resinCmbBox.SelectedItem = _setup.Resin;
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

    private void ResinSelectectionChanged(object sender, EventArgs e)
    {
      Resin resin = (Resin)_resinCmbBox.SelectedItem;
      if (resin != null) {
        _setup.Resin = resin;
        IList<ResinService> services = _setup.GetResinServices(resin);
        ArrayList items = new ArrayList();

        items.Add(_newService);
        items.AddRange((ICollection)services);

        _servicesCmbBox.BeginUpdate();
        _servicesCmbBox.DataSource = null;
        _servicesCmbBox.DataSource = items;

        if (services.Count == 0) {
          _servicesCmbBox.SelectedIndex = 0;
        } else if (services.Count > 0) {
          StringBuilder builder = new StringBuilder("Select Service: [");
          for (int i = 0; i < services.Count; i++) {
            ResinService service = services[i];
            builder.Append(service.ServiceName);
            if (i + 1 < services.Count)
              builder.Append(", ");
          }
          builder.Append(']');

          _servicesCmbBox.Text = builder.ToString();
        }

        _servicesCmbBox.EndUpdate();
      }
    }

    private void ServiceSelectionChanged(object sender, EventArgs e)
    {
      if (_newService.Equals(_servicesCmbBox.SelectedItem)) {
        //new service
        String confFile = _setup.getConfFile(_setup.Resin);
        if (confFile != null)
          _resinConfTxtBox.Text = "Default: [" + confFile + "]";
        else
          _resinConfTxtBox.Text = "Please specify configuration file";

        ResinConf resinConf = _setup.GetResinConf(_setup.Resin, confFile);

        IList servers = resinConf.getServerIds();
        _serverCmbBox.BeginUpdate();
        _serverCmbBox.DataSource = null;
        _serverCmbBox.DataSource = servers;
        _serverCmbBox.EndUpdate();

        updateJavaHomes(null);

        String resinRoot = resinConf.getRootDirectory();
        String logDir = null;
        if (resinRoot == null) {
          logDir = "[" + _setup.Resin.Home + @"]\log";
          resinRoot = "Default: [" + _setup.Resin.Home + "]";
        } else if (Util.IsAbsolutePath(resinRoot)) {
          logDir = "[" + resinRoot + @"]\log";
        } else {
          logDir = "[" + _setup.Resin.Home + '\\' + Util.Canonicalize(resinRoot) + @"]\log";
          resinRoot = "Relative: [" + _setup.Resin.Home + "]" + '\\' + Util.Canonicalize(resinRoot);
        }
        _resinRootTxtBox.Text = resinRoot;

        _logDirTxtBox.Text = logDir;

        if (servers.Count > 0) {
          String id = ((ResinConfServer)servers[0]).ID;
          if ("".Equals(id))
            _serviceNameTxtBox.Text = "Resin";
          else
            _serviceNameTxtBox.Text = "Resin-" + id;
        }

        //        ;

        //        _servicePassTxtBox;


        //        ;
        //        _javaHomeCmbBox;
        //        _previewCmbBox;
        //        _serviceUserCmbBox;
        //        _servicesCmbBox;
        //        _debugPortTxtBox;
        //        _jmxPortTxtBox;

      } else if (_servicesCmbBox.SelectedItem is ResinService) {
        //existing service
        ResinService resinService = (ResinService)_servicesCmbBox.SelectedItem;

        _serverCmbBox.SelectedItem = resinService.Server;
        _serviceNameTxtBox.Text = resinService.ServiceName;
        _resinRootTxtBox.Text = resinService.Root;
        _logDirTxtBox.Text = resinService.LogDirectory;
        _javaHomeCmbBox.Text = resinService.JavaHome;
        _jmxPortTxtBox.Text = resinService.JmxPort.ToString();
        _debugPortTxtBox.Text = resinService.DebugPort.ToString();
      } else {
        _resinConfTxtBox.Text = "";
        _serviceNameTxtBox.Text = "";
        _servicePassTxtBox.Text = "";
        _logDirTxtBox.Text = "";
        _resinRootTxtBox.Text = "";
        _javaHomeCmbBox.Text = "";
        _previewCmbBox.Text = "";
        _serviceUserCmbBox.Text = "";
        _servicesCmbBox.Text = "";
        _debugPortTxtBox.Text = "";
        _jmxPortTxtBox.Text = "";
      }
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

            updateJavaHomes(javaHome);
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

    private void updateJavaHomes(String javaHome)
    {
      ArrayList jdks = new ArrayList();
      jdks.AddRange(_userJdks);
      IList foundJdks = Util.FindJava();
      foreach (String jdk in foundJdks) {
        if (!jdks.Contains(jdk)) {
          jdks.Add(jdk);
        }
      }

      if (javaHome == null && jdks.Count > 0)
        javaHome = jdks[0].ToString();

      _javaHomeCmbBox.BeginUpdate();
      _javaHomeCmbBox.DataSource = jdks;
      if (javaHome == null)
        _javaHomeCmbBox.SelectedIndex = -1;
      else
        _javaHomeCmbBox.SelectedItem = javaHome;

      _javaHomeCmbBox.EndUpdate();
    }

    private void SelectResinRoot(object sender, EventArgs e)
    {
      _folderDlg.RootFolder = _none;
      _folderDlg.SelectedPath = _setup.Resin.Home;
      if (DialogResult.OK.Equals(_folderDlg.ShowDialog())) {
        String resinRoot = _folderDlg.SelectedPath;
        if (resinRoot.Equals(_setup.Resin.Home))
          _resinRootTxtBox.Text = "Default: [" + _setup.Resin.Home + "]";
        else
          _resinRootTxtBox.Text = resinRoot;
      }
    }

    private void SelectResinConf(object sender, EventArgs e)
    {
      String file;
      if (Util.IsAbsolutePath(_resinConfTxtBox.Text)) {
        file = _resinConfTxtBox.Text;
      } else {
        file = _setup.Resin.Home + '\\' + _setup.getConfFile(_setup.Resin);
      }

      int lastSlashIdx = file.LastIndexOf('\\');
      if (lastSlashIdx != -1) {
        _fileDlg.InitialDirectory = file.Substring(0, lastSlashIdx);
        _fileDlg.FileName = file.Substring(lastSlashIdx + 1, file.Length - lastSlashIdx - 1);
      }

      if (DialogResult.OK.Equals(_fileDlg.ShowDialog())) {
        _resinConfTxtBox.Text = _fileDlg.FileName;
      }
    }

    private void ResinRootChanged(object sender, EventArgs e)
    {
      String resinRoot = _resinRootTxtBox.Text;
      if ("".Equals(_logDirTxtBox.Text) || !Util.IsAbsolutePath(_logDirTxtBox.Text))
        _logDirTxtBox.Text = "[" + resinRoot + @"]\log";
    }
  }
}
