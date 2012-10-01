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
 * @author Scott Ferguson
 */

package com.caucho.log;

import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.config.types.RawString;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Configuration for the <log-handler> tag.
 */
@Configurable
public class LogHandlerConfig extends BeanConfig {
  private static final L10N L = new L10N(LogHandlerConfig.class);

  private Level _level;
  
  private Formatter _formatter;
  
  private Filter _filter;

  private Handler _handler;
  
  private String _timestamp = "[%Y/%m/%d %H:%M:%S.%s] {%{thread}} ";
  private PathHandler _pathHandler;
  
  private boolean _isSkipInit;

  public LogHandlerConfig()
  {
    setBeanConfigClass(Handler.class);
  }

  public LogHandlerConfig(boolean isSkipInit)
  {
    this();
    
    _isSkipInit = isSkipInit;
  }
  
  @Override
  protected String getDefaultScope()
  {
    // env/02s7
    return null;
  }
  
  @Override
  protected String getCdiNamed()
  {
    // env/02s7
    return null;
  }

  /**
   * Sets the name of the logger to configure.
   *
   * @deprecated Use setName()
   */
  public void setId(String name)
  {
    // backward compat
    if (name.equals("/"))
      name = "";
    
    setName(name);
  }

  /**
   * Sets the path
   */
  public void setPath(Path path)
  {
    if (_pathHandler == null)
      _pathHandler = new PathHandler();
    
    _pathHandler.setPath(path);
  }

  /**
   * Sets the path-format
   */
  public void setPathFormat(String pathFormat)
  {
    if (_pathHandler == null)
      _pathHandler = new PathHandler();

    _pathHandler.setPathFormat(pathFormat);
  }

  /**
   * Sets the archive-format
   */
  public void setArchiveFormat(String archiveFormat)
  {
    if (_pathHandler == null)
      _pathHandler = new PathHandler();

    _pathHandler.setArchiveFormat(archiveFormat);
  }

  /**
   * Sets the rollover-period
   */
  public void setRolloverPeriod(Period rolloverPeriod)
  {
    if (_pathHandler == null)
      _pathHandler = new PathHandler();

    _pathHandler.setRolloverPeriod(rolloverPeriod);
  }

  /**
   * Sets the rollover-size
   */
  public void setRolloverSize(Bytes size)
  {
    if (_pathHandler == null)
      _pathHandler = new PathHandler();

    _pathHandler.setRolloverSize(size);
  }

  /**
   * Sets the rollover-count
   */
  public void setRolloverCount(int count)
  {
    if (_pathHandler == null)
      _pathHandler = new PathHandler();

    _pathHandler.setRolloverCount(count);
  }

  /**
   * Sets the use-parent-handlers
   */
  public void setUseParentHandlers(boolean useParentHandlers)
    throws ConfigException
  {
  }

  /**
   * Sets the output level.
   */
  public void setLevel(Level level)
    throws ConfigException
  {
    _level = level;
  }

  /**
   * Sets the output level.
   */
  public String getLevel()
  {
    if (_level != null)
      return _level.getName();
    else
      return Level.ALL.getName();
  }

  /**
   * Sets the timestamp.
   */
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }

  /**
   * A format string uses EL expressions and the EL variable `log', which is an
   * instance of LogRecord.
   */
  public void setFormat(RawString format)
  {
    if (_formatter == null) {
      _formatter = new ELFormatter();
    }

    if (_formatter instanceof ELFormatter)
      ((ELFormatter)_formatter).setFormat(format);
  }

  /**
   * A format string uses EL expressions and the EL variable `log', which is an
   * instance of LogRecord.
   */
  public String getFormat()
  {
    if (_formatter != null && _formatter instanceof ELFormatter)
      return ((ELFormatter)_formatter).getFormat();
    else
      return null;
  }

  /**
   * Sets the formatter.
   */
  public void setFormatter(Formatter formatter)
  {
    _formatter = formatter;
  }

  /**
   * Sets the filter.
   */
  public void setFilter(Filter filter)
  {
    _filter = filter;
  }

  /**
   * Returns the tag name.
   */
  @Override
  public String getTagName()
  {
    return "log-handler";
  }
  
  @Configurable
  public void add(Handler handler)
  {
    _handler = handler;
  }

  /**
   * Initialize the log-handler
   */
  @PostConstruct
  @Override
  public void init()
  {
    if (! _isSkipInit)
      initImpl();
  }
  
  /**
   * Should be run with system classloader
   */
  public void initImpl()
    throws ConfigException
  {
    if (_handler != null) {
    }
    else if (_pathHandler != null) {
    }
    else {
      super.init();

      _handler = (Handler) getObject();
    }

    if (_formatter instanceof ELFormatter) {
      ((ELFormatter)_formatter).init();
    }

    if (_timestamp != null) {
      if (_pathHandler != null) {
        _pathHandler.setTimestamp(_timestamp);
      }
      else if (_formatter == null) {
        TimestampFormatter formatter = new TimestampFormatter();
        _formatter = formatter;
        formatter.setTimestamp(_timestamp);
      }
    }
    
    if (_pathHandler != null) {
      _pathHandler.init();

      _handler = _pathHandler;
    }
    
    if (_handler == null) {
      throw new ConfigException(L.l("<log-handler> requires a configured log handler"));
    }
    
    if (getName() == null) {
      throw new ConfigException(L.l("<log-handler> requires a 'name' attribute"));
    }
      
    Logger logger = Logger.getLogger(getName());

    if (! (logger instanceof EnvironmentLogger)) {
      CloseListener listener = new CloseListener(_handler);

      Environment.addClassLoaderListener(listener);
    }
    
    // env/02s9
    if (_level != null)
      _handler.setLevel(_level);

    /* JDK defaults to Level.ALL
    if (_level != null)
      _handler.setLevel(_level);
    else
      _handler.setLevel(Level.INFO);
    */

    if (_formatter != null)
      _handler.setFormatter(_formatter);

    if (_filter != null)
      _handler.setFilter(_filter);

    logger.addHandler(_handler);
  }

  static Level toLevel(String level)
    throws ConfigException
  {
    try {
      return Level.parse(level.toUpperCase());
    } catch (Exception e) {
      throw new ConfigException(L.l("'{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging",
                                    level));
    }
  }
}
