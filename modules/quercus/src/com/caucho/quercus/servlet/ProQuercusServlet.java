/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
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
public class ProQuercusServlet extends QuercusServletImpl
{
  private static final L10N L = new L10N(ProQuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(ProQuercusServlet.class.getName());

  public ProQuercusServlet()
  {
    checkLicense(null);
  }
  
  public ProQuercusServlet(File licenseDirectory)
  {
    try {
      checkLicense(licenseDirectory);
    } catch (ConfigException e) {
      checkLicense(null);
    }
  }

  private void checkLicense(File licenseDirectory)
    throws ConfigException
  {
    try {
      Class<?> cl = Class.forName("com.caucho.license.LicenseCheckImpl"); 

      LicenseCheck license;
      
      if (licenseDirectory != null) {
        Constructor<?> cons = cl.getConstructor(File.class);
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

      throw new ConfigException(L.l("Compiled Quercus a valid Quercus license.\nSee http://www.caucho.com for information on licensing."));
    }
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    synchronized (this) {
      if (_quercus == null)
        _quercus = new ProQuercus();
    }

    return _quercus;
  }
}

