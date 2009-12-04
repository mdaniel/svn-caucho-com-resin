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
using System.Drawing;
using System.Windows.Forms;
using System.ComponentModel;
using System.IO;

namespace Caucho
{
  public partial class SetupWindow : Form
  {
    private static String MSG_INVALID_RESIN_HOME =
      "{0} is an illegal RESIN_HOME. \nRESIN_HOME must have a win32\\isapi_srun.dll\n";

    private static String MSG_INVALID_APACHE_HOME =
      "{0} is an illegal APACHE_HOME. \nAPACHE_HOME is expected to have a conf\\httpd.conf\n";
    
    private static String MSG_INVALID_IIS_HOME =
      "Directory {0} specified for IIS does not exist\n";
    
    private Setup _setup;
    
    public SetupWindow(Setup setup)
    {
      _setup = setup;
      
      InitializeComponent();
    }
    private System.ComponentModel.IContainer components = null;
    protected override void Dispose(bool disposing)
    {
      if (disposing && (components != null))
      {
        components.Dispose();
      }
      base.Dispose(disposing);
    }

    private void InitializeComponent()
    {
      GroupBox generalGrp = new GroupBox();
      Label resinLbl = new Label();
      GroupBox serversGrp = new GroupBox();
      FolderBrowserDialog  folderDlg = new FolderBrowserDialog ();
      TextBox resinDir = new TextBox();
      Button selectResinBtn = new Button();
      ComboBox apacheDirs = new ComboBox();
      Button selectApacheBtn = new Button();
      TextBox iisDir = new TextBox();
      Button selectIISBtn = new Button();
      
      CheckBox apacheCheck = new CheckBox();
      CheckBox iisCheck = new CheckBox();
      Button okButton = new Button();
      Button cancelButton = new Button();
      Button applyButton = new Button();
      Button removeButton = new Button();
      generalGrp.SuspendLayout();
      serversGrp.SuspendLayout();
      SuspendLayout();
      // 
      // 
      generalGrp.Controls.Add(selectResinBtn);
      generalGrp.Controls.Add(resinDir);
      generalGrp.Controls.Add(resinLbl);
      generalGrp.Location = new System.Drawing.Point(12, 9);
      generalGrp.Size = new System.Drawing.Size(472, 64);
      generalGrp.TabIndex = 0;
      generalGrp.TabStop = false;
      generalGrp.Text = "General";
      //groupBox1.Enter += new System.EventHandler(this.groupBox1_Enter);
      // 
      folderDlg.ShowNewFolderButton = false;
      // 
      resinLbl.AutoSize = true;
      resinLbl.Location = new System.Drawing.Point(20, 28);
      resinLbl.Size = new System.Drawing.Size(65, 13);
      resinLbl.TabIndex = 0;
      resinLbl.Text = "&Resin Home";
      // 
      // 
      serversGrp.Controls.Add(iisCheck);
      serversGrp.Controls.Add(apacheCheck);
      serversGrp.Controls.Add(selectIISBtn);
      serversGrp.Controls.Add(iisDir);
      serversGrp.Controls.Add(selectApacheBtn);
      //serversGrp.Controls.Add(apacheDir);
      serversGrp.Controls.Add(apacheDirs);
      serversGrp.Location = new System.Drawing.Point(12, 90);
      serversGrp.Size = new System.Drawing.Size(472, 96);
      serversGrp.TabIndex = 1;
      serversGrp.TabStop = false;
      serversGrp.Text = "Web Servers";
      // 
      // 
      resinDir.Location = new System.Drawing.Point(93, 24);
      resinDir.Size = new System.Drawing.Size(346, 20);
      resinDir.TabIndex = 1;
      resinDir.Text = _setup.ResinHome;
      // 
      // 
      selectResinBtn.Location = new System.Drawing.Point(440, 24);
      selectResinBtn.Size = new System.Drawing.Size(24, 20);
      selectResinBtn.TabIndex = 2;
      selectResinBtn.Text = "...";
      selectResinBtn.UseVisualStyleBackColor = true;
      selectResinBtn.Click += delegate(object sender, EventArgs e) {
        folderDlg.SelectedPath = resinDir.Text;
        folderDlg.ShowDialog();
        
        if (folderDlg.SelectedPath != null &&
            ! "".Equals(folderDlg.SelectedPath) &&
            CheckResinHome(folderDlg.SelectedPath)) {
          resinDir.Text = folderDlg.SelectedPath;
        }
      };
      // 
      // 
      selectApacheBtn.Location = new System.Drawing.Point(440, 25);
      selectApacheBtn.Size = new System.Drawing.Size(24, 20);
      selectApacheBtn.TabIndex = 4;
      selectApacheBtn.Text = "...";
      selectApacheBtn.UseVisualStyleBackColor = true;
      selectApacheBtn.Click += delegate(object sender, EventArgs e) {
        folderDlg.SelectedPath = (String)apacheDirs.SelectedItem;
        folderDlg.ShowDialog();
        
        if (folderDlg.SelectedPath != null &&
            ! "".Equals(folderDlg.SelectedPath) &&
            CheckApacheHome(folderDlg.SelectedPath)) {
          
          int i = 0;
          bool found = false;
          foreach (String str in apacheDirs.Items) {
            if (str.ToLower().Equals(folderDlg.SelectedPath.ToLower())) {
              apacheDirs.SelectedIndex = i;
              found = true;
              break;
            }
            i++;
          }
          
          if (! found) {
            apacheDirs.Items.Add(folderDlg.SelectedPath);
            apacheDirs.SelectedIndex = i;
          }
          
          apacheCheck.Checked = true;
        }
      };
      // 
      // 
      apacheDirs.Location = new System.Drawing.Point(93, 25);
      apacheDirs.Size = new System.Drawing.Size(346, 20);
      apacheDirs.TabIndex = 3;
      
      foreach (String str in _setup.ApacheHomeSet)
        apacheDirs.Items.Add(str);
      
      if (apacheDirs.Items.Count > 0)
        apacheDirs.SelectedIndex = 0;
      // 
      // 
      selectIISBtn.Location = new System.Drawing.Point(440, 58);
      selectIISBtn.Size = new System.Drawing.Size(24, 20);
      selectIISBtn.TabIndex = 6;
      selectIISBtn.Text = "...";
      selectIISBtn.UseVisualStyleBackColor = true;
      selectIISBtn.Click += delegate(object sender, EventArgs e) {
        folderDlg.SelectedPath = iisDir.Text;
        folderDlg.ShowDialog();
        
        if (folderDlg.SelectedPath != null &&
            ! "".Equals(folderDlg.SelectedPath)) {
          iisDir.Text = folderDlg.SelectedPath;
          iisCheck.Checked = true;
        }
      };
      // 
      // 
      iisDir.Location = new System.Drawing.Point(93, 58);
      iisDir.Size = new System.Drawing.Size(346, 20);
      iisDir.TabIndex = 5;
      iisDir.Text = _setup.IISScripts;
      iisCheck.Checked = _setup.IISScripts != null;
      // 
      // 
      apacheCheck.AutoSize = true;
      apacheCheck.Location = new System.Drawing.Point(20, 26);
      apacheCheck.Size = new System.Drawing.Size(63, 17);
      apacheCheck.TabIndex = 7;
      apacheCheck.Text = "Apache";
      apacheCheck.UseVisualStyleBackColor = true;
      apacheCheck.Checked = _setup.ApacheHomeSet.Count > 0;
      // 
      // 
      iisCheck.AutoSize = true;
      iisCheck.Location = new System.Drawing.Point(20, 58);
      iisCheck.Size = new System.Drawing.Size(39, 17);
      iisCheck.TabIndex = 8;
      iisCheck.Text = "IIS";
      iisCheck.UseVisualStyleBackColor = true;
      // 
      // 
      okButton.Location = new System.Drawing.Point(149, 198);
      okButton.Size = new System.Drawing.Size(75, 23);
      okButton.TabIndex = 2;
      okButton.Text = "OK";
      okButton.UseVisualStyleBackColor = true;
      okButton.Click += delegate {
        OK(resinDir.Text,
              apacheDirs.Text,
              apacheCheck.Checked,
              iisDir.Text,
              iisCheck.Checked);
      };
      // 
      // 
      cancelButton.Location = new System.Drawing.Point(235, 198);
      cancelButton.Size = new System.Drawing.Size(75, 23);
      cancelButton.TabIndex = 3;
      cancelButton.Text = "Cancel";
      cancelButton.UseVisualStyleBackColor = true;
      cancelButton.Click += delegate(object sender, EventArgs e) {
        Application.Exit();
      };
      // 
      // 
      applyButton.Location = new System.Drawing.Point(320, 198);
      applyButton.Size = new System.Drawing.Size(75, 23);
      applyButton.TabIndex = 4;
      applyButton.Text = "Apply";
      applyButton.UseVisualStyleBackColor = true;
      applyButton.Click += delegate {
        Apply(resinDir.Text,
              apacheDirs.Text,
              apacheCheck.Checked,
              iisDir.Text,
              iisCheck.Checked);
      };
      // 
      // 
      removeButton.Location = new System.Drawing.Point(405, 198);
      removeButton.Size = new System.Drawing.Size(75, 23);
      removeButton.TabIndex = 5;
      removeButton.Text = "Remove";
      removeButton.UseVisualStyleBackColor = true;
      removeButton.Click += delegate {
        Remove(apacheDirs.Text, apacheCheck.Checked, iisDir.Text, iisCheck.Checked);
      };
      // 
      // 
      AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
      AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
      ClientSize = new System.Drawing.Size(497, 235);
      AutoSize = true;
      AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      Controls.Add(removeButton);
      Controls.Add(applyButton);
      Controls.Add(cancelButton);
      Controls.Add(okButton);
      Controls.Add(serversGrp);
      Controls.Add(generalGrp);
      Text = "Setup " + Version.VERSION;
      generalGrp.ResumeLayout(false);
      generalGrp.PerformLayout();
      serversGrp.ResumeLayout(false);
      serversGrp.PerformLayout();
      ResumeLayout(false);
    }
    
