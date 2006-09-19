/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.server.deploy;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.annotation.*;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.WeakAlarm;
import com.caucho.util.AlarmListener;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.loader.Environment;

import com.caucho.config.ConfigException;

import com.caucho.config.types.FileSetType;
import com.caucho.config.types.Period;

import com.caucho.make.Dependency;

import com.caucho.lifecycle.Lifecycle;

/**
 * The generator for the deploy
 */
abstract public class ExpandDeployGenerator<E extends ExpandDeployController> extends DeployGenerator<E>
  implements AlarmListener {
  private static final Logger log = Log.open(ExpandDeployGenerator.class);
  private static final L10N L = new L10N(ExpandDeployGenerator.class);

  private static final long MIN_CRON_INTERVAL = 5000L;

  private Path _path; // default path
  
  private Path _archiveDirectory;
  private Path _expandDirectory;

  private String _extension = ".jar";
  
  private String _expandPrefix = "";
  private String _expandSuffix = "";
  
  private ArrayList<String> _requireFiles = new ArrayList<String>();

  private TreeSet<String> _controllerNames = new TreeSet<String>();

  private FileSetType _expandCleanupFileSet;

  private Alarm _alarm;
  private long _cronInterval;

  private volatile long _lastCheckTime;
  private volatile boolean _isChecking;
  private long _checkInterval = 1000L;
  private long _digest;
  private volatile boolean _isModified;
  private volatile boolean _isDeploying;

  private final Lifecycle _lifecycle = new Lifecycle();
  
  /**
   * Creates the deploy.
   */
  public ExpandDeployGenerator(DeployContainer<E> container)
  {
    super(container);

    _alarm = new WeakAlarm(this);

    _cronInterval = Environment.getDependencyCheckInterval();
    if (_cronInterval < MIN_CRON_INTERVAL)
      _cronInterval = MIN_CRON_INTERVAL;
  }

  /**
   * Gets the default path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the deploy directory.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandPath(Path path)
  {
    log.config("Use <expand-directory> instead of <expand-path>.  <expand-path> is deprecated.");

    setExpandDirectory(path);
  }

  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandDirectory(Path path)
  {
    _expandDirectory = path;
  }

  /**
   * Gets the war expand directory.
   */
  public Path getExpandDirectory()
  {
    if (_expandDirectory != null)
      return _expandDirectory;
    else
      return _path;
  }

  /**
   * Sets the war expand dir to check for new archive files.
   */
  public void setArchiveDirectory(Path path)
  {
    _archiveDirectory = path;
  }

  /**
   * Gets the war expand directory.
   */
  public Path getArchiveDirectory()
  {
    if (_archiveDirectory != null)
      return _archiveDirectory;
    else
      return _path;
  }

  /**
   * Sets the dependency check interval.
   */
  public void setDependencyCheckInterval(Period period)
  {
    _cronInterval = period.getPeriod();

    if (_cronInterval < 0)
      _cronInterval = Period.INFINITE;
    else if (_cronInterval < MIN_CRON_INTERVAL)
      _cronInterval = MIN_CRON_INTERVAL;
  }

  /**
   * Sets the expand remove file set.
   */
  public void setExpandCleanupFileset(FileSetType fileSet)
  {
    _expandCleanupFileSet = fileSet;
  }

  /**
   * Sets the extension.
   */
  public void setExtension(String extension)
    throws ConfigException
  {
    if (! extension.startsWith("."))
      throw new ConfigException(L.l("deployment extension '{0}' must begin with '.'",
				    extension));

    _extension = extension;
  }

  /**
   * Returns the extension.
   */
  public String getExtension()
  {
    return _extension;
  }

  /**
   * Sets the expand prefix to check for new applications.
   */
  public void setExpandPrefix(String prefix)
    throws ConfigException
  {
    if (! prefix.equals("") &&
	! prefix.startsWith("_") &&
	! prefix.startsWith("."))
      throw new ConfigException(L.l("expand-prefix '{0}' must start with '.' or '_'.",
				    prefix));
			       
    _expandPrefix = prefix;
  }

  /**
   * Gets the expand prefix.
   */
  public String getExpandPrefix()
  {
    return _expandPrefix;
  }

  /**
   * Sets the expand suffix to check for new applications.
   */
  public void setExpandSuffix(String suffix)
    throws ConfigException
  {
    _expandSuffix = suffix;
  }

  /**
   * Gets the expand suffix.
   */
  public String getExpandSuffix()
  {
    return _expandSuffix;
  }

  /**
   * Adds a required file in the expansion.
   */
  public void addRequireFile(String file)
    throws ConfigException
  {
    _requireFiles.add(file);
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns true if the deployment has modified.
   */
  public boolean isModified()
  {
    synchronized (this) {
      long now = Alarm.getCurrentTime();
      
      if (now < _lastCheckTime + _checkInterval || _isChecking) {
	return _isModified;
      }

      _isChecking = true;
      _lastCheckTime = Alarm.getCurrentTime();
    }

    try {
      long digest = getDigest();

      _isModified = _digest != digest;

      return _isModified;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    } finally {
      _isChecking = false;
    }
  }

  /**
   * Configuration checks on init.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (! _lifecycle.toInit())
      return;

    if (getExpandDirectory() == null)
      throw new ConfigException(L.l("<expand-directory> must be specified for deployment of archive expansion."));
    
    if (getArchiveDirectory() == null)
      throw new ConfigException(L.l("<archive-directory> must be specified for deployment of archive expansion."));
  }

  /**
   * Starts the deploy.
   */
  public void start()
  {
    try {
      init();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    if (! _lifecycle.toActive())
      return;

    log.finer(this + " starting");
    
    handleAlarm(_alarm);
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
    if (isModified()) {
      try {
	deploy();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    for (String name : _controllerNames) {
      keys.add(name);
    }
  }

  /**
   * Return true for a matching key.
   */
  protected boolean isDeployedKey(String key)
  {
    return _controllerNames.contains(key);
  }

  /**
   * Forces an update.
   */
  public void update()
  {
    // force modify check
    _lastCheckTime = 0;

    request();
  }
  

  /**
   * Redeploys if modified.
   */
  public void request()
  {
    if (isModified()) {
      try {
	deploy();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Deploys the objects.
   */
  private void deploy()
    throws Exception
  {
    boolean isDeploying = false;
    
    log.finer(this + " redeploy " + _isDeploying);

    try {
      ArrayList<String> updatedNames = null;

      synchronized (this) {
	if (_isDeploying)
	  return;
	else {
	  _isDeploying = true;
	  isDeploying = true;
	}
	  
	TreeSet<String> entryNames = findEntryNames();

	_digest = getDigest();

	if (! _controllerNames.equals(entryNames)) {
	  updatedNames = new ArrayList<String>();
	  
	  for (String name : _controllerNames) {
	    if (! entryNames.contains(name))
	      updatedNames.add(name);
	  }

	  for (String name : entryNames) {
	    if (! _controllerNames.contains(name))
	      updatedNames.add(name);
	  }

	  _controllerNames = entryNames;
	}
      }

      for (int i = 0; updatedNames != null && i < updatedNames.size(); i++) {
	String name = updatedNames.get(i);

	getDeployContainer().update(name);
      }
    } finally {
      if (isDeploying) {
	_isModified = false;
	_isDeploying = false;
      }
    }
  }

  /**
   * Finds the matching entry.
   */
  public E generateController(String name)
  {
    request();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getParentClassLoader());
	
      E controller = createController(name);

      if (controller != null) {
	controller.setExpandCleanupFileSet(_expandCleanupFileSet);

	_controllerNames.add(name); // server/1d19
      }

      return controller;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the digest of the expand and archive directories.
   */
  private long getDigest()
  {
    long archiveDigest = 0;
    
    Path archiveDirectory = getArchiveDirectory();
    if (archiveDirectory != null)
      archiveDigest = archiveDirectory.getCrc64();
    
    long expandDigest = 0;
    
    Path expandDirectory = getExpandDirectory();
    if (expandDirectory != null)
      expandDigest = expandDirectory.getCrc64();

    return archiveDigest * 65521 + expandDigest;
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  private TreeSet<String> findEntryNames()
    throws IOException
  {
    TreeSet<String> entryNames = new TreeSet<String>();

    Path archiveDirectory = getArchiveDirectory();
    Path expandDirectory = getExpandDirectory();

    if (archiveDirectory == null || expandDirectory == null)
      return entryNames;

    String []entryList = archiveDirectory.list();

    // collect all the new entrys
    loop:
    for (int i = 0; i < entryList.length; i++) {
      String archiveName = entryList[i];

      Path archivePath = archiveDirectory.lookup(archiveName);

      String entryName = null;
      
      if (! archivePath.canRead())
        continue;
      else
	entryName = archiveNameToEntryName(archiveName);

      if (entryName != null)
	entryNames.add(entryName);
    }
    
    String []entryExpandList = expandDirectory.list();

    // collect all the new war expand directories
    loop:
    for (int i = 0; i < entryExpandList.length; i++) {
      String pathName = entryExpandList[i];

      /* XXX: this used to be needed to solve issues with NT
      if (CauchoSystem.isCaseInsensitive())
        pathName = pathName.toLowerCase();
      */

      Path rootDirectory = expandDirectory.lookup(pathName);
      
      String entryName = pathNameToEntryName(pathName);

      if (entryName == null)
	continue;
      else if (entryName.endsWith(getExtension()))
	continue;

      if (! isValidDirectory(rootDirectory, pathName))
	continue;

      if (! entryNames.contains(entryName))
	entryNames.add(entryName);
    }

    return entryNames;
  }

  protected boolean isValidDirectory(Path rootDirectory, String pathName)
  {
    
    if (! rootDirectory.isDirectory() || pathName.startsWith(".")) {
      return false;
    }

    if (pathName.equalsIgnoreCase("web-inf") ||
	pathName.equalsIgnoreCase("meta-inf"))
      return false;

    for (int j = 0; j < _requireFiles.size(); j++) {
      String file = _requireFiles.get(j);

      if (! rootDirectory.lookup(file).canRead())
	return false;
    }

    return true;
  }

  /**
   * Converts the expand-path name to the entry name, returns null if
   * the path name is not a valid entry name.
   */
  protected String pathNameToEntryName(String name)
  {
    if (_expandPrefix == null) {
    }
    else if (_expandPrefix.equals("") &&
	     (name.startsWith("_") ||
	      name.startsWith(".") ||
	      name.equalsIgnoreCase("META-INF") ||
	      name.equalsIgnoreCase("WEB-INF"))) {
      return null;
    }
    else if (name.startsWith(_expandPrefix)) {
      name = name.substring(_expandPrefix.length());
    }
    else
      return null;

    if (_expandSuffix == null)
      return name;
    else if (name.endsWith(_expandSuffix))
      return name.substring(0, name.length() - _expandSuffix.length());
    else
      return null;
  }

  /**
   * Converts the archive name to the entry name, returns null if
   * the path name is not a valid entry name.
   */
  protected String archiveNameToEntryName(String archiveName)
  {
    if (! archiveName.endsWith(_extension))
      return null;
    else {
      int sublen = archiveName.length() - _extension.length();
      return pathNameToEntryName(archiveName.substring(0, sublen));
    }
  }

  /**
   * Creates a new entry.
   */
  abstract protected E createController(String name);

  /**
   * Checks for updates.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;
    
    try {
      request();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.queue(_cronInterval);
    }
  }

  /**
   * Stops the deploy.
   */
  public void stop()
  {
    _lifecycle.toStop();
    
    _alarm.dequeue();
  }

  /**
   * Destroys the deploy.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;
    
    _lifecycle.toDestroy();
  }

  /**
   * Tests for equality.
   */
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    ExpandDeployGenerator deploy = (ExpandDeployGenerator) o;

    Path expandDirectory = getExpandDirectory();
    Path deployExpandDirectory = deploy.getExpandDirectory();
    
    if (expandDirectory != deployExpandDirectory &&
	(expandDirectory == null ||
	 ! expandDirectory.equals(deployExpandDirectory)))
      return false;

    return true;
  }

  public String toString()
  {
    String name = getClass().getName();
    int p = name.lastIndexOf('.');
    if (p > 0)
      name = name.substring(p + 1);
    
    return name + "[" + getExpandDirectory() + "]";
  }
}
