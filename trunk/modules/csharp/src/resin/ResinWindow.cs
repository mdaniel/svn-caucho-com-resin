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
using System.Windows.Forms;
using System.Drawing;
using System.ComponentModel;

namespace Caucho
{
  public class ResinWindow : Form
  {
    
    private Resin _resin;
    
    public ResinWindow(Resin resin, String title)
    {
      _resin = resin;
      
      Console.CancelKeyPress += new ConsoleCancelEventHandler(quit);
      
      GroupBox groupBox = new System.Windows.Forms.GroupBox();
      groupBox.SuspendLayout();
      this.SuspendLayout();
      // 
      // groupBox
      // 
      groupBox.Location = new System.Drawing.Point(5, 12);
      groupBox.Size = new System.Drawing.Size(124, 111);
      groupBox.TabIndex = 4;
      groupBox.TabStop = false;
      groupBox.Text = title;
      // 
      // start RadioButton
      //      
      RadioButton radioButton = new RadioButton();
      radioButton.Anchor = System.Windows.Forms.AnchorStyles.Left;
      radioButton.AutoSize = true;
      radioButton.Location = new System.Drawing.Point(27, 25);
      radioButton.Margin = new System.Windows.Forms.Padding(9);
      radioButton.Size = new System.Drawing.Size(47, 17);
      radioButton.TabIndex = 0;
      radioButton.TabStop = true;
      radioButton.Text = "St&art";
      radioButton.UseVisualStyleBackColor = true;
      radioButton.Checked = true;
      groupBox.Controls.Add(radioButton);
      radioButton.CheckedChanged += new EventHandler(processAction);
      // 
      // stop RadioButton
      // 
      radioButton = new RadioButton();
      radioButton.Anchor = System.Windows.Forms.AnchorStyles.Left;
      radioButton.AutoSize = true;
      radioButton.Location = new System.Drawing.Point(27, 51);
      radioButton.Margin = new System.Windows.Forms.Padding(9);
      radioButton.Size = new System.Drawing.Size(47, 17);
      radioButton.TabIndex = 1;
      radioButton.TabStop = true;
      radioButton.Text = "St&op";
      radioButton.UseVisualStyleBackColor = true;
      groupBox.Controls.Add(radioButton);
      // 
      // Quit Button
      // 
      Button button = new System.Windows.Forms.Button();
      button.Anchor = System.Windows.Forms.AnchorStyles.Right;
      button.Location = new System.Drawing.Point(19, 76);
      button.Margin = new System.Windows.Forms.Padding(9);
      button.Size = new System.Drawing.Size(93, 23);
      button.TabIndex = 2;
      button.Text = "&Quit";
      button.UseVisualStyleBackColor = true;
      button.Click += new EventHandler(quit);
      groupBox.Controls.Add(button);
      // 
      // Form1
      // 
      this.Closing += new CancelEventHandler(quit);
      this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
      this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
      this.AutoSize = true;
      this.AutoSizeMode = System.Windows.Forms.AutoSizeMode.GrowAndShrink;
      this.ClientSize = new System.Drawing.Size(135, 130);
      this.Controls.Add(groupBox);
      this.Text = Version.VERSION;
      groupBox.ResumeLayout(false);
      groupBox.PerformLayout();
      this.ResumeLayout(false);
    }    

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