/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.widget;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;

abstract public class WidgetTestCase
  extends TestCase
{
  private MockExternalContext _externalContext;
  private MockExternalConnection _externalConnection;
  private StringBuilder _state = new StringBuilder();
  private static final String STATE_SEPARATOR = "\n";

  public void setUp()
    throws Exception
  {
    _externalContext = new MockExternalContext();
    _externalConnection = new MockExternalConnection();
  }

  public void tearDown()
    throws Exception
  {
    _externalConnection = null;
    _externalContext = null;
    _state.setLength(0);
  }

  protected MockExternalContext getExternalContext()
  {
    return _externalContext;
  }

  protected MockExternalConnection getExternalConnection()
  {
    return _externalConnection;
  }

  protected void setExternalConnection(String urlString)
    throws MalformedURLException
  {
    _externalConnection = new MockExternalConnection(urlString);
  }

  protected void render(Widget widget)
    throws WidgetException, IOException
  {
    WidgetFactory widgetFactory = new WidgetFactory();
    WidgetContext context = widgetFactory.createContext(getExternalContext(), widget);

    try {
      context.render(getExternalConnection());
    }
    finally {
      context.destroy();
    }

    /** XXX: dead code
    WidgetApp app = new WidgetAppImpl();

    app.setExternalContext(getExternalContext());
    app.setWidget(widget);

    app.init();

    try {
      app.render(getExternalConnection());
    }
    finally {
      app.destroy();
    }
     **/
  }

  protected void addState(String state)
  {
    if (_state.length() > 0)
      _state.append(STATE_SEPARATOR);

    if (state == null)
      _state.append("<null>");
    else
      _state.append(state);
  }

  protected void assertState(String ... expected)
  {
    StringBuilder buf = new StringBuilder();

    for (String expect : expected) {
      if (buf.length() > 0)
        buf.append(STATE_SEPARATOR);

      if (expect == null)
        expect = "<null>";

      buf.append(expect);
    }

    String expectedValue = buf.toString();
    String value = _state.toString();

    assertEquals(expectedValue, value);
  }

  protected void assertOutputEquals(String expected)
  {
    String value = getExternalConnection().getOutputAsString();

    value = normalize(value);
    expected = normalize(expected);

    assertEquals(expected, value);
  }

  private String normalize(String value)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);

      if (ch == '\r') {
        int j = i + 1;

        if (j < value.length()) {
          char ch2 = value.charAt(j);

          if (ch2 == '\n') {
            i = j;
            printWriter.println();
          }
          else {
            printWriter.print("\r");
          }
        }
        else {
          printWriter.print("\r");
        }
      }
      else if (ch == '\n') {
        printWriter.println();
      }
      else
        printWriter.print(ch);
    }

    printWriter.flush();

    return stringWriter.toString();
  }

  protected void assertOutputEquals(String ... expected)
  {
    String value = getExternalConnection().getOutputAsString();

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    boolean isFirst = true;

    for (String expect : expected) {

      if (expect == null)
        expect = "<null>";

      if (isFirst)
        isFirst = false;
      else
        printWriter.println();

      printWriter.print(expect);
    }

    printWriter.flush();

    String compare = stringWriter.toString();

    assertEquals(compare, value);
  }
}
