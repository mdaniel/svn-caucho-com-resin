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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.config.types.*;
import com.caucho.soa.client.WebServiceClient;
import com.caucho.ejb.EjbServerManager;
import com.caucho.util.L10N;
import com.caucho.naming.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EjbInjectProgram extends BuilderProgram
{
  private static final Logger log
    = Logger.getLogger(EjbInjectProgram.class.getName());
  private static final L10N L
    = new L10N(EjbInjectProgram.class);

  private String _name;
  private String _beanName;
  private String _mappedName;
  private Class _type;
  private AccessibleInject _field;

  public EjbInjectProgram(String name,
                          String beanName,
                          String mappedName,
                          Class type,
                          AccessibleInject field)
    throws ConfigException
  {
    try {
      _name = name;
      _beanName = beanName;
      _mappedName = mappedName;

      _type = type;

      _field = field;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public String getBeanName()
  {
    return _beanName;
  }

  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    try {
      Object value = lookup();

      if (value == null)
	throw new ConfigException(L.l("'{0}' is an unknown @EJB",
				      _type.getName()));

      _field.inject(bean, value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private Object lookup()
    throws ConfigException
  {
    try {
      Object jndiValue = null;
      Object value = null;
      InitialContext ic = new InitialContext();

      // first see if already bound by the name attribute
      try {
        if (_name != null && ! "".equals(_name)) {
          String fullName = Jndi.getFullName(_name);

          value = ic.lookup(fullName);

          if (value != null) {
            if (! _type.isAssignableFrom(value.getClass())) {

              value = PortableRemoteObject.narrow(value, _type);
            }

            return value;
          }
        }
      } catch (NamingException e) {
        log.finest(e.toString());
      }

      // If not, see if it's bound by mapped-name
      if (_mappedName != null && ! "".equals(_mappedName)) {
        // XXX: s/b link value for stateful(?)
        value = ic.lookup(_mappedName);

        if (value == null) {
          log.fine(L.l("'{0}' is an unknown @EJB mapped-name.", _mappedName));
          return null;
        }
      }
      else {
        EjbServerManager manager = EjbServerManager.getLocal();

        if (manager != null) {
          if (_beanName != null && ! "".equals(_beanName)) {
            try {
              value = ic.lookup(manager.getRemoteJndiPrefix() + "/" + _beanName);
            } catch (NamingException e) {
              log.log(Level.FINEST, e.toString(), e);
            }

            //ejb/0f6d
            try {
              if (value == null)
                value = ic.lookup(manager.getLocalJndiPrefix() + "/" + _beanName);
            } catch (NamingException e) {
              log.log(Level.FINEST, e.toString(), e);
            }
          }

          if (value == null) {
            value = manager.getLocalByInterface(_type);

            if (value instanceof ObjectProxy) {
              jndiValue = value;
              value = ((ObjectProxy) value).createObject(null);
            }
          }

          if (value == null)
            value = manager.getRemoteByInterface(_type);
        }
      }

      if (value == null) {
        EjbRefContext context = EjbRefContext.getLocal();

        if (context != null) {
          // ejb/0f6e
          try {
            value = context.findByBeanName(_field.getDeclaringClass().getName(),
                                           _field.getName(),
                                           _type);
          } catch (NamingException e) {
            log.log(Level.FINEST, e.toString(), e);
          }

          if (value == null) {
            value = context.findByType(_type);
          }
        }
      }

      if (value == null) {
        log.fine(L.l("'{0}' is an unknown @EJB.", _type.getName()));
        return null;
      }

      if (! _type.isAssignableFrom(value.getClass())) {
        try {
          value = PortableRemoteObject.narrow(value, _type);
        } catch (ClassCastException classCastException) {
          EjbServerManager manager = EjbServerManager.getLocal();

          // ejb/0f6d: bean has local and remote interfaces
          if (manager != null) {
            if (_beanName != null && ! "".equals(_beanName)) {
              try {
                value = ic.lookup(manager.getLocalJndiPrefix() + "/" + _beanName);

                if (! _type.isAssignableFrom(value.getClass()))
                  value = PortableRemoteObject.narrow(value, _type);
              } catch (NamingException e) {
                log.log(Level.FINEST, e.toString(), e);
                throw classCastException;
              }
            }
          }
        }
      }

      if (! _type.isAssignableFrom(value.getClass())) {
        throw new ConfigException(L.l("EJB at '{0}' of type {1} is not assignable to field '{2}' of type {3}.",
                                      _name,
                                      value.getClass().getName(),
                                      _field.getName(),
                                      _type.getName()));
      }

      // rebind to name
      if (_name != null && ! "".equals(_name)) {
        log.fine("@EJB binding " + _name + "in JNDI for " + value);

        if (jndiValue != null)
          Jndi.rebindDeepShort(_name, jndiValue);
        else
          Jndi.rebindDeepShort(_name, value);
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (NamingException e) {
      log.finer(String.valueOf(e));
      log.log(Level.FINEST, e.toString(), e);

      return null;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Object configure(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
