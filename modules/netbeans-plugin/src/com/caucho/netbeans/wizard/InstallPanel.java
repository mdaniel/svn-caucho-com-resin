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

package com.caucho.netbeans.wizard;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

final class InstallPanel
  implements WizardDescriptor.Panel, ChangeListener
{
  private final List<EventListener> _listeners
    = new ArrayList<EventListener>();

  private WizardDescriptor _wizard;
  private InstallPanelVisual _component;

  public void addChangeListener(javax.swing.event.ChangeListener l)
  {
    synchronized (_listeners) {
      _listeners.add(l);
    }
  }

  public void removeChangeListener(javax.swing.event.ChangeListener l)
  {
    synchronized (_listeners) {
      _listeners.remove(l);
    }
  }

  public void storeSettings(Object settings)
  {
  }

  public void readSettings(Object settings)
  {
    _wizard = (WizardDescriptor) settings;
  }

  public boolean isValid()
  {
    boolean result = getVisual().isValid();

    _wizard.putProperty(AddInstanceIterator.PROP_ERROR_MESSAGE,
                       getVisual().getErrorMessage());

    return result;
  }

  public java.awt.Component getComponent()
  {
    if (_component == null) {
      _component = new InstallPanelVisual();
      _component.addChangeListener(this);
    }

    return _component;
  }

  public org.openide.util.HelpCtx getHelp()
  {
    return new HelpCtx("resin_addinstall");
  }

  public void stateChanged(javax.swing.event.ChangeEvent event)
  {
    fireChange(event);
  }

  public InstallPanelVisual getVisual()
  {
    return (InstallPanelVisual) getComponent();
  }

  private void fireChange(ChangeEvent event)
  {
    EventListener[] listeners;

    synchronized (_listeners) {
      listeners = _listeners.toArray(new EventListener[_listeners.size()]);
    }

    for (EventListener listener : listeners) {
      if (listener instanceof ChangeListener)
        ((ChangeListener) listener).stateChanged(event);
    }
  }
}
