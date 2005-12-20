/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.sql;

import java.util.Properties;
import java.util.HashMap;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.*;
import javax.sql.*;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.config.Config;
import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.StringAttributeProgram;
import com.caucho.config.ConfigException;

import com.caucho.config.types.InitParam;

import com.caucho.naming.Jndi;

import com.caucho.tools.profiler.*;

/**
 * Configures the database driver.
 */
public class DriverConfig {
  protected static final Logger log = Log.open(DriverConfig.class);
  private static final L10N L = new L10N(DriverConfig.class);

  private static final int TYPE_UNKNOWN = 0;
  private static final int TYPE_DRIVER = 1;
  private static final int TYPE_POOL = 2;
  private static final int TYPE_XA = 3;

  /**
   * The beginning of the URL used to connect to a database with
   * this pooled connection driver.
   */
  private static final String URL_PREFIX = "jdbc:caucho:" ;

  /**
   * The key used to look into the properties passed to the
   * connect method to find the username.
   */
  public static final String PROPERTY_USER = "user" ;
  /**
   * The key used to look into the properties passed to the
   * connect method to find the password.
   */
  public static final String PROPERTY_PASSWORD = "password" ;

  private DBPoolImpl _dbPool;

  private Class _driverClass;

  private String _driverURL;
  private String _user;
  private String _password;
  private Properties _info;

  private BuilderProgramContainer _init = new BuilderProgramContainer();

  private int _driverType;
  private Object _driverObject;

  private ConnectionPoolDataSource _poolDataSource;
  private XADataSource _xaDataSource;
  private Driver _driver;

  private ProfilerPoint _profilerPoint;

  private boolean _isInit;
  private boolean _isStarted;

  /**
   * Null constructor for the Driver interface; called by the JNDI
   * configuration.  Applications should not call this directly.
   */
  public DriverConfig(DBPoolImpl pool)
  {
    _dbPool = pool;

    _info = new Properties();
  }

  /**
   * Returns the DBPool.
   */
  public DBPoolImpl getDBPool()
  {
    return _dbPool;
  }

  /**
   * Sets the driver as data source.
   */
  public void setDriverType(String type)
    throws ConfigException
  {
    if ("ConnectionPoolDataSource".equals(type)) {
      _driverType = TYPE_POOL;
    }
    else if ("XADataSource".equals(type)) {
      _driverType = TYPE_XA;
    }
    else if ("Driver".equals(type)) {
      _driverType = TYPE_DRIVER;
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown driver type. Valid types are 'ConnectionPoolDataSource', 'XADataSource' and 'Driver'"));
  }

  /**
   * Sets the driver as data source.
   */
  public void setDataSource(Object dataSource)
    throws ConfigException
  {
    if (dataSource instanceof String)
      dataSource = Jndi.lookup((String) dataSource);

    if (_driverType == TYPE_XA)
      _xaDataSource = (XADataSource) dataSource;
    else if (_driverType == TYPE_POOL)
      _poolDataSource = (ConnectionPoolDataSource) dataSource;
    else if (dataSource instanceof XADataSource)
      _xaDataSource = (XADataSource) dataSource;
    else if (dataSource instanceof ConnectionPoolDataSource)
      _poolDataSource = (ConnectionPoolDataSource) dataSource;
    else
      throw new ConfigException(L.l("data-source `{0}' is of type `{1}' which does not implement XADataSource or ConnectionPoolDataSource.",
                                    dataSource,
                                    dataSource.getClass().getName()));
  }

  /**
   * Returns the JDBC driver class for the pooled object.
   */
  public Class getDriverClass()
  {
    return _driverClass;
  }

  /**
   * Sets the JDBC driver class underlying the pooled object.
   */
  public void setType(Class driverClass)
    throws ConfigException
  {
    _driverClass = driverClass;

    if (! Driver.class.isAssignableFrom(driverClass) &&
        ! XADataSource.class.isAssignableFrom(driverClass) &&
        ! ConnectionPoolDataSource.class.isAssignableFrom(driverClass))
      throw new ConfigException(L.l("`{0}' is not a valid database type.",
                                    driverClass.getName()));

    Config.checkCanInstantiate(driverClass);
  }

  /**
   * Returns the connection's JDBC url.
   */
  public String getURL()
  {
    return _driverURL;
  }

  /**
   * Sets the connection's JDBC url.
   */
  public void setURL(String url)
  {
    _driverURL = url;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _init.addProgram(program);
  }

  /**
   * Returns the connection's user.
   */
  public String getUser()
  {
    return _user;
  }

  /**
   * Sets the connection's user.
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Returns the connection's password
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the connection's password
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Sets a property from the underlying driver.  Used to set driver
   * properties not handled by DBPool.
   *
   * @param name property name for the driver
   * @param value the driver's value of the property name
   */
  public void setInitParam(InitParam initParam)
  {
    HashMap<String,String> paramMap = initParam.getParameters();

    Iterator<String> iter = paramMap.keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next();

      _info.setProperty(key, paramMap.get(key));
    }
  }

