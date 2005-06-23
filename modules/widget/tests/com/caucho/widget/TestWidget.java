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

/**
 * TODO:
 *
 * - refactor, need ParameterMapFactory
 *
 * - check for TestLifecycleContainment
 *
 * - TextboxWidget has no url()?
 *
 * - WidgetWriter <div class='ffo'><span class='Bar'/><span class='Bar'/></div>
 * - WidgetContext implements VarContext
 *
 * - still some createXXXURL methods, should be getXXX
 *
 *  - base class for propertiues should not assume parameter storage
 * 
 * - all properties should use one interceptor, also one check for transient
 *
 * - test that Properties only save non-default values
 *
 * - refactor State/StateWalker/RootState, start with WidgetParameter{State|StateWalker|RootState}
 *   consider an abstract superclass
 *   should be InitState, ActiveState, URLState
 *
 * - the WidgetURL impl currently reuses one object, it should use a pool
 *
 * - simplified if there is a
 *   refactor X{State|StateWalker|RootState} for each lifecycle
 *   Then ActiveStateWalker just manages those
 *
 *   invocation.setLastModified() and invocation.setExpirationCache()
 *
 * - ExternalConnection.createResourceURL
 *   See javadoc, currently unimplemented.
 *   This is tricky, because it should be possible to specify a file that is
 *   bundled with the class.  But offering access to those files exposes
 *   all of the source so there has to be some way to protect against that
 *   (some kind of crypto hash maybe)  http://server:port/portal/resource/com/hogwarts/FooWidget/resource.gif?HASH=XXYYZZ
 *
 * - setCssClass() - interpret "+" to mean "add to current value", "-" to mean
 *   "remove from current value"
 *
 * - WidgetResponse.addScript(String mimeType, String resourceURL)
 * - WidgetResponse.includeScript(String mimeType, String resourceURL)
 * - WidgetResponse.includeScript(String mimeType, String resourceURL, String language)
 * - WidgetResponse.includeScript(String mimeType, Reader reader)
 *   use same lookup strategy as createResourceURL()
 *   Thre should be some way to do all of the following:
 *      - add a reference to a script that is included by the browser
 *      - add a script that is merged into the content of the response
 *        - parse the included script, pass it variables
 *
 *    mimeType is typically used for javascript or css
 *
 * -  A Qualified naming scheme for widgets and vars
 *
 * - VarDefintion.isPersistent()
 *   a persistent var is associated with the user in some way, similar to
 *   a preference in portlet
 *
 * - WidgetReponse.createTargetURL()
 *   Create a url that targets a widget, the widget must be a
 *   namespace.  This is for iframes.
 *
 * - easily specify an XSL stylesheet,
 *   use same lookup strategy as createResourceURL()
 *
 * - fix destroy() methods, they should never throw exceptions
 */
