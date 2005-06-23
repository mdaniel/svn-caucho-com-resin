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

public class TestLabelWidget
  extends WidgetTestCase
{
  public void testValue()
    throws Exception
  {
    LabelWidget label = new LabelWidget(null, "hello, world");
    render(label);
    assertOutputEquals("<span class='Label'>hello, world</span>");
  }

  public void testValueWithId()
    throws Exception
  {
    LabelWidget label = new LabelWidget("a", "hello, world");
    render(label);
    assertOutputEquals("<span id='a' class='Label'>hello, world</span>");
  }

  /**
   * For LabelWidget, null and empty string are the same.
   */
  public void testNullValue()
    throws Exception
  {
    LabelWidget labelWidget = new LabelWidget();
    render(labelWidget);
    assertOutputEquals("<span class='Label'></span>");
  }

  public void testValueThenNullValue()
    throws Exception
  {
    // added due to bug discovery
    LabelWidget label = new LabelWidget(null, "hello, world");
    render(label);
    assertOutputEquals("<span class='Label'>hello, world</span>");

    LabelWidget labelWidget = new LabelWidget();
    render(labelWidget);
    assertOutputEquals("<span class='Label'>hello, world</span><span class='Label'></span>");
  }

  public void testEmptyValue()
    throws Exception
  {
    LabelWidget labelWidget = new LabelWidget(null, "");
    render(labelWidget);
    assertOutputEquals("<span class='Label'></span>");
  }

  public void testInitValue()
    throws Exception
  {
    AbstractWidgetContainer container = new AbstractWidgetContainer() {
      private LabelWidget _labelWidget;

      {
        _labelWidget = new LabelWidget();

        add(_labelWidget);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        _labelWidget.setValue(init, "hello, world");
      }
    };

    render(container);

    assertOutputEquals("<span class='Label'>hello, world</span>");
  }

  public void testDynamicValue()
    throws Exception
  {
    LabelContainer container = new LabelContainer("hello", "goodbye");

    render(container);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(container);

    assertState(
      "hello",
      "goodbye");
  }

  public void testNullDynamicValue()
    throws Exception
  {
    LabelContainer container = new LabelContainer("hello", null);

    render(container);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(container);

    assertState(
      "hello",
      "");
  }

  public void testEmptyDynamicValue()
    throws Exception
  {
    LabelContainer container = new LabelContainer("hello", "");

    render(container);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(container);

    assertState(
      "hello",
      "");
  }

  public void testDisabled()
    throws Exception
  {
    LabelWidget label = new LabelWidget(null, "hello, world");
    label.setDisabled(true);
    render(label);
    assertOutputEquals("<span class='Label disabled'>hello, world</span>");
  }

  private class LabelContainer
    extends AbstractWidgetContainer
  {
    private String _firstValue;
    private String _secondValue;

    public LabelContainer(String firstValue, String secondValue)
    {
      _firstValue = firstValue;
      _secondValue = secondValue;

      LabelWidget labelWidget = new LabelWidget("a");

      add(labelWidget);
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      super.init(init);
    }

    public void request(WidgetRequest request)
      throws WidgetException
    {
      super.request(request);

      LabelWidget labelWidget = request.find("a");

      String currentValue = labelWidget.getValue(request);

      if (currentValue.equals(""))
        labelWidget.setValue(request, _firstValue);
      else
        labelWidget.setValue(request, _secondValue);
    }

    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      LabelWidget labelWidget = response.find("a");

      String currentValue = labelWidget.getValue(response);

      addState(currentValue);

      WidgetURL url = response.getUrl();

      response.getWriter().write(url.toString());
    }
  };
}
