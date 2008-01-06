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


package com.caucho.netbeans.core;

import java.awt.Component;
import java.io.IOException;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.InstantiatingIterator;
import org.openide.WizardDescriptor.Panel;
import org.openide.util.HelpCtx;

public final class AddInstanceIterator implements InstantiatingIterator
{
  private Panel _panel;

    public Set instantiate() throws IOException {
      return null;
    }

    public void initialize(WizardDescriptor arg0) {
    }

    public void uninitialize(WizardDescriptor arg0) {
    }

    public Panel current()
    {
      if (_panel == null)
	_panel = new InstallPanel();

      return _panel;
    }

    public String name() {
      return "Resin";
    }

    public boolean hasNext() {
      return false;
    }

    public boolean hasPrevious() {
      return false;
    }

    public void nextPanel() {
    }

    public void previousPanel() {
    }

    public void addChangeListener(ChangeListener arg0) {
    }

    public void removeChangeListener(ChangeListener arg0) {
    }

  private static class InstallPanel implements WizardDescriptor.Panel {
    public void removeChangeListener(ChangeListener l)
    {
    }

    public void addChangeListener(ChangeListener l)
    {
    }

    public void storeSettings(Object settings)
    {
    }

    public void readSettings(Object settings)
    {
    }

    public boolean isValid()
    {
      return true;
    }

    public HelpCtx getHelp()
    {
      return HelpCtx.DEFAULT_HELP;
    }

    public Component getComponent()
    {
      return new JPanel();
    }
  }
}
