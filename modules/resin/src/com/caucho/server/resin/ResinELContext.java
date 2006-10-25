/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.resin;

import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.naming.Jndi;
import com.caucho.util.Alarm;
import com.caucho.util.CauchoSystem;
import com.caucho.vfs.Path;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class ResinELContext
  extends ConfigELContext
{
  private final JavaVar _javaVar = new JavaVar();
  private final ResinVar _resinVar = new ResinVar();
  private final ServerVar _serverVar = new ServerVar();

  public ResinELContext()
  {
    setValue("java", new JavaVar());
    setValue("resin", new ResinVar());
    setValue("server", new ServerVar());

    setValue("fmt", new com.caucho.config.functions.FmtFunctions());

    try {
      setValue("jndi", Jndi.class.getMethod("lookup", new Class[] { String.class }));
      setValue("jndi:lookup", Jndi.class.getMethod("lookup", new Class[] { String.class }));
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  public JavaVar getJavaVar()
  {
    return _javaVar;
  }

  public ResinVar getResinVar()
  {
    return _resinVar;
  }

  public ServerVar getServerVar()
  {
    return _serverVar;
  }

  public Object getValue(String var)
  {
    System.out.println("XXX: getValue " + var + " " + this);

    // backwards compatibility

    if ("resinHome".equals(var) || "resin-home".equals(var))
      return getResinHome();

    if ("serverRoot".equals(var) || "server-root".equals(var))
      return getRootDirectory();

    return super.getValue(var);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  abstract public Path getResinHome();

  abstract public Path getRootDirectory();

  abstract public Path getResinConf();

  abstract public String getServerId();

  abstract public boolean isResinProfessional();

  /**
   * Java variables
   */
  public class JavaVar {
    /**
     * Returns true for JDK 5
     */
    public boolean isJava5()
    {
      return CauchoSystem.isJdk15();
    }
    /**
     * Returns the JDK version
     */
    public String getVersion()
    {
      return System.getProperty("java.version");
    }
  }

  public class ResinVar {
    /**
     * Returns the -server id
     */
    public String getServerId()
    {
      return ResinELContext.this.getServerId();
    }

    /**
     * @deprecated use {@link #getServerId()}
     */
    public String getId()
    {
      return getServerId();
    }


    /**
     * Returns the local address
     *
     * @return IP address
     */
    public String getAddress()
    {
      try {
        if (Alarm.isTest())
          return "127.0.0.1";
        else
          return InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
        Logger.getLogger(ResinELContext.class.getName()).log(Level.FINE, e.toString(), e);

        return "localhost";
      }
    }

    /**
     * Returns the resin config.
     */
    public Path getConf()
    {
      return ResinELContext.this.getResinConf();
    }

    /**
     * Returns the resin home.
     */
    public Path getHome()
    {
      return ResinELContext.this.getResinHome();
    }

    /**
     * Returns the root directory.
     *
     * @return the root directory
     */
    public Path getRoot()
    {
      return ResinELContext.this.getRootDirectory();
    }

    /**
     * @deprecated use {@link #getRoot()}
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * @deprecated use {@link #getRoot()}
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }

    /**
     * Returns the version
     *
     * @return version
     */
    public String getVersion()
    {
      if (Alarm.isTest())
        return "3.1.test";
      else
        return com.caucho.Version.VERSION;
    }

    /**
     * Returns the local hostname
     *
     * @return version
     */
    public String getHostName()
    {
      try {
        if (Alarm.isTest())
          return "localhost";
        else
          return InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
        Logger.getLogger(ResinELContext.class.getName()).log(Level.FINE, e.toString(), e);

        return "localhost";
      }
    }

    /**
     * Returns true for Resin professional.
     */
    public boolean isProfessional()
    {
      return ResinELContext.this.isResinProfessional();
    }
  }

  public class ServerVar {
    public String getId()
    {
      return ResinELContext.this.getServerId();
    }

    /**
     * Returns the root directory.
     *
     * @return the root directory
     */
    public Path getRoot()
    {
      return ResinELContext.this.getRootDirectory();
    }

    /**
     * @deprecated use {@link #getRoot()}
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * @deprecated use {@link #getRoot()}
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }
  }
}
