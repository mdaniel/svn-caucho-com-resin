/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.security;

import com.caucho.config.*;
import com.caucho.config.types.Period;
import com.caucho.server.dispatch.ServletConfigException;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.security.CachingPrincipal;
import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An authenticator using JDBC.
 *
 * <p>The default table schema looks something like:
 * <pre>
 * CREATE TABLE LOGIN (
 *   username VARCHAR(250) NOT NULL,
 *   password VARCHAR(250),
 *   cookie VARCHAR(250),
 *   PRIMARY KEY (username)
 * );
 * </pre>
 *
 * <code><pre>
 * &lt;security:JdbcAuthenticator data-source="jdbc/user"/>
 * </pre></code>
 */

@ApplicationScoped
public class JdbcAuthenticator extends DatabaseAuthenticator {
}
