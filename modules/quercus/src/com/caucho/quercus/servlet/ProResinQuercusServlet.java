/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.servlet;

import com.caucho.config.ConfigException;
import com.caucho.license.LicenseCheck;
import com.caucho.quercus.*;
import com.caucho.util.L10N;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class ProResinQuercusServlet extends ResinQuercusServlet
{
  private static final L10N L = new L10N(ProResinQuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(ProResinQuercusServlet.class.getName());
  
  public ProResinQuercusServlet()
  {
    checkLicense(null);
  }
  
  public ProResinQuercusServlet(File licenseDirectory)
  {
    try {
      checkLicense(licenseDirectory);
    } catch (ConfigException e) {
      
      if (licenseDirectory != null)
        checkLicense(null);
      else
        throw e;
    }
  }
  
  private void checkLicense(File licenseDirectory)
    throws ConfigException
  {
    try {
      Class cl = Class.forName("com.caucho.license.LicenseCheckImpl");

      LicenseCheck license;

      if (licenseDirectory != null) {
        Constructor cons = cl.getConstructor(File.class);
        license = (LicenseCheck) cons.newInstance(licenseDirectory);
      }
      else
        license = (LicenseCheck) cl.newInstance();

      license.requirePersonal(1);
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      Throwable cause = e.getCause();
      
      if (cause == null)
        cause = e;
      
      if (cause instanceof ConfigException)
        throw (ConfigException) cause;
      
      throw new ConfigException(L.l("Compiled Quercus a valid Resin Personal license.\nSee http://www.caucho.com for information on licensing."));
    }
  }

  @Override
  public void setProfileProbability(double probability)
  {
    getQuercus().setProfileProbability(probability);
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null) {
      _quercus = new ProResinQuercus();
    }

    return _quercus;
  }
}

