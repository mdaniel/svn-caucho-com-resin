/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package com.caucho.v5.config.core;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.NoAspect;
import com.caucho.v5.config.type.FlowBean;
import com.caucho.v5.config.types.FileSetType;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;

/**
 * Imports values from a separate file.
 */
@NoAspect
public class ImportConfig extends ControlConfig implements FlowBean
{
  private static final L10N L = new L10N(ImportConfig.class);
  private static final Logger log
    = Logger.getLogger(ImportConfig.class.getName());

  private PathImpl _path;
  private FileSetType _fileSet;
  private boolean _isOptional;
  private boolean _isRecover;
  
  public ImportConfig()
  {
  }

  /**
   * Sets the import path.
   */
  @ConfigArg(0)
  public void setPath(PathImpl path)
  {
    if (path == null) {
      throw new NullPointerException(L.l("'path' may not be null for 'import'"));
    }
    
    _path = path;
  }

  /**
   * Sets the import fileset.
   */
  public void setFileset(FileSetType fileSet)
  {
    _fileSet = fileSet;
  }
  
  /**
   * Sets true if the path is optional.
   */
  public void setOptional(boolean optional)
  {
    _isOptional = optional;
  }
  
  /**
   * Sets true if the import should recover from errors.
   */
  public void setRecover(boolean isRecover)
  {
    _isRecover = isRecover;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    try {
      initImpl();
    } catch (Exception e) {
      throw e;
    }
  }
  
  private void initImpl()
    throws Exception
  {
    if (_path == null) {
      if (_isOptional) {
        return;
      }
      else if (_fileSet == null) {
        throw new ConfigException(L.l("'path' attribute missing from 'import'."));
      }
    }
    else if (_path.canRead() && ! _path.isDirectory()) {
    }
    else if (_isOptional && ! _path.exists()) {
      log.finest(L.l("'import' '{0}' is not readable.", _path));

      EnvLoader.addDependency(new Depend(_path));
      return;
    }
    else {
      throw new ConfigException(L.l("Required file '{0}' can not be read for 'import'.",
                                    _path.getNativePath()));
    }
    
    Object object = getObject();

    ArrayList<PathImpl> paths;

    if (_fileSet != null) {
      paths = _fileSet.getPaths();
      
      for (PathImpl root : _fileSet.getRoots()) {
        EnvLoader.addDependency(new Depend(root));
      }
    }
    else {
      paths = new ArrayList<PathImpl>();
      paths.add(_path);
    }

    for (int i = 0; i < paths.size(); i++) {
      PathImpl path = paths.get(i);
      
      if (path.isDirectory()) {
        continue;
      }

      log.config(L.l("import '{0}'", path.getNativePath()));

      EnvLoader.addDependency(new Depend(path));
      
      //String recoverAttr = RecoverableProgram.ATTR;
      //Object oldRecover = Config.getCurrentVar(recoverAttr);

      try {
        //Config.setProperty(recoverAttr, _isRecover);
        
        configure(object, path);
      } catch (RuntimeException e) {
        if (! _isRecover)
          throw e;

        log.log(Level.WARNING, e.toString(), e);
        EnvLoader.setConfigException(e);
      } finally {
        //Config.setProperty(recoverAttr, oldRecover);
      }
    }
  }
  
  protected void configure(Object bean, PathImpl path)
  {
    ConfigContext config = new ConfigContext();
    
    config.configure2(bean, path);
  }
}

