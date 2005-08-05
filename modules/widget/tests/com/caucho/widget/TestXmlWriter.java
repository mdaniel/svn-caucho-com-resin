/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.vfs.XmlWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

public class TestXmlWriter
  extends TestCase
{
  private StringWriter _stringWriter;
  private XmlWriter _out;

  protected void setUp()
    throws Exception
  {
    super.setUp();
    _stringWriter = new StringWriter();
    _out = new XmlWriter(_stringWriter);
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

  public void testXmlIsDefault()
  {
    _out.startElement("foo");
    _out.writeText("hello, world");
    _out.endElement("foo");

    assertEquals("text/xml", _out.getContentType());
    assertResult("<foo>hello, world</foo>");
  }

  public void testIsPrintWriter()
  {
    assertTrue("instanceof PrintWriter", (_out instanceof java.io.PrintWriter));
  }

  public void testPrint()
    throws IOException
  {
    _out.print("hello");

    assertResult("hello");
  }

  public void testElement()
  {
    _out.startElement("foo");
    _out.endElement("foo");

    assertResult("<foo/>");
  }

  public void testAttribute()
  {
    _out.startElement("foo");
    _out.writeAttribute("att", "the<>&\"'value");
    _out.endElement("foo");

    assertResult("<foo att='the&lt;&gt;&amp;&quot;&rsquo;value'/>");
  }

  public void testAttributeMultiple()
  {
    _out.startElement("foo");
    _out.writeAttribute("att", "the<>&\"'value", "two", "thr ee");
    _out.endElement("foo");

    assertResult("<foo att='the&lt;&gt;&amp;&quot;&rsquo;value two thr ee'/>");
  }

  public void testAttributeMultipleWithNull()
  {
    _out.startElement("foo");
    _out.writeAttribute("att", "the<>&\"'value", null, "two", "thr ee");
    _out.endElement("foo");

    assertResult("<foo att='the&lt;&gt;&amp;&quot;&rsquo;value two thr ee'/>");
  }

  public void testAttributeBoolean()
  {
    _out.startElement("foo");
    _out.writeAttribute("selected", true);
    _out.endElement("foo");

    assertResult("<foo selected='true'/>");
  }

  public void testFlush()
  {
    _out.startElement("foo");
    _out.flush();

    assertResult("<foo/>");
  }

  public void testWriteTextChar()
  {
    _out.writeText('a');
    _out.writeText('<');

    _out.startElement("foo");
    _out.writeText('a');
    _out.writeText('<');
    _out.endElement("foo");

    assertResult("a&lt;<foo>a&lt;</foo>");
  }

  public void testWriteTextString()
  {
    _out.writeText("the<>&\"'value");

    _out.startElement("foo");
    _out.writeText("the<>&\"'value");
    _out.endElement("foo");

    assertResult("the&lt;&gt;&amp;&quot;&rsquo;value<foo>the&lt;&gt;&amp;&quot;&rsquo;value</foo>");
  }

  public void testWriteTextObject()
  {
    Object obj = new Object() {
      public String toString()
      {
        return "the<>&\"'value";
      }
    };

    _out.writeText(obj);

    _out.startElement("foo");
    _out.writeText(obj);
    _out.endElement("foo");

    assertResult("the&lt;&gt;&amp;&quot;&rsquo;value<foo>the&lt;&gt;&amp;&quot;&rsquo;value</foo>");
  }

  public void testWriteTextCharArray()
  {
    char[] buf = "the<>&\"'value".toCharArray();

    _out.writeText(buf);

    _out.startElement("foo");
    _out.writeText(buf);
    _out.endElement("foo");

    assertResult("the&lt;&gt;&amp;&quot;&rsquo;value<foo>the&lt;&gt;&amp;&quot;&rsquo;value</foo>");
  }

  public void testWriteTextCharArrayRange()
  {
    char[] buf = "the<>&\"'value".toCharArray();

    _out.writeText(buf, 2, 4);

    _out.startElement("foo");
    _out.writeText(buf, 2, 4);
    _out.endElement("foo");

    assertResult("e&lt;&gt;&amp;<foo>e&lt;&gt;&amp;</foo>");
  }

  public void testWriteComment()
  {
    _out.writeComment("hello");

    _out.startElement("foo");
    _out.writeComment("<!-- hello -->");
    _out.endElement("foo");

    assertResult("<!-- hello --><foo><!-- &lt;!-- hello --&gt; --></foo>");
  }

  public void testWriteCausesCloseElement()
  {
    _out.startElement("foo");
    _out.write("hello");
    _out.endElement("foo");

    assertResult("<foo>hello</foo>");
  }

  public void testPrintlnCausesCloseElement()
  {
    _out.startElement("foo");
    _out.println();
    _out.write("hello");
    _out.endElement("foo");

    assertResult(
      "<foo>\nhello</foo>"
    );
  }

  public void testIndent()
  {
    _out.setIndenting(true);

    _out.startElement("foo");
    _out.println();

    _out.startElement("bar");
    _out.writeText("barContents");
    _out.endElement("bar");
    _out.println();

    _out.startElement("baz");
    _out.println();
    _out.writeText("bazContents");
    _out.println();
    _out.endElement("baz");
    _out.println();

    _out.startElement("bung");
    _out.println();

    _out.startElement("bar");
    _out.writeText("barContents");
    _out.endElement("bar");

    _out.println();
    _out.endElement("bung");
    _out.println();

    _out.endElement("foo");
    _out.println();

    assertResult(
      "<foo>",
      "  <bar>barContents</bar>",
      "  <baz>",
      "    bazContents",
      "  </baz>",
      "  <bung>",
      "    <bar>barContents</bar>",
      "  </bung>",
      "</foo>"
    );
  }

  public void testIndentWithEmptyElement()
  {
    // in response to discovery of bug

    _out.setIndenting(true);

    _out.startElement("foo");
    _out.println();

    _out.startElement("bar");
    _out.endElement("bar");
    _out.println();

    _out.startElement("baz");
    _out.endElement("baz");
    _out.println();

    _out.endElement("foo");
    _out.println();

     assertResult(
      "<foo>",
      "  <bar/>",
      "  <baz/>",
      "</foo>"
     );
  }
}
