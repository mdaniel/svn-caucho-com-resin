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
package com.caucho.netbeans;

import com.caucho.netbeans.actions.DeleteAction;
import com.caucho.netbeans.actions.PropertiesAction;
import java.awt.Component;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import javax.swing.Action;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;
import org.openide.util.datatransfer.PasteType;

public class ResinNode extends Node {

  private ResinInstance _resin;

  public ResinNode(Children h, ResinInstance resin) throws IllegalStateException {
    super(h);
    _resin = resin;
    setName(resin.getName());
    setDisplayName(resin.getDisplayName());
  }

  public ResinNode(Children h, Lookup lookup, ResinInstance resin) throws IllegalStateException {
    super(h, lookup);
    _resin = resin;
    setName(resin.getName());
    setDisplayName(resin.getDisplayName());
  }

  public ResinInstance getResinServerInstance() {
    return _resin;
  }

  @Override
  public Node cloneNode() {
    ResinNode clone = new ResinNode(this.getChildren(), getLookup(), _resin);

    return clone;
  }

  @Override
  public Image getIcon(int i) {
    return null;
  }

  @Override
  public Image getOpenedIcon(int i) {
    return null;
  }

  @Override
  public HelpCtx getHelpCtx() {
    return HelpCtx.DEFAULT_HELP;
  }

  @Override
  public boolean canRename() {
    return false;
  }

  @Override
  public boolean canDestroy() {
    return false;
  }

  @Override
  public Action[] getActions(boolean context) {
    return new Action[]{
              //SystemAction.get(StartAction.class),
              //SystemAction.get(StopAction.class),
              //SystemAction.get(RestartAction.class),
              null,
              SystemAction.get(DeleteAction.class),
              null,
              SystemAction.get(PropertiesAction.class)
            };
  }

  @Override
  public PropertySet[] getPropertySets() {
    return new PropertySet[]{};
  }

  @Override
  public Transferable clipboardCopy() throws IOException {
    return null;
  }

  @Override
  public Transferable clipboardCut() throws IOException {
    return null;
  }

  @Override
  public Transferable drag() throws IOException {
    return null;
  }

  @Override
  public boolean canCopy() {
    return false;
  }

  @Override
  public boolean canCut() {
    return false;
  }

  @Override
  public PasteType[] getPasteTypes(Transferable t) {
    return new PasteType[]{};
  }

  @Override
  public PasteType getDropType(Transferable t, int i, int i1) {
    return null;
  }

  @Override
  public NewType[] getNewTypes() {
    return new NewType[]{};
  }

  @Override
  public boolean hasCustomizer() {
    return false;
  }

  @Override
  public Component getCustomizer() {
    return null;
  }

  @Override
  public Handle getHandle() {
    return null;
  }
}
