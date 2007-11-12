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


package com.caucho.netbeans.util;

import com.caucho.netbeans.PluginLogger;
import com.caucho.netbeans.core.ResinDeploymentManager;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.openide.ErrorManager;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbBundle;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

public final class ResinProperties
{
  private final static PluginLogger log = new PluginLogger(ResinProperties.class);

  /**
   * Java platform property which is used as a java platform ID
   */
  public static final String PLAT_PROP_ANT_NAME = "platform.ant.name"; //NOI18N

  // properties
  public static final String PROP_SERVER_PORT
    = InstanceProperties.HTTP_PORT_NUMBER;
  private static final String PROP_SERVER_PORT_TIMESTAMP
    = "server_port_timestamp";  // NOI18N

  private static final String PROP_DISPLAY_NAME
    = InstanceProperties.DISPLAY_NAME_ATTR;
  public static final String PROP_MONITOR = "monitor_enabled"; // NOI18N
  private static final String PROP_DEBUG_PORT = "debugger_port";   // NOI18N
  private static final String PROP_JAVA_PLATFORM = "java_platform";   // NOI18N
  private static final String PROP_JAVA_OPTS = "java_opts";       // NOI18N
  private static final String PROP_SOURCES = "sources";         // NOI18N
  private static final String PROP_JAVADOCS = "javadocs";        // NOI18N
  private static final String PROP_HOST = "host";            // NOI18N
  public static final String PROP_PROXY_ENABLED
    = "proxy_enabled";   // NOI18N
  public static final String PROP_AUTOLOAD_ENABLED
    = "autoload_enabled";   // NOI18N

  private static final String DEF_VALUE_JAVA_OPTS = ""; // NOI18N
  private static final boolean DEF_VALUE_MONITOR = true;
  private static final int DEF_VALUE_DEBUG_PORT = 11999;
  private static final String DEF_VALUE_HOST = "localhost"; // NOI18N
  private static final boolean DEF_VALUE_PROXY_ENABLED = true;
  private static final boolean DEF_VALUE_AUTOLOAD_ENABLED = false;

  private ResinDeploymentManager _manager;
  private InstanceProperties _instanceProperties;
  private File _resinHome;
  private File _confFile;

  /**
   * Creates a new instance of ResinProperties
   */
  public ResinProperties(ResinDeploymentManager manager)
    throws IllegalArgumentException
  {
    _manager = manager;
    _instanceProperties = manager.getInstanceProperties();
  }

  /**
   * Return RESIN_HOME directory.
   */
  public File getResinHome()
  {
    return _resinHome;
  }

  /**
   * Return RESIN_CONF directory or null if not defined.
   */
  public File getResinConf()
  {
    return _confFile;
  }

  public boolean getProxyEnabled()
  {
    String val = _instanceProperties.getProperty(PROP_PROXY_ENABLED);
    return val != null
           ? Boolean.valueOf(val).booleanValue()
           : DEF_VALUE_PROXY_ENABLED;
  }

  public void setProxyEnabled(boolean enabled)
  {
    _instanceProperties.setProperty(PROP_PROXY_ENABLED, Boolean.toString(enabled));
  }

  public boolean getAutoloadEnabled()
  {
    String val = _instanceProperties.getProperty(PROP_AUTOLOAD_ENABLED);
    return val != null
           ? Boolean.valueOf(val).booleanValue()
           : DEF_VALUE_AUTOLOAD_ENABLED;
  }

  public void setAutoloadEnabled(boolean enabled)
  {
    _instanceProperties.setProperty(PROP_AUTOLOAD_ENABLED, Boolean.toString(enabled));
  }

  public String getHttpConnecterUrl()
  {
    // TODO: 
    return "http://localhost:" + getServerPort();
  }

  public String getProviderUrl()
  {
    return "unimplemnted://provider_url";
  }

  public String getDomainName()
  {
    return "resin"; // NOI18N
  }

  public String getServerName()
  {
    return "resin"; // NOI18N
  }

  public JavaPlatform getJavaPlatform()
  {
    String currentJvm = _instanceProperties.getProperty(PROP_JAVA_PLATFORM);
    JavaPlatformManager jpm = JavaPlatformManager.getDefault();
    JavaPlatform[] installedPlatforms = jpm.getPlatforms(null,
                                                         new Specification(
                                                           "J2SE",
                                                           null)); // NOI18N
    for (int i = 0; i < installedPlatforms.length; i++) {
      String platformName = (String) installedPlatforms[i].getProperties()
        .get(PLAT_PROP_ANT_NAME);
      if (platformName != null && platformName.equals(currentJvm)) {
        return installedPlatforms[i];
      }
    }
    // return default platform if none was set
    return jpm.getDefaultPlatform();
  }

