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

import java.io.StringWriter;
import java.io.PrintWriter;

public class TestWidgetWriterXhtml
  extends TestCase
{
  private StringWriter _stringWriter;
  private WidgetWriter _out;

  protected void setUp()
    throws Exception
  {
    super.setUp();
    _stringWriter = new StringWriter();
    _out = new WidgetWriter(_stringWriter);
    _out.setContentType("application/xhtml+xml");
  }

  protected void tearDown()
    throws Exception
  {
    _out = null;
    _stringWriter = null;
    super.tearDown();
  }

  protected void assertResult(String expect)
  {
    assertEquals(expect, _stringWriter.toString());
  }

  protected void assertResult(String ... expected)
  {
    StringWriter buf = new StringWriter();
    PrintWriter compare = new PrintWriter(buf);

    for (String expect : expected) {
      compare.println(expect);
    }

    String expectedValue = buf.toString();
    String value = _stringWriter.toString();

    assertEquals(expectedValue, value);
  }

  public void testB()
  {
    _out.startElement("b");
    _out.writeText("hello");
    _out.startElement("i");
    _out.writeText("and");
    _out.endElement("i");
    _out.writeText("goodbye");
    _out.endElement("b");

    assertResult("<b>hello<i>and</i>goodbye</b>");
  }

  public void testDiv()
  {
    _out.startElement("div");
    _out.endElement("div");

    _out.startElement("div");
    _out.writeText("hello, world");
    _out.endElement("div");

    assertResult(
      "<div>",
      "</div>",
      "<div>",
      "hello, world",
      "</div>");
  }

  public void testMeta()
  {
    _out.startElement("head");

    _out.startElement("meta");
    _out.endElement("meta");

    _out.startElement("meta");
    _out.writeAttribute("foo", "bar");
    _out.endElement("meta");

    _out.endElement("head");

    assertResult(
      "<head>",
      "<meta />",
      "<meta foo='bar' />",
      "</head>"
    );
  }

  public void testTitle()
  {
    _out.startElement("head");

    _out.startElement("title");
    _out.endElement("title");

    _out.startElement("title");
    _out.writeText("hello, world");
    _out.endElement("title");

    _out.endElement("head");

    assertResult(
      "<head>",
      "<title></title>",
      "<title>hello, world</title>",
      "</head>"
    );
  }

  public void testH1()
  {
    _out.writeText("foo");
    _out.writeText("bar");

    _out.startElement("h1");
    _out.writeText("hello, world");
    _out.endElement("h1");
    _out.writeText("baz");
    _out.println();

    assertResult(
      "foobar",
      "<h1>hello, world</h1>",
      "baz"
    );
  }

  public void testP()
  {
    _out.writeText("foo");
    _out.writeText("bar");

    _out.startElement("p");
    _out.writeText("hello, world");
    _out.endElement("p");
    _out.writeText("baz");
    _out.println();

    assertResult(
      "foobar",
      "<p>",
      "hello, world",
      "</p>",
      "baz"
    );
  }

  public void testUl()
  {
    _out.writeText("foo");

    _out.startElement("ul");

    _out.startElement("li");
    _out.writeText("hello");
    _out.endElement("li");

    _out.startElement("li");
    _out.writeText("and");
    _out.endElement("li");

    _out.startElement("li");
    _out.startElement("p");
    _out.writeText("then");
    _out.endElement("p");
    _out.endElement("li");

    _out.startElement("li");
    _out.writeText("goodbye");
    _out.endElement("li");

    _out.endElement("ul");

    assertResult(
      "foo",
      "<ul>",
      "<li>hello</li>",
      "<li>and</li>",
      "<li>",
      "<p>",
      "then",
      "</p>",
      "</li>",
      "<li>goodbye</li>",
      "</ul>"
    );
  }

  public void testBr()
  {
    _out.writeText("foo");
    _out.startElement("br");
    _out.endElement("br");
    _out.writeText("bar");

    _out.writeText("foo");
    _out.writeElement("br");
    _out.writeText("bar");
    _out.println();

    assertResult(
      "foo<br />",
      "barfoo<br />",
      "bar"
    );
  }

  public void testHr()
  {
    _out.writeText("foo");
    _out.writeElement("hr");
    _out.writeText("bar");
    _out.println();

    assertResult(
      "foo",
      "<hr />",
      "bar"
    );
  }

  public void testAttributeBoolean()
  {
    _out.startElement("foo");
    _out.writeAttribute("selected", true);
    _out.endElement("foo");

    assertResult("<foo selected='SELECTED'></foo>");
  }

}
