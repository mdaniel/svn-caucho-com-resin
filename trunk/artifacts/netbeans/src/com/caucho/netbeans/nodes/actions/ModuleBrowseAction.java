/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 * @author Sam
 */


package com.caucho.netbeans.nodes.actions;

import com.caucho.netbeans.nodes.ResinModule;

import org.openide.ErrorManager;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Browse module action
 */
public class ModuleBrowseAction
  extends NodeAction
{

  protected void performAction(Node[] nodes)
  {
    for (Node node : nodes) {
      ResinModule module = (ResinModule) node.getCookie(ResinModule.class);
      try {
        URLDisplayer.getDefault().showURL(new URL(module.getWebURL()));
      }
      catch (MalformedURLException e) {
        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
      }
    }
  }

  protected boolean enable(Node[] nodes)
  {
    for (Node node : nodes) {
      ResinModule module = (ResinModule) node.getCookie(ResinModule.class);
      if (module == null || module.getWebURL() == null) {
        return false;
      }
    }
    return true;
  }

  public String getName()
  {
    return NbBundle.getMessage(AdminConsoleAction.class, "LBL_Browse");
  }

  protected boolean asynchronous()
  {
    return false;
  }

  public HelpCtx getHelpCtx()
  {
    return HelpCtx.DEFAULT_HELP;
  }
}
