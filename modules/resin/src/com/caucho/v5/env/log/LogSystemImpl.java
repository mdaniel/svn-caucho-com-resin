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

package com.caucho.v5.env.log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.io.StreamImpl;
import com.caucho.v5.management.server.LogMessage;
import com.caucho.v5.network.NetworkSystemBartender;
import com.caucho.v5.subsystem.RootDirectorySystem;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.WeakAlarm;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.TempStream;
import com.caucho.v5.vfs.WriteStream;

/**
 * The LogService records log entries in a persistent database for
 * administrative debugging.
 */
@InlineConfig
public class LogSystemImpl extends LogSystem
{
  private static long EXPIRE_TIMEOUT = 14L * 24 * 3600 * 1000;
  
  private static final HashMap<String, Integer> LEVEL_MAP
    = new HashMap<String, Integer>();
  
  private NameDatabase _nameDatabase;
  private LogDatabase _logDatabase;
  
  private Level _level = Level.INFO;
  
  private LogHandler _handler;
  
  @SuppressWarnings("unused")
  private LogSystemAdmin _admin;
  
  private String _serverPrefix;
  
  private long _expireTimeout = EXPIRE_TIMEOUT;
  
  private boolean _isActive;
  
  private Alarm _alarm;

  private LogReaper _logReaper;
  
  private LogSystemImpl()
  {
    
  }
  
  public static LogSystemImpl createAndAddService()
  {
    SystemManager system = preCreate(LogSystem.class);
    
    LogSystemImpl service = new LogSystemImpl();
    system.addSystem(LogSystem.class, service);
    
    /*
    CandiManager cdiManager =
      CandiManager.getCurrent(system.getClassLoader());
    
    cdiManager.addSingleton(service);
    */
    
    return service;
  }
  
  @Override
  public void setLevel(Level level)
  {
    if (level == null)
      throw new NullPointerException();
    
    _level = level;
  }
  
  @Override
  public void setExpireTimeout(long timeout)
  {
    _expireTimeout = timeout;
  }
  
  @Override
  public long getExpireTimeout()
  {
    return _expireTimeout;
  }
  
