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

import com.caucho.netbeans.PluginLogger;
import com.caucho.netbeans.core.ResinConfiguration;

import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.openide.WizardDescriptor;

import javax.swing.*;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;

public final class AddInstanceIterator
  implements WizardDescriptor.InstantiatingIterator
{
  private static final PluginLogger log = new PluginLogger(AddInstanceIterator.class);

  public final static String PROP_ERROR_MESSAGE
    = "WizardPanel_errorMessage";

  private final static String PROP_CONTENT_DATA
    = "WizardPanel_contentData";

  private final static String PROP_CONTENT_SELECTED_INDEX
    = "WizardPanel_contentSelectedIndex";

  private final static String PROP_DISPLAY_NAME
    = "ServInstWizard_displayName";

  private final static String[] CONTENT_DATA
    = new String[] { "Resin Instance Properties" };

  private WizardDescriptor _wizard;
  private InstallPanel _panel;

  public void removeChangeListener(javax.swing.event.ChangeListener l)
  {
  }

  public void addChangeListener(javax.swing.event.ChangeListener l)
  {
  }

  public void uninitialize(WizardDescriptor wizard)
  {
  }

  public void initialize(WizardDescriptor wizard)
  {
    _wizard = wizard;
  }

  public void previousPanel()
  {
    throw new NoSuchElementException();
  }

  public void nextPanel()
  {
    throw new NoSuchElementException();
  }

  public String name()
  {
    return null;
  }

  public Set instantiate()
    throws java.io.IOException
  {
    ResinConfiguration resinConfiguration = _panel.getVisual().getResinConfiguration();

    String displayName = (String) _wizard.getProperty(PROP_DISPLAY_NAME);

    resinConfiguration.setDisplayName(displayName);
    resinConfiguration.setUsername("resin");
    resinConfiguration.setPassword("resin");

    Set<InstanceProperties> set = new HashSet<InstanceProperties>();

    try {
      set.add(resinConfiguration.toInstanceProperties());
    }
    catch (Exception ex) {
      log.log(Level.SEVERE, ex);
    }

    return set;
  }

  public boolean hasPrevious()
  {
    return false;
  }

  public boolean hasNext()
  {
    return false;
  }

  public WizardDescriptor.Panel current()
  {
    if (_panel == null)
      _panel = new InstallPanel();

    setContentData((JComponent) _panel.getComponent());
    setContentSelectedIndex((JComponent) _panel.getComponent());

    return _panel;
  }

  private void setContentData(JComponent component)
  {
    if (component.getClientProperty(PROP_CONTENT_DATA) == null) {
      component.putClientProperty(PROP_CONTENT_DATA, CONTENT_DATA);
    }
  }

  private void setContentSelectedIndex(JComponent component)
  {
    if (component.getClientProperty(PROP_CONTENT_SELECTED_INDEX) == null) {
      component.putClientProperty(PROP_CONTENT_SELECTED_INDEX, 0);
    }
  }

}