  public void setJavaPlatform(JavaPlatform javaPlatform)
  {
    _instanceProperties.setProperty(PROP_JAVA_PLATFORM,
                   (String) javaPlatform.getProperties()
                     .get(PLAT_PROP_ANT_NAME));
  }

  public String getJavaOpts()
  {
    String val = _instanceProperties.getProperty(PROP_JAVA_OPTS);
    return val != null ? val : DEF_VALUE_JAVA_OPTS;
  }

  public void setJavaOpts(String javaOpts)
  {
    _instanceProperties.setProperty(PROP_JAVA_OPTS, javaOpts);
  }

  public boolean getMonitor()
  {
    String val = _instanceProperties.getProperty(PROP_MONITOR);
    return val != null
           ? Boolean.valueOf(val).booleanValue()
           : DEF_VALUE_MONITOR;
  }

  public void setMonitor(boolean enabled)
  {
    _instanceProperties.setProperty(PROP_MONITOR, Boolean.toString(enabled));
  }

  public int getDebugPort()
  {
    String val = _instanceProperties.getProperty(PROP_DEBUG_PORT);
    if (val != null) {
      try {
        return Integer.parseInt(val);
      }
      catch (NumberFormatException nfe) {
        // no op
      }
    }
    return DEF_VALUE_DEBUG_PORT;
  }

  public void setDebugPort(int port)
  {
    _instanceProperties.setProperty(PROP_DEBUG_PORT, Integer.toString(port));
  }


  public int getServerPort()
  {
    try {
      return readServerPort();
    }
    catch (IllegalStateException e) {
      return 6080;
    }
  }

  private int readServerPort()
    throws IllegalStateException
  {
    File resinConf = new File(_confFile, "conf/resin.xml");

    // do not parse the file if the value is cached and the file did not change
    try {
      String serverPort = _instanceProperties.getProperty(PROP_SERVER_PORT);
      String serverPortTimestamp = _instanceProperties.getProperty(PROP_SERVER_PORT_TIMESTAMP);

      if (serverPort != null && serverPortTimestamp != null) {
        int serverPortValue = Integer.parseInt(serverPort);
        long timestamp = Long.parseLong(serverPortTimestamp);
        if (resinConf.lastModified() == timestamp) {
          return serverPortValue;
        }
      }
    }
    catch (NumberFormatException e) {
      log.log(Level.INFO, e);
    }

    // TODO: read server port from resin.conf
    String msg = NbBundle.getMessage(ResinProperties.class,
                                     "MSG_CannotReadServerPort",
                                     resinConf);

    throw new IllegalStateException(msg);
  }

  public void setServerPort(int portValue)
    throws IllegalStateException
  {
    String msg = NbBundle.getMessage(ResinProperties.class,
                                     "MSG_CannotWriteServerPort");

    throw new IllegalStateException(msg);
  }

  public List/*<URL>*/ getClasses()
  {
    String[] nbFilter = new String[]{"httpmonitor", "schema2beans"};

    String[] implFilter = new String[]{"-impl.jar"};
    // tomcat and jwsdp libs
    List retValue = listUrls(new File(_resinHome, "lib/commons/j2ee"),
                             new String[]{});    // NOI18N
//        retValue.addAll(listUrls(new File(homeDir, "jaxb/lib"),    implFilter)); // NOI18N
    return retValue;
  }

  public List/*<URL>*/ getSources()
  {
    String path = _instanceProperties.getProperty(PROP_SOURCES);
    if (path == null) {
      return new ArrayList();
    }
    return tokenizePath(path);
  }

  public void setSources(List/*<URL>*/ path)
  {
    _instanceProperties.setProperty(PROP_SOURCES, buildPath(path));
    _manager.getJ2eePlatform().notifyLibrariesChanged();
  }

  public List/*<URL>*/ getJavadocs()
  {
    String path = _instanceProperties.getProperty(PROP_JAVADOCS);
    if (path == null) {
      ArrayList list = new ArrayList();
      try {
        // tomcat docs
        File j2eeDoc = InstalledFileLocator.getDefault()
          .locate("docs/javaee5-doc-api.zip", null, false); // NOI18N
        if (j2eeDoc != null) {
          list.add(Utils.fileToUrl(j2eeDoc));
        }
      }
      catch (MalformedURLException e) {
        ErrorManager.getDefault().notify(e);
      }
      return list;
    }
    return tokenizePath(path);
  }

