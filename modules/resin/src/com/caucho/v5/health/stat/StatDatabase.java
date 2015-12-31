/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import io.baratine.db.Cursor;
import io.baratine.service.Result;
import io.baratine.service.ResultFuture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.sql.DataSource;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.kraken.KrakenSystem;
import com.caucho.v5.kraken.query.QueryKraken;
import com.caucho.v5.kraken.table.TableKraken;
import com.caucho.v5.kraken.table.TableManagerKraken;
import com.caucho.v5.management.server.StatServiceValue;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.FreeList;
import com.caucho.v5.vfs.Path;

/**
 * statistics
 */
class StatDatabase
{
  private static final Logger log
    = Logger.getLogger(StatDatabase.class.getName());
  
  private static final long HOUR = 3600L * 1000L;
  private static final long DAY = 24L * HOUR;
  private static final long WEEK = 7L * DAY;
  
  private String _serverId;
  private DataSource _db;

  private TableKraken _nameTable;
  private TableKraken _dataTable;

  private FreeList<StatConnection> _freeStatList
    = new FreeList<StatConnection>(4);
  
  private ConcurrentHashMap<String,Long> _nameMap
    = new ConcurrentHashMap<String,Long>();
  
  private ConcurrentHashMap<Long,StatDataCache> _statCache
    = new ConcurrentHashMap<>();
  
  private AtomicReference<StatBaseline> _baselineRef
    = new AtomicReference<>();

  private AtomicBoolean _isCacheInit = new AtomicBoolean();

  private ReaperTask _reaperTask;
  
  // 2 week timeout for baseline
  private long _dataTimeout = 14 * DAY;
  private long _reaperTimeout = 3 * 3600 * 1000L;
  
  private long _samplePeriod = 60 * 1000L;
  
  private QueryKraken _nameInsert;

  private QueryKraken _dataInsert;
  private QueryKraken _selectQuery;

  private TableManagerKraken _tableManager;

  private QueryKraken _nameSelect;

