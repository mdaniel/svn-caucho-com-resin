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

package com.caucho.widget.xml;

import com.caucho.widget.*;

import java.net.URL;
import java.util.HashMap;
import java.io.FileNotFoundException;

public class TestXml
  extends WidgetTestCase
{
  private HashMap<String, Object> _varMap = new HashMap<String, Object>();

  public TestXml()
  {
    _varMap.put("test", this);
  }

  public void addState(String state)
  {
    super.addState(state);
  }

  protected Widget createFromXml(String xmlFile)
    throws WidgetException, FileNotFoundException
  {
    WidgetFactory widgetFactory = new WidgetFactory();

    URL url = getClass().getResource(xmlFile);

    if (url == null)
      throw new FileNotFoundException(xmlFile);

    Widget widget = widgetFactory.createFromXml(url, _varMap);

    return widget;
  }

  public void testBasic()
    throws Exception
  {
    Widget widget = createFromXml("basic.xml");

    render(widget);

    assertOutputEquals("hello, world\n");
  }

  public void testMultiple()
    throws Exception
  {
    Widget widget = createFromXml("multiple.xml");

    render(widget);

    assertOutputEquals(
      "hello",
      "goodbye",
      "");
  }

  public void testLifecycle()
    throws Exception
  {
    Widget widget = createFromXml("lifecycle.xml");

    render(widget);

    assertState(
      "init",
      "invocation",
      "request",
      "response",
      "url",
      "destroy"
    );
  }

  public void testEmptyScript()
    throws Exception
  {

    try {
      Widget widget = createFromXml("emptyScript.xml");
    }
    catch (WidgetException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("no contents"));

      return;
    }

    fail("expecting exception");
  }

  public void testSetVar()
    throws Exception
  {
    Widget widget = createFromXml("setVar.xml");

    render(widget);

    assertOutputEquals(
      "<span class='Textbox'>hello, world</span>"
    );
  }

  public void testContainment()
    throws Exception
  {
    Widget widget = createFromXml("containment.xml");

    render(widget);

    assertOutputEquals(
        "<div class='Box horizontal'>"
      + "<span class='Label'>hello</span>"
      + "<div class='Box horizontal'>"
      + "<span class='Label'>goodbye</span>"
      + "</div>"
      + "</div>"
    );
  }

  public void testAttributeSetter()
    throws Exception
  {
    Widget widget = createFromXml("attributeSetter.xml");

    render(widget);

    assertOutputEquals(
      "<span class='Label'>hello, world</span>"
    );

  }

  public void testNameAsXmlElement()
    throws Exception
  {
    Widget widget = createFromXml("nameAsXmlElement.xml");

    render(widget);

    assertOutputEquals(
      "<span class='Label'>hello, world</span>"
    );
  }

  public void testComment()
    throws Exception
  {
    Widget widget = createFromXml("comment.xml");

    render(widget);

    assertOutputEquals(
      "<span class='Label'>hello, world</span>"
    );
  }

  public void testTextNotAllowed()
    throws Exception
  {
    try {
      createFromXml("textNotAllowed.xml");
    }
    catch (WidgetException ex) {
      return;
    }

    fail("expecting exception");
  }
}
