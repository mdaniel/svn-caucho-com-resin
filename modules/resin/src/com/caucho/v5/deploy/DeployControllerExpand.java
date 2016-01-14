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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.FileSetType;
import com.caucho.v5.config.types.PathPatternType;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.io.IoUtil;
import com.caucho.v5.loader.DependencyContainer;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.util.Crc64;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;
import com.caucho.v5.vfs.WriteStream;

/**
 * A deployment entry that expands from an archive (Jar/Zip) file.
 */
abstract public class DeployControllerExpand<I extends DeployInstance>
  extends DeployControllerBase<I>
  implements DeployControllerExpandApi<I>
{
  private static final L10N L = new L10N(DeployControllerExpand.class);
  private static final Logger log
    = Logger.getLogger(DeployControllerExpand.class.getName());
  
  public static final String APPLICATION_HASH_PATH
    = "META-INF/baratine.application-hash";

  private final String _autoDeployStage;
  
  private PathImpl _rootDirectory;
  private PathImpl _archivePath;
  
  private DeployContainerService<I,?> _container;
  
  private boolean _isAllowRepository = false;
  //private Repository _repository;
  //private RepositorySpi _repositorySpi;
  
  private PathImpl _bartenderPath;

  private FileSetType _expandCleanupFileSet;
  
  //private DeployTagItem _deployItem;
  //private DeployListener _deployListener;
  
  private DependencyContainer _depend = new DependencyContainer();
  private long _dependencyCheckInterval = _depend.getCheckInterval();
  
  private Dependency _versionDependency;

  private String _rootHash;

  private Object _applicationExtractLock = new Object();

  // classloader for the manifest entries
  private DynamicClassLoader _manifestLoader;
  private Manifest _manifest;
  private Cancel _bartenderWatch;
  private String _archiveConfig;
  private boolean _isInitWatches;

  protected DeployControllerExpand(String id)
  {
    this(id, null, null, null);
  }

  protected DeployControllerExpand(String id,
                                   ClassLoader loader,
                                   PathImpl rootDirectory,
                                   DeployContainerService<I,?> container)
  {
    super(id, loader);

    if (rootDirectory == null) {
      rootDirectory = VfsOld.getPwd(getParentClassLoader());
    }

    _rootDirectory = rootDirectory;
    _container = container;
    
    _autoDeployStage = "server-" + SystemManager.getCurrentId();
    
    int p = id.indexOf('/');
    
    String type = id.substring(0, p);
    String tail = id.substring(p + 1);
    
    // p = tail.indexOf('/');
    
    if (type.equals("pods")) {
      // cluster/n0.pod service uses cluster/pod for its repository
      //String cluster = tail.substring(0, p);
      String pod = tail; // .substring(p + 1);
      
      p = pod.indexOf('.');
      
      if (p > 0 && Character.isDigit(pod.indexOf(p + 1))) {
        pod = pod.substring(0, p);
        
        tail = pod;
      }
    }
    
    //_bartenderPath = Vfs.lookup("bfs:///system/deploy/" + type + "/" + tail);
    
    String ext = getArchiveExtension();
    
    /*
    if (type.equals("pods")) {
      ext = ".jar";
    }
    */
    
    _bartenderPath = VfsOld.lookup("bfs:///system/" + type + "/" + tail + ext);

    // _bartenderWatch = new BartenderWatch();
  }

  /**
   * Gets the root directory
   */
  public PathImpl getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the root directory
   */
  protected void setRootDirectory(PathImpl rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Gets the archive path.
   */
  public PathImpl getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Sets the archive path.
   */
  public void setArchivePath(PathImpl path)
  {
    _archivePath = path;
  }

  /**
   * Gets the archive extension.
   */
  public String getArchiveExtension()
  {
    return ".jar";
  }

  /**
   * Sets the archive extension.
   */
  /*
  public void setArchiveExtension(String path)
  {
    _archiveExtension = path;
  }
  */

  /**
   * Sets the archive config file.
   */
  public void setArchiveConfig(String path)
  {
    _archiveConfig = path;
  }

  /**
   * Gets the archive config file.
   */
  public String getArchiveConfig()
  {
    return _archiveConfig;
  }
  
  public void setAllowRepository(boolean isAllowRepository)
  {
    _isAllowRepository = isAllowRepository;
  }
  
  public boolean isAllowRepository()
  {
    return _isAllowRepository;
  }

  /**
   * Returns the repository
   */
  /*
  public Repository getRepository()
  {
    return _repository;
  }
  */

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

  @Override
  public void addParentExpandCleanupFileSet(FileSetType fileSet)
  {
    if (_expandCleanupFileSet == null)
      _expandCleanupFileSet = fileSet;
    else
      _expandCleanupFileSet.add(fileSet);
  }

  /**
   * Sets the archive auto-remove file set.
   */
  @Configurable
  public void addExpandCleanupFileSet(PathPatternType include)
  {
    if (_expandCleanupFileSet == null)
      _expandCleanupFileSet = new FileSetType();

    _expandCleanupFileSet.addInclude(include);
  }

  /**
   * Sets the archive auto-remove file set.
   */
  @Configurable
  public void addExpandPreserveFileset(PathPatternType exclude)
  {
    if (_expandCleanupFileSet == null)
      _expandCleanupFileSet = new FileSetType();
    
    _expandCleanupFileSet.addExclude(exclude);
  }

  public String getAutoDeployStage()
  {
    return _autoDeployStage;
  }
  
  public void setDependencyCheckInterval(long period)
  {
    _dependencyCheckInterval = period;
    _depend.setCheckInterval(period);
  }
  
  /**
   * Final calls for init.
   */
  @Override
  protected void initEnd()
  {
    super.initEnd();
    
    _rootHash = readRootHash();
  }

  /**
   * Merges with the new controller.
   */
  @Override
  public void merge(DeployController<I> newControllerV)
  {
    super.merge(newControllerV);

    DeployControllerExpand<I> newController;
    newController = (DeployControllerExpand<I>) newControllerV;

    if (newController._expandCleanupFileSet != null)
      _expandCleanupFileSet = newController._expandCleanupFileSet;

    if (newController.getArchivePath() != null) {
      setArchivePath(newController.getArchivePath());
    }

    /*
    if (newController.getArchiveExtension() != null) {
      setArchiveExtension(newController.getArchiveExtension());
    }
    */
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
      initDependencies();
      
      extractApplication();
    } catch (Exception e) {
      // XXX: better exception
      throw new RuntimeException(e);
    }
  }

  /**
   * Deploys the controller
   */
  public void undeploy()
  {
    undeployImpl();
  }
  
  /**
   * Deploys the controller
   */
  protected void undeployImpl()
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " undeploying");
    }
    
    try {
      removeExpandDirectory();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void preConfigureInstance(DeployInstanceBuilder<I> deployInstance)
    throws Exception
  {
    extractApplication();
    
    //addManifestClassPath();
    
    super.preConfigureInstance(deployInstance);
  }
  
  @Override
  protected void configureInstance(DeployInstanceBuilder<I> deployInstance)
    throws Exception
  {
    super.configureInstance(deployInstance);
  }

  @Override
  protected void postConfigureInstance(DeployInstanceBuilder<I> deployInstance)
    throws Exception
  {
    super.postConfigureInstance(deployInstance);
    
    // initDependencies();
  }

  /**
   * Extract an application from the repository.
   */
  protected void extractApplication()
    throws IOException
  {
    //importToRepository();
    
    synchronized (_applicationExtractLock) {
      boolean isExtract = extractFromBartender();
      
      if (! isExtract) {
        isExtract = extractFromArchive();
      }

      postExtract(isExtract);
    }
  }
  
  /**
   * Called after the application is extracted from the repository.
   */
  protected void postExtract(boolean isExtract)
    throws IOException
  {
    PathImpl path = getRootDirectory().lookup("META-INF/MANIFEST.MF");
    if (path.canRead()) {
      ReadStream is = path.openRead();
      
      try {
        _manifest = new Manifest(is);
      } catch (IOException e) {
        log.warning(L.l("{0} Manifest file cannot be read for '{1}'.\n  {2}",
                        this, getRootDirectory(), e));

        log.log(Level.FINE, e.toString(), e);
      } finally {
        is.close();
      }
    }
  }

  /**
   * Adds any class path from the manifest.
   */
  /*
  protected void addManifestClassPath()
    throws IOException
  {
    DynamicClassLoader loader = EnvLoader.getDynamicClassLoader();

    if (loader == null)
      return;

    Manifest manifest = getManifest();

    if (manifest == null)
      return;

    Attributes main = manifest.getMainAttributes();

    if (main == null)
      return;

    String classPath = main.getValue("Class-Path");

    PathImpl pwd = null;

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
  */

  public String getAutoDeployTag()
  {
    return (getAutoDeployStage() + "/" + getIdType() + "/" + getIdKey());
  }

  /**
   * Extract the contents from the repository into the root directory.
   */
  protected boolean extractFromBartender()
    throws IOException
  {
    try {
      if (! isBartenderArchive()) {
        return false;
      }
      
      String hash = getDigest();

      if (hash != null && hash.equals(_rootHash)) {
        return false;
      }
      
      PathImpl pwd = getRootDirectory();

      removeExpandDirectory(pwd);

      pwd.mkdirs();
      
      if (log.isLoggable(Level.FINE)) {
        log.fine("extract repository " + _bartenderPath + " (" + this + ")"
                 + "\n  root-dir=" + getRootDirectory());
      }

      // _repositorySpi.expandToPath(treeHash, pwd);
      
      extractBartender(pwd);

      writeRootHash(hash);
      
      _rootHash = hash;

      return true;
    } catch (ConfigException e) {
      throw e;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  protected boolean isBartenderArchive()
  {
    return _bartenderPath != null && _bartenderPath.canRead();
  }
    
  protected void extractBartender(PathImpl rootDir)
    throws IOException
  {
    String fileName = _bartenderPath.getTail();

    expandToPath(_bartenderPath, rootDir, fileName);
  }
    

  /**
   * Extract the contents from the repository into the root directory.
   */
  private boolean extractFromArchive()
    throws IOException
  {
    try {
      PathImpl archivePath = getArchivePath();
      
      if (archivePath == null || ! archivePath.canRead()) {
        return false;
      }
      
      String hash = Long.toHexString(archivePath.getCrc64());
      
      if (hash != null && hash.equals(_rootHash)) {
        return false;
      }
      
      PathImpl pwd = getRootDirectory();

      removeExpandDirectory(pwd);

      pwd.mkdirs();

      expandZipToPath(archivePath, pwd);

      writeRootHash(hash);
      
      _rootHash = hash;

      return true;
    } catch (ConfigException e) {
      throw e;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  private String getDigest()
  {
    return Long.toHexString(calculateDigest());
  }
  
  protected long calculateDigest()
  {
    try {
      long crc64 = _bartenderPath.getCrc64();

      String []list = _bartenderPath.list();
      Arrays.sort(list);

      for (String fileName : list) {
        crc64 = Crc64.generate(crc64, fileName);

        PathImpl path = _bartenderPath.lookup(fileName);

        crc64 = Crc64.generate(crc64, path.getCrc64());
      }

      return crc64;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return 0;
    }
  }
  
  private  void expandToPath(PathImpl src, PathImpl dst, String fileName)
    throws IOException
  {
    //Path srcPath = src.lookup(fileName);
    PathImpl srcPath = src;
    
    if (fileName.endsWith(getArchiveExtension())
        && ! ".jar".equals(getArchiveExtension())) {
      expandZipToPath(srcPath, dst);
    }
    else if (srcPath.isFile()) {
      dst.lookup(fileName).getParent().mkdirs();
      
      try (WriteStream os = dst.lookup(fileName).openWrite()) {
        try (ReadStream is = srcPath.openRead()) {
          os.writeStream(is);
        }
      }
    }
    else if (srcPath.isDirectory()) {
      PathImpl dstPath = dst.lookup(fileName);
      
      for (String subFile : srcPath.list()) {
        expandToPath(srcPath, dstPath, subFile);
      }
    }
  }
  
  protected void expandZipToPath(PathImpl src, PathImpl dst)
    throws IOException
  {
    try (ReadStream is = src.openRead()) {
      try (ZipInputStream zis = new ZipInputStream(is)) {
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
          String name = entry.getName();

          if (entry.isDirectory()) {
            continue;
          }

          PathImpl subPath = dst.lookup(name);
          subPath.getParent().mkdirs();

          try (WriteStream os = subPath.openWrite()) {
            os.writeStream(zis);
          }

          zis.closeEntry();
        }
      }
    }
  }

  private boolean isBartenderHashMatch(PathImpl path)
  {
    /*
    String tag = getId();
    String treeHash = _repositorySpi.getTagContentHash(tag);
  
    Path archivePath = getArchivePath();

    if (treeHash != null && archivePath != null && archivePath.canRead()) {
      throw new ConfigException(L.l("{0} cannot be deployed from both an archive {1} and cluster deployment.",
                                    this, archivePath.getNativePath()));
    }

    if (treeHash == null) {
      tag = getAutoDeployTag();

      treeHash = _repositorySpi.getTagContentHash(tag);
    }
  
    if (treeHash == null) {
      return false;
    }
  
    if (treeHash.equals(_rootHash)) {
      return false;
   }
    */
    
    return false;
  }

  /**
   * Reads the saved application root hash which is stored in META-INF, so
   * the application is not extracted twice.
   */
  private String readRootHash()
  {
    PathImpl path = _rootDirectory.lookup(APPLICATION_HASH_PATH);
    
    ReadStream is = null;
    try {
      is = path.openRead();
      
      String rootHash = is.readLine();
      
      return rootHash;
    } catch (FileNotFoundException e) {
      log.log(Level.ALL, e.toString(), e);
      
      return null;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    } finally {
      IoUtil.close(is);
    }
  }
  

  /**
   * Saves the saved application root hash which is stored in META-INF, so
   * the application is not extracted twice.
   */
  private void writeRootHash(String hash)
    throws IOException
  {
    PathImpl path = _rootDirectory.lookup(APPLICATION_HASH_PATH);

    WriteStream os = null;
    
    try {
      path.getParent().mkdirs();
      
      os = path.openWrite();
      
      os.println(hash);
    } finally {
      IoUtil.close(os);
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
    return _depend.isModifiedNow();
  }
  
  @Override
  protected boolean controllerLogModified(Logger log)
  {
    return _depend.logModified(log);
  }

  protected final void initDependencies()
  {
    initWatches();
    
    _depend = new DependencyContainer();
    _depend.setCheckInterval(_dependencyCheckInterval);
    
    addDependencies();
  }
  
  protected boolean initWatches()
  {
    if (_isInitWatches) {
      return false;
    }
    
    _isInitWatches = true;

    /*
    if (_bartenderPath != null) {
      _bartenderWatch = _bartenderPath.watch(new BartenderWatch());
    }
    */
    
    return true;
  }
  
  protected void setModified()
  {
    if (_depend != null) {
      _depend.setModified(true);;
    }
  }

  protected void addDependencies()
  {
    if (getArchivePath() != null) {
      addDependency(getArchivePath());
    }
    
    if (_bartenderPath != null) {
      addDependency(_bartenderPath);
    }
  }
  
  protected void addDependency(PathImpl path)
  {
    addDependency(new Depend(path));
  }
  
  protected void addDependency(Depend depend)
  {
    _depend.add(depend);
  }
  
  public Dependency getVersionDependency()
  {
    return _versionDependency;
  }
  
  public void setVersionDependency(Dependency versionDependency)
  {
    _versionDependency = versionDependency;
  }

  protected void removeExpandDirectory()
  {
    PathImpl pwd = getRootDirectory();

    if (pwd.isDirectory()) {
      removeExpandDirectory(pwd);
    }
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param path root directory to start removal
   */
  protected void removeExpandDirectory(PathImpl path)
  {
    String prefix = path.getPath();

    if (! prefix.endsWith("/")) {
      prefix = prefix + "/";
    }
    
    removeExpandDirectory(path, prefix);
  }
  
  @Override
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
  protected void removeExpandDirectory(PathImpl path, String prefix)
  {
    try {
      if (path.isDirectory() && ! path.isLink()) {
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
  protected void removeExpandFile(PathImpl path, String prefix)
    throws IOException
  {
    if (_expandCleanupFileSet == null
        || _expandCleanupFileSet.isMatch(path, prefix)) {
      path.remove();
    }
  }
  
  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    
    /*
    if (_deployItem != null) {
      _deployItem.removeNotificationListener(_deployListener);
    }
    */
    
    _bartenderWatch.cancel();
  }
  
  @Override
  protected void onRemove()
  {
    super.onRemove();
    
    String tag = getId();
    
    // server/6b0e, server/1h03
    /*
    String treeHash = _repositorySpi.getTagContentHash(tag);
    String rootHash = _rootHash;
    
    if (treeHash == null && rootHash != null) {
      undeploy();
    }
    */
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

    DeployControllerBase<?> controller = (DeployControllerBase<?>) o;

    // XXX: s/b getRootDirectory?
    return getId().equals(controller.getId());
  }
  
  /*
  private class BartenderWatch implements Watch {
    @Override
    public void onUpdate(String path)
    {
      alarm();
    }
  }
  */
  
  /*
  private static class DeployListener implements DeployNotificationListener
  {
    private WeakReference<DeployContainerService<?,?>> _container;
    private String _tag;
    
    DeployListener(DeployContainerService<?,?> container, String tag)
    {
      _container = new WeakReference<DeployContainerService<?,?>>(container);
      _tag = tag;
      
      // 
    }

    @Override
    public void onStart()
    {
      DeployContainerService<?,?> container = _container.get();
      
      if (container != null) {
        DeployController<?> controller = container.findControllerById(_tag);
        
        if (controller != null) {
          controller.start();
        }
      }
    }

    @Override
    public void onStop()
    {
      DeployContainerService<?,?> container = _container.get();
      
      if (container != null) {
        DeployController<?> controller = container.findControllerById(_tag);
        
        if (controller != null)
          controller.stop();
      }
    }
  }
  */
}
