/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;

import com.caucho.resin.HttpEmbed;
import com.caucho.resin.ResinEmbed;
import com.caucho.resin.WebAppEmbed;

/**
 * Resin runner runs a JUnit 4 test backed by a Resin context.
 * 
 * TODO Add more Javadoc since this is a public API.
 */
public class ResinRunner extends BlockJUnit4ClassRunner {
  private int _httpPort = 8086;
  private String _webApplicationContext = "/";
  private String _webApplicationRoot = ".";
  private String _resinXmlPath = null;

  private ResinEmbed _resinEmbeddedContainer;

  public ResinRunner(Class<?> testClass) throws Throwable
  {
    super(testClass);

    ResinConfiguration resinConfiguration = testClass
        .getAnnotation(ResinConfiguration.class);

    _httpPort = resinConfiguration.httpPort();
    _webApplicationContext = resinConfiguration.webApplicationContext();
    _webApplicationRoot = resinConfiguration.webApplicationRoot();
    _resinXmlPath = resinConfiguration.resinXml();
  }

  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier)
  {
    ResinEmbed resinEmbeddedContainer = getResinEmbeddedContainer();

    super.runChild(method, notifier);
  }

  protected ResinEmbed getResinEmbeddedContainer()
  {
    if (_resinEmbeddedContainer == null) {
      if (_resinXmlPath == null) {
        _resinEmbeddedContainer = new ResinEmbed();
      } else {
        _resinEmbeddedContainer = new ResinEmbed(_resinXmlPath);
      }

      _resinEmbeddedContainer.addScanRoot();

      _resinEmbeddedContainer.addPort(new HttpEmbed(_httpPort));
      _resinEmbeddedContainer.addWebApp(new WebAppEmbed(_webApplicationContext,
          _webApplicationRoot));

      _resinEmbeddedContainer.start();
    }

    return _resinEmbeddedContainer;
  }
}