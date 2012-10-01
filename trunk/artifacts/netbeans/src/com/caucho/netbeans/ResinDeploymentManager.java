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
 * @author Sam
 */
package com.caucho.netbeans;

import com.caucho.netbeans.ide.ResinTarget;
import com.caucho.netbeans.ide.ResinTargetModuleID;
import org.netbeans.modules.j2ee.deployment.common.api.ConfigurationException;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.openide.execution.NbProcessDescriptor;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

public final class ResinDeploymentManager
        implements DeploymentManager {

  private static final Logger log = Logger.getLogger(ResinDeploymentManager.class.getName());
  private static final PluginL10N L = new PluginL10N(ResinDeploymentManager.class);
  private final ResinConfiguration _resinConfiguration;
  private ResinInstance _resin;
  private ResinProcess _resinProcess;
  private TargetModuleID[] _runningModules = new TargetModuleID[0];
  private ResinPlatformImpl _j2eePlatform;

  public ResinDeploymentManager(ResinInstance resin)
          throws DeploymentManagerCreationException {
    _resin = resin;

    // XXX: what is connected for?
    _resinConfiguration = new ResinConfiguration(resin);
    _resinProcess = new ResinProcess(_resinConfiguration);
  }

  public ResinConfiguration getResinConfiguration() {
    return _resinConfiguration;
  }

  public ResinProcess getResinProcess() {
    if (_resinProcess == null) {
      _resinProcess = new ResinProcess(_resinConfiguration);
      _resinProcess.init();
    }
    return _resinProcess;
  }

  public Target[] getTargets()
          throws IllegalStateException {
    return new ResinTarget[]{
              new ResinTarget(_resinConfiguration)};
  }

  public TargetModuleID[] getRunningModules(ModuleType moduleType,
          Target[] target)
          throws TargetException, IllegalStateException {
    return _runningModules;
  }

  public TargetModuleID[] getNonRunningModules(ModuleType moduleType,
          Target[] target)
          throws TargetException, IllegalStateException {
    return new TargetModuleID[0];
  }

  public TargetModuleID[] getAvailableModules(ModuleType moduleType,
          Target[] target)
          throws TargetException, IllegalStateException {
    return new TargetModuleID[0];
  }

  public DeploymentConfiguration createConfiguration(DeployableObject deployableObject)
          throws InvalidModuleException {
    return null;
    /*
    ModuleType type = deployableObject.getType();

    if (type == ModuleType.WAR)
    throw new UnsupportedOperationException("XXX: unimplemented");
    else if (type == ModuleType.EAR)
    throw new UnsupportedOperationException("XXX: unimplemented");
    else if (type == ModuleType.EJB)
    throw new UnsupportedOperationException("XXX: unimplemented");
    else {
    throw new InvalidModuleException(L.l("Unsupported module type ''{0}''", type));
    }
     */
  }

  public ProgressObject distribute(Target[] targets,
          File archive,
          File plan)
          throws IllegalStateException {
    InputStream in = null;
    OutputStream out = null;

    try {
      WarConfiguration warConfiguration = ResinModuleConfigurationFactory.create().find(archive);
      String contextRoot = warConfiguration.getContextRoot();
      String contextDir = contextRoot;
      if ("/".equals(contextDir)) {
        contextDir = "ROOT";
      }

      ResinTarget target = (ResinTarget) targets[0];
      ResinConfiguration resinConfiguration = target.getResinConfiguration();

      String url = "http://"
              + resinConfiguration.getServerAddress()
              + ':'
              + resinConfiguration.getServerPort()
              + contextRoot;


      ResinTargetModuleID moduleId = new ResinTargetModuleID(target,
              contextRoot,
              url,
              archive.getPath());

      File webappDir = resinConfiguration.getWebapps();
      if (!webappDir.isAbsolute()) {
        webappDir = new File(resinConfiguration.getResinRoot(), webappDir.getPath());
      }

      webappDir = new File(webappDir, contextDir);

      JarFile jar = new JarFile(archive.getPath());

      Enumeration<JarEntry> entries = jar.entries();

      int len;
      byte[] buffer = new byte[0xFFFF];

      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();

        if (entry.isDirectory()) {
          final File file = new File(webappDir, name);
          file.mkdirs();
        } else {
          final File file = new File(webappDir, name);
          file.getParentFile().mkdirs();

          in = jar.getInputStream(entry);
          out = new FileOutputStream(file);
          while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
            out.flush();
          }
          out.close();
          in.close();
        }
      }

      return new SuccessProgressObject(targets, moduleId);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ConfigurationException ex) {
      throw new RuntimeException(ex);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ex) {
        }
      }

      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
        }
      }
    }
  }

  public ProgressObject distributeGit(Target[] targets,
          File archive,
          File plan)
          throws IllegalStateException {
    try {
      WarConfiguration warConfiguration = ResinModuleConfigurationFactory.create().find(archive);
      String contextRoot = warConfiguration.getContextRoot();
      ResinTarget target = (ResinTarget) targets[0];
      ResinConfiguration resinConfiguration = target.getResinConfiguration();

      String url = "http://"
              + resinConfiguration.getServerAddress()
              + ':'
              + resinConfiguration.getServerPort()
              + contextRoot;
      ResinTargetModuleID moduleId = new ResinTargetModuleID(target,
              contextRoot,
              url,
              archive.getPath());

      File resinHome = resinConfiguration.getResinHome();
      ResinInstance resin = resinConfiguration.getResinInstance();

      StringBuilder args = new StringBuilder("-jar ");
      args.append('"').append(resinHome.getPath()).append("/lib/resin.jar\"");

      args.append(" -resin-home \"" + resinHome + "\"");
      args.append(" deploy ");
      args.append(" -user ").append(resin.getUser());
      args.append(" -password ").append(resin.getPassword());

      StringBuilder contextBuilder = new StringBuilder();
      for (char c : contextRoot.toCharArray()) {
        if (c == '/') {
        } else if (c == ' ') {
          contextBuilder.append("%20");
        } else {
          contextBuilder.append(c);
        }
      }

      if (contextBuilder.length() == 0) {
        contextBuilder.append("/");
      }

      args.append(" -name ").append(contextBuilder);
      args.append(" \"").append(archive.getPath()).append("\"");

      log.info("deployment arguments: " + args);

      //Console console;

      final String uri = _resinConfiguration.getResinInstance().getUrl();

      String classpath = null;

      String displayName = _resinConfiguration.getDisplayName();

      File javaExe;

      File javaHome = _resinConfiguration.calculateJavaHome();

      javaExe = new File(javaHome, "bin/java");

      if (!javaExe.exists()) {
        javaExe = new File(javaHome, "bin/java.exe");
      }

      if (!javaExe.exists()) {
        throw new IllegalStateException(L.l("Cannot find java exe in ''{0}''",
                javaHome));
      }

      NbProcessDescriptor processDescriptor = new NbProcessDescriptor(javaExe.getAbsolutePath(),
              args.toString(),
              displayName);

      log.log(Level.SEVERE, L.l("Deploying {0} to {1}", archive, contextRoot));

      final Process process = processDescriptor.exec(null,
              new String[]{},
              true,
              resinHome);
      new Echo(process.getInputStream()).start();
      new Echo(process.getErrorStream()).start();

      process.waitFor();

      return new SuccessProgressObject(targets, moduleId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ProgressObject distribute(Target[] target,
          InputStream archive,
          InputStream plan)
          throws IllegalStateException {
    return null;
  }

  public ProgressObject distribute(Target[] target,
          ModuleType type,
          InputStream archive,
          InputStream plan)
          throws IllegalStateException {
    return null;
  }

  public ProgressObject start(TargetModuleID[] targetModuleIDs)
          throws IllegalStateException {
    _runningModules = targetModuleIDs;

    return new SuccessProgressObject(targetModuleIDs);
  }

  public ProgressObject stop(TargetModuleID[] targetModuleIDs)
          throws IllegalStateException {
    _runningModules = new TargetModuleID[0];

    return new SuccessProgressObject();
  }

  public ProgressObject undeploy(TargetModuleID[] targetModuleIDs)
          throws IllegalStateException {
    return new SuccessProgressObject();
  }

  public boolean isRedeploySupported() {
    return false;
  }

  @Override
  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
          File archive,
          File plan) {
    return null;
  }

  @Override
  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
          InputStream archive,
          InputStream plan) {
    return null;
  }

  public void release() {
  }

  public Locale getDefaultLocale() {
    return null;
  }

  public Locale getCurrentLocale() {
    return null;
  }

  public void setLocale(Locale locale)
          throws UnsupportedOperationException {
  }

  public Locale[] getSupportedLocales() {
    return null;
  }

  public boolean isLocaleSupported(Locale locale) {
    return false;
  }

  public DConfigBeanVersionType getDConfigBeanVersion() {
    return null;
  }

  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType dConfigBeanVersionType) {
    return false;
  }

  public void setDConfigBeanVersionSupported(DConfigBeanVersionType version)
          throws DConfigBeanVersionUnsupportedException {
  }

  public void setDConfigBeanVersion(DConfigBeanVersionType dConfigBeanVersionType)
          throws DConfigBeanVersionUnsupportedException {
  }

  public ResinPlatformImpl getJ2eePlatform() {
    /*
    if (_j2eePlatform == null)
    _j2eePlatform = new ResinPlatformImpl(_resinConfiguration);

    return _j2eePlatform;
     */
    return null;
  }

  public String getUri() {
    return _resin.getUrl();
  }

  private static class Echo extends Thread {

    private BufferedReader _in;

    public Echo(InputStream in) {
      _in = new BufferedReader(new InputStreamReader(in));
    }

    public void run() {
      String line;
      try {
        while ((line = _in.readLine()) != null) {
          System.out.println(line);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
