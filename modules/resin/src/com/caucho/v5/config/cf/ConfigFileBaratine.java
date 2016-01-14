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

package com.caucho.v5.config.cf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.NoAspect;
import com.caucho.v5.config.core.ControlConfig;
import com.caucho.v5.config.type.FlowBean;
import com.caucho.v5.config.types.FileSetType;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;

/**
 * Imports properties values from a separate file.
 */
@NoAspect
public class ConfigFileBaratine extends ControlConfig implements FlowBean
{
  private static final L10N L = new L10N(ConfigFileBaratine.class);
  private static final Logger log
    = Logger.getLogger(ConfigFileBaratine.class.getName());

  private final ConfigContext _config;
  
  private PathImpl _path;
  private FileSetType _fileSet;
  private boolean _isOptional;
  private boolean _isRecover;
  private String _mode = "[default]";
  
  public ConfigFileBaratine()
  {
    _config = ConfigContext.getCurrent();
  }

  /**
   * Sets the path for the config file
   */
  public void setPath(PathImpl path)
  {
    if (path == null)
      throw new NullPointerException(L.l("'path' may not be null for 'import'"));
    
    _path = path;
  }

  /**
   * Sets the config fileset.
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
  
  public void setRecover(boolean isRecover)
  {
    _isRecover = isRecover;
  }
  
  public void setMode(String mode)
  {
    if (mode == null || "".equals(mode)) {
      _mode = "[default]";
    }
    else if (mode.startsWith("[")) {
      _mode = mode;
    }
    else {
      _mode = "[" + mode + "]";
    }
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_path == null) {
      if (_fileSet == null)
        throw new ConfigException(L.l("'path' attribute missing from import."));
    }

    ArrayList<PathImpl> paths;

    if (_fileSet != null)
      paths = _fileSet.getPaths();
    else {
      paths = new ArrayList<PathImpl>();
      paths.add(_path);
    }

    for (int i = 0; i < paths.size(); i++) {
      PathImpl path = paths.get(i);

      try {
        log.config(L.l("import '{0}'", path.getNativePath()));

        EnvLoader.addDependency(new Depend(path));
        
        ConfigFileParser parser = new ConfigFileParser(_config);
        parser.parse(path);
      } catch (FileNotFoundException e) {
        if (! _isOptional)
          throw new ConfigException(L.l("Required file '{0}' can not be read for import.",
                                        path.getNativePath()));
        
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }
}

