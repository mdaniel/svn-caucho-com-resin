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
namespace Caucho
{
  partial class SetupForm
  {
    /// <summary>
    /// Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;

    /// <summary>
    /// Clean up any resources being used.
    /// </summary>
    /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
    protected override void Dispose(bool disposing)
    {
      if (disposing && (components != null))
      {
        components.Dispose();
      }
      base.Dispose(disposing);
    }

    #region Windows Form Designer generated code

    /// <summary>
    /// Required method for Designer support - do not modify
    /// the contents of this method with the code editor.
    /// </summary>
    private void InitializeComponent()
    {
      System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(SetupForm));
      this._tableLayoutPanel1 = new System.Windows.Forms.TableLayoutPanel();
      this._generalGrp = new System.Windows.Forms.GroupBox();
      this._resinCmbBox = new System.Windows.Forms.ComboBox();
      this._selectResinBtn = new System.Windows.Forms.Button();
      this._resinLogoImg = new System.Windows.Forms.PictureBox();
      this._resinLbl = new System.Windows.Forms.Label();
      this._tabControl = new System.Windows.Forms.TabControl();
      this._servicesTab = new System.Windows.Forms.TabPage();
      this._serverCmbBox = new System.Windows.Forms.ComboBox();
      this._serverLbl = new System.Windows.Forms.Label();
      this._debugPortLbl = new System.Windows.Forms.Label();
      this._debugPortTxtBox = new System.Windows.Forms.TextBox();
      this._jmxPortLbl = new System.Windows.Forms.Label();
      this._jmxPortTxtBox = new System.Windows.Forms.TextBox();
      this._selectResinConfBtn = new System.Windows.Forms.Button();
      this._resinConfTxtBox = new System.Windows.Forms.TextBox();
      this._servicePassTxtBox = new System.Windows.Forms.TextBox();
      this._serviceNameTxtBox = new System.Windows.Forms.TextBox();
      this._logDirTxtBox = new System.Windows.Forms.TextBox();
      this._resinRootTxtBox = new System.Windows.Forms.TextBox();
      this._javaHomeCmbBox = new System.Windows.Forms.ComboBox();
      this._resinConfLbl = new System.Windows.Forms.Label();
      this._previewCmbBox = new System.Windows.Forms.ComboBox();
      this._previewLbl = new System.Windows.Forms.Label();
      this._serviceUserCmbBox = new System.Windows.Forms.ComboBox();
      this._servicePassLbl = new System.Windows.Forms.Label();
      this._serviceUserLbl = new System.Windows.Forms.Label();
      this._serviceRefreshBtn = new System.Windows.Forms.Button();
      this._serviceRemoveBtn = new System.Windows.Forms.Button();
      this.serviceInstallBtn = new System.Windows.Forms.Button();
      this._serviceNameLbl = new System.Windows.Forms.Label();
      this._servicesLbl = new System.Windows.Forms.Label();
      this._servicesCmbBox = new System.Windows.Forms.ComboBox();
      this._selectLogDirBtn = new System.Windows.Forms.Button();
      this._logDirLbl = new System.Windows.Forms.Label();
      this._selectResinRootBtn = new System.Windows.Forms.Button();
      this._resinRootLbl = new System.Windows.Forms.Label();
      this._selectJavaHomeBtn = new System.Windows.Forms.Button();
      this._javaHomeLbl = new System.Windows.Forms.Label();
      this._pluginsTab = new System.Windows.Forms.TabPage();
      this._apacheDirs = new System.Windows.Forms.ComboBox();
      this._selectIISBtn = new System.Windows.Forms.Button();
      this._iisCheck = new System.Windows.Forms.CheckBox();
      this._apacheCheck = new System.Windows.Forms.CheckBox();
      this._refreshWebBtn = new System.Windows.Forms.Button();
      this._removeWebBtn = new System.Windows.Forms.Button();
      this._installWebBtn = new System.Windows.Forms.Button();
      this._iisDir = new System.Windows.Forms.TextBox();
      this._selectApacheBtn = new System.Windows.Forms.Button();
      this._folderDlg = new System.Windows.Forms.FolderBrowserDialog();
      this._fileDlg = new System.Windows.Forms.OpenFileDialog();
      this._extraParams = new System.Windows.Forms.Label();
      this._extraParamsTxbBox = new System.Windows.Forms.TextBox();
      this._watchDogPortLbl = new System.Windows.Forms.Label();
      this._watchdogPortTxtBox = new System.Windows.Forms.TextBox();
      this._tableLayoutPanel1.SuspendLayout();
      this._generalGrp.SuspendLayout();
      ((System.ComponentModel.ISupportInitialize)(this._resinLogoImg)).BeginInit();
      this._tabControl.SuspendLayout();
      this._servicesTab.SuspendLayout();
      this._pluginsTab.SuspendLayout();
      this.SuspendLayout();
      // 
      // _tableLayoutPanel1
      // 
      this._tableLayoutPanel1.AutoSize = true;
      this._tableLayoutPanel1.ColumnCount = 1;
      this._tableLayoutPanel1.ColumnStyles.Add(new System.Windows.Forms.ColumnStyle());
      this._tableLayoutPanel1.Controls.Add(this._generalGrp, 0, 0);
      this._tableLayoutPanel1.Controls.Add(this._tabControl, 0, 1);
      this._tableLayoutPanel1.Location = new System.Drawing.Point(1, 3);
      this._tableLayoutPanel1.Name = "_tableLayoutPanel1";
      this._tableLayoutPanel1.RowCount = 2;
      this._tableLayoutPanel1.RowStyles.Add(new System.Windows.Forms.RowStyle(System.Windows.Forms.SizeType.Percent, 100F));
      this._tableLayoutPanel1.RowStyles.Add(new System.Windows.Forms.RowStyle(System.Windows.Forms.SizeType.Absolute, 486F));
      this._tableLayoutPanel1.RowStyles.Add(new System.Windows.Forms.RowStyle(System.Windows.Forms.SizeType.Absolute, 20F));
      this._tableLayoutPanel1.Size = new System.Drawing.Size(472, 569);
      this._tableLayoutPanel1.TabIndex = 0;
      // 
      // _generalGrp
      // 
      this._generalGrp.AutoSize = true;
      this._generalGrp.AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      this._generalGrp.Controls.Add(this._resinCmbBox);
      this._generalGrp.Controls.Add(this._selectResinBtn);
      this._generalGrp.Controls.Add(this._resinLogoImg);
      this._generalGrp.Controls.Add(this._resinLbl);
      this._generalGrp.Dock = System.Windows.Forms.DockStyle.Fill;
      this._generalGrp.Location = new System.Drawing.Point(3, 3);
      this._generalGrp.Name = "_generalGrp";
      this._generalGrp.Size = new System.Drawing.Size(466, 77);
      this._generalGrp.TabIndex = 0;
      this._generalGrp.TabStop = false;
      this._generalGrp.Text = "General";
      // 
      // _resinCmbBox
      // 
      this._resinCmbBox.Location = new System.Drawing.Point(91, 34);
      this._resinCmbBox.Name = "_resinCmbBox";
      this._resinCmbBox.Size = new System.Drawing.Size(295, 21);
      this._resinCmbBox.TabIndex = 1;
      this._resinCmbBox.Validating += new System.ComponentModel.CancelEventHandler(this.ResinCmbBoxValidating);
      this._resinCmbBox.SelectionChangeCommitted += new System.EventHandler(this.ResinSelectectionCommitted);
      // 
      // _selectResinBtn
      // 
      this._selectResinBtn.BackColor = System.Drawing.Color.Transparent;
      this._selectResinBtn.ForeColor = System.Drawing.SystemColors.ControlText;
      this._selectResinBtn.Location = new System.Drawing.Point(395, 34);
      this._selectResinBtn.Name = "_selectResinBtn";
      this._selectResinBtn.Size = new System.Drawing.Size(61, 22);
      this._selectResinBtn.TabIndex = 2;
      this._selectResinBtn.Text = "...";
      this._selectResinBtn.UseVisualStyleBackColor = false;
      this._selectResinBtn.Click += new System.EventHandler(this.SelectResinBtnClick);
      // 
      // _resinLogoImg
      // 
      this._resinLogoImg.BackColor = System.Drawing.Color.Transparent;
      this._resinLogoImg.Image = ((System.Drawing.Image)(resources.GetObject("_resinLogoImg.Image")));
      this._resinLogoImg.Location = new System.Drawing.Point(374, -3);
      this._resinLogoImg.Name = "_resinLogoImg";
      this._resinLogoImg.Size = new System.Drawing.Size(86, 52);
      this._resinLogoImg.TabIndex = 3;
      this._resinLogoImg.TabStop = false;
      // 
      // _resinLbl
      // 
      this._resinLbl.AutoSize = true;
      this._resinLbl.Location = new System.Drawing.Point(20, 36);
      this._resinLbl.Name = "_resinLbl";
      this._resinLbl.Size = new System.Drawing.Size(65, 13);
      this._resinLbl.TabIndex = 0;
      this._resinLbl.Text = "Resin Home";
      // 
      // _tabControl
      // 
      this._tabControl.Controls.Add(this._servicesTab);
      this._tabControl.Controls.Add(this._pluginsTab);
      this._tabControl.Dock = System.Windows.Forms.DockStyle.Fill;
      this._tabControl.Location = new System.Drawing.Point(3, 86);
      this._tabControl.Name = "_tabControl";
      this._tabControl.SelectedIndex = 0;
      this._tabControl.Size = new System.Drawing.Size(466, 480);
      this._tabControl.TabIndex = 1;
      // 
      // _servicesTab
      // 
      this._servicesTab.Controls.Add(this._watchDogPortLbl);
      this._servicesTab.Controls.Add(this._watchdogPortTxtBox);
      this._servicesTab.Controls.Add(this._extraParamsTxbBox);
      this._servicesTab.Controls.Add(this._extraParams);
      this._servicesTab.Controls.Add(this._serverCmbBox);
      this._servicesTab.Controls.Add(this._serverLbl);
      this._servicesTab.Controls.Add(this._debugPortLbl);
      this._servicesTab.Controls.Add(this._debugPortTxtBox);
      this._servicesTab.Controls.Add(this._jmxPortLbl);
      this._servicesTab.Controls.Add(this._jmxPortTxtBox);
      this._servicesTab.Controls.Add(this._selectResinConfBtn);
      this._servicesTab.Controls.Add(this._resinConfTxtBox);
      this._servicesTab.Controls.Add(this._servicePassTxtBox);
      this._servicesTab.Controls.Add(this._serviceNameTxtBox);
      this._servicesTab.Controls.Add(this._logDirTxtBox);
      this._servicesTab.Controls.Add(this._resinRootTxtBox);
      this._servicesTab.Controls.Add(this._javaHomeCmbBox);
      this._servicesTab.Controls.Add(this._resinConfLbl);
      this._servicesTab.Controls.Add(this._previewCmbBox);
      this._servicesTab.Controls.Add(this._previewLbl);
      this._servicesTab.Controls.Add(this._serviceUserCmbBox);
      this._servicesTab.Controls.Add(this._servicePassLbl);
      this._servicesTab.Controls.Add(this._serviceUserLbl);
      this._servicesTab.Controls.Add(this._serviceRefreshBtn);
      this._servicesTab.Controls.Add(this._serviceRemoveBtn);
      this._servicesTab.Controls.Add(this.serviceInstallBtn);
      this._servicesTab.Controls.Add(this._serviceNameLbl);
      this._servicesTab.Controls.Add(this._servicesLbl);
      this._servicesTab.Controls.Add(this._servicesCmbBox);
      this._servicesTab.Controls.Add(this._selectLogDirBtn);
      this._servicesTab.Controls.Add(this._logDirLbl);
      this._servicesTab.Controls.Add(this._selectResinRootBtn);
      this._servicesTab.Controls.Add(this._resinRootLbl);
      this._servicesTab.Controls.Add(this._selectJavaHomeBtn);
      this._servicesTab.Controls.Add(this._javaHomeLbl);
      this._servicesTab.Location = new System.Drawing.Point(4, 22);
      this._servicesTab.Name = "_servicesTab";
      this._servicesTab.Padding = new System.Windows.Forms.Padding(3);
      this._servicesTab.Size = new System.Drawing.Size(458, 454);
      this._servicesTab.TabIndex = 0;
      this._servicesTab.Text = "Resin Windows Service Install";
      this._servicesTab.UseVisualStyleBackColor = true;
      // 
      // _serverCmbBox
      // 
      this._serverCmbBox.FormattingEnabled = true;
      this._serverCmbBox.Location = new System.Drawing.Point(101, 268);
      this._serverCmbBox.Name = "_serverCmbBox";
      this._serverCmbBox.Size = new System.Drawing.Size(281, 21);
      this._serverCmbBox.TabIndex = 40;
      this._serverCmbBox.SelectedValueChanged += new System.EventHandler(this.ServerSelectionChanged);
      // 
      // _serverLbl
      // 
      this._serverLbl.AutoSize = true;
      this._serverLbl.Location = new System.Drawing.Point(15, 274);
      this._serverLbl.Name = "_serverLbl";
      this._serverLbl.Size = new System.Drawing.Size(38, 13);
      this._serverLbl.TabIndex = 39;
      this._serverLbl.Text = "Server";
      // 
      // _debugPortLbl
      // 
      this._debugPortLbl.AutoSize = true;
      this._debugPortLbl.Location = new System.Drawing.Point(15, 354);
      this._debugPortLbl.Name = "_debugPortLbl";
      this._debugPortLbl.Size = new System.Drawing.Size(61, 13);
      this._debugPortLbl.TabIndex = 38;
      this._debugPortLbl.Text = "Debug Port";
      // 
      // _debugPortTxtBox
      // 
      this._debugPortTxtBox.Location = new System.Drawing.Point(101, 349);
      this._debugPortTxtBox.Name = "_debugPortTxtBox";
      this._debugPortTxtBox.Size = new System.Drawing.Size(281, 20);
      this._debugPortTxtBox.TabIndex = 37;
      // 
      // _jmxPortLbl
      // 
      this._jmxPortLbl.AutoSize = true;
      this._jmxPortLbl.Location = new System.Drawing.Point(16, 327);
      this._jmxPortLbl.Name = "_jmxPortLbl";
      this._jmxPortLbl.Size = new System.Drawing.Size(50, 13);
      this._jmxPortLbl.TabIndex = 36;
      this._jmxPortLbl.Text = "JMX Port";
      // 
      // _jmxPortTxtBox
      // 
      this._jmxPortTxtBox.Location = new System.Drawing.Point(101, 322);
      this._jmxPortTxtBox.Name = "_jmxPortTxtBox";
      this._jmxPortTxtBox.Size = new System.Drawing.Size(281, 20);
      this._jmxPortTxtBox.TabIndex = 35;
      // 
      // _selectResinConfBtn
      // 
      this._selectResinConfBtn.Location = new System.Drawing.Point(391, 103);
      this._selectResinConfBtn.Name = "_selectResinConfBtn";
      this._selectResinConfBtn.Size = new System.Drawing.Size(61, 22);
      this._selectResinConfBtn.TabIndex = 28;
      this._selectResinConfBtn.Text = "...";
      this._selectResinConfBtn.UseVisualStyleBackColor = true;
      this._selectResinConfBtn.Click += new System.EventHandler(this.SelectResinConf);
      // 
      // _resinConfTxtBox
      // 
      this._resinConfTxtBox.Location = new System.Drawing.Point(101, 105);
      this._resinConfTxtBox.Name = "_resinConfTxtBox";
      this._resinConfTxtBox.Size = new System.Drawing.Size(281, 20);
      this._resinConfTxtBox.TabIndex = 27;
      this._resinConfTxtBox.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this._resinConfTxtBox_KeyPress);
      this._resinConfTxtBox.Validating += new System.ComponentModel.CancelEventHandler(this.ResinConfTxtBoxValidating);
      // 
      // _servicePassTxtBox
      // 
      this._servicePassTxtBox.Location = new System.Drawing.Point(101, 213);
      this._servicePassTxtBox.Name = "_servicePassTxtBox";
      this._servicePassTxtBox.PasswordChar = '*';
      this._servicePassTxtBox.Size = new System.Drawing.Size(281, 20);
      this._servicePassTxtBox.TabIndex = 22;
      this._servicePassTxtBox.UseSystemPasswordChar = true;
      // 
      // _serviceNameTxtBox
      // 
      this._serviceNameTxtBox.Location = new System.Drawing.Point(101, 160);
      this._serviceNameTxtBox.Name = "_serviceNameTxtBox";
      this._serviceNameTxtBox.Size = new System.Drawing.Size(281, 20);
      this._serviceNameTxtBox.TabIndex = 15;
      // 
      // _logDirTxtBox
      // 
      this._logDirTxtBox.Location = new System.Drawing.Point(101, 134);
      this._logDirTxtBox.Name = "_logDirTxtBox";
      this._logDirTxtBox.Size = new System.Drawing.Size(281, 20);
      this._logDirTxtBox.TabIndex = 10;
      this._logDirTxtBox.Validating += new System.ComponentModel.CancelEventHandler(this.LogDirTxtBoxValidating);
      // 
      // _resinRootTxtBox
      // 
      this._resinRootTxtBox.Location = new System.Drawing.Point(101, 77);
      this._resinRootTxtBox.Name = "_resinRootTxtBox";
      this._resinRootTxtBox.Size = new System.Drawing.Size(281, 20);
      this._resinRootTxtBox.TabIndex = 7;
      this._resinRootTxtBox.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.ResinRootTxtBoxKeyPress);
      this._resinRootTxtBox.Validating += new System.ComponentModel.CancelEventHandler(this.ResinRootTxtBoxValidating);
      // 
      // _javaHomeCmbBox
      // 
      this._javaHomeCmbBox.Location = new System.Drawing.Point(101, 49);
      this._javaHomeCmbBox.Name = "_javaHomeCmbBox";
      this._javaHomeCmbBox.Size = new System.Drawing.Size(281, 21);
      this._javaHomeCmbBox.TabIndex = 4;
      this._javaHomeCmbBox.Validating += new System.ComponentModel.CancelEventHandler(this.JavaHomeCmbBoxValidating);
      this._javaHomeCmbBox.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.JavaHomeCmbBoxKeyPress);
      // 
      // _resinConfLbl
      // 
      this._resinConfLbl.AutoSize = true;
      this._resinConfLbl.Location = new System.Drawing.Point(16, 108);
      this._resinConfLbl.Name = "_resinConfLbl";
      this._resinConfLbl.Size = new System.Drawing.Size(59, 13);
      this._resinConfLbl.TabIndex = 26;
      this._resinConfLbl.Text = "Resin Conf";
      // 
      // _previewCmbBox
      // 
      this._previewCmbBox.FormattingEnabled = true;
      this._previewCmbBox.Items.AddRange(new object[] {
            "No",
            "Yes"});
      this._previewCmbBox.Location = new System.Drawing.Point(101, 241);
      this._previewCmbBox.Name = "_previewCmbBox";
      this._previewCmbBox.Size = new System.Drawing.Size(281, 21);
      this._previewCmbBox.TabIndex = 25;
      this._previewCmbBox.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.PreviewCmbBoxKeyPress);
      this._previewCmbBox.KeyDown += new System.Windows.Forms.KeyEventHandler(this._previewCmbBox_KeyDown);
      // 
      // _previewLbl
      // 
      this._previewLbl.AutoSize = true;
      this._previewLbl.Location = new System.Drawing.Point(16, 245);
      this._previewLbl.Name = "_previewLbl";
      this._previewLbl.Size = new System.Drawing.Size(45, 13);
      this._previewLbl.TabIndex = 24;
      this._previewLbl.Text = "Preview";
      // 
      // _serviceUserCmbBox
      // 
      this._serviceUserCmbBox.FormattingEnabled = true;
      this._serviceUserCmbBox.Location = new System.Drawing.Point(101, 186);
      this._serviceUserCmbBox.Name = "_serviceUserCmbBox";
      this._serviceUserCmbBox.Size = new System.Drawing.Size(281, 21);
      this._serviceUserCmbBox.TabIndex = 23;
      // 
      // _servicePassLbl
      // 
      this._servicePassLbl.AutoSize = true;
      this._servicePassLbl.Location = new System.Drawing.Point(16, 216);
      this._servicePassLbl.Name = "_servicePassLbl";
      this._servicePassLbl.Size = new System.Drawing.Size(53, 13);
      this._servicePassLbl.TabIndex = 21;
      this._servicePassLbl.Text = "Password";
      // 
      // _serviceUserLbl
      // 
      this._serviceUserLbl.AutoSize = true;
      this._serviceUserLbl.Location = new System.Drawing.Point(16, 190);
      this._serviceUserLbl.Name = "_serviceUserLbl";
      this._serviceUserLbl.Size = new System.Drawing.Size(68, 13);
      this._serviceUserLbl.TabIndex = 19;
      this._serviceUserLbl.Text = "Service User";
      // 
      // _serviceRefreshBtn
      // 
      this._serviceRefreshBtn.Location = new System.Drawing.Point(391, 422);
      this._serviceRefreshBtn.Name = "_serviceRefreshBtn";
      this._serviceRefreshBtn.Size = new System.Drawing.Size(61, 22);
      this._serviceRefreshBtn.TabIndex = 18;
      this._serviceRefreshBtn.Text = "Refresh";
      this._serviceRefreshBtn.UseVisualStyleBackColor = true;
      this._serviceRefreshBtn.Click += new System.EventHandler(this.ServiceRefreshBtnClick);
      // 
      // _serviceRemoveBtn
      // 
      this._serviceRemoveBtn.Location = new System.Drawing.Point(293, 422);
      this._serviceRemoveBtn.Name = "_serviceRemoveBtn";
      this._serviceRemoveBtn.Size = new System.Drawing.Size(93, 22);
      this._serviceRemoveBtn.TabIndex = 17;
      this._serviceRemoveBtn.Text = "Remove";
      this._serviceRemoveBtn.UseVisualStyleBackColor = true;
      // 
      // serviceInstallBtn
      // 
      this.serviceInstallBtn.Location = new System.Drawing.Point(195, 422);
      this.serviceInstallBtn.Name = "serviceInstallBtn";
      this.serviceInstallBtn.Size = new System.Drawing.Size(93, 22);
      this.serviceInstallBtn.TabIndex = 16;
      this.serviceInstallBtn.Text = "Install/Change";
      this.serviceInstallBtn.UseVisualStyleBackColor = true;
      this.serviceInstallBtn.Click += new System.EventHandler(this.ServiceInstallBtnClick);
      // 
      // _serviceNameLbl
      // 
      this._serviceNameLbl.AutoSize = true;
      this._serviceNameLbl.Location = new System.Drawing.Point(16, 163);
      this._serviceNameLbl.Name = "_serviceNameLbl";
      this._serviceNameLbl.Size = new System.Drawing.Size(74, 13);
      this._serviceNameLbl.TabIndex = 14;
      this._serviceNameLbl.Text = "Service Name";
      // 
      // _servicesLbl
      // 
      this._servicesLbl.AutoSize = true;
      this._servicesLbl.Location = new System.Drawing.Point(15, 10);
      this._servicesLbl.Name = "_servicesLbl";
      this._servicesLbl.Size = new System.Drawing.Size(48, 13);
      this._servicesLbl.TabIndex = 13;
      this._servicesLbl.Text = "Services";
      // 
      // _servicesCmbBox
      // 
      this._servicesCmbBox.FormattingEnabled = true;
      this._servicesCmbBox.Location = new System.Drawing.Point(101, 10);
      this._servicesCmbBox.Name = "_servicesCmbBox";
      this._servicesCmbBox.Size = new System.Drawing.Size(281, 21);
      this._servicesCmbBox.TabIndex = 12;
      this._servicesCmbBox.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.ServicesCmbBoxKeyPress);
      this._servicesCmbBox.SelectedValueChanged += new System.EventHandler(this.ServiceSelectionChanged);
      // 
      // _selectLogDirBtn
      // 
      this._selectLogDirBtn.Location = new System.Drawing.Point(391, 132);
      this._selectLogDirBtn.Name = "_selectLogDirBtn";
      this._selectLogDirBtn.Size = new System.Drawing.Size(61, 22);
      this._selectLogDirBtn.TabIndex = 11;
      this._selectLogDirBtn.Text = "...";
      this._selectLogDirBtn.UseVisualStyleBackColor = true;
      this._selectLogDirBtn.Click += new System.EventHandler(this.SelectLogDirectory);
      // 
      // _logDirLbl
      // 
      this._logDirLbl.AutoSize = true;
      this._logDirLbl.Location = new System.Drawing.Point(16, 137);
      this._logDirLbl.Name = "_logDirLbl";
      this._logDirLbl.Size = new System.Drawing.Size(70, 13);
      this._logDirLbl.TabIndex = 9;
      this._logDirLbl.Text = "Log Directory";
      // 
      // _selectResinRootBtn
      // 
      this._selectResinRootBtn.Location = new System.Drawing.Point(391, 75);
      this._selectResinRootBtn.Name = "_selectResinRootBtn";
      this._selectResinRootBtn.Size = new System.Drawing.Size(61, 22);
      this._selectResinRootBtn.TabIndex = 8;
      this._selectResinRootBtn.Text = "...";
      this._selectResinRootBtn.UseVisualStyleBackColor = true;
      this._selectResinRootBtn.Click += new System.EventHandler(this.SelectResinRoot);
      // 
      // _resinRootLbl
      // 
      this._resinRootLbl.AutoSize = true;
      this._resinRootLbl.Location = new System.Drawing.Point(16, 80);
      this._resinRootLbl.Name = "_resinRootLbl";
      this._resinRootLbl.Size = new System.Drawing.Size(60, 13);
      this._resinRootLbl.TabIndex = 6;
      this._resinRootLbl.Text = "Resin Root";
      // 
      // _selectJavaHomeBtn
      // 
      this._selectJavaHomeBtn.Location = new System.Drawing.Point(391, 49);
      this._selectJavaHomeBtn.Name = "_selectJavaHomeBtn";
      this._selectJavaHomeBtn.Size = new System.Drawing.Size(61, 22);
      this._selectJavaHomeBtn.TabIndex = 5;
      this._selectJavaHomeBtn.Text = "...";
      this._selectJavaHomeBtn.UseVisualStyleBackColor = true;
      this._selectJavaHomeBtn.Click += new System.EventHandler(this.SelectJavaHome);
      // 
      // _javaHomeLbl
      // 
      this._javaHomeLbl.AutoSize = true;
      this._javaHomeLbl.Location = new System.Drawing.Point(16, 54);
      this._javaHomeLbl.Name = "_javaHomeLbl";
      this._javaHomeLbl.Size = new System.Drawing.Size(61, 13);
      this._javaHomeLbl.TabIndex = 3;
      this._javaHomeLbl.Text = "Java Home";
      // 
      // _pluginsTab
      // 
      this._pluginsTab.Controls.Add(this._apacheDirs);
      this._pluginsTab.Controls.Add(this._selectIISBtn);
      this._pluginsTab.Controls.Add(this._iisCheck);
      this._pluginsTab.Controls.Add(this._apacheCheck);
      this._pluginsTab.Controls.Add(this._refreshWebBtn);
      this._pluginsTab.Controls.Add(this._removeWebBtn);
      this._pluginsTab.Controls.Add(this._installWebBtn);
      this._pluginsTab.Controls.Add(this._iisDir);
      this._pluginsTab.Controls.Add(this._selectApacheBtn);
      this._pluginsTab.Location = new System.Drawing.Point(4, 22);
      this._pluginsTab.Name = "_pluginsTab";
      this._pluginsTab.Padding = new System.Windows.Forms.Padding(3);
      this._pluginsTab.Size = new System.Drawing.Size(458, 431);
      this._pluginsTab.TabIndex = 1;
      this._pluginsTab.Text = "Web Server Plugins";
      this._pluginsTab.UseVisualStyleBackColor = true;
      // 
      // _apacheDirs
      // 
      this._apacheDirs.FormattingEnabled = true;
      this._apacheDirs.Location = new System.Drawing.Point(95, 17);
      this._apacheDirs.Name = "_apacheDirs";
      this._apacheDirs.Size = new System.Drawing.Size(287, 21);
      this._apacheDirs.TabIndex = 30;
      this._apacheDirs.Text = "Select Apache Server ...";
      // 
      // _selectIISBtn
      // 
      this._selectIISBtn.Location = new System.Drawing.Point(391, 46);
      this._selectIISBtn.Name = "_selectIISBtn";
      this._selectIISBtn.Size = new System.Drawing.Size(64, 22);
      this._selectIISBtn.TabIndex = 29;
      this._selectIISBtn.Text = "...";
      this._selectIISBtn.UseVisualStyleBackColor = true;
      // 
      // _iisCheck
      // 
      this._iisCheck.AutoSize = true;
      this._iisCheck.Location = new System.Drawing.Point(18, 46);
      this._iisCheck.Name = "_iisCheck";
      this._iisCheck.Size = new System.Drawing.Size(39, 17);
      this._iisCheck.TabIndex = 28;
      this._iisCheck.Text = "IIS";
      this._iisCheck.UseVisualStyleBackColor = true;
      // 
      // _apacheCheck
      // 
      this._apacheCheck.AutoSize = true;
      this._apacheCheck.Location = new System.Drawing.Point(18, 20);
      this._apacheCheck.Name = "_apacheCheck";
      this._apacheCheck.Size = new System.Drawing.Size(63, 17);
      this._apacheCheck.TabIndex = 27;
      this._apacheCheck.Text = "Apache";
      this._apacheCheck.UseVisualStyleBackColor = true;
      // 
      // _refreshWebBtn
      // 
      this._refreshWebBtn.Location = new System.Drawing.Point(394, 394);
      this._refreshWebBtn.Name = "_refreshWebBtn";
      this._refreshWebBtn.Size = new System.Drawing.Size(61, 22);
      this._refreshWebBtn.TabIndex = 26;
      this._refreshWebBtn.Text = "Refresh";
      this._refreshWebBtn.UseVisualStyleBackColor = true;
      // 
      // _removeWebBtn
      // 
      this._removeWebBtn.Location = new System.Drawing.Point(296, 394);
      this._removeWebBtn.Name = "_removeWebBtn";
      this._removeWebBtn.Size = new System.Drawing.Size(93, 22);
      this._removeWebBtn.TabIndex = 25;
      this._removeWebBtn.Text = "Remove";
      this._removeWebBtn.UseVisualStyleBackColor = true;
      // 
      // _installWebBtn
      // 
      this._installWebBtn.Location = new System.Drawing.Point(197, 394);
      this._installWebBtn.Name = "_installWebBtn";
      this._installWebBtn.Size = new System.Drawing.Size(93, 22);
      this._installWebBtn.TabIndex = 24;
      this._installWebBtn.Text = "Install";
      this._installWebBtn.UseVisualStyleBackColor = true;
      // 
      // _iisDir
      // 
      this._iisDir.Location = new System.Drawing.Point(92, 46);
      this._iisDir.Name = "_iisDir";
      this._iisDir.Size = new System.Drawing.Size(290, 20);
      this._iisDir.TabIndex = 23;
      // 
      // _selectApacheBtn
      // 
      this._selectApacheBtn.Location = new System.Drawing.Point(391, 20);
      this._selectApacheBtn.Name = "_selectApacheBtn";
      this._selectApacheBtn.Size = new System.Drawing.Size(64, 22);
      this._selectApacheBtn.TabIndex = 21;
      this._selectApacheBtn.Text = "...";
      this._selectApacheBtn.UseVisualStyleBackColor = true;
      // 
      // _folderDlg
      // 
      this._folderDlg.Description = "Please locate your Resin installation";
      this._folderDlg.RootFolder = System.Environment.SpecialFolder.MyComputer;
      this._folderDlg.ShowNewFolderButton = false;
      // 
      // _fileDlg
      // 
      this._fileDlg.FileName = "openFileDialog1";
      // 
      // _extraParams
      // 
      this._extraParams.AutoSize = true;
      this._extraParams.Location = new System.Drawing.Point(15, 379);
      this._extraParams.Name = "_extraParams";
      this._extraParams.Size = new System.Drawing.Size(69, 13);
      this._extraParams.TabIndex = 41;
      this._extraParams.Text = "Extra Params";
      // 
      // _extraParamsTxbBox
      // 
      this._extraParamsTxbBox.Location = new System.Drawing.Point(101, 376);
      this._extraParamsTxbBox.Name = "_extraParamsTxbBox";
      this._extraParamsTxbBox.Size = new System.Drawing.Size(281, 20);
      this._extraParamsTxbBox.TabIndex = 42;
      // 
      // _watchDogPortLbl
      // 
      this._watchDogPortLbl.AutoSize = true;
      this._watchDogPortLbl.Location = new System.Drawing.Point(15, 300);
      this._watchDogPortLbl.Name = "_watchDogPortLbl";
      this._watchDogPortLbl.Size = new System.Drawing.Size(81, 13);
      this._watchDogPortLbl.TabIndex = 44;
      this._watchDogPortLbl.Text = "WatchDog Port";
      this._watchDogPortLbl.Click += new System.EventHandler(this.label1_Click);
      // 
      // _watchdogPortTxtBox
      // 
      this._watchdogPortTxtBox.Location = new System.Drawing.Point(101, 295);
      this._watchdogPortTxtBox.Name = "_watchdogPortTxtBox";
      this._watchdogPortTxtBox.Size = new System.Drawing.Size(281, 20);
      this._watchdogPortTxtBox.TabIndex = 43;
      this._watchdogPortTxtBox.TextChanged += new System.EventHandler(this.textBox1_TextChanged);
      // 
      // SetupForm
      // 
      this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
      this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
      this.AutoSize = true;
      this.AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      this.ClientSize = new System.Drawing.Size(722, 717);
      this.Controls.Add(this._tableLayoutPanel1);
      this.ForeColor = System.Drawing.SystemColors.ControlText;
      this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
      this.Name = "SetupForm";
      this.Text = "Resin Setup";
      this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.SetupFormClosing);
      this._tableLayoutPanel1.ResumeLayout(false);
      this._tableLayoutPanel1.PerformLayout();
      this._generalGrp.ResumeLayout(false);
      this._generalGrp.PerformLayout();
      ((System.ComponentModel.ISupportInitialize)(this._resinLogoImg)).EndInit();
      this._tabControl.ResumeLayout(false);
      this._servicesTab.ResumeLayout(false);
      this._servicesTab.PerformLayout();
      this._pluginsTab.ResumeLayout(false);
      this._pluginsTab.PerformLayout();
      this.ResumeLayout(false);
      this.PerformLayout();

    }

    #endregion

    private System.Windows.Forms.TableLayoutPanel _tableLayoutPanel1;
    private System.Windows.Forms.TabControl _tabControl;
    private System.Windows.Forms.TabPage _servicesTab;
    private System.Windows.Forms.Button _selectResinConfBtn;
    private System.Windows.Forms.TextBox _resinConfTxtBox;
    private System.Windows.Forms.TextBox _servicePassTxtBox;
    private System.Windows.Forms.TextBox _serviceNameTxtBox;
    private System.Windows.Forms.TextBox _logDirTxtBox;
    private System.Windows.Forms.TextBox _resinRootTxtBox;
    private System.Windows.Forms.ComboBox _javaHomeCmbBox;
    private System.Windows.Forms.Label _resinConfLbl;
    private System.Windows.Forms.ComboBox _previewCmbBox;
    private System.Windows.Forms.Label _previewLbl;
    private System.Windows.Forms.ComboBox _serviceUserCmbBox;
    private System.Windows.Forms.Label _servicePassLbl;
    private System.Windows.Forms.Label _serviceUserLbl;
    private System.Windows.Forms.Button _serviceRefreshBtn;
    private System.Windows.Forms.Button _serviceRemoveBtn;
    private System.Windows.Forms.Button serviceInstallBtn;
    private System.Windows.Forms.Label _serviceNameLbl;
    private System.Windows.Forms.Label _servicesLbl;
    private System.Windows.Forms.ComboBox _servicesCmbBox;
    private System.Windows.Forms.Button _selectLogDirBtn;
    private System.Windows.Forms.Label _logDirLbl;
    private System.Windows.Forms.Button _selectResinRootBtn;
    private System.Windows.Forms.Label _resinRootLbl;
    private System.Windows.Forms.Button _selectJavaHomeBtn;
    private System.Windows.Forms.Label _javaHomeLbl;
    private System.Windows.Forms.TabPage _pluginsTab;
    private System.Windows.Forms.Button _selectIISBtn;
    private System.Windows.Forms.CheckBox _iisCheck;
    private System.Windows.Forms.CheckBox _apacheCheck;
    private System.Windows.Forms.Button _refreshWebBtn;
    private System.Windows.Forms.Button _removeWebBtn;
    private System.Windows.Forms.Button _installWebBtn;
    private System.Windows.Forms.TextBox _iisDir;
    private System.Windows.Forms.Button _selectApacheBtn;
    private System.Windows.Forms.GroupBox _generalGrp;
    private System.Windows.Forms.Button _selectResinBtn;
    private System.Windows.Forms.PictureBox _resinLogoImg;
    private System.Windows.Forms.ComboBox _resinCmbBox;
    private System.Windows.Forms.Label _resinLbl;
    private System.Windows.Forms.Label _debugPortLbl;
    private System.Windows.Forms.TextBox _debugPortTxtBox;
    private System.Windows.Forms.Label _jmxPortLbl;
    private System.Windows.Forms.TextBox _jmxPortTxtBox;
    private System.Windows.Forms.ComboBox _serverCmbBox;
    private System.Windows.Forms.Label _serverLbl;
    private System.Windows.Forms.FolderBrowserDialog _folderDlg;
    private System.Windows.Forms.OpenFileDialog _fileDlg;
    private System.Windows.Forms.ComboBox _apacheDirs;
    private System.Windows.Forms.Label _extraParams;
    private System.Windows.Forms.TextBox _extraParamsTxbBox;
    private System.Windows.Forms.Label _watchDogPortLbl;
    private System.Windows.Forms.TextBox _watchdogPortTxtBox;
  }
}