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
 * @author Rodrigo Westrupp;
 */

package com.caucho.config.j2ee;

import com.caucho.config.*;
import com.caucho.loader.*;
import com.caucho.naming.*;
import com.caucho.amber.cfg.*;
import com.caucho.vfs.*;
import com.caucho.util.L10N;

import javax.naming.*;
import javax.persistence.*;
import javax.persistence.spi.*;
import java.net.URL;
import java.io.*;
import java.util.*;
import java.util.logging.*;


public class PersistenceUnitProgram extends BuilderProgram
{
  private static final Logger log
    = Logger.getLogger(PersistenceUnitProgram.class.getName());
  private static final L10N L = new L10N(PersistenceUnitProgram.class);
  
  private static final String SERVICE
    = "META-INF/services/javax.persistence.spi.PersistenceProvider";

  private static final EnvironmentLocal<HashMap<String,PersistenceUnitInfo>>
    _localInfo = new EnvironmentLocal<HashMap<String,PersistenceUnitInfo>>();

  private static final EnvironmentLocal<PersistenceProvider[]> _localProviders
    = new EnvironmentLocal<PersistenceProvider[]>();

  private AccessibleInject _field;
  private PersistenceUnit _persistenceUnit;

  private String _name;
  private String _jndiName;
  private String _unitName;

  PersistenceUnitProgram(PersistenceUnit persistenceUnit,
			 AccessibleInject field)
  {
    _field = field;
    _persistenceUnit = persistenceUnit;

    if (! field.getType().isAssignableFrom(EntityManager.class))
      throw new ConfigException(L.l("@PersistenceUnit field '{0}' of type '{0}' must be assignable from EntityManager.",
				    field.getName(),
				    field.getType().getName()));

    _name = _persistenceUnit.name();
    _unitName = _persistenceUnit.unitName();

    if (_name == null || "".equals(_name)) {
    }
    else if (_name.startsWith("java:"))
      _jndiName = _name;
    else
      _jndiName = "java:comp/env/" + _name;
  }

  @Override
  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    EntityManager manager = null;

    Context ic = null;

    try {
      ic = new InitialContext();
    } catch (NamingException e) {
      throw new ConfigException(e);
    }

    if (_jndiName != null) {
      try {
	manager = (EntityManager) ic.lookup(_jndiName);

	_field.inject(bean, manager);

	return;
      } catch (NamingException e) {
      }
    }

    manager = findAmberPersistenceUnit(ic);

    if (manager == null) {
      manager = findProviderPersistenceUnit();
    }

    if (manager == null)
      throw new ConfigException(L.l(getLocation() + "@PersistenceUnit(unitName='{0}') is an unknown persistence-unit.",
				    _unitName));

    _field.inject(bean, manager);


    if (_jndiName != null) {
      try {
	Jndi.rebindDeep(_jndiName, manager);
      } catch (NamingException e) {
      }
    }

