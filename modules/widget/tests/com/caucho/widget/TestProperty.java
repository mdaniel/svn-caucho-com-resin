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

import java.io.IOException;

public class TestProperty
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    render(widget);

    assertState("null: hello");
  }

  public void testSetInInit()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      {
        setId("a");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        setProp(init, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    render(widget);

    assertState("a: goodbye");
  }

  public void testSetInInitNoId()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        setProp(init, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    render(widget);

    assertState("null: goodbye");
  }

  public void testSetInRequest()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      {
        setId("a");
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        setProp(request, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    render(widget);

    assertState("a: goodbye");
  }

  public void testReinvoke()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      {
        setId("a");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        addState("" + getId() + ": " + getProp(response));

        setProp(response, "goodbye");

        WidgetURL url  = response.getUrl();

        response.getWriter().write(url.toString());
      }
    };

    render(widget);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(widget);

    assertState("a: hello",
                "a: goodbye");
  }

  /**
   * A property cannot be set unless there is an id.
   */
  public void testImmutable()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        setProp(request, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    try {
      render(widget);
    }
    catch (UnsupportedOperationException ex) {
      return;
    }


    fail("expecting exception");
  }

  /**
   * A property can be set in init() even if there is no id.
   */
  public void testImmutableInInit()
    throws Exception
  {
    Widget widget = new SimplePropertyWidget()
    {
      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        setProp(init, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    render(widget);

    assertState("null: goodbye");
  }

  public void testInherited()
    throws Exception
  {
    InheritedPropertyWidget top = new InheritedPropertyWidget()
    {
      {
        setId("top");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    InheritedPropertyWidget middle = new InheritedPropertyWidget()
    {
      {
        setId("middle");
        setProp("goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    InheritedPropertyWidget bottom = new InheritedPropertyWidget()
    {
      {
        setId("bottom");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("" + getId() + ": " + getProp(response));

        super.response(response);
      }
    };

    middle.add(bottom);
    top.add(middle);

    render(top);

    assertState(
      "top: hello",
      "middle: goodbye",
      "bottom: goodbye");
  }

  public void testTransient()
    throws Exception
  {
    TransientWidget widget = new TransientWidget();
    widget.setTransient(true);

    render(widget);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(widget);

    url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(widget);

    assertState(
      "a: hello",
      "a: hello",
      "a: hello");
  }

  public void testSwitchToTransient()
    throws Exception
  {
    TransientWidget widget = new TransientWidget();

    render(widget);

    String url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(widget);

    url = getExternalConnection().getOutputAsString();

    setExternalConnection(url);

    render(widget);

    assertState(
      "a: hello",
      "a: goodbye",
      "a: hello");
  }

  private class SimplePropertyWidget
    extends AbstractWidgetContainer
  {
    private StringProperty _property = createProperty();

    protected StringProperty createProperty()
    {
      return new StringProperty(this, "prop", false, "hello");
    }

    public void setProp(String prop)
    {
      _property.setValue(prop);
    }

    public String getProp()
    {
      return _property.getValue();
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      super.init(init);

      _property.init(init);
    }

    public void setProp(VarContext context, String prop)
    {
      _property.setValue(context, prop);
    }

    public String getProp(VarContext context)
    {
      return _property.getValue(context);
    }
  };

  private class InheritedPropertyWidget
    extends SimplePropertyWidget
  {
    protected StringProperty createProperty()
    {
      return new StringProperty(this, "prop", true, "hello");
    }
  }

  private class TransientWidget
    extends SimplePropertyWidget
  {
    private TransientProperty _transientProperty = new TransientProperty(this);

    {
      setId("a");
    }

    public void setTransient(boolean isTransient)
    {
      _transientProperty.setValue(isTransient);
    }

    public boolean isTransient()
    {
      return _transientProperty.getValue();
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      super.init(init);

      _transientProperty.init(init);
    }

    public void setTransient(VarContext context, boolean isTransient)
    {
      _transientProperty.setValue(context, isTransient);
    }

    public boolean isTransient(VarContext context)
    {
      return _transientProperty.getValue(context);
    }

    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      super.response(response);

      String prop = getProp(response);

      addState("" + getId() + ": " + prop);

      if (prop.equals("goodbye"))
        setTransient(response, true);

      setProp(response, "goodbye");

      WidgetURL url  = response.getUrl();

      response.getWriter().write(url.toString());
    }
  };

}
