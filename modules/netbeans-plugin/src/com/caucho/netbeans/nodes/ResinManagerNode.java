/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.netbeans.nodes;

import com.caucho.netbeans.core.ResinDeploymentManager;
import com.caucho.netbeans.customizer.Customizer;
import com.caucho.netbeans.nodes.actions.AdminConsoleAction;
import com.caucho.netbeans.nodes.actions.ServerLogAction;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;

import java.awt.*;

/**
 * The main server node representing the server instance.
 */
public class ResinManagerNode
  extends AbstractNode
  implements Node.Cookie
{

  private ResinDeploymentManager manager;

  public ResinManagerNode(Lookup lookup)
  {
    super(new Children.Array());
    manager
      = (ResinDeploymentManager) lookup.lookup(ResinDeploymentManager.class);
    getCookieSet().add(this);
    setIconBaseWithExtension("com/caucho/netbeans/resources/resin.png"); // NOI18N
  }

  public String getShortDescription()
  {
    return manager.getDisplayName();
  }

  public javax.swing.Action[] getActions(boolean context)
  {
    return new javax.swing.Action[]{null,
                                    SystemAction.get(AdminConsoleAction.class),
                                    SystemAction.get(ServerLogAction.class),};
  }

  public Component getCustomizer()
  {
    return new Customizer(manager);
  }

  public boolean hasCustomizer()
  {
    return true;
  }

  public ResinDeploymentManager getManger()
  {
    return manager;
  }
}
