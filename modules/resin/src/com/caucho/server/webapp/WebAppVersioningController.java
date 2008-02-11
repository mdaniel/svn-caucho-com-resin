/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.types.PathBuilder;
import com.caucho.log.Log;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.management.j2ee.WebModule;
import com.caucho.server.deploy.DeployConfig;
import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.deploy.EnvironmentDeployController;
import com.caucho.server.host.Host;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.vfs.Path;

import javax.servlet.jsp.el.ELException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A configuration entry for a versioning web-app.
 */
public class WebAppVersioningController extends WebAppController {
  private static final L10N L = new L10N(WebAppVersioningController.class);
  private static final Logger log
    = Logger.getLogger(WebAppController.class.getName());

  private static final long EXPIRE_PERIOD = 3600 * 1000L;

  private long _versionRolloverTime = EXPIRE_PERIOD;
  
  private ArrayList<WebAppController> _controllerList
    = new ArrayList<WebAppController>();

  private final WebAppExpandDeployGenerator _generator;

  private long _restartTime;
  
  private WebAppController _primaryController;
  private boolean _isModified = true;


  public WebAppVersioningController(String contextPath,
				    WebAppExpandDeployGenerator generator,
				    WebAppContainer container)
  {
    super(contextPath, contextPath, null, container);

    _generator = generator;
  }

  /**
   * Merges two entries.
   */
  /*
  protected WebAppController merge(WebAppController newController)
  {
    if (getConfig() != null && getConfig().getURLRegexp() != null)
      return newController;
    else if (newController.getConfig() != null &&
	     newController.getConfig().getURLRegexp() != null)
      return this;
    else {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(getParentClassLoader());

	//  The contextPath comes from current web-app
	WebAppController mergedController
	  = new WebAppController(getContextPath(),
				 getRootDirectory(),
				 _container);

	// server/1h1{2,3}
	// This controller overrides configuration from the new controller
	mergedController.mergeController(this);
	mergedController.mergeController(newController);

	return mergedController;
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }
  }
  */

  void setModified(boolean isModified)
  {
    _isModified = isModified;
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  @Override
  public WebApp request()
  {
    if (_isModified)
      updateVersion();

    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.request();
    else
      return null;
  }

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  @Override
  public WebApp subrequest()
  {
    if (_isModified)
      updateVersion();

    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.request();
    else
      return null;
  }

  /**
   * Starts the entry.
   */
  @Override
  protected WebApp startImpl()
  {
    if (_isModified)
      updateVersion();

    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.request();
    else
      return null;
  }

  /**
   * Initialize the controller.
   */
  protected void initBegin()
  {
    /*
    super.initBegin();
    */
  }

  private void updateVersion()
  {
    synchronized (this) {
      if (! _isModified)
	return;
      _isModified = false;
      
      _controllerList = new ArrayList<WebAppController>();

      ArrayList<String> versionNames = _generator.getVersionNames(getId());

      if (versionNames != null) {
	Collections.sort(versionNames, new VersionNameComparator());
	
	for (int i = 0; i < versionNames.size() && i < 2; i++) {
	  String versionName = versionNames.get(i);

	  WebAppController newController
	    = _container.getWebAppGenerator().findController(versionName);

	  _controllerList.add(newController);
	}

	//Collections.sort(_controllerList, new VersionComparator());
    
	_primaryController = _controllerList.get(0);

	if (_restartTime > 0 && _controllerList.size() > 1) {
	  long expireTime = Alarm.getCurrentTime() + _versionRolloverTime;

	  _primaryController.setOldWebApp(_controllerList.get(1),
					  expireTime);
	  
	}

	_restartTime = Alarm.getCurrentTime();
      }
    }
  }
  
  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "WebAppVersioningController" +  "[" + getId() + "]";
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
