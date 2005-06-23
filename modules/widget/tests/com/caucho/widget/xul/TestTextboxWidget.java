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

package com.caucho.widget.xul;

import com.caucho.widget.*;

import java.io.IOException;

public class TestTextboxWidget
  extends WidgetTestCase
{
  public void testValue()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello, world");
    render(textbox);
    assertOutputEquals("<span class='Textbox'>hello, world</span>");
  }

  public void testValueWithId()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget("a", "hello, world");
    render(textbox);
    assertOutputEquals("<span id='a' class='Textbox'>hello, world</span>");
  }

  public void testEdit()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer()
    {
      {
        add(new TextboxWidget("a", "hello"));
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        TextboxWidget textbox = request.find("a");

        textbox.setEdit(request, true);
      }
    };

    render(container);

    assertOutputEquals("<span id='a' class='Textbox'><input name='a' value='hello'/></span>");
  }

  public void testDynamicEdit()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer()
    {
      {
        add(new TextboxWidget("a", "hello"));
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        TextboxWidget textbox = request.find("a");

        addState(textbox.isEdit(request) ? "true" : "false");

        textbox.setEdit(request, true);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        response.getWriter().write(url.toString());
      }
    };

    render(container);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(container);

    assertState(
      "false",
      "true");
  }

  public void testSetEditWithNoId()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer()
    {
      private TextboxWidget _textbox =  new TextboxWidget(null, "hello");

      {
        add(_textbox);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        addState(_textbox.isEdit(request) ? "true" : "false");

        _textbox.setEdit(request, true);
      }
    };

    try {
      render(container);
    }
    catch (UnsupportedOperationException ex) {
      return;
    }

    fail("expecting exception");
  }

  public void testMultiline()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello, world");
    textbox.setMultiline(true);
    render(textbox);
    assertOutputEquals("<span class='Textbox'>hello, world</span>");
  }

  public void testMultilineEdit()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer()
    {
      {
        TextboxWidget textbox =  new TextboxWidget("a", "hello");
        textbox.setMultiline(true);

        add(textbox);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        TextboxWidget textbox = request.find("a");

        textbox.setEdit(request, true);
      }
    };

    render(container);

    assertOutputEquals("<span id='a' class='Textbox'><textarea name='a'>hello</textarea></span>");
  }

  public void testSize()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello");
    textbox.setEdit(true);
    textbox.setSize(1);
    render(textbox);
    assertOutputEquals("<span class='Textbox'><input size='1' value='hello'/></span>");
  }

  public void testSizeMultiline()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello");
    textbox.setEdit(true);
    textbox.setSize(1);
    textbox.setMultiline(true);
    render(textbox);
    assertOutputEquals("<span class='Textbox'><textarea cols='1'>hello</textarea></span>");
  }

  public void testCols()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello");
    textbox.setEdit(true);
    textbox.setCols(2);
    render(textbox);
    assertOutputEquals("<span class='Textbox'><input size='2' value='hello'/></span>");
  }

  public void testColsMultiline()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello");
    textbox.setEdit(true);
    textbox.setCols(2);
    textbox.setMultiline(true);
    render(textbox);
    assertOutputEquals("<span class='Textbox'><textarea cols='2'>hello</textarea></span>");
  }

  public void testSizeAndCols()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello");
    textbox.setEdit(true);
    textbox.setSize(1);
    textbox.setCols(2);
    render(textbox);
    assertOutputEquals("<span class='Textbox'><input size='1' value='hello'/></span>");
  }

  public void testSizeAndColsMultiline()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello");
    textbox.setEdit(true);
    textbox.setSize(1);
    textbox.setCols(2);
    textbox.setMultiline(true);
    render(textbox);
    assertOutputEquals("<span class='Textbox'><textarea cols='2'>hello</textarea></span>");
  }

  public void testDisabled()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello, world");
    textbox.setDisabled(true);
    render(textbox);
    assertOutputEquals("<span class='Textbox disabled'>hello, world</span>");
  }

  public void testDisabledEdit()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer()
    {
      {
        TextboxWidget textbox = new TextboxWidget("a", "hello");
        textbox.setDisabled(true);
        add(textbox);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        TextboxWidget textbox = request.find("a");

        textbox.setEdit(request, true);
      }
    };

    render(container);

    assertOutputEquals("<span id='a' class='Textbox disabled'><input name='a' disabled='true' value='hello'/></span>");
  }

  public void testReadonly()
    throws Exception
  {
    TextboxWidget textbox = new TextboxWidget(null, "hello, world");
    textbox.setReadonly(true);
    render(textbox);
    assertOutputEquals("<span class='Textbox readonly'>hello, world</span>");
  }

  public void testReadonlyEdit()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer()
    {
      {
        TextboxWidget textbox = new TextboxWidget("a", "hello");
        textbox.setReadonly(true);
        add(textbox);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        TextboxWidget textbox = request.find("a");

        textbox.setEdit(request, true);
      }
    };

    render(container);

    assertOutputEquals("<span id='a' class='Textbox readonly'><input name='a' readonly='true' value='hello'/></span>");
  }

}