    private void Apply(String resinHome, String apacheHome, bool apache, String iisScripts, bool iis) {
      if (! CheckResinHome(resinHome))
        return;

      if (apache && ! CheckApacheHome(apacheHome))
        return;
      
      if (iis && ! CheckIIS(iisScripts))
        return;
      
      Util.SetResinInRegistry(resinHome);
      
      if (apache) {
        try {
          ConfigureInfo result = _setup.SetupApache(resinHome, apacheHome);
          if (result.Status == ConfigureInfo.SETUP_OK) {
            String serviceName = _setup.FindApacheServiceName(apacheHome);
            Console.WriteLine("ServiceName : " + serviceName);
            if (serviceName != null)
              RestartApache("Apache is successfully configured.", serviceName);
            else
              MessageBox.Show(
                "Apache is successfully configured.",
                "Configure Apache",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information,
                MessageBoxDefaultButton.Button1,
                MessageBoxOptions.ServiceNotification);
          } else if (result.Status == ConfigureInfo.SETUP_ALREADY) {
            MessageBox.Show(
              "Apache appears to have already been configred. No action was taken.",
              "Configure Apache",
              MessageBoxButtons.OK,
              MessageBoxIcon.Information,
              MessageBoxDefaultButton.Button1,
              MessageBoxOptions.ServiceNotification);
          }
        } catch (Exception e) {
          Console.WriteLine(e.StackTrace);
          MessageBox.Show(
            e.Message,
            "Configure Apache Error",
            MessageBoxButtons.OK,
            MessageBoxIcon.Warning,
            MessageBoxDefaultButton.Button1,
            MessageBoxOptions.ServiceNotification);
          
          return;
        }
      }
      
      if (iis) {
        try {
          ConfigureInfo result = _setup.SetupIIS(resinHome, iisScripts);
          if (result.Status == ConfigureInfo.ISAPI_IO_ERROR) {
            if (MessageBox.Show(String.Format(@"Setup made the required configuration changes, but was unable
to copy isapi_srun.dll due to the following error :
{0}
Would you like the setup to attempt stopping IIS Service and copying the filter?", result.Exception.Message),
                                "Configure IIS",
                                MessageBoxButtons.YesNo,
                                MessageBoxIcon.Question,
                                MessageBoxDefaultButton.Button1) == DialogResult.Yes){
              _setup.StopIIS();
              _setup.CopyIsapiFilter(resinHome, iisScripts);
              RestartIIS("Isapi filter was successfully copied.", false);
            }
            
          } else {
            RestartIIS("Resin was successfully added.", true);
          }
        } catch (Exception e) {
          MessageBox.Show(
            e.Message,
            "Configure IIS Error",
            MessageBoxButtons.OK,
            MessageBoxIcon.Warning,
            MessageBoxDefaultButton.Button1,
            MessageBoxOptions.ServiceNotification);
          
          return;
        }
      }
    }
    
    private void Remove(String apacheHome, bool apache, String iisScripts, bool iis) {
      if (apache && ! CheckApacheHome(apacheHome))
        return;
      
      if (iis && ! CheckIIS(iisScripts))
        return;
      
      if (apache) {
        try {
          ConfigureInfo result = _setup.RemoveApache(apacheHome);
          
          if (result.Status == ConfigureInfo.REMOVED_OK) {
            String serviceName = _setup.FindApacheServiceName(apacheHome);
            
            if (serviceName != null)
              RestartApache("Apache is successfully re-configured.", serviceName);
            else
              MessageBox.Show(
                "Apache is successfully configured.",
                "Configure Apache",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information,
                MessageBoxDefaultButton.Button1,
                MessageBoxOptions.ServiceNotification);
          } else if (result.Status == ConfigureInfo.REMOVED_ALREADY){
            MessageBox.Show(
              "Apache did not appear to use Resin. No action was taken.",
              "Configure Apache",
              MessageBoxButtons.OK,
              MessageBoxIcon.Information,
              MessageBoxDefaultButton.Button1,
              MessageBoxOptions.ServiceNotification);
          }
        } catch (Exception e) {
          MessageBox.Show(
            e.Message,
            "Configure Apache Error",
            MessageBoxButtons.OK,
            MessageBoxIcon.Warning,
            MessageBoxDefaultButton.Button1,
            MessageBoxOptions.ServiceNotification);
          
          return;
        }
      }
      
      if (iis) {
        try {
          ConfigureInfo result = _setup.RemoveIIS(iisScripts);
          if (result.Status == ConfigureInfo.ISAPI_IO_ERROR) {
            if (MessageBox.Show(String.Format(@"Setup made the required configuration changes, but was unable
to remove isapi_srun.dll due to the following error :
{0}
Would you like the setup to attempt stopping IIS Service and removing the filter?", result.Exception.Message),
                                "Configure IIS",
                                MessageBoxButtons.YesNo,
                                MessageBoxIcon.Question,
                                MessageBoxDefaultButton.Button1) == DialogResult.Yes){
              _setup.StopIIS();
              _setup.RemoveIsapiFilter( iisScripts);
              RestartIIS("Isapi filter was successfully removed.", false);
            }
          } else if (result.Status == ConfigureInfo.REMOVED_ALREADY){
            MessageBox.Show(
              "Resin does not appear to be configured for IIS. No action was taken.",
              "Configure IIS",
              MessageBoxButtons.OK,
              MessageBoxIcon.Warning,
              MessageBoxDefaultButton.Button1,
              MessageBoxOptions.ServiceNotification);
            return;
          } else {
            RestartIIS("Resin Was Successfully Removed.", true);
          }
        } catch (Exception e) {
          MessageBox.Show(
            e.Message,
            "Configure IIS Error",
            MessageBoxButtons.OK,
            MessageBoxIcon.Warning,
            MessageBoxDefaultButton.Button1,
            MessageBoxOptions.ServiceNotification);
          
          return;
        }
      }
    }
    
    private void OK(String resinHome, String apacheHome, bool apache, String iisHome, bool iis) {
      Apply(resinHome, apacheHome, apache, iisHome, iis);
      Application.Exit();
    }
    
    private void RestartIIS(String message, bool restart) {
      if (MessageBox.Show(message + String.Format("\nWould you like to {0} IIS", restart?"Restart":"Start"),
                          "Configure IIS Success",
                          MessageBoxButtons.YesNo,
                          MessageBoxIcon.Question) == DialogResult.No)
        return;
      
      _setup.RestartIIS();
      
      MessageBox.Show("IIS is successfully restared.",
                      "Restart IIS",
                      MessageBoxButtons.OK,
                      MessageBoxIcon.Information);

    }
    
    private void RestartApache(String message, String serviceName) {
      if (MessageBox.Show(message + String.Format("\nWould you like to Restart {0} Service", serviceName),
                          "Configure Apache Success",
                          MessageBoxButtons.YesNo,
                          MessageBoxIcon.Question) == DialogResult.No)
        return;
      
      _setup.RestartService(serviceName);
      
      MessageBox.Show(String.Format("{0} is successfully restared.", serviceName),
                      "Restart Apache",
                      MessageBoxButtons.OK,
                      MessageBoxIcon.Information);
    }
    
    public bool CheckResinHome(String home){
      if (_setup.IsValidResinHome(home))
        return true;
      
      MessageBox.Show(
        String.Format(MSG_INVALID_RESIN_HOME, home),
        "Invalid Resin Home",
        MessageBoxButtons.OK,
        MessageBoxIcon.Warning,
        MessageBoxDefaultButton.Button1,
        MessageBoxOptions.ServiceNotification);
      
      return false;
    }

    public bool CheckApacheHome(String home){
      if (_setup.IsValidApacheHome(home))
        return true;
      
      MessageBox.Show(
        String.Format(MSG_INVALID_APACHE_HOME, home),
        "Invalid Apache Home",
        MessageBoxButtons.OK,
        MessageBoxIcon.Warning,
        MessageBoxDefaultButton.Button1,
        MessageBoxOptions.ServiceNotification);
      
      return false;
    }
    
    public bool CheckIIS(String scripts){
      if (Directory.Exists(scripts))
        return true;
      
      MessageBox.Show(
        String.Format(MSG_INVALID_IIS_HOME, scripts),
        "Invalid IIS Scripts Directory",
        MessageBoxButtons.OK,
        MessageBoxIcon.Warning,
        MessageBoxDefaultButton.Button1,
        MessageBoxOptions.ServiceNotification);
      
      return false;
    }
  }
}
