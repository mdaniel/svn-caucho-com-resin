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

package com.caucho.hemp.jdbc;

import com.caucho.config.*;
import com.caucho.bam.*;
import com.caucho.server.security.*;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import javax.annotation.*;
import javax.sql.*;
import javax.webbeans.*;

import java.net.*;

/**
 * service manager
 */
public class JdbcServiceManager extends AbstractBamServiceManager
{
  private static final L10N L = new L10N(JdbcServiceManager.class);
  private static final Logger log
    = Logger.getLogger(JdbcServiceManager.class.getName());
  
  @In private BamBroker _broker;
  private DataSource _db;

  private String _tablePrefix = "hemp_";

  private String _dataTable;
  private String _nodeTable;
  private String _nodeTypeTable;
  private String _userTable;

  private HashMap<String,HostItem> _hostMap
    = new HashMap<String,HostItem>();

  private XmppUserDomainAdmin _admin;

  /**
   * Configures the owning server's database
   */
  public void setDatabase(DataSource db)
  {
    _db = db;
  }

  @PostConstruct
  public void init()
  {
    if (_db == null)
      throw new ConfigException(L.l("JdbcServiceManager requires a configured database"));

    _userTable = _tablePrefix + "user";
    _nodeTable = _tablePrefix + "node";
    _nodeTypeTable = _tablePrefix + "node_type";
    _dataTable = _tablePrefix + "data";

    initDatabase();

    _broker.addServiceManager(this);

    String host = "localhost";
    
    _admin = new XmppUserDomainAdmin(this, host);
    _admin.register();
  }
  /**
   * Returns the service with the given name, or null if this is not
   * a known service
   */
  public BamService findService(String jid)
  {
    int p = jid.indexOf('@');

    if (p < 0)
      return null;

    String node = jid.substring(0, p);
    String domain = jid.substring(p + 1);

    System.out.println("JID: " + jid);

    return null;
  }

  public void addUser(String host,
		      String name,
		      String password,
		      String email)
  {
    HostItem hostItem = createHost(host);

    String jid = name + "@" + host;

    Connection conn = null;

    try {
      PasswordDigest digest = new PasswordDigest();
      digest.init();

      String pwDigest = digest.getPasswordDigest(jid, password, "resin");

      conn = _db.getConnection();

      String sql = ("insert into " + _userTable
		    + " (jid,name,email,password)"
		    + " values (?,?,?,?)");

      PreparedStatement pstmt = conn.prepareStatement(sql);

      pstmt.setString(1, jid);
      pstmt.setString(2, name);
      pstmt.setString(3, email);
      pstmt.setString(4, pwDigest);

      pstmt.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
	if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }
  }

  private HostItem createHost(String host)
  {
    return null;
  }

  private void initDatabase()
  {
    Connection conn = null;

    try {
      conn = _db.getConnection();

      Statement stmt = conn.createStatement();
      
      String sql;

      try {
	sql = ("select hash, jid from " + _nodeTable + " where 1=0");

	ResultSet rs = stmt.executeQuery(sql);

	return;
      } catch (SQLException e) {
      }
      
      sql = ("create table " + _nodeTypeTable + " ("
	     + "  id integer primary key auto_increment,"
	     + "  category varchar(255),"
	     + "  type varchar(255),"
	     + "  class_name varchar(255)"
	     + ")");

      stmt.executeUpdate(sql);
      
      sql = ("create table " + _userTable + " ("
	     + "  id integer primary key,"
	     + "  host_id integer,"
	     + "  jid varchar(255) unique,"
	     + "  name varchar(255),"
	     + "  email varchar(255),"
	     + "  password varchar(28),"
	     + "  category integer,"
	     + "  role integer"
	     + ")");

      stmt.executeUpdate(sql);
      
      sql = ("create table " + _nodeTable + " ("
	     + "  id integer primary key,"
	     + "  user_id integer,"
	     + "  jid varchar(255),"
	     + "  node varchar(255),"
	     + "  category integer"
	     + ")");

      stmt.executeUpdate(sql);
      
      sql = ("create table " + _dataTable + "("
	     + "  id varchar(28) primary key,"
	     + "  owner integer,"
	     + "  value blob"
	     + ")");

      stmt.executeUpdate(sql);
    } catch (SQLException e) {
      throw ConfigException.create(e);
    } finally {
      try {
	if (conn != null)
	  conn.close();
      } catch (SQLException e) {
      }
    }
  }

  private void addCategory(Connection conn,
			   String category,
			   String type,
			   String className)
    throws SQLException
  {
    String sql = ("insert into " + _nodeTypeTable
		  + " (category,type,class_name) values (?,?,?)");
    
    PreparedStatement pstmt = conn.prepareStatement(sql);
    
    pstmt.setString(1, category);
    pstmt.setString(2, type);
    if (className != null)
      pstmt.setString(3, className);
    else
      pstmt.setNull(3, 0);

    pstmt.executeUpdate();

    try {
      pstmt.close();
    } catch (SQLException e) {
    }
  }

  @PreDestroy
  public void destroy()
  {
    // _broker.removeServiceManager(this);
  }
}
