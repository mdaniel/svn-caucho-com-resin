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

    public SetupForm(Setup setup)
    {
      _setup = setup;

      InitializeComponent();
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
  }
}