  /**
   * Sets a property from the underlying driver.  Used to set driver
   * properties not handled by DBPool.
   *
   * @param name property name for the driver
   * @param value the driver's value of the property name
   */
  public void setInitParam(String key, String value)
  {
    _info.setProperty(key, value);
  }

  /**
   * Returns the properties.
   */
  public Properties getInfo()
  {
    return _info;
  }

  /**
   * Returns the driver object.
   */
  public Driver getDriver()
    throws SQLException
  {
    Object obj = getDriverObject();

    if (obj instanceof Driver)
      return (Driver) obj;
    else
      return null;
  }

  /**
   * Sets the driver object.
   */
  public void setDriver(Driver driver)
    throws SQLException
  {
    _driver = driver;
    _driverObject = driver;
  }

  /**
   * Returns the driver pool.
   */
  public ConnectionPoolDataSource getPoolDataSource()
    throws SQLException
  {
    return _poolDataSource;
  }

  /**
   * Sets the pooled data source driver.
   */
  public void setPoolDataSource(ConnectionPoolDataSource pDataSource)
    throws SQLException
  {
    _poolDataSource = pDataSource;
    _driverObject = _poolDataSource;
  }

  /**
   * Returns any XADataSource.
   */
  public XADataSource getXADataSource()
  {
    return _xaDataSource;
  }

  /**
   * Sets the xa data source driver.
   */
  public void setXADataSource(XADataSource xaDataSource)
    throws SQLException
  {
    _xaDataSource = xaDataSource;
    _driverObject = _xaDataSource;
  }

  /**
   * Returns true if the driver is XA enabled.
   */
  public boolean isXATransaction()
  {
    return _xaDataSource != null && _dbPool.isXA();
  }

  /**
   * Returns true if the driver is XA enabled.
   */
  public boolean isLocalTransaction()
  {
    return _dbPool.isXA();
  }

  /**
   * Configure a ProfilerPointConfig, used to create a ProfilerPoint
   * that is then passed to setProfiler().
   * The returned ProfilerPointConfig has a default name set to the URL of
   * this driver,
   */
  public ProfilerPointConfig createProfilerPoint()
  {
    ProfilerPointConfig profilerPointConfig = new ProfilerPointConfig();

    profilerPointConfig.setName(getURL());
    profilerPointConfig.setCategorizing(true);

    return profilerPointConfig;
  }

  /**
   * Enables profiling for this driver.
   */
  public void setProfilerPoint(ProfilerPoint profilerPoint)
  {
    _profilerPoint = profilerPoint;
  }

  /**
   * Initialize the pool's data source
   *
   * <ul>
   * <li>If data-source is set, look it up in JNDI.
   * <li>Else if the driver is a pooled or xa data source, use it.
   * <li>Else create wrappers.
   * </ul>
   */
  synchronized void initDataSource(boolean isTransactional, boolean isSpy)
    throws SQLException
  {
    if (_isStarted)
      return;

    _isStarted = true;

    if (_xaDataSource == null && _poolDataSource == null) {
      initDriver();

      Object driverObject = getDriverObject();

      if (driverObject == null) {
        throw new SQLExceptionWrapper(L.l("driver `{0}' has not been configured for pool {1}.  <database> needs a <driver type='...'>.",
                                          _driverClass, getDBPool().getName()));
      }

      if (_driverType == TYPE_XA)
        _xaDataSource = (XADataSource) _driverObject;
      else if (_driverType == TYPE_POOL)
        _poolDataSource = (ConnectionPoolDataSource) _driverObject;
      else if (_driverType == TYPE_DRIVER)
        _driver = (Driver) _driverObject;
      else if (driverObject instanceof XADataSource)
        _xaDataSource = (XADataSource) _driverObject;
      else if (_driverObject instanceof ConnectionPoolDataSource)
        _poolDataSource = (ConnectionPoolDataSource) _driverObject;
      else if (_driverObject instanceof Driver)
        _driver = (Driver) _driverObject;
      else
        throw new SQLExceptionWrapper(L.l("driver `{0}' has not been configured for pool {1}.  <database> needs a <driver type='...'>.",
                                          _driverClass, getDBPool().getName()));

      /*
      if (! isTransactional && _xaDataSource != null) {
	throw new SQLExceptionWrapper(L.l("XADataSource `{0}' must be configured as transactional.  Either configure it with <xa>true</xa> or use the database's ConnectionPoolDataSource driver or the old java.sql.Driver driver.",
					  _xaDataSource));
      }
      */
    }

    if (_profilerPoint != null) {
      if (log.isLoggable(Level.FINE))
        log.fine(_profilerPoint.toString());

      if (_xaDataSource != null)
        _xaDataSource = new XADataSourceWrapper(_profilerPoint, _xaDataSource);
      else if (_poolDataSource != null)
        _poolDataSource = new ConnectionPoolDataSourceWrapper(_profilerPoint, _poolDataSource);
      else if (_driver != null)
        _driver = new DriverWrapper(_profilerPoint, _driver);
    }
  }

