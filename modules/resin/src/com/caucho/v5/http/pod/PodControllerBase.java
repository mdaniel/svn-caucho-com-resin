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

package com.caucho.v5.http.pod;

import io.baratine.service.Cancel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.pod.NodePodAmp;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.DeployControllerEnvironment;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.deploy.DeployInstanceEnvironment;
import com.caucho.v5.util.Crc64;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.WriteStream;

/**
 * A configuration entry for an pod-application, the services deployed to
 * a pod's node.
 */
public class PodControllerBase<I extends DeployInstanceEnvironment>
  extends DeployControllerEnvironment<I,PodConfigApp>
{
  private static final Logger log
    = Logger.getLogger(PodControllerBase.class.getName());
  private static final L10N L = new L10N(PodControllerBase.class);
  
  private final String _podNodeName;
  private final String _podName;
  private final int _podNodeIndex;
  
  private String _tag;
  
  private PodContainer _podContainer;
  private ArrayList<Cancel> _watchList = new ArrayList<>();
  
  private ArrayList<PathImpl> _archivePaths = new ArrayList<>();
  private ArrayList<PathImpl> _libraryPaths = new ArrayList<>();
  
  private ArrayList<PodAppWeb> _webList = new ArrayList<>();

  private PodBartender _pod;

  PodControllerBase(String id,
                    PathImpl rootDirectory,
                    PodBartender pod,
                    PodContainer podContainer)
  {
    super(id, 
          rootDirectory,
          podContainer.getClassLoader(),
          null,
          null); // podContainer.getDeployContainer());
    
    //String id = handle.getId();
    
    int q = id.lastIndexOf('/');

    _podNodeName = id.substring(q + 1);
    
    int p = _podNodeName.lastIndexOf('.');
    
    if (p > 0) {
      _podName = _podNodeName.substring(0, p);
      
      String podIndex = _podNodeName.substring(p + 1);
      
      if ('0' <= podIndex.charAt(0) && podIndex.charAt(0) <= '9') {
        _podNodeIndex = Integer.parseInt(_podNodeName.substring(p + 1));
      }
      else {
        _podNodeIndex = -1;
      }
    }
    else {
      _podName = _podNodeName;
      _podNodeIndex = 0;
    }
    
    _pod = pod;

    _podContainer = podContainer;
  }
  
  @Override
  public String getArchiveExtension()
  {
    return ".bar";
  }
  
  /*
  protected void initConfig(PodAppController controller,
                            PodContainer container)
  {
    for (PodAppConfig config : container.getPodDefaultList()) {
      controller.addConfigDefault(config);
    }
  }
  */

  public String getPodName()
  {
    return _podName;
  }

  public int getPodNodeIndex()
  {
    return _podNodeIndex;
  }

  public NodePodAmp getPodNode()
  {
    return _pod.getNode(getPodNodeIndex());
  }

  public String getTag()
  {
    return _tag;
  }

  /**
   * Adds an application to the pod.
   */
  /*
  public void addApplication(PodAppConfig appConfig)
  {
    _applications.add(appConfig);
    
    super.addConfigDefault(appConfig);
  }
  
  public Iterable<PodAppConfig> getApplications()
  {
    return _applications;
  }
  */
  /*
  public ArrayList<Path> getApplicationPaths()
  {
    return _archivePaths;
  }
  */

  /*
  public void addLibraryPath(Path path)
  {
    _libraryPaths.add(path);
  }
  */

  public ShutdownModeAmp getShutdownMode()
  {
    return getContainer().getShutdownMode();
  }
  
  /*
  @Override
  public void setConfig(PodConfigApp podConfig)
  {
    super.setConfig(podConfig);
  }
  */
  
  /*
  @Override
  public void updateConfigImpl(Object config)
  {
    PodConfigApp podConfig = (PodConfigApp) config;
    
    addConfigDefault(podConfig);
    
    setModified();
  }
  */

  @Override
  public void addConfigDefault(PodConfigApp podConfig)
  {
    super.addConfigDefault(podConfig);
    
    if (podConfig.getTag() != null) {
      _tag = podConfig.getTag();
    }
    
    _archivePaths.addAll(podConfig.getArchivePaths());
    _libraryPaths.addAll(podConfig.getLibraryPaths());
    
    _webList.addAll(podConfig.getWebList());

    /*
    */
  }
  
  @Override
  protected boolean initWatches()
  {
    if (! super.initWatches()) {
      return false;
    }
    
    for (PathImpl path : _archivePaths) {
      _watchList.add(path.watch(x->onWatch(x)));
    }

    for (PathImpl path : _libraryPaths) {
      _watchList.add(path.watch(x->onWatch(x)));
    }

    // for timing, check for updates that might occur before watches are set
    DeployHandle<?> handle = getHandle();
    
    if (handle != null) {
      handle.alarm();
    }
    
    return true;
  }
  
  @Override
  public boolean destroy()
  {
    boolean result = super.destroy();
    
    for (Cancel handle : _watchList) {
      handle.cancel();
    }
    
    return result;
  }
  
  private void onWatch(String path)
  {
    // log.info("POD-FILE-UPDATE: " + path);
   
    DeployHandle<?> handle = getHandle();
    
    if (handle != null) {
      handle.alarm();
    }
  }
  
  protected DeployHandle<?> getHandle()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public ArrayList<PathImpl> getArchivePaths()
  {
    return _archivePaths;
  }
  
  public ArrayList<PathImpl> getLibraryPaths()
  {
    return _libraryPaths;
  }
  
  public ArrayList<PodAppWeb> getWebList()
  {
    return _webList;
  }
  
  public PodContainer getContainer()
  {
    return _podContainer;
  }

  @Override
  protected void addDependencies()
  {
    super.addDependencies();
    
    for (PathImpl path : getArchivePaths()) {
      addDependency(path);
    }
    
    for (PathImpl path : getLibraryPaths()) {
      addDependency(path);
    }
  }

  public Iterable<Depend> getDependList()
  {
    ArrayList<Depend> dependList = new ArrayList<>();
    
    for (PathImpl path : getArchivePaths()) {
      dependList.add(new Depend(path));
    }
    
    for (PathImpl path : getLibraryPaths()) {
      dependList.add(new Depend(path));
    }
    
    return dependList;
  }
  
  @Override
  public boolean isNameMatch(String key)
  {
    return _podNodeName.equals(key) || super.isNameMatch(key);
  }
  
  /*
  @Override
  protected DeployControllerService<I> createServiceImpl()
  {
    return super.createServiceImpl();
  }
  */
  
  @Override
  public boolean isControllerModified()
  {
    boolean isModified = super.isControllerModified();
    
    return isModified;
  }
  

  @Override
  protected boolean extractFromBartender()
      throws IOException
  {
    PathImpl pwd = getRootDirectory();
    
    String archiveHash = calculateArchiveHash();

    // log.info("EXTRACT-BARTENDER: " + pwd.getNativePath() + " " + archiveHash);
    
    if (archiveHash == null && ! pwd.exists()) {
      return false;
    }
    
    String oldHash = readExpandHash();

    // log.info("EXTRACT-BARTENDER-HASH: " + pwd + " " + archiveHash);
    
    if (archiveHash != null && archiveHash.equals(oldHash)) {
      return false;
    }
    
    if (extractFromBartenderImpl()) {
      writeExpandHash(archiveHash);
    }
    
    return true;
  }
  
  private String readExpandHash()
  {
    PathImpl hashFile = getRootDirectory().lookup("META-INF/baratine.hash");
    
    if (! hashFile.canRead()) {
      return null;
    }
    
    try (ReadStream is = hashFile.openRead()) {
      return is.readLine().trim();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }
  }
  
  private void writeExpandHash(String hash)
  {
    PathImpl hashFile = getRootDirectory().lookup("META-INF/baratine.hash");
    
    try {
      hashFile.getParent().mkdirs();
    
      try (WriteStream os = hashFile.openWrite()) {
        os.println(hash);
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  private String calculateArchiveHash()
  {
    long crc = 0;

    for (PathImpl archivePath : getArchivePaths()) {
      long crcFile = archivePath.getCrc64();
      
      crc = combineCrc(crc, crcFile);
    }    

    for (PathImpl libraryPath : getLibraryPaths()) {
      long crcFile = libraryPath.getCrc64();
  
      crc = combineCrc(crc, crcFile);
    }
    
    if (crc == 0) {
      return null;
    }
    else {
      return Long.toHexString(crc);
    }
  }
  
  private long combineCrc(long crc, long crcFile)
  {
    if (crcFile == 0) {
      return crc;
    }
    else if (crc == 0) {
      return crcFile;
    }
    else {
      return Crc64.generate(crc, crcFile);
    }
  }
  
  /**
   * Extract the contents from the repository into the root directory.
   */
  protected boolean extractFromBartenderImpl()
    throws IOException
  {
    try {
      PathImpl pwd = getRootDirectory();
      
      // log.info("EXTRACT-INFO: " + pwd.getNativePath());

      removeExpandDirectory(pwd);

      pwd.mkdirs();

      // Path archivePath = getArchivePath();

      for (PathImpl archivePath : getArchivePaths()) {
        String tail = archivePath.getTail();
        
        if (tail.endsWith(".bar")) {
          expandArchive(pwd, archivePath);
        }
        else if (tail.endsWith(".jar")) {
          expand(pwd.lookup("lib").lookup(tail), archivePath);
        }
        else {
          expand(pwd, archivePath);
        }
      }

      for (PathImpl libPath : getLibraryPaths()) {
        String tail = libPath.getTail();

        if (tail.endsWith(".bar")) {
          expandArchive(pwd.lookup("libs").lookup(tail), libPath);
        }
        else if (tail.endsWith(".jar")) {
          expand(pwd.lookup("libs").lookup(tail), libPath);
        }
        else if (libPath.isDirectory()) {
          expand(pwd.lookup("libs"), libPath);
        }
        else {
          System.out.println("UNEXP: " + libPath);
        }
      }

      return true;
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.toString(), e);
      throw e;
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  private void expand(PathImpl dst, PathImpl src)
    throws IOException
  {
    if (src.isDirectory()) {
      dst.mkdirs();
      
      for (String name : src.list()) {
        expand(dst.lookup(name), src.lookup(name));
      }
    }
    else if (src.isFile()) {
      dst.getParent().mkdirs();
      
      // log.info("EXPAND-FILE: " + dst + " " + src); //  + " " + src.getLength());
      try (WriteStream out = dst.openWrite()) {
        src.writeToStream(out);
      }
    }
  }
  
  /**
   * Expands from a .bar archive into the destination.
   */
  private void expandArchive(PathImpl dst, PathImpl archivePath)
    throws IOException
  {
    // log.info("PRE-EXPAND: " + archivePath  + " len:" + archivePath.getLength() + " " + Long.toHexString(archivePath.getCrc64()));
    
    try (ReadStream is = archivePath.openRead()) {
      // log.info("READING: " + archivePath + " avail:" + is.available());
      
      try (ZipInputStream in = new ZipInputStream(is)) {
        ZipEntry entry;
        
        while ((entry = in.getNextEntry()) != null) {
          // log.info("ENTRY: " + entry + " " + entry.getName());
          
          if (entry.getName() == null || entry.getName().isEmpty()) {
            continue;
          }
          
          PathImpl path = dst.lookup(entry.getName());

          try {
            if (entry.isDirectory()) {
              path.mkdirs();
            }
            else {
              path.getParent().mkdirs();
            
              try (WriteStream out = path.openWrite()) {
                out.writeStream(in);
              }
            }
          } catch (Exception e) {
            log.warning(L.l("Exception while expanding {0} to {1}\n  {2}", 
                            archivePath, dst.getNativePath(), e));
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
    } catch (Exception e) {
      log.warning(L.l("Archive {0} cannot be read while expanding to {1}\n  {2}",
                      archivePath, dst.getNativePath(), e));
      log.log(Level.FINER, e.toString(), e);
    }
  }
}
