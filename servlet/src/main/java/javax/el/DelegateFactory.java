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
 * @author Paul Cowan
 */

package javax.el;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.Properties;
import java.util.logging.*;

public class DelegateFactory
{
  private static final Logger log
    = Logger.getLogger(DelegateFactory.class.getName());

  public static <T> T newInstance(Class<T> elClass, 
                                  String fallbackImplName,
                                  Object arg)
  {
    String delegateClassName = null;

    String propertyName = elClass.getName();
    String servicename = String.format("META-INF/services/%s", propertyName);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL url = loader.getResource(servicename);

    if (url != null) {
      InputStream is = null;
      try {
        is = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        delegateClassName = reader.readLine();
      } catch (IOException e) {
        log.log(Level.FINEST, "error reading from " + url, e);
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException e) {
          log.log(Level.FINEST, "error closing input stream " + url, e);
        }
      }
    }

    if (delegateClassName == null) {
      String javaHome = System.getProperty("java.home");
      char slash = File.separatorChar;
      File file = new File(javaHome + slash + "lib" + slash + "el.properties");

      if (file.exists()) {
        Properties elProperties = new Properties();
        Reader reader = null;
        try {
          reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
          elProperties.load(reader);
          delegateClassName = elProperties.getProperty(propertyName);
        } catch (FileNotFoundException e) {
          log.log(Level.FINEST, "file " + file + " does not exist", e);
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
          log.log(Level.FINEST, "error reading from file " + file);
        } finally {
          try {
            if (reader != null)
              reader.close();
          } catch (IOException e) {
            log.log(Level.FINEST, e.getMessage(), e);
          }
        }
      }
    }

    if (delegateClassName == null)
      delegateClassName = System.getProperty(propertyName);

    if (delegateClassName == null)
      delegateClassName = fallbackImplName;

    try {
      Class<?> c = loader.loadClass(delegateClassName);
      
      if (! elClass.isAssignableFrom(c)) {
        String msg = String.format("class %s is not assignable from %s", 
                                   delegateClassName,
                                   elClass.getName());
        throw new ELException(msg);
      }
      
      Class<T> delegateClass = (Class<T>) c;
      Constructor<T> constructor = null;
      
      try {
        if (arg != null) {
          constructor = findConstructor(delegateClass, arg.getClass());
        } else {
          constructor = delegateClass.getConstructor(null);
        }
      } catch (NoSuchMethodException e) {
        String msg = String.format("class %s does not declare constructor accepting instance of %s", 
                                   delegateClassName,
                                   arg.getClass().getName());
        throw new ELException(msg, e);
      }
      
      T result = null;
      if (arg != null) {
        result = constructor.newInstance(arg);
      } else {
        result = constructor.newInstance();
      }
      
      if (result == null)
        result = delegateClass.newInstance();

      return result;
    } catch (InstantiationException e) {
      throw new ELException(String.format("can't create an instance of class %s",
                                          delegateClassName), e);
    } catch (IllegalAccessException e) {
      throw new ELException(String.format("can't create an instance of class %s",
                                          delegateClassName), e);
    } catch (InvocationTargetException e) {
      throw new ELException(String.format("can't create an instance of class %s",
                                          delegateClassName), e);
    } catch (ClassNotFoundException e) {
      throw new ELException(e.getMessage(), e);
    }
  }
    
  private static <T> Constructor<T> findConstructor(Class<T> targetClass, 
                                                    Class<?> argClass)
    throws NoSuchMethodException
  {
    Constructor<?> []constructors = targetClass.getConstructors();
    for (Constructor<?> constructor : constructors) {
      Class<?> []params = constructor.getParameterTypes();
      if (params.length == 1 && params[0].isAssignableFrom(argClass)) {
        return (Constructor<T>) constructor;
      }
    }
    
    throw new NoSuchMethodException(String.format("class %s does not declare a constructor accepting instance of %s",
                                                  targetClass, argClass));
  }
   
}
