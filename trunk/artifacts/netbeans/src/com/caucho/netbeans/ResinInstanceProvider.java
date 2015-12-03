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

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.server.ServerInstance;
import org.netbeans.api.server.properties.InstanceProperties;
import org.netbeans.api.server.properties.InstancePropertiesManager;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceCreationException;
import org.netbeans.spi.server.ServerInstanceFactory;
import org.netbeans.spi.server.ServerInstanceProvider;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;

public final class ResinInstanceProvider implements ServerInstanceProvider {

  private final static Logger log = Logger.getLogger(ResinInstanceProvider.class.getName());
  private static ResinInstanceProvider instance;
  private final ChangeSupport _listeners = new ChangeSupport(this);
  private final List<ResinInstance> _resins = new ArrayList<ResinInstance>();
  private InstancePropertiesManager _persistence;

  static {
    //Logger.getLogger("org.netbeans.modules.j2ee").setLevel(Level.FINEST);
  }

  public static List<ResinInstanceProvider> getProviders(boolean initialize) {
    List<ResinInstanceProvider> result = new ArrayList<ResinInstanceProvider>();

    return result;
  }

  public synchronized final static ResinInstanceProvider getInstance() {
    if (instance == null) {

      instance = new ResinInstanceProvider();
      instance.init();
    }

    return instance;
  }

  private ResinInstanceProvider() {
  }

  private void init() {
    _persistence = InstancePropertiesManager.getInstance();
    List<InstanceProperties> list = _persistence.getProperties("com.caucho.resin");

    for (InstanceProperties properties : list) {
      ResinInstance resin = new ResinInstance(properties);

      ServerInstance server = ServerInstanceFactory.createServerInstance(resin);
      resin.setServerInstance(server);

      _resins.add(resin);
    }
  }

  public ResinInstance getResinInstance(String url) {
    for (ResinInstance resin : _resins) {
      if (url.equals(resin.getUrl())) {
        return resin;
      }
    }

    throw new IllegalStateException("Instance does not appear to exist");
  }

  public void echo() {
    /*
    for (String serverInstanceID : Deployment.getDefault().getServerInstanceIDs()) {
      String displayName = Deployment.getDefault().getServerInstanceDisplayName(serverInstanceID);
      J2eePlatform j2eePlatform = Deployment.getDefault().getJ2eePlatform(serverInstanceID);
      System.out.println("Display-name: " + displayName);
      System.out.println("j2ee-platform: " + displayName);
    }*/
  }

  public ServerInstance instantiate(ResinInstance resin) {
    InstanceProperties properties = _persistence.createProperties("com.caucho.resin");
    resin.persist(properties);

    ServerInstance server = ServerInstanceFactory.createServerInstance(resin);

    _resins.add(resin);

    createJ2eeInstance(resin);

    _listeners.fireChange();

    return server;
  }

  private void createJ2eeInstance(ResinInstance resin) {
    try {
      String url = resin.getUrl();
      org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties p =
              org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.createInstancePropertiesWithoutUI(url, resin.getAddress(), resin.getDisplayName(), resin.getDisplayName(), new HashMap<String, String>());
    } catch (InstanceCreationException ex) {
      Exceptions.printStackTrace(ex);
    }
  }

  void remove(ResinInstance resin) {
    _resins.remove(resin);
    resin.getInstanceProperties().remove();
    try {
      org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.removeInstance(resin.getUrl());
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
  }

  @Override
  public List<ServerInstance> getInstances() {
    List<ServerInstance> result = new ArrayList<ServerInstance>(_resins.size());

    for (ResinInstance resin : _resins) {
      result.add(resin.getServerInstance());
    }

    echo();

    return result;
  }

  @Override
  public void addChangeListener(ChangeListener listener) {
    _listeners.addChangeListener(listener);
  }

  @Override
  public void removeChangeListener(ChangeListener listener) {
    _listeners.removeChangeListener(listener);
  }
}