  StatDatabase(Path path)
  {
    try {
      ServerBartender server = BartenderSystem.getCurrentSelfServer();

      _serverId = server.getDisplayName();

      //_nameTable = escapeName("stat_name");
      //_dataTable = escapeName("stat_data");

      /*
      _mbeanServer = Jmx.getMBeanServer();

      DataSourceImpl dataSource = new DataSourceImpl();
      dataSource.setPath(path);
      dataSource.setRemoveOnError(true);
      dataSource.init();

      _db = dataSource;
      */

      initDatabase();

      //_reaperTask = new ReaperTask();
      //new WeakAlarm(_reaperTask).queue(_reaperTimeout);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private static String escapeName(String name)
  {
    StringBuilder cleanName = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if ('a' <= ch && ch <= 'z'
          || 'A' <= ch && ch <= 'Z'
          || '0' <= ch && ch <= '9'
          || ch == '_') {
        cleanName.append(ch);
      }
      else
        cleanName.append('_');
    }

    return cleanName.toString();
  }
  
  private void initDatabase()
  {
    KrakenSystem kraken = KrakenSystem.getCurrent();
    
    _tableManager = kraken.getTableManager();

    String nameTableSql
      = ("create table local.caucho_stat_name ("
         +  "  id int64 primary key"
         +  ", name string"
         +  ", type string"
         +  ", spec string"
         +  ")");
    
    _nameTable = _tableManager.createTable("local.caucho_stat_name", nameTableSql);
    
    String dataTableSql
      = ("create table local.caucho_stat_data ("
         +  "  time int64"
         +  ", id int64"
         +  ", value double"
         +  ", primary key (time,id)"
         +  ")");
    
    _dataTable = _tableManager.createTable("local.caucho_stat_data", dataTableSql);
    
    String nameInsertSql
      = ("insert into local.caucho_stat_name (id, name, type, spec)"
          + " values(?, ?, ?, ?)");
    
    _nameInsert = _tableManager.query(nameInsertSql);
    
    String nameSelectSql
      = ("select id, name from local.caucho_stat_name");
    
    _nameSelect = _tableManager.query(nameSelectSql);
    
    _nameInsert = _tableManager.query(nameInsertSql);
    
    String dataInsertSql
      = ("insert into local.caucho_stat_data (id, time, value) values (?,?,?)");
    
    _dataInsert = _tableManager.query(dataInsertSql);
    
    String selectSql
      = ("select id, time, value from local.caucho_stat_data"
         + " where id=? and ? <= time and time <= ?");
    
    _selectQuery = _tableManager.query(selectSql);
    
    initLoadAttributes();
    // initLoadCache();
  }

  public String []getStatisticsNames()
  {
    ArrayList<String> names = new ArrayList<>();

    for (Cursor cursor : _nameSelect.findAll()) {
      long id = cursor.getLong(1);
      String name = cursor.getString(2);

      names.add(name);
    }

    String []nameArray = new String[names.size()];
    names.toArray(nameArray);

    return nameArray;
  }

  /**
   * XXX: need to reproduce the zero-out code for the downtime.
   */
  void initLoadAttributesOld()
    throws SQLException
  {
    Connection conn = _db.getConnection();

    try {
      String maxTimeSql = ("select max(time) from " + _dataTable);
          
      PreparedStatement maxTimeStmt = conn.prepareStatement(maxTimeSql);
      
      long maxTime = CurrentTime.getCurrentTime();
      
      try {
        ResultSet rs = maxTimeStmt.executeQuery();
        
        if (rs.next())
          maxTime = rs.getLong(1);
        
        rs.close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
      
      String insertSql = ("insert into " + _dataTable
                          + " (id,time,value)"
                          + " values (?,?,?)");
    
      PreparedStatement insertStmt = conn.prepareStatement(insertSql);
      Statement nameStmt = conn.createStatement();

      String sql = ("select id, name, type, spec from " + _nameTable);
      
      long now = CurrentTime.getCurrentTime();

      ResultSet rs = nameStmt.executeQuery(sql);

      while (rs.next()) {
        long id = rs.getLong(1);
        String name = rs.getString(2);
        String type = rs.getString(3);
        String spec = rs.getString(4);
        
        _nameMap.put(name, id);
        
        try {
          maxTimeStmt.setLong(1, id);

          boolean isAdd = true;

          if (maxTime + _samplePeriod < now) {
            insertStmt.setLong(1, id);
            insertStmt.setLong(2, maxTime + _samplePeriod);
            insertStmt.setDouble(3, 0);
            insertStmt.executeUpdate();
          }
          else {
            isAdd = false;
          }

          if (isAdd) {
            insertStmt.setLong(1, id);
            insertStmt.setLong(2, now - _samplePeriod);
            insertStmt.setDouble(3, 0);
            insertStmt.executeUpdate();
          }
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      conn.close();
    }
  }

  private void initLoadCache()
  {
    long now = CurrentTime.getCurrentTime();
      
    String sql = ("select id,time,value from local.caucho_stat_data "
                  + " where time >= " + (now - StatDataCache.CACHE_PERIOD));

    QueryKraken query = _tableManager.query(sql);
    
    for (Cursor cursor : query.findAll(new Object [0])) {
      long id = cursor.getLong(1);
      long time = cursor.getLong(2);
      double value = cursor.getDouble(3);
      
      // StatDataCache cache = getCache(id);
      
      addInitialCacheData(now, id, time, value);
    }
      
      /*
      Statement stmt = conn.createStatement();
      
      ResultSet rs = stmt.executeQuery(sql);
        
      while (rs.next()) {
        long id = rs.getLong(1);
        long time = rs.getLong(2);
        double value = rs.getDouble(3);
        
        addInitialCacheData(now, id, time, value);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      conn.close();
    }
    */
  }

  private void initLoadAttributes()
  {
    String sql = ("select id,name,type,spec from local.caucho_stat_name");
          
    QueryKraken query = _tableManager.query(sql);

    for (Cursor cursor : query.findAll(new Object [0])) {
      long id = cursor.getLong(1);
      String name = cursor.getString(2);
      String type = cursor.getString(3);
      String spec = cursor.getString(4);

      if (name != null) {
        _nameMap.put(name, id);
      }
    }
      
      /*
      Statement stmt = conn.createStatement();
      
      ResultSet rs = stmt.executeQuery(sql);
        
      while (rs.next()) {
        long id = rs.getLong(1);
        long time = rs.getLong(2);
        double value = rs.getDouble(3);
        
        addInitialCacheData(now, id, time, value);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      conn.close();
    }
    */
  }

  private boolean isDatabaseValid()
    throws SQLException
  {
    Connection conn = _db.getConnection();

    try {
      Statement stmt = conn.createStatement();

      String sql;
      sql = ("select id, name, type, spec"
             + " from " + _nameTable + " where 1=0");

      ResultSet rs = stmt.executeQuery(sql);

      rs.next();

      sql = ("select id, time, value"
             + " from " + _dataTable + " where 1=0");
      rs = stmt.executeQuery(sql);

      rs.next();

      return true;
    } catch (SQLException e) {
      log.log(Level.FINEST, e.toString(), e);

      return false;
    } finally {
      conn.close();
    }
  }

  void addSampleMetadata(long id,
                         String name)
  {
    if (_nameMap.containsKey(name)) {
      return;
    }

    _nameMap.put(name, id);

    ResultFuture<Object> future = new ResultFuture<>();

    _nameInsert.exec(future, id, name, "long", name);

    Object value = future.get(1, TimeUnit.SECONDS);
  }

  void addSampleData(long now, long []ids, double []data)
  {
    for (int i = 0; i < ids.length; i++) {
      Result<Object> result = Result.ignore();
      
      _dataInsert.exec(result, ids[i], now, data[i]);
        
      addCacheData(now, ids[i], data[i]);
    }
  }
  
  private void addCacheData(long now, long id, double value)
  {
    StatDataCache cache = getCache(id);
    
    cache.clearToNow(now);
    cache.set(now, value);
  }
  
  private void addInitialCacheData(long now, long id, long time, double value)
  {
    StatDataCache cache = getCache(id);
    
    cache.clearToNow(now);
    cache.set(time, value);
  }

  private StatDataCache getCache(long id)
  {
    StatDataCache cache = _statCache.get(id);
    
    if (cache == null) {
      cache = new StatDataCache(id);
      
      _statCache.putIfAbsent(id, cache);
      
      cache = _statCache.get(id);
    }
    
    return cache;
  }

  long getSampleId(String name)
  {
    Long id = _nameMap.get(name);
      
    if (id != null) {
      return id;
    }

    return 0;
  }

  synchronized ArrayList<StatServiceValue> sampleData(long id,
                                                      long beginTime,
                                                      long endTime,
                                                      long stepTime)
  {
    return sampleData(id, beginTime, endTime, stepTime, false);
  }

  synchronized ArrayList<StatServiceValue> sampleData(long id,
                                                      long beginTime,
                                                      long endTime,
                                                      long stepTime,
                                                      boolean isPrecise)
  {
    if (stepTime <= 0) {
      stepTime = 1;
    }
    
    long now = CurrentTime.getCurrentTime();

    if (now < beginTime + StatDataCache.CACHE_PERIOD && ! isPrecise) {
      return sampleCacheData(id, beginTime, endTime, stepTime);
    }

    ArrayList<StatValue> data = new ArrayList<>();
    
    // DERP:
    for (Cursor cursor : _selectQuery.findAll(new Object[] { id, beginTime, endTime })) {
      long cursorId = cursor.getLong(1);
      long time = cursor.getLong(2);
      double value = cursor.getDouble(3);
      
      data.add(new StatValue(time, value)); 
    }
    
    Collections.sort(data, new StatValueComparator());
    
    return processData(data, stepTime, isPrecise);
  }
  
  private ArrayList<StatServiceValue> processData(ArrayList<StatValue> rawData,
                                                  long stepTime,
                                                  boolean isPrecise)
  {
    ArrayList<StatServiceValue> data = new ArrayList<StatServiceValue>();

    long chunkStartTime = 0;
    long chunkEndTime = 0;
    double sum = 0;
    int count = 0;
    double min = Integer.MAX_VALUE;
    double max = Integer.MIN_VALUE;

    for (StatValue rawItem : rawData) {
      long time = rawItem.getTime();
      double value = rawItem.getValue();
        
      if (value == 0 && isPrecise) {
        continue;
      }

      if (chunkEndTime <= time) {
        if (chunkStartTime > 0) {
          data.add(new StatServiceValue(chunkStartTime, count, sum, min, max));
        }

        chunkStartTime = time - time % stepTime;
        chunkEndTime = chunkStartTime + stepTime;
        count = 0;
        sum = 0;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
      }

      count++;
      sum += value;

      min = Math.min(min, value);
      max = Math.max(max, value);
    }

    if (chunkStartTime > 0) {
      data.add(new StatServiceValue(chunkStartTime, count, sum, min, max));
    }

    return data; 
  }

  ArrayList<StatServiceValue> sampleCacheData(long id,
                                              long beginTime,
                                              long endTime,
                                              long stepTime)
  {
    if (_isCacheInit.compareAndSet(false, true)) {
      initLoadCache();
    }
    
    ArrayList<StatServiceValue> result = new ArrayList<StatServiceValue>();
    
    StatDataCache cache = getCache(id);
    
    // double min = Double.MAX_VALUE;
    // double max = Double.MIN_VALUE;

    long chunkStartTime = 0;
    long chunkEndTime = 0;
    double sum = 0;
    int count = 0;
    double min = Integer.MAX_VALUE;
    double max = Integer.MIN_VALUE;
    
    for (long time = beginTime; time <= (endTime + 60000L) ; time += 60000L) {
      double value = cache.get(time);
      
      if (Double.isNaN(value)) {
        continue;
      }
      
      if (chunkEndTime <= time) {
        if (chunkStartTime > 0 && count > 0) {
          result.add(new StatServiceValue(chunkStartTime, count, sum, min, max));
        }

        chunkStartTime = time - time % stepTime;
        chunkEndTime = chunkStartTime + stepTime;
        count = 0;
        sum = 0;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
      }

      count++;
      sum += value;

      min = Math.min(min, value);
      max = Math.max(max, value);
    }
    
    if (chunkStartTime > 0 && count > 0) {
      result.add(new StatServiceValue(chunkStartTime, count, sum, min, max));
    }

    return result;
  }

  public double getBaselineAverage(long id, long startTime, long endTime)
  {
    long now = CurrentTime.getCurrentTime();
    
    StatBaseline baseline = getBaseline(now);
    
    if (baseline == null) {
      return Double.NaN;
    }
    
    return baseline.getAverage(id, startTime, endTime);
  }

  private synchronized StatBaseline getBaseline(long now)
  {
    long start = now - 2 * WEEK;
    
    start -= start % HOUR;
    
    StatBaseline baseline = _baselineRef.get();
    
    if (baseline != null && baseline.getTimeMin() == start) {
      return baseline;
    }
    
    baseline = fillBaseline(start);
    
    _baselineRef.set(baseline);
    
    return baseline;
  }
  
  private StatBaseline fillBaseline(long startTime)
  {
    long endTime = startTime + 2 * WEEK;

    StatBaseline baseline = new StatBaseline(startTime, endTime, HOUR);

    StatConnection conn = null;

    try {
      conn = null;

      PreparedStatement stmt = conn.getSelectBaseline();
      stmt.setLong(1, startTime);
      stmt.setLong(2, endTime);

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        long id = rs.getLong(1);
        long time = rs.getLong(2);
        double value = rs.getDouble(3);
        
        baseline.add(id, time, value);
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      conn.close();
    }

    return baseline;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class StatAttribute {
    private int _id;
    private String _name;
    private ObjectName _objectName;
    private String _attr;
    private String _description;

    StatAttribute(int id,
                  String name,
                  ObjectName objectName,
                  String attr,
                  String description)
    {
      _id = id;
      _name = name;
      _objectName = objectName;
      _attr = attr;
      _description = description;
    }

    public int getId()
    {
      return _id;
    }

    public String getName()
    {
      return _name;
    }

    public ObjectName getObjectName()
    {
      return _objectName;
    }

    public String getAttribute()
    {
      return _attr;
    }

    public String getDescription()
    {
      return _description;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "," + _attr + "]";
    }
  }

  class StatConnection {
    private Connection _conn;

    private PreparedStatement _insertSampleStmt;
    private PreparedStatement _loadNameIdStmt;
    private PreparedStatement _insertNameStmt;
    private PreparedStatement _selectNamesStmt;

    private PreparedStatement _selectDataStmt;
    private PreparedStatement _selectBaselineStmt;

    StatConnection(Connection conn)
    {
      _conn = conn;
    }

    PreparedStatement getInsertSample()
    {
      try {
        if (_insertSampleStmt == null) {
          String sql = ("insert into " + _dataTable
              + " (id,time,value)"
              + " values (?,?,?)");

          _insertSampleStmt = _conn.prepareStatement(sql);
        }

        return _insertSampleStmt;
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }

    PreparedStatement getSelectData()
    {
      try {
        if (_selectDataStmt == null) {
          String sql = ("select time, value"
                        + " from " + _dataTable
                        + " where ?<=time and time<=? and id=?"
                        + " order by time");

          _selectDataStmt = _conn.prepareStatement(sql);
        }

        return _selectDataStmt;
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }

    PreparedStatement getSelectBaseline()
    {
      try {
        if (_selectBaselineStmt == null) {
          String sql = ("select id, time, value"
                        + " from " + _dataTable
                        + " where ?<=time and time<=?");

          _selectBaselineStmt = _conn.prepareStatement(sql);
        }

        return _selectBaselineStmt;
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }

    PreparedStatement getLoadNameId()
    {
      try {
        if (_loadNameIdStmt == null) {
          String sql = ("select id from " + _nameTable
                        + " where name=?");

          _loadNameIdStmt = _conn.prepareStatement(sql);
        }

        return _loadNameIdStmt;
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }

    PreparedStatement getInsertName()
    {
      try {
        if (_insertNameStmt == null) {
          String sql = ("insert into " + _nameTable
                        + " (id, name, type, spec)"
                        + " values (?, ?, ?, ?)");

          _insertNameStmt = _conn.prepareStatement(sql);
        }

        return _insertNameStmt;
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }

    PreparedStatement getSelectNames()
    {
      try {
        if (_selectNamesStmt == null) {
          String sql = ("select id, name from " + _nameTable);

          _selectNamesStmt = _conn.prepareStatement(sql);
        }

        return _selectNamesStmt;
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }

    void close()
    {
      try {
        Connection conn = _conn;
        _conn = null;

        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  class ReaperTask implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        Connection conn = null;

        try {
          conn = _db.getConnection();

          String sql = ("delete from " + _dataTable
                        + " where time <=?");

          PreparedStatement pStmt = conn.prepareStatement(sql);

          long now = CurrentTime.getCurrentTime();
          pStmt.setLong(1, now - _dataTimeout);
          
          pStmt.executeUpdate();
        } finally {
          if (conn != null)
            conn.close();
        }
      } catch (SQLException e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        long now = CurrentTime.getCurrentTime();
        long next = (now + _reaperTimeout);
        next = next - next % (_reaperTimeout);

        alarm.queueAt(next);
      }
    }
  }
  
  private static class StatValue {
    private final long _time;
    private final double _value;
    
    StatValue(long time, double value)
    {
      _time = time;
      _value = value;
    }
    
    public long getTime()
    {
      return _time;
    }
    
    public double getValue()
    {
      return _value;
    }
  }
  
  private static class StatValueComparator implements Comparator<StatValue>
  {
    public int compare(StatValue a, StatValue b)
    {
      return Long.signum(a.getTime() - b.getTime());
    }
  }
}
