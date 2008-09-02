/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.repository;

import com.caucho.config.ConfigException;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.server.resin.Resin;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The module repository holds the module jars for osgi and ivy.
 */
public class ModuleRepository
{
  private static final Logger log
    = Logger.getLogger(ModuleRepository.class.getName());
  private static final L10N L = new L10N(ModuleRepository.class);
  
  private static final EnvironmentLocal<ModuleRepository> _currentRepository
    = new EnvironmentLocal<ModuleRepository>();
    
  private Path _root;

  private ArrayList<Resolver> _resolverList = new ArrayList<Resolver>();

  /**
   * The module repository is created once by the Management class.
   */
  public ModuleRepository()
  {
    _currentRepository.set(this);
  }

  public void setPath(Path root)
  {
    _root = root;
  }

  private Path getRoot()
  {
    if (_root == null)
      _root = Resin.getCurrent().getManagement().getPath().lookup("ivy");

    return _root;
  }

  public void addResolver(ResolverConfig resolverConfig)
  {
    _resolverList.add(resolverConfig.getResolver());
  }

  public void init()
  {
    try {
      getRoot().mkdirs();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the current repository.
   */
  public static ModuleRepository getCurrent()
  {
    return _currentRepository.get();
  }

  /**
   * Returns the path to the named artifact if it exists.
   */
  public Path findArtifact(String org,
			   String module,
			   String artifact,
			   String rev,
			   String ext)
  {
    if (module == null)
      throw new NullPointerException(L.l("'module' is required in findArtifact"));
    
    if (artifact == null)
      artifact = module;
    
    Path path = findArtifactInCache(org, module, artifact, rev, ext);

    if (path != null)
      return path;

    ModuleNotFoundException exn = null;

    for (Resolver resolver : _resolverList) {
      DataSource source = null;
      try {
	source = resolver.resolveArtifact(org, module, artifact, rev, ext);
      } catch (ModuleNotFoundException e) {
	log.log(Level.FINEST, e.toString(), e);
	
	exn = e;
      }

      if (source != null) {
	return fillCache(org, module, artifact, rev, ext, source);
      }
    }

    if (exn != null)
      throw exn;

    return null;
  }

  private Path fillCache(String org,
			 String module,
			 String artifact,
			 String rev,
			 String ext,
			 DataSource dataSource)
  {
    try {
      Path path = getRoot().lookup(org);
      path = path.lookup(module);
      path = path.lookup(ext + "s");
      path.mkdirs();

      path = path.lookup(artifact + "_" + rev + "." + ext);

      WriteStream os = path.openWrite();
      try {
	InputStream is = dataSource.openInputStream();

	os.writeStream(is);

	is.close();
      } finally {
	os.close();
      }

      return path;
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    } finally {
      dataSource.close();
    }
  }

  private Path findArtifactInCache(String org,
				   String module,
				   String artifact,
				   String rev,
				   String ext)
  {
    Path path = getRoot().lookup(org);

    if (! (path.isDirectory() && path.canRead()))
      return null;

    path = path.lookup(module);

    if (! (path.isDirectory() && path.canRead()))
      return null;

    path = path.lookup(ext + "s");

    if (! (path.isDirectory() && path.canRead()))
      return null;

    if (rev == null) {
      ArrayList<String> revList = findRevList(path, artifact + "_", "." + ext);

      if (revList == null || revList.size() == 0)
	return null;

      rev = revList.get(0);
    }

    String name = artifact + "_" + rev + "." + ext;

    path = path.lookup(name);

    if (! (path.canRead() && path.isFile()))
      return null;
    
    return path;
  }

  private Path findArtifactInCacheValidate(String org,
					   String module,
					   String artifact,
					   String rev,
				   String ext)
  {
    Path path = getRoot().lookup(org);

    if (! (path.isDirectory() && path.canRead()))
      throw new ConfigException(L.l("org='{0}' is an unknown module organization.",
				    org));

    path = path.lookup(module);

    if (! (path.isDirectory() && path.canRead()))
      throw new ConfigException(L.l("org={0},module={1} is an unknown module.",
				    org, module));

    path = path.lookup(ext + "s");

    if (! (path.isDirectory() && path.canRead()))
      throw new ConfigException(L.l("org={0},module={1} does not have any {2}s.",
				    org, module, ext));

    if (rev == null) {
      ArrayList<String> revList = findRevList(path, artifact + "_", "." + ext);

      if (revList == null || revList.size() == 0) {
	throw new ConfigException(L.l("org={0}, module={1} has no valid {2}s version.",
				    org, module, ext));
      }

      rev = revList.get(0);
    }

    String name = artifact + "_" + rev + "." + ext;

    path = path.lookup(name);

    if (! (path.canRead() && path.isFile())) {
      throw new ConfigException(L.l("org={0}, module={1}, rev={2} is an unknown {3} version.",
				    org, module, rev, ext));
    }
    
    return path;
  }

  private ArrayList<String> findRevList(Path path,
					String prefix,
					String suffix)
  {
    ArrayList<String> revList = new ArrayList<String>();
    
    try {
      for (String name : path.list()) {
	if (name.startsWith(prefix) && name.endsWith(suffix)) {
	  int len = name.length() - prefix.length();
	  
	  String rev = name.substring(prefix.length(), len);

	  revList.add(rev);
	}
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    return revList;
  }
}