  /**
   * Returns the driver object configured for the database.
   */
  synchronized Object getDriverObject()
    throws SQLException
  {
    if (_driverObject != null)
      return _driverObject;
    else if (_driverClass == null)
      return null;

    if (log.isLoggable(Level.CONFIG))
      log.config("loading driver: " + _driverClass.getName());

    try {
      _driverObject = _driverClass.newInstance();
    } catch (Exception e) {
      throw new SQLExceptionWrapper(e);
    }

    return _driverObject;
  }

  /**
   * Creates a connection.
   */
  PooledConnection createPooledConnection(String user, String password)
    throws SQLException
  {
    PooledConnection conn = null;
    if (_xaDataSource != null) {
      if (user == null && password == null)
        conn = _xaDataSource.getXAConnection();
      else
        conn = _xaDataSource.getXAConnection(user, password);

      /*
      if (! _isTransactional) {
	throw new SQLExceptionWrapper(L.l("XADataSource `{0}' must be configured as transactional.  Either configure it with <xa>true</xa> or use the database's ConnectionPoolDataSource driver or the old java.sql.Driver driver.",
					  _xaDataSource));
      }
      */
    }
    else if (_poolDataSource != null) {
      /*
      if (_isTransactional) {
	throw new SQLExceptionWrapper(L.l("ConnectionPoolDataSource `{0}' can not be configured as transactional.  Either use the database's XADataSource driver or the old java.sql.Driver driver.",
					  _poolDataSource));
      }
      */

      if (user == null && password == null)
        conn = _poolDataSource.getPooledConnection();
      else
        conn = _poolDataSource.getPooledConnection(user, password);
    }

    return conn;
  }

  /**
   * Creates a connection.
   */
  Connection createDriverConnection(String user, String password)
    throws SQLException
  {
    if (_xaDataSource != null || _poolDataSource != null)
      throw new IllegalStateException();

    if (_driver == null)
      throw new IllegalStateException();

    Driver driver = _driver;
    String url = getURL();

    if (url == null)
      throw new SQLException(L.l("can't create connection with null url"));

    Properties properties = getInfo();

    if (user != null)
      properties.put("user", user);
    else
      properties.put("user", "");

    if (password != null)
      properties.put("password", password);
    else
      properties.put("password", "");

    Connection conn;
    if (driver != null)
      conn = driver.connect(url, properties);
    else
      conn = java.sql.DriverManager.getConnection(url, properties);

    return conn;
  }

  /**
   * Initializes the JDBC driver.
   */
  public void initDriver()
    throws SQLException
  {
    if (_isInit)
      return;

    _isInit = true;

    Object driverObject = getDriverObject();

    if (driverObject != null) {
    }
    else if (_xaDataSource != null || _poolDataSource != null)
      return;
    else {
      throw new SQLExceptionWrapper(L.l("driver `{0}' has not been configured for pool {1}.  <database> needs either a <data-source> or a <type>.",
                                        _driverClass, getDBPool().getName()));
    }

    try {
      // server/14g1
      if (_driverURL != null) { // && ! (driverObject instanceof Driver)) {
        StringAttributeProgram program;
        program = new StringAttributeProgram("url", _driverURL);
        program.configure(driverObject);
      }
    } catch (Throwable e) {
      if (driverObject instanceof Driver)
        log.log(Level.FINEST, e.toString(), e);
      else
        throw new SQLExceptionWrapper(e);
    }

    try {
      if (_user != null) { // && ! (driverObject instanceof Driver)) {
        StringAttributeProgram program;
        program = new StringAttributeProgram("user", _user);
        program.configure(driverObject);
      }
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);

      if (! (driverObject instanceof Driver))
        throw new SQLExceptionWrapper(e);
    }

    try {
      if (_password != null) { // && ! (driverObject instanceof Driver)) {
        StringAttributeProgram program;
        program = new StringAttributeProgram("password", _password);
        program.configure(driverObject);
      }
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);

      if (! (driverObject instanceof Driver))
        throw new SQLExceptionWrapper(e);
    }

    try {
      if (_init != null) {
        _init.configure(driverObject);
        _init = null;
      }

      Config.init(driverObject);
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Returns a string description of the pool.
   */
  public String toString()
  {
    return "Driver[" + _driverURL + "]";
  }
}
