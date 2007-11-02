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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package org.netbeans.modules.j2ee.resin.nodes;

import java.awt.Component;
import java.awt.Label;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class ResinInstanceNode extends AbstractNode implements Node.Cookie
{
  private static final Logger log = Logger.getLogger(ResinInstanceNode.class.getName());

  
  private static String ICON_BASE = "org/netbeans/modules/j2ee/resin/resources/server.gif"; // NOI18N
  
  public ResinInstanceNode(Lookup lookup)
  {
    super(new Children.Array());
    getCookieSet().add(this);
    setIconBaseWithExtension(ICON_BASE);
  }
  
  public String getDisplayName()
  {
    return NbBundle.getMessage(ResinInstanceNode.class, "TXT_ResinInstanceNode");
  }
  
  public String getShortDescription()
  {
    return "http://localhost:8080"; // NOI18N
  }
  
  public javax.swing.Action[] getActions(boolean context)
  {
    return new javax.swing.Action[]{};
  }
  
  public boolean hasCustomizer()
  {
    return true;
  }
  
  public Component getCustomizer()
  {
    JPanel panel = new JPanel();
    panel.add(new Label("< Put your customizer implementation here! >")); // NOI18N
    return panel;
  }
}