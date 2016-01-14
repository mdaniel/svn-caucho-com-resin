/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.deploy;

import io.baratine.files.Watch;
import io.baratine.service.Cancel;
import io.baratine.service.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.FileSetType;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.deploy2.DeployMode;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.WeakAlarm;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * The generator for the deploy
 */
abstract public class DeployGeneratorExpand<I extends DeployInstance,
                                            M extends DeployControllerExpandApi<I>>
  extends DeployGenerator<I,M>
  implements AlarmListener, DeployUpdateListener
{
  private static final Logger log
    = Logger.getLogger(DeployGeneratorExpand.class.getName());
  private static final L10N L = new L10N(DeployGeneratorExpand.class);

  private static final long MIN_CRON_INTERVAL = 5000L;

  private final String _id;
  
  private PathImpl _path; // default path
  private ClassLoader _loader;

  private PathImpl _containerRootDirectory;
  private PathImpl _archiveDirectory;
  private PathImpl _expandDirectory;
  
  private PathImpl _bartenderPath;
  private Cancel _bartenderWatch;
  
  //private final DeploySystem _deploySystem;
  
  private String _extension = ".jar";
  
  private String _expandPrefix = "";
  private String _expandSuffix = "";
  
  private String _pathSuffix;

  // private boolean _isVersioning;
  private boolean _isAutoCreate = true;
  
  private ArrayList<String> _requireFiles = new ArrayList<String>();

  private FileSetType _expandCleanupFileSet;
  
  private ExpandManagerDirectory _directoryManager;
  private ExpandManagerArchive _archiveManager;
  
  private Alarm _alarm;
  private long _cronInterval;

  //
  // runtime values
  //
  
  private ExpandManager _expandManager;
  private Set<String> _deployedKeys = new TreeSet<String>();
  // private Set<String> _versionKeys = new TreeSet<String>();
  
  private long _lastCheckTime;
  private AtomicBoolean _isChecking = new AtomicBoolean();
  private long _checkInterval = 1000L;
  private volatile boolean _isModified;
  private AtomicBoolean _isDeploying = new AtomicBoolean();
  private final AtomicBoolean _isInit = new AtomicBoolean();
  private ExpandManagerBartender _bartenderManager;

  /**
   * Creates the deploy.
   */
  public DeployGeneratorExpand(String id,
                               DeployContainerService<I,M> container,
                               PathImpl containerRootDirectory)
  {
    super(container);
    
    _id = id;

    _containerRootDirectory = containerRootDirectory;

    _alarm = new WeakAlarm(this);

    _checkInterval = EnvLoader.getDependencyCheckInterval();
    
    _cronInterval = Math.max(EnvLoader.getDependencyCheckInterval(),
                             MIN_CRON_INTERVAL);

    _loader = Thread.currentThread().getContextClassLoader();
    
    //_deploySystem = DeploySystem.getCurrent();
    //_deploySystem.addUpdateListener(this);
    
    //_repository = RepositorySystem.getCurrentRepository();
    //_repository.addListener(id, this);
    
    /*
    String type = id.substring(0, p);
    String tail = id.substring(p + 1);
    
    int p = id.indexOf('/');
    
    if (p > 0) {
      type = id.substring(0, p);
      tail = id.substring(p + 1);
    }
    else {
      type = id;
      tail = "";
    }
    */
    
    _bartenderPath = lookupBartender(id);
    //_bartenderWatch = new BartenderWatch();
    
    // _deploySystem.addWatch(_bartenderPath, _bartenderWatch);
    
    // _bartenderPath.addWatch(_bartenderWatch);
    
    container.addWatch(_bartenderPath, new BartenderWatch(),
                       Result.onOk(h->{ _bartenderWatch = h; }));
  }
  
  protected PathImpl lookupBartender(String id)
  {
    return VfsOld.lookup("bfs:///system/" + id);
  }
  
  public String getId()
  {
    return _id;
  }

  PathImpl getContainerRootDirectory()
  {
    return _containerRootDirectory;
  }

  /**
   * Sets the war expand dir to check for new archive files.
   */
  public void setArchiveDirectory(PathImpl path)
  {
    _archiveDirectory = path;
  }

  /**
   * Gets the war expand directory.
   */
  public PathImpl getArchiveDirectory()
  {
    if (_archiveDirectory != null)
      return _archiveDirectory;
    else
      return _path;
  }

  /**
   * Returns the location for deploying an archive with the specified name.
   *
   * @param name a name, without an extension
   */
  public PathImpl getArchivePath(String name)
  {
    return getArchiveDirectory().lookup(name + getExtension());
  }
  
  protected PathImpl getBartenderPath()
  {
    return _bartenderPath;
  }
  
  /**
   * true if the webapps directory should be an elastic suffix.
   */
  public String getPathSuffix()
  {
    return _pathSuffix;
  }
  
  /**
   * true if the webapps directory should be an elastic suffix.
   */
  public void setPathSuffix(String pathSuffix)
  {
    _pathSuffix = pathSuffix;
  }

  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandPath(PathImpl path)
  {
    log.config("Use <expand-directory> instead of <expand-path>.  <expand-path> is deprecated.");

    setExpandDirectory(path);
  }

  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandDirectory(PathImpl path)
  {
    _expandDirectory = path;
  }

  /**
   * Gets the war expand directory.
   */
  public PathImpl getExpandDirectory()
  {
    if (_expandDirectory != null)
      return _expandDirectory;
    else
      return _path;
  }
  
  public boolean isAutoCreate()
  {
    return _isAutoCreate;
  }
  
  public void setAutoCreate(boolean isAutoCreate)
  {
    _isAutoCreate = isAutoCreate;
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

  public long getDependencyCheckInterval()
  {
    return _cronInterval;
  }

  @Configurable
  public void addExpandCleanupFileset(FileSetType include)
  {
    if (_expandCleanupFileSet == null) {
      _expandCleanupFileSet = new FileSetType();
    }

    _expandCleanupFileSet.add(include);
  }

  @Configurable
  public void addExpandPreserveFileset(FileSetType exclude)
  {
    if (_expandCleanupFileSet == null) {
      _expandCleanupFileSet = new FileSetType();
    }

    _expandCleanupFileSet.addInverse(exclude);
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
    if (! prefix.equals("")
        && ! prefix.startsWith("_")
        && ! prefix.startsWith("."))
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

  public void setEntryNamePrefix(String entryNamePrefix)
  {
    // _entryNamePrefix = entryNamePrefix;
  }

  /**
   * Gets the default path.
   */
  public PathImpl getPath()
  {
    return _path;
  }

  /**
   * Sets the deploy directory.
   */
  @ConfigArg(0)
  public void setPath(PathImpl path)
  {
    _path = path;
  }
  
  /**
   * Adds a required file in the expansion.
   */
  public void addRequireFile(String file)
    throws ConfigException
  {
    _requireFiles.add(file);
  }
  
  protected ArrayList<String> getRequireFiles()
  {
    return _requireFiles;
  }

  /**
   * Sets true to enable versioning
   */
  public void setVersioning(boolean isVersioning)
  {
    //_isVersioning = isVersioning;
  }

  /**
   * Sets true to enable versioning
   */
  public void setMultiversionRouting(boolean isVersioning)
  {
    //_isVersioning = isVersioning;
  }

  /**
   * Sets true to enable versioning
   */
  public boolean isVersioning()
  {
    // return _isVersioning;
    return false;
  }

  /**
   * Returns the log.
   */
  @Override
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Configuration checks on init.
   */
  @Override
  protected void initImpl()
    throws ConfigException
  {
    if (_isInit.getAndSet(true)) {
      return;
    }
    
    super.initImpl();
    
    if (_pathSuffix != null && ! "".equals(_pathSuffix)) {
      String tail = _path.getTail();
      
      tail += "-" + _pathSuffix;
      
      _path = _path.getParent().lookup(tail);
    }

    if (getExpandDirectory() == null) {
      throw new ConfigException(L.l("<expand-directory> must be specified for deployment of archive expansion."));
    }

    if (getArchiveDirectory() == null) {
      throw new ConfigException(L.l("<archive-directory> must be specified for deployment of archive expansion."));
    }
    
    if (isAutoCreate()) {
      try {
        getExpandDirectory().mkdirs();
      } catch (Exception e) {
        log.info(e.toString());
        
        if (log.isLoggable(Level.FINEST)) {
          log.log(Level.FINEST, e.toString(), e);
        }
      }
    }
    
    _directoryManager = createExpandManagerDirectory();
    
    _archiveManager = createExpandManagerArchive();
    
    _bartenderManager = createExpandManagerBartender();
    
    // _repositoryManager = new ExpandRepositoryManager(id);
  }
  
  protected ExpandManagerDirectory createExpandManagerDirectory()
  {
    return new ExpandManagerDirectory(getId(), 
                                      getExpandDirectory(),
                                      getExpandPrefix(),
                                      getExpandSuffix(),
                                      _requireFiles);
  }
  
  protected ExpandManagerArchive createExpandManagerArchive()
  {
    return new ExpandManagerArchive(getId(), 
                                    getArchiveDirectory(),
                                    getExtension());
  }
  
  protected ExpandManagerBartender createExpandManagerBartender()
  {
    return new ExpandManagerBartender(getId(), _bartenderPath); 
  }

  /**
   * Starts the deploy.
   */
  @Override
  protected void startImpl()
  {
    super.startImpl();
    
    deploy();
    
    handleAlarm(_alarm);
  }

  /**
   * Returns the location of an expanded archive, or null if no archive with
   * the passed name is deployed.
   *
   * @param key a name, without an extension
   */
  public PathImpl getExpandPath(String key)
  {
    if (! isDeployedKey(key)) {
      return null;
    }
    
    String pathName = getExpandPrefix() + key + getExpandSuffix();
    
    PathImpl path = getExpandDirectory().lookup(pathName);
    
    return path;
  }

  /**
   * Returns true if the deployment has modified.
   */
  @Override
  public boolean isModified()
  {
    if (! _isChecking.compareAndSet(false, true)) {
      return _isModified;
    }

    try {
      long now = CurrentTime.getCurrentTime();

      if (now < _lastCheckTime + _checkInterval) {
        return _isModified;
      }

      _lastCheckTime = CurrentTime.getCurrentTime();
      
      if (DeployMode.MANUAL.equals(getRedeployMode())) {
        return false;
      }

      if (_expandManager != null) {
        _isModified = _expandManager.isModified();
        // _digest = _expandManager.getDigest();
      }
      else {
        _isModified = true;
      }
      
      return _isModified;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    } finally {
      _isChecking.set(false);
    }
  }

  /**
   * Log the reason for modification
   */
  @Override
  public boolean logModified(Logger log)
  {
    ExpandManager expandManager = _expandManager;
    
    if (expandManager != null) {
      return expandManager.logModified(log);
    }
    else {
      return false;
    }
  }

  /**
   * Returns the deployed keys.
   */
  @Override
  protected void fillDeployedNames(Set<String> names)
  {
    updateIfModified();
    
    for (String key : _deployedKeys) {
      String name = keyToName(key);
      
      if (name != null) {
        names.add(name);
      }
    }
  }

  /**
   * Return true for a matching key.
   */
  protected boolean isDeployedKey(String key)
  {
    if (key == null) {
      return false;
    }
    else if (_deployedKeys.contains(key)) {
      return true;
    }
    else if (_expandManager.getKeySet().contains(key)) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Creates a new entry.
   */
  abstract protected M createController(String key);

  protected String keyToName(String key)
  {
    return key;
  }

  protected String nameToKey(String name)
  {
    return name;
  }
  
  /**
   * Redeploys if modified.
   */
  @Override
  public void updateIfModified()
  {
    if (isModified()) {
      //log.log(Level.WARNING, "DeployGeneratorExpand isModified (true) ");
      update();
    } else {
      //log.log(Level.WARNING, "DeployGeneratorExpand isModified (false) ");
    }
  }

  
  /**
   * Redeploys if modified.
   */
  public void updateIfModifiedNow()
  {
    _lastCheckTime = 0;
    
    if (isModified()) {
      update();
    }
  }

  /**
   * Deploys the objects.
   */
  @Override
  public final void update()
  {
    if (! _isDeploying.compareAndSet(false, true)) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);
      
      beforeUpdate();
      
      deploy();
      
      /*
      if (! oldVersion.equals(newVersion)) {
        afterUpdate();
      }
      */
    } finally {
      thread.setContextClassLoader(oldLoader);
      
      _isDeploying.set(false);
    }
  }
  
  protected void beforeUpdate()
  {
    
  }
  
  protected void afterUpdate()
  {
    
  }
  
  private void deploy()
  {
    try {
      if (_directoryManager == null) {
        return;
      }
      
      _expandManager = new ExpandManager(getId(),
                                         _directoryManager,
                                         _archiveManager,
                                         _bartenderManager);
      
      _deployedKeys = _expandManager.getBaseKeySet();
            
      _isModified = false;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /*
  public ExpandVersion getPrimaryVersion(String key)
  {
    return _expandManager.getPrimaryVersion(key);
  }
  */

  /**
   * Finds the matching entry.
   */
  @Override
  public final void generateController(String name, ArrayList<M> controllerList)
  {
    updateIfModifiedNow();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      String key = nameToKey(name);

      /*
      ExpandVersion version = _expandManager.getPrimaryVersion(key);
      
      if (version == null) {
        version = _expandManager.getVersion(key);
      }
      
      if (version == null) {
        return;
      }
      */
      
      if (_expandManager == null) {
        return;
      }
      
      Set<String> keys = _expandManager.getKeySet();
      
      if (! keys.contains(key)) {
        return;
      }
      
      M controller = createController(key);

      if (controller != null) {
        controller.addParentExpandCleanupFileSet(_expandCleanupFileSet);
        controllerList.add(controller);

        // _controllerNames.add(name); // server/1d19
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  //
  // DeployNetworkService callbacks
  //
  
  @Override
  public void onUpdate(String tag)
  {
    update();
  }

  public String[] getNames()
  {
    Set<String> deployedKeys = _deployedKeys;
    
    String[] names = new String[deployedKeys.size()];

    int i = 0;

    for (String key : deployedKeys) {
      names[i++] = key;
    }

    return names;
  }

  private String getNamesAsString()
  {
    StringBuilder builder = new StringBuilder();

    for (String name : _deployedKeys) {
      if (builder.length() > 0)
        builder.append(", ");

      builder.append(name);
    }

    builder.insert(0, '[');
    builder.append(']');

    return builder.toString();
  }
  
  /**
   * Deploy the archive.
   */
  public boolean deploy(String key)
  {
    update();
    
    //M controller = getDeployContainer().findController(keyToName(key));
    DeployHandle<I> handle = getDeployContainer().findHandle(keyToName(key));

    if (handle == null) {
      if (log.isLoggable(Level.FINE))
        log.finer(L.l("{0} can't deploy '{1}' because it's not a known controller: {2}",
                      this, key, getNamesAsString()));

      return false;
    }
    
    return true;
  }

  /**
   * Start the archive.
   */
  public boolean start(String name)
  {
    // M controller = getDeployContainer().findController(keyToName(name));

    DeployHandle<I> handle = getDeployContainer().findHandle(keyToName(name));

    if (handle == null) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("{0} unknown name '{1}' in start", this, name));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("{0} known names are {1} in start", this, getNamesAsString()));

      return false;
    }

    handle.start();
    
    return true;
  }

  /**
   * Returns an exception for the named archive or null if there is no exception
   */
  public Throwable getConfigException(String name)
  {
    //M controller = getDeployContainer().findController(keyToName(name));
    
    DeployHandle<I> handle = getDeployContainer().findHandle(keyToName(name));

    if (handle == null) {
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("unknown name '{0}'", name));

      if (log.isLoggable(Level.FINER))
        log.finer(L.l("known names are {0}", getNamesAsString()));

      return new ConfigException(L.l("unknown name '{0}'", name));
    }

    //return controller.getConfigException();
    return handle.getConfigException();
  }
  /**
   * Stop the archive.
   */
  public boolean stop(String name)
  {
    //M controller = getDeployContainer().findController(keyToName(name));
    DeployHandle<I> handle = getDeployContainer().findHandle(keyToName(name));

    if (handle == null) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("unknown name '{0}'", name));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("known names are {0}", getNamesAsString()));

      return false;
    }

    handle.stop(ShutdownModeAmp.GRACEFUL);
    
    return true;
  }

  /**
   * Undeploy the archive.
   */
  public boolean undeploy(String name)
  {
    //M controller = getDeployContainer().findController(keyToName(name));
    DeployHandle<I> handle = getDeployContainer().findHandle(keyToName(name));

    if (handle == null) {
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("unknown name '{0}'", name));

      if (log.isLoggable(Level.FINER))
        log.finer(L.l("known names are {0}", getNamesAsString()));

      return false;
    }

    PathImpl archivePath = getArchivePath(name);
    PathImpl expandPath = getExpandPath(name);

    handle.stop(ShutdownModeAmp.GRACEFUL);

    try {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(L.l("deleting {0}", archivePath));
      }

      archivePath.removeAll();
    }
    catch (IOException ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, ex.toString(), ex);
    }

    try {
      if (expandPath != null) {
        if (log.isLoggable(Level.FINEST))
          log.finest(L.l("deleting {0}", expandPath));

        expandPath.removeAll();
      }
    }
    catch (IOException ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, ex.toString(), ex);
    }

    getDeployContainer().update(keyToName(name));

    return true;
  }

  /*
  @Override
  public void onTagChange(String tag)
  {
    _lastCheckTime = 0;

    alarm();
  }
  */


  /**
   * Checks for updates.
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    if (isDestroyed()) {
      return;
    }
    
    try {
      alarm();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.runAfter(_cronInterval);
    }
  }
  
  private void alarm()
  {
    // XXX: tck, but no QA test
    // server/10ka
    if (DeployMode.AUTOMATIC.equals(getRedeployMode()) && isActive()) {
      // updateIfModified();
      getDeployContainer().update();
    }
  }

  /**
   * Stops the deploy.
   */
  @Override
  protected void stopImpl()
  {
    _alarm.dequeue();
    
    /*
    if (_deploySystem != null) {
      _deploySystem.removeUpdateListener(this);
    }
    */
    
    if (_bartenderWatch != null) {
      _bartenderWatch.cancel();
    }
    // _bartenderPath.removeWatch(_bartenderWatch);

    super.stopImpl();
  }

  /**
   * Tests for equality.
   */
  @Override
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DeployGeneratorExpand<?,?> deploy = (DeployGeneratorExpand<?,?>) o;

    PathImpl expandDirectory = getExpandDirectory();
    PathImpl deployExpandDirectory = deploy.getExpandDirectory();

    if (expandDirectory != deployExpandDirectory &&
        (expandDirectory == null ||
         ! expandDirectory.equals(deployExpandDirectory)))
      return false;

    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getExpandDirectory() + "]";
  }
  
  class BartenderWatch implements Watch {
    @Override
    public void onUpdate(String path)
    {
      _lastCheckTime = 0;

      alarm();
    }
  }
  
  /*
  class VersionDependency implements Dependency {
    private Set<String> _oldVersionKeys;
    
    VersionDependency()
    {
      _versionKeys = _oldVersionKeys;
    }
    
    @Override
    public boolean isModified()
    {
      return ! _versionKeys.equals(_oldVersionKeys);
    }
    
    @Override
    public boolean logModified(Logger log)
    {
      if (! _versionKeys.equals(_oldVersionKeys)) {
        log.info(DeployGeneratorExpand.this + " version is modified");
        return true;
      }
      
      return false;
    }
    
  }
  */
}
