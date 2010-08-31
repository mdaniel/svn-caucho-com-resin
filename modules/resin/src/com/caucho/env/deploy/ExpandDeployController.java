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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.env.deploy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.caucho.config.types.FileSetType;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryService;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.env.repository.RepositoryTagListener;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.make.DependencyContainer;
import com.caucho.server.deploy.RepositoryDependency;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * A deployment entry that expands from an archive (Jar/Zip) file.
 */
abstract public class ExpandDeployController<I extends DeployInstance>
  extends DeployController<I>
  implements RepositoryTagListener {
  private static final L10N L = new L10N(ExpandDeployController.class);
  private static final Logger log
    = Logger.getLogger(ExpandDeployController.class.getName());

  private final String _autoDeployTag;
  
  private Path _rootDirectory;
  private Path _archivePath;

  private Repository _repository;

  private FileSetType _expandCleanupFileSet;
  
  private DeployTagItem _deployItem;
  
  private DependencyContainer _depend = new DependencyContainer();

  private Object _archiveExpandLock = new Object();

  // classloader for the manifest entries
  private DynamicClassLoader _manifestLoader;
  private Manifest _manifest;

  protected ExpandDeployController(String id)
  {
    this(id, null, null);
  }

  protected ExpandDeployController(String id,
                                   ClassLoader loader,
                                   Path rootDirectory)
  {
    super(id, loader);

    if (rootDirectory == null)
      rootDirectory = Vfs.getPwd(getParentClassLoader());

    _rootDirectory = rootDirectory;
    
    _autoDeployTag = "server/" + ResinSystem.getCurrentId() + "/" + id; 
  }

  /**
   * Gets the root directory
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the root directory
   */
  protected void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Gets the archive path.
   */
  public Path getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Sets the archive path.
   */
  public void setArchivePath(Path path)
  {
    _archivePath = path;
  }

  /**
   * Returns the repository
   */
  public Repository getRepository()
  {
    return _repository;
  }

  /**
   * Returns the manifest.
   */
  public Manifest getManifest()
  {
    return _manifest;
  }

  /**
   * Returns the manifest as an attribute map
   */
  public Map<String,String> getManifestAttributes()
  {
    if (_manifest == null)
      return null;
    
    Map<String,String> map = new TreeMap<String,String>();

    Attributes attr = _manifest.getMainAttributes();

    if (attr != null) {
      for (Map.Entry<Object,Object> entry : attr.entrySet()) {
        map.put(String.valueOf(entry.getKey()),
                String.valueOf(entry.getValue()));
      }
    }

    return map;
  }

  /**
   * Sets the archive auto-remove file set.
   */
  public void setExpandCleanupFileSet(FileSetType fileSet)
  {
    _expandCleanupFileSet = fileSet;
  }

  public String getAutoDeployTag()
  {
    return _autoDeployTag;
  }
  
  /**
   * Final calls for init.
   */
  @Override
  protected void initEnd()
  {
    super.initEnd();
    
    RepositoryService repositoryService = RepositoryService.create(); 
    _repository = repositoryService.getRepository();
    _repository.addListener(getId(), this);
    
    DeployUpdateService deployService = DeployUpdateService.create();

    deployService.addTag(getId());
    _deployItem = deployService.getTagItem(getId());
  }

  /**
   * Merges with the old controller.
   */
  @Override
  protected void mergeController(DeployController<I> oldControllerV)
  {
    super.mergeController(oldControllerV);

    ExpandDeployController<I> oldController;
    oldController = (ExpandDeployController<I>) oldControllerV;

    if (oldController._expandCleanupFileSet != null)
      _expandCleanupFileSet = oldController._expandCleanupFileSet;

    if (oldController.getArchivePath() != null)
      setArchivePath(oldController.getArchivePath());
  }

  /**
   * Deploys the controller
   */
  public void deploy()
  {
    deployImpl();
  }
  
  /**
   * Deploys the controller
   */
  protected void deployImpl()
    {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " deploying");
    
    try {
      expandArchive();
    } catch (Exception e) {
      // XXX: better exception
      throw new RuntimeException(e);
    }
  }
  
  @Override
  protected void configureInstance(I deployInstance)
    throws Exception
  {
    expandArchive();
    
    addManifestClassPath();
    
    super.configureInstance(deployInstance);
    
    addDependencies();
  }
  
  @Override
  public void onTagChange(String tag)
  {
    alarm();
  }

  /**
   * Expand an archive file.  The _archiveExpandLock must be obtained
   * before the expansion.
   */
  private void expandArchive()
    throws IOException
  {
    // save any .war file to the server-specific repository
    for (int i = 0; ! commitArchive() && i < 3; i++) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
    }
    
    synchronized (_archiveExpandLock) {
      expandRepository();

      Path path = getRootDirectory().lookup("META-INF/MANIFEST.MF");
      if (path.canRead()) {
        ReadStream is = path.openRead();
        try {
          _manifest = new Manifest(is);
        } catch (IOException e) {
          log.warning(L.l("Manifest file cannot be read for '{0}'.\n{1}",
                          getRootDirectory(), e));

          log.log(Level.FINE, e.toString(), e);
        } finally {
          is.close();
        }
      }
    }
  }

  /**
   * Adds any class path from the manifest.
   */
  protected void addManifestClassPath()
    throws IOException
  {
    DynamicClassLoader loader = Environment.getDynamicClassLoader();
    if (loader == null)
      return;

    Manifest manifest = getManifest();

    if (manifest == null)
      return;

    Attributes main = manifest.getMainAttributes();

    if (main == null)
      return;

    String classPath = main.getValue("Class-Path");

    Path pwd = null;

    if (getArchivePath() != null)
      pwd = getArchivePath().getParent();
    else
      pwd = getRootDirectory();

    if (classPath == null) {
    }
    else if (_manifestLoader != null)
      _manifestLoader.addManifestClassPath(classPath, pwd);
    else
      loader.addManifestClassPath(classPath, pwd);
  }

  /**
   * Adds any updated .war file to the server-specific repository. The war
   * will be expanded as part of the usual repository system.
   *
   * The commitArchive() can return false if the war update fails, for example
   * if the war is in the process of updating.
   */
  private boolean commitArchive()
    throws IOException
  {
    Path archivePath = getArchivePath();

    if (archivePath == null)
      return true;

    if (! archivePath.canRead())
      return true;
    
    String hash = Long.toHexString(archivePath.getCrc64());
    
    if (log.isLoggable(Level.FINE)){
      log.fine(this + " updating .war repository for " + archivePath);
    }

    try {
      HashMap<String,String> props = new HashMap<String,String>();
      
      props.put("archive-digest", hash);
      
      _repository.putTagArchive(_autoDeployTag, 
                                archivePath,
                                ".war auto-update", 
                                props);
      
      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    }
  }

  /**
   * Expand an archive.  The _archiveExpandLock must be obtained before the
   * expansion.
   */
  private boolean expandRepository()
    throws IOException
  {
    try {
      if (_repository == null)
        return false;
      
      String tag = getId();
      String treeHash = _repository.getTagContentHash(tag);

      if (treeHash == null) {
        tag = _autoDeployTag;

        treeHash = _repository.getTagContentHash(tag);
      }
      
      if (treeHash == null)
        return false;
      
      Path pwd = getRootDirectory();

      pwd.mkdirs();

      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " expanding repository tag=" + tag
                 + "\n  root=" + getRootDirectory()
                 + "\n  contentHash=" + treeHash);
      }

      _repository.expandToPath(treeHash, pwd);

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  @Override
  protected boolean isControllerModified()
  {
    return _depend.isModified();
  }
  
  @Override
  protected boolean isControllerModifiedNow()
  {
    return _depend.isModified();
  }
  
  @Override
  protected boolean controllerLogModified(Logger log)
  {
    return _depend.logModified(log);
  }

  protected void addDependencies()
  {
    _depend = new DependencyContainer();
    
    if (getArchivePath() != null)
      _depend.add(new Depend(getArchivePath()));

    String value = getRepository().getTagContentHash(getId());
    _depend.add(new RepositoryDependency(getId(), value));
    
    value = getRepository().getTagContentHash(_autoDeployTag);
    _depend.add(new RepositoryDependency(_autoDeployTag, value));
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param path root directory to start removal
   */
  protected void removeExpandDirectory(Path path)
  {
    String prefix = path.getPath();

    if (! prefix.endsWith("/"))
      prefix = prefix + "/";

    removeExpandDirectory(path, prefix);
  }
  
  public Throwable getConfigException()
  {
    return null;
  }
 
  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param dir root directory to start removal
   */
  protected void removeExpandDirectory(Path path, String prefix)
  {
    try {
      if (path.isDirectory()) {
        String []list = path.list();
        for (int i = 0; list != null && i < list.length; i++) {
          removeExpandDirectory(path.lookup(list[i]), prefix);
        }
      }

      removeExpandFile(path, prefix);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Removes an expanded file.
   */
  protected void removeExpandFile(Path path, String prefix)
    throws IOException
  {
    if (_expandCleanupFileSet == null
        || _expandCleanupFileSet.isMatch(path, prefix)) {
      path.remove();
    }
  }
  
  //
  // state callbacks
  //
  
  @Override
  protected void onActive()
  {
    super.onActive();
    
    if (_deployItem != null && ! "error".equals(_deployItem.getState()))
      _deployItem.toStart();
  }
  
  @Override
  protected void onError(Throwable e)
  {
    super.onError(e);
    
    if (_deployItem != null)
      _deployItem.toError(e);
  }
  
  @Override
  protected void onStop()
  {
    super.onStop();
    
    if (_deployItem != null)
      _deployItem.toStop();
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    return getId().hashCode();
  }

  /**
   * Returns equality.
   */
  @Override
  public boolean equals(Object o)
  {
    // server/125g
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DeployController<?> controller = (DeployController<?>) o;

    // XXX: s/b getRootDirectory?
    return getId().equals(controller.getId());
  }
}