      /*
    try {
      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("injecting value {0} from '{1}' into field {2}", value, _jndiName,  _field));
    } catch (RuntimeException e) {
      throw e;
    } catch (NamingException e) {
      log.finer(String.valueOf(e));
      log.log(Level.FINEST, e.toString(), e);
    } catch (Exception e) {
      throw new ConfigException(e);
    }
      */
  }

  @Override
  public Object configureImpl(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private EntityManager findAmberPersistenceUnit(Context ic)
    throws ConfigException
  {
    try {
      String jndiPrefix = "java:comp/env/persistence/_amber_PersistenceUnit";

      String jndiName = null;

      if (! "".equals(_unitName))
        jndiName = jndiPrefix + '/' + _unitName;
      else {
        NamingEnumeration<NameClassPair> iter = ic.list(jndiPrefix);

        if (iter == null)
          return null;

        String ejbJndiName = null;
        while (iter.hasMore()) {
          NameClassPair pair = iter.next();

          if (pair.getName().equals("resin-ejb"))
            ejbJndiName = jndiPrefix + '/' + pair.getName();
          else {
            jndiName = jndiPrefix + '/' + pair.getName();
            break;
          }
        }

        if (jndiName == null)
          jndiName = ejbJndiName;
      }

      return (EntityManager) ic.lookup(jndiName);
    } catch (NamingException e) {
      log.finest("@PersistenceUnit amber lookup: " + e.toString());
    }

    return null;
  }

  private EntityManager findProviderPersistenceUnit()
    throws ConfigException
  {
    PersistenceUnitInfo info = findPersistenceUnitInfo(_unitName);

    PersistenceProvider []providers = getProviderList();

    for (int i = 0; i < providers.length; i++) {
      EntityManagerFactory factory
	= providers[i].createContainerEntityManagerFactory(info, null);

      if (factory != null)
	return factory.createEntityManager();
    }

    return null;
  }

  private PersistenceUnitInfo findPersistenceUnitInfo(String unitName)
    throws ConfigException
  {
    HashMap<String,PersistenceUnitInfo> unitMap = _localInfo.getLevel();

    if (unitMap == null) {
      unitMap = loadUnitMap();
      _localInfo.set(unitMap);
    }

    PersistenceUnitInfo info = null;

    if ("".equals(unitName) && unitMap.size() == 1) {
      Iterator<PersistenceUnitInfo> iter = unitMap.values().iterator();

      if (iter.hasNext())
	info = iter.next();
    }
    else
      info = unitMap.get(unitName);

    return info;
  }

  private HashMap<String,PersistenceUnitInfo> loadUnitMap()
  {
    HashMap<String,PersistenceUnitInfo> unitMap;
    unitMap = new HashMap<String,PersistenceUnitInfo>();

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      Enumeration e = loader.getResources("META-INF/persistence.xml");
      while (e.hasMoreElements()) {
	URL url = (URL) e.nextElement();

	PersistenceConfig pConfig = new PersistenceConfig();

	Path path = Vfs.lookup(url.toString());

	new Config().configure(pConfig, path);

	for (PersistenceUnitConfig pUnit : pConfig.getUnitList()) {
	  unitMap.put(pUnit.getName(), pUnit);
	}
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(getLocation() + e.toString(), e);
    }

    return unitMap;
  }

  private static PersistenceProvider []getProviderList()
  {
    PersistenceProvider []providers = _localProviders.getLevel();

    if (providers != null)
      return providers;

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    ArrayList<PersistenceProvider> list = new ArrayList<PersistenceProvider>();

    try {
      Enumeration e = loader.getResources(SERVICE);

      while (e.hasMoreElements()) {
        URL url = (URL) e.nextElement();

        PersistenceProvider provider = loadProvider(url, loader);

        if (provider != null)
          list.add(provider);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    providers = new PersistenceProvider[list.size()];
    list.toArray(providers);

    _localProviders.set(providers);

    return providers;
  }

  private static PersistenceProvider loadProvider(URL url, ClassLoader loader)
  {
    InputStream is = null;
    try {
      is = url.openStream();
      int ch;

      while ((ch = is.read()) >= 0) {
        if (Character.isWhitespace((char) ch)) {
        }
        else if (ch == '#') {
          for (; ch >= 0 && ch != '\n' && ch != '\r'; ch = is.read()) {
          }
        }
        else {
          StringBuilder sb = new StringBuilder();

          for (;
               ch >= 0 && ! Character.isWhitespace((char) ch);
               ch = is.read()) {
            sb.append((char) ch);
          }

          String className = sb.toString();

          Class cl = Class.forName(className, false, loader);

          return (PersistenceProvider) cl.newInstance();
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }

    return null;
  }

  private String getLocation()
  {
    return _field.getDeclaringClass().getName() + '.' + _field.getName() + ": ";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _field + "," + _persistenceUnit + "]";
  }
}