public class TestWidget
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    Widget widget = new Widget() {

      public String getId()
      {
        return null;
      }

      public String getTypeName()
      {
        return "Test";
      }

      public boolean isNamespace()
      {
        return false;
      }

      public void add(Widget widget)
      {
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
      }

      public void destroy(WidgetDestroy destroy)
      {
      }
    };

    render(widget);
  }

  public void testLifecycle()
    throws Exception
  {
    Widget widget = new LifecycleWidget(null);

    WidgetFactory widgetFactory = new WidgetFactory();

    WidgetContext context = widgetFactory.createContext(getExternalContext(), widget);

    assertState(
      "Lifecycle[id=]init");

    try {
      context.render(getExternalConnection());
      context.render(getExternalConnection());
    }
    finally {
      context.destroy();
    }

    assertState(
      "Lifecycle[id=]init",

      "Lifecycle[id=]invocation",
      "Lifecycle[id=]request",
      "Lifecycle[id=]response",

      "Lifecycle[id=]invocation",
      "Lifecycle[id=]request",
      "Lifecycle[id=]response",

      "Lifecycle[id=]destroy"
    );
  }


  public void testInterfaces()
    throws Exception
  {
    Widget widget = new Widget() {

      public String getId()
      {
        return "a";
      }

      public String getTypeName()
      {
        return "A";
      }

      public boolean isNamespace()
      {
        return false;
      }

      public void add(Widget widget)
        throws UnsupportedOperationException
      {
        throw new UnsupportedOperationException();
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        addState("init instanceof WidgetInit: " + (init instanceof WidgetInit));
        addState("init instanceof WidgetInvocation: " + (init instanceof WidgetInvocation));
        addState("init instanceof WidgetRequest: " + (init instanceof WidgetRequest));
        addState("init instanceof WidgetResponse: " + (init instanceof WidgetResponse));
        addState("init instanceof WidgetURL: " + (init instanceof WidgetURL));
      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        addState("invocation instanceof WidgetInit: " + (invocation instanceof WidgetInit));
        addState("invocation instanceof WidgetInvocation: " + (invocation instanceof WidgetInvocation));
        addState("invocation instanceof WidgetRequest: " + (invocation instanceof WidgetRequest));
        addState("invocation instanceof WidgetResponse: " + (invocation instanceof WidgetResponse));
        addState("invocation instanceof WidgetURL: " + (invocation instanceof WidgetURL));
      }


      public void request(WidgetRequest request)
        throws WidgetException
      {
        addState("request instanceof WidgetInit: " + (request instanceof WidgetInit));
        addState("request instanceof WidgetInvocation: " + (request instanceof WidgetInvocation));
        addState("request instanceof WidgetRequest: " + (request instanceof WidgetRequest));
        addState("request instanceof WidgetResponse: " + (request instanceof WidgetResponse));
        addState("request instanceof WidgetURL: " + (request instanceof WidgetURL));

      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState("response instanceof WidgetInit: " + (response instanceof WidgetInit));
        addState("response instanceof WidgetInvocation: " + (response instanceof WidgetInvocation));
        addState("response instanceof WidgetRequest: " + (response instanceof WidgetRequest));
        addState("response instanceof WidgetResponse: " + (response instanceof WidgetResponse));
        addState("response instanceof WidgetURL: " + (response instanceof WidgetURL));

        response.getUrl().toString();
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        addState("url instanceof WidgetInit: " + (url instanceof WidgetInit));
        addState("url instanceof WidgetInvocation: " + (url instanceof WidgetInvocation));
        addState("url instanceof WidgetRequest: " + (url instanceof WidgetRequest));
        addState("url instanceof WidgetResponse: " + (url instanceof WidgetResponse));
        addState("url instanceof WidgetURL: " + (url instanceof WidgetURL));
      }

      public void destroy(WidgetDestroy destroy)
      {
        addState("destroy instanceof WidgetInit: " + (destroy instanceof WidgetInit));
        addState("destroy instanceof WidgetInvocation: " + (destroy instanceof WidgetInvocation));
        addState("destroy instanceof WidgetRequest: " + (destroy instanceof WidgetRequest));
        addState("destroy instanceof WidgetResponse: " + (destroy instanceof WidgetResponse));
        addState("destroy instanceof WidgetURL: " + (destroy instanceof WidgetURL));
      }
    };

    render(widget);

    assertState(
      "init instanceof WidgetInit: true",
      "init instanceof WidgetInvocation: false",
      "init instanceof WidgetRequest: false",
      "init instanceof WidgetResponse: false",
      "init instanceof WidgetURL: false",

      "invocation instanceof WidgetInit: false",
      "invocation instanceof WidgetInvocation: true",
      "invocation instanceof WidgetRequest: false",
      "invocation instanceof WidgetResponse: false",
      "invocation instanceof WidgetURL: false",

      "request instanceof WidgetInit: false",
      "request instanceof WidgetInvocation: false",
      "request instanceof WidgetRequest: true",
      "request instanceof WidgetResponse: false",
      "request instanceof WidgetURL: false",

      "response instanceof WidgetInit: false",
      "response instanceof WidgetInvocation: false",
      "response instanceof WidgetRequest: false",
      "response instanceof WidgetResponse: true",
      "response instanceof WidgetURL: false",

      "url instanceof WidgetInit: false",
      "url instanceof WidgetInvocation: false",
      "url instanceof WidgetRequest: false",
      "url instanceof WidgetResponse: false",
      "url instanceof WidgetURL: true",

      "destroy instanceof WidgetInit: true",
      "destroy instanceof WidgetInvocation: false",
      "destroy instanceof WidgetRequest: false",
      "destroy instanceof WidgetResponse: false",
      "destroy instanceof WidgetURL: false"
    );
  }

  public void testLifecycleContainment()
    throws Exception
  {
    LifecycleWidget top = new LifecycleWidget("top");
    LifecycleWidget middle = new LifecycleWidget("mid");
    LifecycleWidget bottom1 = new LifecycleWidget("bt1");
    LifecycleWidget bottom2 = new LifecycleWidget("bt2");

    top.setNamespace(true);
    middle.setNamespace(true);
    bottom1.setNamespace(true);

    middle.add(bottom1);
    middle.add(bottom2);
    top.add(middle);

    WidgetFactory widgetFactory = new WidgetFactory();
    WidgetContext context = widgetFactory.createContext(getExternalContext(), top);

    try {
      context.render(getExternalConnection());
      context.render(getExternalConnection());
    }
    finally {
      context.destroy();
    }

    assertState(
      "Lifecycle[id=bt1]init",
      "Lifecycle[id=bt2]init",
      "Lifecycle[id=mid]init",
      "Lifecycle[id=top]init",

      "Lifecycle[id=bt1]invocation",
      "Lifecycle[id=bt2]invocation",
      "Lifecycle[id=mid]invocation",
      "Lifecycle[id=top]invocation",

      "Lifecycle[id=bt1]request",
      "Lifecycle[id=bt2]request",
      "Lifecycle[id=mid]request",
      "Lifecycle[id=top]request",

      "Lifecycle[id=bt1]response",
      "Lifecycle[id=bt2]response",
      "Lifecycle[id=mid]response",
      "Lifecycle[id=top]response",

      "Lifecycle[id=bt1]invocation",
      "Lifecycle[id=bt2]invocation",
      "Lifecycle[id=mid]invocation",
      "Lifecycle[id=top]invocation",

      "Lifecycle[id=bt1]request",
      "Lifecycle[id=bt2]request",
      "Lifecycle[id=mid]request",
      "Lifecycle[id=top]request",

      "Lifecycle[id=bt1]response",
      "Lifecycle[id=bt2]response",
      "Lifecycle[id=mid]response",
      "Lifecycle[id=top]response",

      "Lifecycle[id=bt1]destroy",
      "Lifecycle[id=bt2]destroy",
      "Lifecycle[id=mid]destroy",
      "Lifecycle[id=top]destroy"

    );
  }

  public void testWriter()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        response.getWriter().print("hello, world");
      }
    };

    render(widget);

    assertOutputEquals("hello, world");
  }

  public void testWriterContainment()
    throws Exception
  {
    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        response.getWriter().print("hello");

        super.response(response);

        response.getWriter().print("goodbye");
      }
    };

    widget.add(
      new AbstractWidget() {
        public void response(WidgetResponse response)
          throws WidgetException, IOException
        {
          response.getWriter().print(" and ");
        }
      });

    render(widget);

    assertOutputEquals("hello and goodbye");
  }

  public void testOutputStream()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        response.getOutputStream().write("hello, world".getBytes());
      }
    };

    render(widget);

    assertOutputEquals("hello, world");
  }

  public void testOutputStreamContainment()
    throws Exception
  {
    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        response.getOutputStream().write("hello".getBytes());

        super.response(response);

        response.getOutputStream().write("goodbye".getBytes());
      }
    };

    widget.add(
      new AbstractWidget() {
        public void response(WidgetResponse response)
          throws WidgetException, IOException
        {
          response.getOutputStream().write(" and ".getBytes());
        }
      });

    render(widget);

    assertOutputEquals("hello and goodbye");
  }

  public void testIdValid()
    throws Exception
  {
    String[] validIds = new String[]{
      "hello",
      "Hello",
      "h1ello",
      "hello1",
    };

    for (String validId : validIds) {
      Widget widget = new LifecycleWidget(validId);

      assertEquals(validId, widget.getId());
    }
  }

  public void testIdInvalid()
    throws Exception
  {
    String[] invalidIds = new String[]{
      "1hello",
      "h_ello",
      "h*ello",
      " hello",
      "he llo",
      "hello ",
    };

    for (String invalidId : invalidIds) {
      try {
        Widget widget = new LifecycleWidget(invalidId);
        render(widget);
      }
      catch (IllegalArgumentException ex) {
        assertTrue(ex.getMessage().contains("invalid id"));
        continue;
      }

      fail("expecting exception for id `" + invalidId + "'");
    }
  }

  public void testDuplicateId()
    throws Exception
  {
    LifecycleWidget widget = new LifecycleWidget("container");
    LifecycleWidget widget1 = new LifecycleWidget("b");
    LifecycleWidget widget2 = new LifecycleWidget("b");

    widget.add(widget1);

    try {
      widget.add(widget2);

      render(widget);
    }
    catch (IllegalStateException ex) {
      assertTrue(ex.getMessage().contains("duplicate id"));
      return;
    }

    fail("expecting exception");
  }

  public void testDuplicateIdChildDuplicatesParent()
    throws Exception
  {
    LifecycleWidget widget = new LifecycleWidget("container");
    LifecycleWidget widget1 = new LifecycleWidget("container");

    widget.add(widget1);

    try {
      render(widget);
    }
    catch (IllegalStateException ex) {
      assertTrue(ex.getMessage().contains("duplicate id"));
      return;
    }

    fail("expecting exception");
  }

  public void testFind()
    throws Exception
  {
    MiddleFindingWidget top = new MiddleFindingWidget("top");
    MiddleFindingWidget middle = new MiddleFindingWidget("middle");
    MiddleFindingWidget bottom = new MiddleFindingWidget("bottom");

    middle.add(bottom);
    top.add(middle);

    render(top);

    assertState(
      "init: from bottom find `middle' returns MiddleFinding[id=middle]",
      "init: from middle find `middle' returns MiddleFinding[id=middle]",
      "init: from top find `middle' returns MiddleFinding[id=middle]",
      "invocation: from bottom find `middle' returns MiddleFinding[id=middle]",
      "invocation: from middle find `middle' returns MiddleFinding[id=middle]",
      "invocation: from top find `middle' returns MiddleFinding[id=middle]",
      "request: from bottom find `middle' returns MiddleFinding[id=middle]",
      "request: from middle find `middle' returns MiddleFinding[id=middle]",
      "request: from top find `middle' returns MiddleFinding[id=middle]",
      "response: from bottom find `middle' returns MiddleFinding[id=middle]",
      "response: from middle find `middle' returns MiddleFinding[id=middle]",
      "response: from top find `middle' returns MiddleFinding[id=middle]"
    );
  }

  public void testFindNamespace()
    throws Exception
  {
    MiddleFindingWidget verytop = new MiddleFindingWidget("verytop");
    MiddleFindingWidget top = new MiddleFindingWidget("top");
    MiddleFindingWidget middle = new MiddleFindingWidget("middle");
    MiddleFindingWidget bottom = new MiddleFindingWidget("bottom");

    top.setNamespace(true);
    middle.setNamespace(true);
    bottom.setNamespace(true);

    middle.add(bottom);
    top.add(middle);
    verytop.add(top);

    render(verytop);

    assertState(
      "init: from bottom find `middle' returns null",
      "init: from middle find `middle' returns MiddleFinding[id=middle]",
      "init: from top find `middle' returns MiddleFinding[id=middle]",
      "init: from verytop find `middle' returns null",
      "invocation: from bottom find `middle' returns null",
      "invocation: from middle find `middle' returns MiddleFinding[id=middle]",
      "invocation: from top find `middle' returns MiddleFinding[id=middle]",
      "invocation: from verytop find `middle' returns null",
      "request: from bottom find `middle' returns null",
      "request: from middle find `middle' returns MiddleFinding[id=middle]",
      "request: from top find `middle' returns MiddleFinding[id=middle]",
      "request: from verytop find `middle' returns null",
      "response: from bottom find `middle' returns null",
      "response: from middle find `middle' returns MiddleFinding[id=middle]",
      "response: from top find `middle' returns MiddleFinding[id=middle]",
      "response: from verytop find `middle' returns null"
    );
  }

  public void testMissingSuperInit()
    throws Exception
  {
    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public void init(WidgetInit init)
        throws WidgetException
      {
      }
    };

    widget.add(
      new AbstractWidget() {
      });

    try {
      render(widget);
    }
    catch (IllegalStateException ex) {
      return;
    }

    fail("expecting exception");
  }

  public class MiddleFindingWidget
    extends AbstractWidgetContainer
  {
    public MiddleFindingWidget(String id)
    {
      super(id);
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      setTypeName("MiddleFinding");

      super.init(init);

      addState("init: from " + getId() + " find `middle' returns " + init.find("middle"));
    }

    public void invocation(WidgetInvocation invocation)
      throws WidgetException
    {
      super.invocation(invocation);

      addState("invocation: from " + getId() + " find `middle' returns " + invocation.find("middle"));
    }

    public void request(WidgetRequest request)
      throws WidgetException
    {
      super.request(request);

      addState("request: from " + getId() + " find `middle' returns " + request.find("middle"));
    }

    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      super.response(response);

      addState("response: from " + getId() + " find `middle' returns " + response.find("middle"));
    }
  }

  private class LifecycleWidget extends AbstractWidgetContainer
  {
    public LifecycleWidget(String id)
    {
      super(id);
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      super.init(init);

      addState(toString() + "init");
    }

    public void invocation(WidgetInvocation invocation)
      throws WidgetException
    {
      super.invocation(invocation);

      addState(toString() + "invocation");
    }

    public void request(WidgetRequest request)
      throws WidgetException
    {
      super.request(request);

      addState(toString() + "request");
    }

    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      super.response(response);

      addState(toString() + "response");
    }

    public void destroy(WidgetDestroy destroy)
    {
      super.destroy(destroy);

      addState(toString() + "destroy");
    }
  }


}
