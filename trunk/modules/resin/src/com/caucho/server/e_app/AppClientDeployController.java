/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.e_app;

import com.caucho.config.Config;
import com.caucho.server.deploy.ExpandDeployController;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * A configuration entry for Enterprise Application clients
 */
public class AppClientDeployController extends ExpandDeployController<EntAppClient> {
  private static final Logger log
    = Logger.getLogger(AppClientDeployController.class.getName());
  private static final L10N L = new L10N(AppClientDeployController.class);

  // The ear name
  private String _name = "";

  // private Var _hostVar = new Var();

  private JarPath _clientJar;

  private ArrayList<Path> _configList = new ArrayList<Path>();

  private boolean _isInit;

  public AppClientDeployController()
  {
    super("client");
  }

  /**
   * Sets the root.
   */
  public void setRootDirectory(Path path)
  {
    super.setRootDirectory(path);
  }

  /**
   * Adds a config file.
   */
  public void addConfig(Path path)
  {
    _configList.add(path);
  }

  /**
   * Adds a config file.
   */
  public void addEar(Path path)
  {
    System.out.println("ADD EAR: " + path);
    
    JarPath jar = JarPath.create(path);

    Path app = jar.lookup("META-INF/application.xml");

    System.out.println("  APP: " + app + " " + app.exists());

    if (app.exists())
      addConfig(app);

    app = jar.lookup("META-INF/resin-application.xml");

    System.out.println("  APP: " + app + " " + app.exists());

    if (app.exists())
      addConfig(app);
  }

  /**
   * Executes the main.
   */
  public void main(String []args)
    throws Throwable
  {
    System.out.println("MAIN()");
    init();
    start();

    EntAppClient appClient = request();
    if (appClient != null)
      appClient.main(args);
  }

  /**
   * Executes the main.
   */
  public void main(String mainClass, String []args)
    throws Throwable
  {
    System.out.println("MAIN: " + mainClass);
    init();
    start();

    EntAppClient appClient = request();
    System.out.println("MAIN: " + appClient + " " + mainClass);
    if (appClient != null)
      appClient.main(mainClass, args);
  }

  // XXX: temp
  public ClassLoader getLoader()
  {
    init();
    start();

    EntAppClient appClient = request();

    if (appClient != null)
      return appClient.getClassLoader();
    else
      return null;
  }

  /**
   * Returns the application object.
   */
  protected EntAppClient instantiateDeployInstance()
  {
    return new EntAppClient(this, getId());
  }

  /**
   * Creates the application.
   */
  protected void configureInstance(EntAppClient appClient)
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    Path rootDir = null;
    try {
      thread.setContextClassLoader(getParentClassLoader());

      appClient.setArchivePath(getArchivePath());

      rootDir = getRootDirectory();

      if (rootDir == null)
        throw new NullPointerException("Null root-directory");

      /*
        if (! rootDir.isDirectory()) {
        throw new ConfigException(L.l("root-directory `{0}' must specify a directory.",
        rootDir.getPath()));
        }
      */

      appClient.setRootDirectory(rootDir);

      thread.setContextClassLoader(appClient.getClassLoader());
      Vfs.setPwd(rootDir);

      addManifestClassPath();

      configApplication(appClient);

      // configClientApplication(appClient);

      Path curr = rootDir;

      Iterator<String> it = curr.iterator();

      while (it.hasNext()) {

        String s = it.next();

        Path path = curr.lookup(s);

        if (path.isDirectory()) {
        }
        else if (s.endsWith(".jar")) {
          appClient.getClassLoader().addJar(path);

          _clientJar = JarPath.create(path);

          configClientApplication(appClient);
        }
      }

      for (int i = 0; i < _configList.size(); i++)
        configClientConfig(appClient, _configList.get(i));

      appClient.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void configApplication(EntAppClient appClient)
    throws Exception
  {
    Path rootDir = getRootDirectory();

    Path xml = rootDir.lookup("META-INF/application.xml");

    if (xml.canRead())
      new Config().configureBean(appClient, xml);

    xml = rootDir.lookup("META-INF/resin-application.xml");

    if (xml.canRead())
      new Config().configureBean(appClient, xml);

    /*
     * XXX:
     config = EnterpriseApplication.parseApplicationConfig(rootDir, xml);
    */

    /*
      ArrayList<Path> ejbModules = config.getEjbModules();
      EJBClientInterface ejbClient = null;

      if (ejbModules.size() > 0) {
      Class cl = Class.forName("com.caucho.iiop.IiopClient");
      ejbClient = (EJBClientInterface) cl.newInstance();
      }
    */

    /*
      for (int i = 0; i < ejbModules.size(); i++) {
      Path path = ejbModules.get(i);

      appClient.getClassLoader().addJar(path);

      if (ejbClient != null)
      ejbClient.addEJBJar(path);
      }

      if (ejbClient != null)
      ejbClient.initEJBs();

      ArrayList<Path> javaModules = config.getJavaModules();

      for (int i = 0; i < javaModules.size(); i++) {
      Path path = javaModules.get(i);

      appClient.getClassLoader().addJar(path);

      _clientJar = JarPath.create(path);

      Manifest manifest = _clientJar.getManifest();
      String mainClass = manifest.getMainAttributes().getValue("Main-Class");

      appClient.setMainClass(mainClass);
      }
    */
  }

  private void configClientApplication(EntAppClient appClient)
    throws Exception
  {
    if (_clientJar == null)
      return;

    Path xml = _clientJar.lookup("META-INF/application-client.xml");

    if (! xml.canRead())
      return;

    new Config().configureBean(appClient, xml,
                               "com/caucho/server/e_app/app-client.rnc");
  }

  private void configClientConfig(EntAppClient appClient, Path xml)
    throws Exception
  {
    if (! xml.canRead())
      return;

    new Config().configureBean(appClient, xml);
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof AppClientDeployController))
      return false;

    AppClientDeployController controller = (AppClientDeployController) o;

    return getId().equals(controller.getId());
  }

  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "AppClientDeployController[" + getId() + "]";
  }
}
