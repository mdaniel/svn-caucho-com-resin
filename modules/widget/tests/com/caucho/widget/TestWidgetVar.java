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

public class TestWidgetVar
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void setMessage(VarContext context, String value)
      {
        context.setVar(this, MESSAGE, value);
      }

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        setMessage(request, "hello, world");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    render(widget);

    assertState("hello, world");
  }

  public void testDefault()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    render(widget);

    assertState("hello");
  }

  public void testSetInWidgetContext()
    throws Exception
  {
    MockExternalContext externalContext = new MockExternalContext();
    MockExternalConnection externalConnection = new MockExternalConnection();

    WidgetFactory widgetFactory = new WidgetFactory();

    MessageWidget widget = new MessageWidget();

    WidgetContext context = widgetFactory.createContext(externalContext, widget);

    widget.setMessage(context, "goodbye");

    context.render(externalConnection);

    String output = externalConnection.getOutputAsString();

    assertEquals("goodbye", output);
  }

  private class MessageWidget
    extends AbstractWidget
  {
    private static final String MESSAGE = "com.caucho.widget.message";

    public void setMessage(VarContext context, String message)
    {
      context.setVar(this, MESSAGE, message);
    }

    public String getMessage(VarContext context)
    {
      return context.getVar(this, MESSAGE);
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      super.init(init);
      setMessage(init, "hello");
    }

    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      String message = getMessage(response);

      response.getWriter().print(message);
    }
  };
  
  public void testSetInInit()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void setMessage(VarContext context, String message)
      {
        context.setVar(this, MESSAGE, message);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);

        setMessage(init, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    render(widget);

    assertState("goodbye");
  }

  public void testSetAndGetInInit()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void setMessage(VarContext context, String message)
      {
        context.setVar(this, MESSAGE, message);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);

        setMessage(init, "goodbye");

        String message = getMessage(init);

        addState(message);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
      }
    };

    render(widget);

    assertState("goodbye");
  }

  public void testSetInInvocation()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void setMessage(VarContext context, String message)
      {
        context.setVar(this, MESSAGE, message);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);
      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        setMessage(invocation, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    render(widget);

    assertState("goodbye");
  }

  public void testSetAndGetInInvocation()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void setMessage(VarContext context, String message)
      {
        context.setVar(this, MESSAGE, message);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);
      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        setMessage(invocation, "goodbye");

        String message = getMessage(invocation);

        addState(message);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
      }
    };

    render(widget);

    assertState("goodbye");
  }

  public void testSetInRequest()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void setMessage(VarContext context, String message)
      {
        context.setVar(this, MESSAGE, message);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);

      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        setMessage(request, "goodbye");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    render(widget);

    assertState("goodbye");
  }

  public void testInheritance()
    throws Exception
  {
    final String MESSAGE1 = "com.caucho.widget.TestWidget.message1";
    final String MESSAGE2 = "com.caucho.widget.TestWidget.message2";

    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public void request(WidgetRequest request)
        throws WidgetException
      {
        request.setVar(this, MESSAGE1, "value1");
        request.setVar(this, MESSAGE2, "value2");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message1 = response.getVar(this, MESSAGE1);
        String message2 = response.getVar(this, MESSAGE2);

        addState(message1);
        addState(message2);

        super.response(response);
      }
    };

    widget.add(
      new AbstractWidget() {
        public void init(WidgetInit init)
          throws WidgetException
        {
          VarDefinition message1Def = new VarDefinition(MESSAGE1, String.class);
          init.addVarDefinition(message1Def);

          VarDefinition message2Def = new VarDefinition(MESSAGE2, String.class);
          message2Def.setInherited(true);
          init.addVarDefinition(message2Def);
        }

        public void response(WidgetResponse response)
          throws WidgetException, IOException
        {
          String message1 = response.getVar(this, MESSAGE1);
          String message2 = response.getVar(this, MESSAGE2);

          addState(message1);
          addState(message2);

          super.response(response);
        }
      });

    render(widget);

    // value2 is inherited from it's parent
    assertState("value1", "value2", "<null>", "value2");
  }

  // prompted by bug discovery
  public void testInheritanceNoDefaultOrValue()
    throws Exception
  {
    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition varDef = new VarDefinition("var", String.class);
        varDef.setInherited(true);
        init.addVarDefinition(varDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String value = response.getVar(this, "var");

        addState(value);

        super.response(response);
      }
    };

    widget.add(
      new AbstractWidget() {
        public void init(WidgetInit init)
          throws WidgetException
        {
          VarDefinition varDef = new VarDefinition("var", String.class);
          varDef.setInherited(true);
          init.addVarDefinition(varDef);
        }

        public void response(WidgetResponse response)
          throws WidgetException, IOException
        {
          String value = response.getVar(this, "var");

          addState(value);

          super.response(response);
        }
      });

    render(widget);

    assertState("<null>", "<null>");
  }

  public void testDefaultInheritance()
    throws Exception
  {
    final String MESSAGE = "com.caucho.widget.TestWidget.message";

    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      {
        setId("top");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("topdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      {
        setId("middle");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("middledefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer()
    {
      {
        setId("bottom");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("bottomdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    middle.add(bottom);
    top.add(middle);

    render(top);

    assertState("topdefault", "middledefault", "bottomdefault");
  }

  public void testDefaultInheritanceInheritValue()
    throws Exception
  {
    // middle and bottom do not have a default value.
    // Defaults are not inherited, so the value
    // is not inherited from top.
    final String MESSAGE = "com.caucho.widget.TestWidget.message";

    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      {
        setId("top");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("topdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      {
        setId("middle");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer()
    {
      {
        setId("bottom");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    middle.add(bottom);
    top.add(middle);

    render(top);

    assertState("topdefault", "<null>", "<null>");
  }

  public void testDefaultInheritanceSetInInitOverrides()
    throws Exception
  {
    final String MESSAGE = "com.caucho.widget.TestWidget.message";

    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      {
        setId("top");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("topdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      {
        setId("middle");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("middledefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);

        init.setVar(this, messageDef, "middleoverride");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer()
    {
      {
        setId("bottom");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);

        messageDef.setValue("bottomdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    middle.add(bottom);
    top.add(middle);

    render(top);

    assertState("topdefault", "middleoverride", "middleoverride");
  }

  public void testDefaultInheritanceSetInInvocationOverrides()
    throws Exception
  {
    final String MESSAGE = "com.caucho.widget.TestWidget.message";

    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      {
        setId("top");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("topdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      {
        setId("middle");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("middledefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);

      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        invocation.setVar(this, MESSAGE, "middleoverride");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer()
    {
      {
        setId("bottom");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("bottomdefault");
        messageDef.setInherited(true);
        init.addVarDefinition(messageDef);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = response.getVar(this, MESSAGE);

        addState(message);

        super.response(response);
      }
    };

    middle.add(bottom);
    top.add(middle);

    render(top);

    assertState("topdefault", "middleoverride", "middleoverride");
  }

  public void testIncompatibleType()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        init.addVarDefinition(messageDef);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        request.setVar(this, MESSAGE, new Integer(1));
      }
    };

    try {
      render(widget);
    }
    catch (ClassCastException ex) {
      assertTrue(ex.getMessage().contains("is incompatible with"));

      return;
    }

    fail("expecting exception");
  }

  public void testDefaultIncompatibleType()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue(new Integer(1));
        init.addVarDefinition(messageDef);
      }
    };

    try {
      render(widget);
    }
    catch (ClassCastException ex) {
      assertTrue(ex.getMessage().contains("is incompatible with"));
      return;
    }

    fail("expecting exception");
  }

  public void testAllowNull()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        init.addVarDefinition(messageDef);
      }

      public void request(WidgetRequest request)
      {
        request.setVar(this, MESSAGE, null);
      }
    };

    render(widget);
  }

  public void testNotNull()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setAllowNull(false);
        init.addVarDefinition(messageDef);
      }

      public void request(WidgetRequest request)
      {
        request.setVar(this, MESSAGE, null);
      }
    };

    try {
      render(widget);
    }
    catch (IllegalArgumentException ex) {
      assertTrue(ex.getMessage().contains("cannot be null"));
      return;
    }

    fail("expecting exception");
  }

  public void testDefaultNotNull()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setAllowNull(false);
        messageDef.setValue(null);
        init.addVarDefinition(messageDef);
      }
    };

    try {
      render(widget);
    }
    catch (IllegalArgumentException ex) {
      assertTrue(ex.getMessage().contains("cannot be null"));
      return;
    }

    fail("expecting exception");
  }

  public void testLifecycle()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public void setMessage(VarContext context, String value)
      {
        context.setVar(this, MESSAGE, value);
      }

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        setTypeName("Message");

        super.init(init);

        VarDefinition messageVar = new VarDefinition(MESSAGE, String.class);

        messageVar.setValue(getMessage(init) + " (default)");
        init.addVarDefinition(messageVar);

        addState(getMessage(init));

        setMessage(init, getMessage(init) + " init");

        addState(getMessage(init));
      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        setMessage(invocation, getMessage(invocation) +  " invocation");

        addState(getMessage(invocation));
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        super.request(request);

        setMessage(request, getMessage(request) +  " request");

        addState(getMessage(request));
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        setMessage(response, getMessage(response) + " response");

        addState(getMessage(response));

        super.response(response);
      }
    };

    render(widget);

    assertState(
      "null (default)",
      "null (default) init",
      "null (default) init invocation",
      "null (default) init invocation request",
      "null (default) init invocation request response");
  }

  public void testConstant()
    throws Exception
  {
    final VarDefinition MESSAGE
      = new VarDefinition("com.caucho.widget.TestWidget.message", String.class) {
        {
          setValue("hello");
        }
      };

    Widget widgetA = new AbstractWidget()
    {
      {
        setId("a");
      }

      public void setMessage(VarContext context, String value)
      {
        context.setVar(this, MESSAGE, value);
      }

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        setMessage(init, getId() + ": hello");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    Widget widgetB = new AbstractWidget()
    {
      {
        setId("b");
      }

      public void setMessage(VarContext context, String value)
      {
        context.setVar(this, MESSAGE, value);
      }

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        setMessage(init, getId() + ": hello");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        String message = getMessage(response);

        addState(message);
      }
    };

    AbstractWidgetContainer widget = new AbstractWidgetContainer() {};

    widget.add(widgetA);
    widget.add(widgetB);

    render(widget);

    assertState(
      "a: hello",
      "b: hello");
  }

}
