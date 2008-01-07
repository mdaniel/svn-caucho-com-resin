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


package com.caucho.netbeans.ide;

import com.caucho.netbeans.ide.ui.AddServerLocationPanel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceCreationException;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.InstantiatingIterator;
import org.openide.WizardDescriptor.Panel;

public final class AddInstanceIterator implements InstantiatingIterator, ChangeListener
{
  private static final Logger log
    = Logger.getLogger(AddInstanceIterator.class.getName());
  
  private static final String []RESIN_REQUIRED_JARS;
  
  private Panel _panel;
  private WizardDescriptor _wizard;
  private ArrayList<ChangeListener> _listeners = new ArrayList<ChangeListener>();

  private int _index;
  private Panel []_panels;
  
  private String _userName;
  private String _password;
  
  private String _resinHome = "/home/ferg/ws/resin";
  
  private String _host = "localhost";
  private int _port = 8081;
  
  public String getResinHome()
  {
    return _resinHome;
  }
  
  public void setResinHome(String resinHome)
  {
    _resinHome = resinHome;
  }
  
  /**
   * Checks if the resin-home is valid by checking for expected jar files
   */
  public boolean isResinHomeValid()
  {
    File resinHome = new File(_resinHome);
    
    if (! resinHome.isDirectory())
      return false;
    
    File lib = new File(resinHome, "lib");
    
    if (! lib.isDirectory())
      return false;
    
    for (String jar : RESIN_REQUIRED_JARS) {
      File file = new File(lib, jar);
      
      if (! file.canRead()) {    
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * Checks if the server is valid.
   */
  public boolean isValid()
  {
    return isResinHomeValid();
  }

  /**
   * Creates a new server instance
   *
   * @return a set containing the instance
   * @throws java.io.IOException
   */
  public Set instantiate() throws IOException 
  {
    HashSet<InstanceProperties> set = new HashSet<InstanceProperties>();
    
    if (! isValid())
      return set;
    
    String url = "resin:" + _host + ":" + _port;
    String displayName = "Resin " + _host + ":" + _port;
    log.info("SET URL:" + url);
    try {
      InstanceProperties ip;
      ip = InstanceProperties.createInstanceProperties(url, _userName, 
                                                       _password, displayName);
      log.info("IP: " + ip);                                             
      ip.setProperty("resin.home", getResinHome());
      ip.setProperty("resin.host", _host);
      ip.setProperty("resin.port", String.valueOf(_port));
      log.info("SET resin.home " + ip.getProperty("resin.home") + " " + ip);
      set.add(ip);
    } catch (InstanceCreationException e) {
      // XXX: should show
      
      log.log(Level.SEVERE, e.getMessage(), e);
    }
    
    return set;
  }

  public void initialize(WizardDescriptor wizard)
  {
    _wizard = wizard;
    _panels = new Panel[] { 
      new AddServerLocationPanel(this),
    };
    _index = 0;
  }

  public void uninitialize(WizardDescriptor wizard) 
  {
  }

  public Panel current()
  {
    Panel panel = _panels[_index];

    String []steps = new String[] { "Resin Home Location" };
    JComponent comp = (JComponent) panel.getComponent();
    comp.putClientProperty("WizardPanel_contentData", steps);
    comp.putClientProperty("WizardPanel_contentSelectedIndex", _index);

    return panel;
  }

  public String name()
  {
    return "Resin 3.1";
  }

  public boolean hasNext() 
  {
    return _index + 1 < _panels.length;
  }

  public boolean hasPrevious() 
  {
    return _index > 0;
  }

  public void nextPanel()
  {
    if (_index + 1 < _panels.length)
      _index++;
  }

  public void previousPanel() 
  {
    if (_index > 0)
      _index--;
  }

  public void addChangeListener(ChangeListener listener)
  {
    synchronized (_listeners) {
      _listeners.add(listener);
    }
  }

  public void removeChangeListener(ChangeListener listener)
  {
    synchronized (_listeners) {
      _listeners.remove(listener);
    }
  }
  
  public void stateChanged(ChangeEvent event)
  {
    HashSet<ChangeListener> listeners;
    
    synchronized (_listeners) {
      listeners = new HashSet<ChangeListener>(_listeners);
    }
    
    for (ChangeListener listener : listeners)
      listener.stateChanged(event);
  }
  
  static {
    RESIN_REQUIRED_JARS = new String[] {
      "resin.jar",
      "resin-util.jar",
      "jsdk-15.jar",
      "webbeans-16.jar",
    };
  }
}
