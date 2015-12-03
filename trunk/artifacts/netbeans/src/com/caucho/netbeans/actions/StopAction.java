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
 * @author Alex Rojkov
 */

package com.caucho.netbeans.actions;

import com.caucho.netbeans.ResinNode;
import java.util.Arrays;
import org.netbeans.api.server.CommonServerUIs;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

public class StopAction extends NodeAction {

  private static StopAction instance;

  public static StopAction getInstance() {
    if (instance == null) {
      instance = new StopAction();
    }

    return instance;
  }

  public StopAction() {
  }

  @Override
  protected void performAction(Node[] nodes) {

    ResinNode resin = (ResinNode) nodes[0];

    resin.getResinServerInstance().remove();
  }

  @Override
  protected boolean enable(Node[] nodes) {
    return true;
  }

  @Override
  protected boolean asynchronous() {
    return false;
  }

  @Override
  public String getName() {
    return NbBundle.getMessage(ViewServerLogAction.class, "stop-resin");
  }

  @Override
  protected String iconResource() {
    return null;
  }

  @Override
  public HelpCtx getHelpCtx() {
    return HelpCtx.DEFAULT_HELP;
  }
}