  public void setJavadocs(List/*<URL>*/ path)
  {
    _instanceProperties.setProperty(PROP_JAVADOCS, buildPath(path));
    _manager.getJ2eePlatform().notifyLibrariesChanged();
  }

  public String getHost()
  {
    String val = _instanceProperties.getProperty(PROP_HOST);
    return val != null ? val : DEF_VALUE_HOST;
  }

  public String getDisplayName()
  {
    return _instanceProperties.getProperty(PROP_DISPLAY_NAME);
  }

  // private helper methods -------------------------------------------------

  private static String buildPath(List/*<URL>*/ path)
  {
    String PATH_SEPARATOR = System.getProperty("path.separator"); // NOI18N
    StringBuffer sb = new StringBuffer(path.size() * 16);
    for (Iterator i = path.iterator(); i.hasNext();) {
      sb.append(Utils.urlToString((URL) i.next()));
      if (i.hasNext()) {
        sb.append(PATH_SEPARATOR);
      }
    }
    return sb.toString();
  }

  /**
   * Split an Ant-style path specification into components. Tokenizes on
   * <code>:</code> and <code>;</code>, paying attention to DOS-style
   * components such as <samp>C:\FOO</samp>. Also removes any empty
   * components.
   *
   * @param path an Ant-style path (elements arbitrary) using DOS or Unix
   *             separators
   *
   * @return a tokenization of that path into components
   */
  private static List/*<URL>*/ tokenizePath(String path)
  {
    try {
      List/*<URL>*/ l = new ArrayList();
      StringTokenizer tok = new StringTokenizer(path, ":;", true); // NOI18N
      char dosHack = '\0';
      char lastDelim = '\0';
      int delimCount = 0;
      while (tok.hasMoreTokens()) {
        String s = tok.nextToken();
        if (s.length() == 0) {
          // Strip empty components.
          continue;
        }
        if (s.length() == 1) {
          char c = s.charAt(0);
          if (c == ':' || c == ';') {
            // Just a delimiter.
            lastDelim = c;
            delimCount++;
            continue;
          }
        }
        if (dosHack != '\0') {
          // #50679 - "C:/something" is also accepted as DOS path
          if (lastDelim == ':' &&
              delimCount == 1 &&
              (s.charAt(0) == '\\' || s.charAt(0) == '/'))
          {
            // We had a single letter followed by ':' now followed by \something or /something
            s = "" + dosHack + ':' + s;
            // and use the new token with the drive prefix...
          }
          else {
            // Something else, leave alone.
            l.add(Utils.fileToUrl(new File(Character.toString(dosHack))));
            // and continue with this token too...
          }
          dosHack = '\0';
        }
        // Reset count of # of delimiters in a row.
        delimCount = 0;
        if (s.length() == 1) {
          char c = s.charAt(0);
          if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            // Probably a DOS drive letter. Leave it with the next component.
            dosHack = c;
            continue;
          }
        }
        l.add(Utils.fileToUrl(new File(s)));
      }
      if (dosHack != '\0') {
        //the dosHack was the last letter in the input string (not followed by the ':')
        //so obviously not a drive letter.
        //Fix for issue #57304
        l.add(Utils.fileToUrl(new File(Character.toString(dosHack))));
      }
      return l;
    }
    catch (MalformedURLException e) {
      ErrorManager.getDefault().notify(e);
      return new ArrayList();
    }
  }


  private static List<URL> listUrls(final File folder, final String[] filter)
  {
    File[] jars = folder.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String name)
      {
        if (!name.endsWith(".jar") || !dir.equals(folder)) {
          return false;
        }
        for (int i = 0; i < filter.length; i++) {
          if (name.indexOf(filter[i]) != -1) {
            return false;
          }
        }
        return true;
      }
    });
    if (jars == null) {
      return new ArrayList();
    }
    List<URL> urls = new ArrayList<URL>(jars.length);
    for (int i = 0; i < jars.length; i++) {
      try {
        urls.add(Utils.fileToUrl(jars[i]));
      }
      catch (MalformedURLException e) {
        ErrorManager.getDefault().notify(e);
      }
    }
    return urls;
  }
}
