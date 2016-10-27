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
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.deploy.DeployControllerApi;
import com.caucho.env.deploy.DeployControllerType;
import com.caucho.env.deploy.ExpandVersion;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

/**
 * A configuration entry for a versioning web-app.
 */
public class WebAppVersioningController extends WebAppController {
  private static final L10N L = new L10N(WebAppVersioningController.class);
  private static final Logger log
    = Logger.getLogger(WebAppController.class.getName());

  private static final long EXPIRE_PERIOD = 3600 * 1000L;

  private final String _baseKey;
  
  private long _versionRolloverTime = EXPIRE_PERIOD;

  private ArrayList<WebAppController> _controllerList
    = new ArrayList<WebAppController>();

  private final WebAppExpandDeployGenerator _generator;
  
  private long _restartTime;
  
  private ExpandVersion _primaryVersion;
  private WebAppController _primaryController;
  
  private ArrayList<DeployControllerApi<WebApp>> _mergeList
    = new ArrayList<DeployControllerApi<WebApp>>();
  
  private boolean _isModified = false;
  private AtomicBoolean _isUpdating = new AtomicBoolean();

  public WebAppVersioningController(String id,
                                    String baseKey,
                                    String contextPath,
                                    WebAppExpandDeployGenerator generator,
                                    WebAppContainer container)
  {
    super(id + "-0.0.0.versioning", null, container, contextPath);

    _baseKey = baseKey;
    _generator = generator;
  }

  void setModified(boolean isModified)
  {
    _isModified = isModified;
  }

  @Override
  public boolean isVersioning()
  {
    return true;
  }
  
  @Override
  public DeployControllerType getControllerType()
  {
    return DeployControllerType.VERSIONING;
  }

  @Override
  public String getVersion()
  {
    if (_primaryController != null)
      return _primaryController.getVersion();
    else
      return "";
  }
  
  @Override
  public WebAppAdmin getAdmin()
  {
    if (_primaryController != null)
      return _primaryController.getAdmin();
    else
      return null;
  }

  /**
   * Returns the instance for a top-level request
   *
   * @return the request object or null for none.
   */
  @Override
  public WebApp instantiateDeployInstance()
  {
    updateVersion();
    
    WebAppController controller = _primaryController;

    if (controller != null) {
      WebApp webApp = controller.request();
      
      return webApp;
    }
    else
      throw new NullPointerException(getClass().getName());
  }

  /**
   * Starts the entry.
   */
  @Override
  protected WebApp startImpl()
  {
    // server/1h2g
    // super.startImpl();
    
    updateVersion();

    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.request();
    else
      return null;
  }

  @Override
  public WebApp getDeployInstance()
  {
    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.request();
    else
      return null;
  }
  
  @Override
  protected void stopImpl()
  {
    // server/1h20, server/27b0 (in order)
    WebAppController controller = _primaryController;

    if (controller != null) {
      controller.stop();
    }
    
    for (WebAppController ctrl : _controllerList) {
      ctrl.stop();
    }
  }
  
  @Override
  protected void stopLazyImpl()
  {
  }
  
  @Override
  protected void destroyInstance(WebApp instance)
  {
  }

  /**
   * Initialize the controller.
   */
  @Override
  protected void initBegin()
  {
    /*
    super.initBegin();
    */
  }

  /**
   * Initialize the controller.
   */
  @Override
  protected void initEnd()
  {
    /*
    super.initBegin();
    */
  }

  @Override
  public boolean isModified()
  {
    boolean isModified = updateVersion();
    
    return isModified;
  }
  
  @Override
  public void merge(DeployControllerApi<WebApp> newControllerV)
  {
    _mergeList.add(newControllerV);

    if (_primaryController != null)
      _primaryController.merge(newControllerV);
    else {
      // server/12ab
      super.merge(newControllerV);
    }
  }

  
  public boolean updateVersion()
  {
//    _isModified = true;

    return updateVersionImpl();
  }

  private boolean updateVersionImpl()
  {
    if (! _isUpdating.compareAndSet(false, true))
      return false;

    try {
      synchronized (this) {
        ExpandVersion oldPrimaryVersion = _primaryVersion;
        WebAppController oldPrimaryController = _primaryController;
        
        WebAppController newPrimaryController = null;

        ExpandVersion version = _generator.getPrimaryVersion(_baseKey);

        if (oldPrimaryVersion != null && oldPrimaryVersion.equals(version))
          return false;

        if (version != null) {
          newPrimaryController = _generator.createVersionController(version);
          newPrimaryController.merge(newPrimaryController);
        }

        if (newPrimaryController == null) {
          throw new ConfigException(L.l(this + " does not have an implementing version"));
        }

        if (newPrimaryController == oldPrimaryController)
          return false;

        log.fine(this + " updating primary to " + newPrimaryController);

        if (oldPrimaryController != null
            && oldPrimaryController != newPrimaryController) {
          _controllerList.add(oldPrimaryController);
        }
        
        // server/1h20
        newPrimaryController.init();

        newPrimaryController.setVersionAlias(true);
        // server/12ab
        newPrimaryController.merge(this);
        
        // server/1h35
        for (DeployControllerApi<WebApp> newController : _mergeList) {
          newPrimaryController.merge(newController);
        }
        
        _primaryController = newPrimaryController;
        
        _primaryVersion = version;

        _controllerList.remove(newPrimaryController);

        int size = _controllerList.size();
        if (size > 0) {
          WebAppController oldController = _controllerList.get(size - 1);

          long expireTime = CurrentTime.getCurrentTime() + _versionRolloverTime;

          _primaryController.setOldWebApp(oldController, expireTime);
        }

        _restartTime = CurrentTime.getCurrentTime();
        clearCache();
        
        return true;
      }
    } finally {
      _isUpdating.set(false);
    }
  }
  
  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + getId() + "]";
  }

  static class VersionNameComparator implements Comparator<String>
  {
    public int compare(String versionA, String versionB)
    {
      /*
      String versionA = a.getVersion();
      String versionB = b.getVersion();
      */

      int lengthA = versionA.length();
      int lengthB = versionB.length();

      int indexA = 0;
      int indexB = 0;

      while (indexA < lengthA && indexB < lengthB) {
        int valueA = 0;
        int valueB = 0;
        char chA;
        char chB;

        for (;
             indexA < lengthA
               && '0' <= (chA = versionA.charAt(indexA)) && chA <= '9';
             indexA++) {
          valueA = 10 * valueA + chA - '0';
        }

        for (;
             indexB < lengthB
               && '0' <= (chB = versionB.charAt(indexB)) && chB <= '9';
             indexB++) {
          valueB = 10 * valueB + chB - '0';
        }

        if (valueA < valueB)
          return 1;
        else if (valueB < valueA)
          return -1;

        while (indexA < lengthA && indexB < lengthB
               && ! ('0' <= (chA = versionA.charAt(indexA)) && chA <= '9')
               && ! ('0' <= (chB = versionB.charAt(indexB)) && chB <= '9')) {

          if (chA < chB)
            return 1;
          else if (chB < chA)
            return -1;

          indexA++;
          indexB++;
        }

        if (indexA < lengthA
            && ! ('0' <= (chA = versionA.charAt(indexA)) && chA <= '9'))
          return 1;
        else if (indexB < lengthB
                 && ! ('0' <= (chB = versionB.charAt(indexB)) && chB <= '9'))
          return -1;
      }

      if (indexA != lengthA)
        return 1;
      else if (indexB != lengthB)
        return -1;
      else
        return 0;
    }
  }
}
