/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.junit;

import java.net.URL;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.resin.BeanContainerRequest;
import com.caucho.v5.resin.ResinBeanContainer;

/**
 * Resin bean container runner runs a JUnit 4 test backed by a Resin context.
 * 
 * TODO Add more Javadoc since this is a public API.
 */
// TODO Put the entire test in a session context?
public class ResinBeanContainerRunner extends BlockJUnit4ClassRunner {
  private Class<?> _testClass;
  private String _testClassModule;

  private ResinBeanContainer _beanContainer;
  private ResinBeanConfiguration _beanConfiguration;

  public ResinBeanContainerRunner(Class<?> testClass) throws Throwable
  {
    super(testClass);

    _testClass = testClass;
    _testClassModule = getTestClassModule(_testClass);

    _beanConfiguration = testClass.getAnnotation(ResinBeanConfiguration.class);
  }

  @Override
  protected Object createTest() throws Exception
  {
    CandiManager manager = getResinContext().getInstance(CandiManager.class);

    // Make the test class a CDI bean, but do not actually register it with CDI.
    return manager.createTransientObject(_testClass);
  }

  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier)
  {
    ResinBeanContainer beanContainer = getResinContext();

    // Each method is treated as a separate HTTP request.
    BeanContainerRequest request = beanContainer.beginRequest();

    try {
      super.runChild(method, notifier);
    } finally {
      request.close();
    }
  }

  protected ResinBeanContainer getResinContext()
  {
    if (_beanContainer == null) {
      _beanContainer = new ResinBeanContainer();

      _beanContainer.setWorkDirectory(System.getProperty("java.io.tmpdir"));
      _beanContainer.setModule(_testClassModule);
      
      addPackageModule();
      
      if (_beanConfiguration != null) {
        for (String path : _beanConfiguration.classPath()) {
          _beanContainer.addClassPath(path);
        }

        for (String conf : _beanConfiguration.beansXml()) {
          _beanContainer.addBeansXml(conf);
        }
      }

      _beanContainer.start();
    }

    return _beanContainer;
  }
  
  private void addPackageModule()
  {
    ClassLoader loader = _testClass.getClassLoader();
    
    String testPackage = _testClass.getPackage().getName();
    
    URL url = loader.getResource(testPackage.replace('.', '/') + "/META-INF");
    
    if (url != null) {
      _beanContainer.addPackageModule(_testClassModule, testPackage);
    }
    
    url = loader.getResource(testPackage.replace('.', '/') + "/beans.xml");
    
    if (url != null) {
      _beanContainer.addPackageModule(_testClassModule, testPackage);
    }
  }

  private String getTestClassModule(final Class<?> testClass)
  {
    String testClassName = testClass.getName().replace('.', '/') + ".class";
    String packageName = Thread.currentThread().getContextClassLoader()
        .getResource(testClassName).toString();

    // Strip the class-name off the end...
    packageName = packageName.substring(0, packageName.indexOf(testClassName));

    // Strip off '!' in case of jar and zip files...
    if (packageName.indexOf('!') != -1) {
      packageName = packageName.substring(packageName.indexOf('!'));
    }

    // Strip protocol part of URL...
    packageName = packageName.substring(packageName.indexOf("file:") + 5);

    return packageName;
  }
}