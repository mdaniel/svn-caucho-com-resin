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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.junit;

import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.model.*;

import com.caucho.resin.*;

/**
 * ResinJUnit runner runs a JUnit within the context of Resin.
 */
public class ResinJUnitRunner extends BlockJUnit4ClassRunner {
  private Class<?> _testClass;

  private ResinBeanContainer _resinContext;
  private ResinDescription _resinDescription;

  public ResinJUnitRunner(Class<?> testClass)
    throws Throwable
  {
    super(testClass);

    _testClass = testClass;

    _resinDescription = testClass.getAnnotation(ResinDescription.class);
  }

  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier)
  {
    ResinBeanContainer resinContext = getResinContext();
    RequestContext request = resinContext.beginRequest();

    try {
      super.runChild(method, notifier);
    } finally {
      request.close();
    }
  }

  @Override
  protected Object createTest()
    throws Exception
  {
    return getResinContext().getInstance(_testClass);
  }

  protected ResinBeanContainer getResinContext()
  {
    if (_resinContext == null) {
      _resinContext = new ResinBeanContainer();

      String userName = System.getProperty("user.name");
      String workDir = "file:/tmp/" + userName;

      _resinContext.setWorkDirectory(workDir);

      if (_resinDescription != null) {
        for (String module : _resinDescription.modules()) {
          _resinContext.addModule(module);
        }

        for (String conf : _resinDescription.contextConfig()) {
          _resinContext.addBeansXml(conf);
        }
      }

      _resinContext.start();
    }

    return _resinContext;
  }
}
