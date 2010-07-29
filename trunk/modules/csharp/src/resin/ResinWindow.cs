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
using System.Windows.Forms;
using System.Drawing;
using System.ComponentModel;

namespace Caucho
{
  public class ResinWindow : Form
  {
    
    private Resin _resin;
    private GroupBox groupBox;
    private RadioButton startRadioBtn;
    private RadioButton stopRadioBtn;
    private Button button;
    private String _title = "Resin";
    
    public ResinWindow(Resin resin, String title)
    {
      _resin = resin;
      _title = title;
      InitializeComponent();
    }

    #region Windows Form Designer generated code
    private void InitializeComponent()
    {
      this.groupBox = new System.Windows.Forms.GroupBox();
      this.startRadioBtn = new System.Windows.Forms.RadioButton();
      this.stopRadioBtn = new System.Windows.Forms.RadioButton();
      this.button = new System.Windows.Forms.Button();
      this.groupBox.SuspendLayout();
      this.SuspendLayout();
      // 
      // groupBox
      // 
      this.groupBox.Controls.Add(this.startRadioBtn);
      this.groupBox.Controls.Add(this.stopRadioBtn);
      this.groupBox.Controls.Add(this.button);
      this.groupBox.Location = new System.Drawing.Point(5, 12);
      this.groupBox.Name = "groupBox";
      this.groupBox.Size = new System.Drawing.Size(143, 128);
      this.groupBox.TabIndex = 4;
      this.groupBox.TabStop = false;
      this.groupBox.Text = "Resin";
      // 
      // startRadioBtn
      // 
      this.startRadioBtn.Anchor = System.Windows.Forms.AnchorStyles.Left;
      this.startRadioBtn.AutoSize = true;
      this.startRadioBtn.Checked = true;
      this.startRadioBtn.Location = new System.Drawing.Point(27, 33);
      this.startRadioBtn.Margin = new System.Windows.Forms.Padding(9);
      this.startRadioBtn.Name = "startRadioBtn";
      this.startRadioBtn.Size = new System.Drawing.Size(47, 17);
      this.startRadioBtn.TabIndex = 0;
      this.startRadioBtn.TabStop = true;
      this.startRadioBtn.Text = "St&art";
      this.startRadioBtn.UseVisualStyleBackColor = true;
      this.startRadioBtn.CheckedChanged += new System.EventHandler(this.processAction);
      // 
      // stopRadioBtn
      // 
      this.stopRadioBtn.Anchor = System.Windows.Forms.AnchorStyles.Left;
      this.stopRadioBtn.AutoSize = true;
      this.stopRadioBtn.Location = new System.Drawing.Point(27, 59);
      this.stopRadioBtn.Margin = new System.Windows.Forms.Padding(9);
      this.stopRadioBtn.Name = "stopRadioBtn";
      this.stopRadioBtn.Size = new System.Drawing.Size(47, 17);
      this.stopRadioBtn.TabIndex = 1;
      this.stopRadioBtn.TabStop = true;
      this.stopRadioBtn.Text = "St&op";
      this.stopRadioBtn.UseVisualStyleBackColor = true;
      // 
      // button
      // 
      this.button.Anchor = System.Windows.Forms.AnchorStyles.Right;
      this.button.Location = new System.Drawing.Point(27, 94);
      this.button.Margin = new System.Windows.Forms.Padding(9);
      this.button.Name = "button";
      this.button.Size = new System.Drawing.Size(93, 23);
      this.button.TabIndex = 2;
      this.button.Text = "&Quit";
      this.button.UseVisualStyleBackColor = true;
      this.button.Click += new System.EventHandler(this.quit);
      // 
      // ResinWindow
      // 
      this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
      this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
      this.AutoSize = true;
      this.AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      this.ClientSize = new System.Drawing.Size(206, 182);
      this.Controls.Add(this.groupBox);
      this.Name = "ResinWindow";
      this.Text = "Resin/4.0.s100209";
      this.Closing += new System.ComponentModel.CancelEventHandler(this.quit);
      this.groupBox.ResumeLayout(false);
      this.groupBox.PerformLayout();
      this.ResumeLayout(false);

    }
    #endregion

    void processAction(object sender, EventArgs e)
    {
      RadioButton button = (RadioButton) sender;
      
      if (button.Checked)
        _resin.StartResin();
      else
        _resin.StopResin();
    }
    
    void quit(Object sender,  ConsoleCancelEventArgs args){
      quit_();
    }
    
    void quit(Object sender, EventArgs evt) {
      quit_();
    }
    
    void quit(Object sender, CancelEventArgs evt) {
      quit_();
    }
    
    void quit_(){
      try {
        _resin.StopResin();
      } catch(Exception e) {
        _resin.Error(e.Message, e);
      }
      
      Application.Exit();
    }
  }
}