  /**
   * Creates the full log name a partial name.
   */
  @Override
  public String createFullType(String name)
  {
    int p = name.indexOf('|');
    
    if (p == 2 && Character.isDigit(name.charAt(0)))
      return name;
    
    if (_serverPrefix == null)
      return "00|" + name;
    else
      return _serverPrefix + name;
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public void log(String fullType, String message)
  {
    if (! _isActive) {
      if (message != null && message.length() > 512) {
        message = message.substring(0, 512);
      }

      System.out.println("log is not active: " + fullType + " " + message);
      return;
    }
    
    long timestamp = CurrentTime.getCurrentTime();
    
    log(timestamp, fullType, "", _level, message);
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public void log(String fullType, String name, Level level, String message)
  {
    if (! _isActive) {
      if (message != null && message.length() > 512) {
        message = message.substring(0, 512);
      }
      
      System.out.println("log is not active: " + fullType + " " + message);
      return;
    }
    
    long timestamp = CurrentTime.getCurrentTime();
    
    log(timestamp, fullType, name, level, message);
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public void log(long timestamp, 
                  String fullType, 
                  String name, 
                  Level level,
                  String message)
  {
    if (!_isActive)
      return;
    
    Objects.requireNonNull(fullType);
    Objects.requireNonNull(name);

    long typeId = _nameDatabase.getNameId(fullType);
    long nameId = _nameDatabase.getNameId(name);

    _logDatabase.log(timestamp, typeId, nameId, level, message);
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public void logStream(String fullType, InputStream is)
  {
    if (! _isActive) {
      System.out.println("log is not active: " + fullType);
      return;
    }

    long timestamp = CurrentTime.getCurrentTime();
    
    long typeId = _nameDatabase.getNameId(fullType);
    int nameId = 0;
    
    Level level = _level;
    
    _logDatabase.log(timestamp, typeId, nameId, level, is);
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public void logStream(long timestamp, String fullType, String name,
                        Level level, InputStream is)
  {
    if (!_isActive)
      return;
    
    long typeId = _nameDatabase.getNameId(fullType);
    long nameId = _nameDatabase.getNameId(name);
    
    _logDatabase.log(timestamp, typeId, nameId, level, is);
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public WriteStream openLogStream(String fullType)
  {
    if (! _isActive) {
      System.out.println("log is not active: " + fullType);
      return null;
    }

    long timestamp = CurrentTime.getCurrentTime();
    
    return openLogStream(timestamp, fullType, null, _level);
  }
  
  /**
   * Logs a message given a full name.
   */
  @Override
  public WriteStream openLogStream(long timestamp, String fullType,
                                   String name, Level level)
  {
    if (! _isActive) {
      return null;
    }
    
    StreamImpl s = new LogStream(timestamp, fullType, name, level);
    
    return new WriteStream(s);
  }
  
  @Override
  public LogMessage[] findMessages(String fullType, String levelName,
                                   long minTime, long maxTime)
  {
    return findMessagesByName(fullType, null, levelName, minTime, maxTime);
  }
  
  @Override
  public LogMessage[] findMessages(String []fullTypes, String levelName,
                                   long minTime, long maxTime)
  {
    return findMessagesByName(fullTypes, null, levelName, minTime, maxTime);
  }

  @Override
  public LogMessage[] findMessagesByName(String fullType, String logName,
                                         String levelName, long minTime,
                                         long maxTime)
  {
    if (!_isActive)
      throw new IllegalStateException();
    
    long typeId = _nameDatabase.getNameId(fullType);
    
    Integer valueObject = LEVEL_MAP.get(levelName);
    int levelValue;
    
    if (valueObject != null)
      levelValue = valueObject.intValue();
    else
      levelValue = _level.intValue();
    
    if (maxTime < 0)
      maxTime = Long.MAX_VALUE / 2;
    
    return _logDatabase.findMessages(typeId,
                                     logName,
                                     levelValue,
                                     minTime,
                                     maxTime);
  }
  
  @Override
  public LogMessage[] findMessagesByName(String []fullTypes, String logName,
                                         String levelName, long minTime,
                                         long maxTime)
  {
    if (!_isActive)
      throw new IllegalStateException();
    
    int []typeIds = _nameDatabase.getNameIds(fullTypes);
    
    Integer valueObject = LEVEL_MAP.get(levelName);
    int levelValue;
    
    if (valueObject != null)
      levelValue = valueObject.intValue();
    else
      levelValue = _level.intValue();
    
    if (maxTime < 0)
      maxTime = Long.MAX_VALUE / 2;
    
    return _logDatabase.findMessages(typeIds,
                                     logName,
                                     levelValue,
                                     minTime,
                                     maxTime);
  }  
  @Override
  public long[] findMessageTimes(String fullType, String levelName,
                                   long minTime, long maxTime)
  {
    return findMessageTimesByName(fullType, null, levelName, minTime, maxTime);
  }
  
  public long[] findMessageTimesByName(String fullType, String logName,
                                       String levelName, long minTime,
                                       long maxTime)
  {
    if (!_isActive) {
      throw new IllegalStateException();
    }
    
    long typeId = _nameDatabase.getNameId(fullType);
    
    Integer valueObject = LEVEL_MAP.get(levelName);
    int levelValue;
    
    if (valueObject != null)
      levelValue = valueObject.intValue();
    else
      levelValue = _level.intValue();
    
    if (maxTime < 0)
      maxTime = Long.MAX_VALUE / 2;

    long []times = _logDatabase.findMessageTimes(typeId,
                                                 logName,
                                                 levelValue,
                                                 minTime,
                                                 maxTime);
    
    if (times != null) {
      Arrays.sort(times);
    }
    
    return times;
  }
  
  /**
   * Starts the log service.
   * 
   * @throws SQLException
   */
  @Override
  public void start()
    throws SQLException
  {
    NetworkSystemBartender network = NetworkSystemBartender.current();
    
    if (network != null) {
      int index = network.selfServer().getServerIndex();
      
      _serverPrefix = String.format("%02d|", index);
    }
    else {
      _serverPrefix = "00|";
    }
    
    Path dataDirectory = RootDirectorySystem.getCurrentDataDirectory();
    
    Path path = dataDirectory.resolve("log");
    
    _nameDatabase = new NameDatabase();
    _logDatabase = new LogDatabase(_nameDatabase);
    
    _isActive = true;
    
    /*
    Logger log = Logger.getLogger("");
    _handler = new LogHandler(this);
    _handler.setLevel(_level);
    log.addHandler(_handler);
    */

    _logReaper = new LogReaper();
    _alarm = new WeakAlarm(_logReaper);
    _alarm.runAfter(60000);

    _admin = new LogSystemAdmin(this);
  }
  
  /**
   * Starts the log service.
   */
  @Override
  public void stop(ShutdownModeAmp mode)
  {
    _isActive = false;
    
    // _alarm.dequeue();
    
    Logger log = Logger.getLogger("");
    log.removeHandler(_handler);
    
    // _database.close();
  }
  
  class LogReaper implements AlarmListener
  {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        _logDatabase.deleteOldEntries(_expireTimeout);
      } finally {
        if (_isActive)
          alarm.runAfter(24 * 3600 * 1000);
      }
    }
  }
  
  class LogStream extends StreamImpl
  {
    private long _timestamp;
    private String _type;
    private String _name;
    private Level _level;
    
    private TempStream _ts;
    
    public LogStream(long timestamp, String type, String name, Level level)
    {
      _timestamp = timestamp;
      _type = type;
      _name = name;
      _level = level;
      
      _ts = new TempStream();
    }
    
    @Override
    public void write(byte[] buffer, int offset, int length, boolean isEnd)
      throws IOException
    {
      _ts.write(buffer, offset, length, isEnd);
    }
    
    @Override
    public void close() throws IOException
    {
      TempStream ts = _ts;
      _ts = null;
      
      ReadStream is = ts.openRead();
      
      try {
        logStream(_timestamp, _type, _name, _level, is);
      } finally {
        is.close();
      }
    }
  }
  
  static {
    LEVEL_MAP.put("all", Level.ALL.intValue());
    LEVEL_MAP.put("finest", Level.FINEST.intValue());
    LEVEL_MAP.put("finer", Level.FINER.intValue());
    LEVEL_MAP.put("fine", Level.FINE.intValue());
    LEVEL_MAP.put("config", Level.CONFIG.intValue());
    LEVEL_MAP.put("info", Level.INFO.intValue());
    LEVEL_MAP.put("warning", Level.WARNING.intValue());
    LEVEL_MAP.put("severe", Level.SEVERE.intValue());
    LEVEL_MAP.put("off", Level.OFF.intValue());
  }
}
