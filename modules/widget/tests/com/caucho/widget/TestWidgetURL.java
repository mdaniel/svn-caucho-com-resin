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

public class TestWidgetURL
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/");
  }

  public void testParameter()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("helloworld");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/?widget=helloworld");
  }

  public void testParameterIdRequired()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("helloworld");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    try {
      render(widget);
    }
    catch (UnsupportedOperationException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("required"));
      return;
    }

    fail("expecting exception");
  }

  public void testParameterNull()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter((String) null);

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/");
  }

  public void testParameterEmptyString()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/?widget=");
  }

  public void testPathInfo()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setPathInfo("hello.gif");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/hello.gif");
  }

  public void testPathInfoNull()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setPathInfo(null);

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/");
  }

  public void testPathInfoEmptyString()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setPathInfo("");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        super.response(response);

        String url = response.getUrl().toString();

        addState(url);
      }
    };

    render(widget);

    assertState("http://mock:80/");
  }

  public void testLifecycleContainment()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("helloWorldFromTop");
        url.setParameter("foo", "fooValueFromTop");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("helloWorldFromMiddle");
        url.setParameter("foo", "fooValueFromMiddle");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("helloWorldFromBottom");
        url.setParameter("foo", "fooValueFromBottom");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    bottom.setId("bottom");

    middle.setId("middle");
    middle.setNamespace(true);
    middle.add(bottom);

    top.setId("top");
    top.add(middle);

    render(top);

    assertState(
      "http://mock:80/?top=helloWorldFromTop&top.foo=fooValueFromTop&middle=helloWorldFromMiddle&middle.foo=fooValueFromMiddle&middle-bottom=helloWorldFromBottom&middle-bottom.foo=fooValueFromBottom",
      "http://mock:80/?top=helloWorldFromTop&top.foo=fooValueFromTop&middle=helloWorldFromMiddle&middle.foo=fooValueFromMiddle&middle-bottom=helloWorldFromBottom&middle-bottom.foo=fooValueFromBottom",
      "http://mock:80/?top=helloWorldFromTop&top.foo=fooValueFromTop&middle=helloWorldFromMiddle&middle.foo=fooValueFromMiddle&middle-bottom=helloWorldFromBottom&middle-bottom.foo=fooValueFromBottom"
      );
  }

  public void testPathInfoContainment()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setPathInfo("hello.gif");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setPathInfo("hello.gif");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    middle.setId("middle");

    top.setId("top");
    top.add(middle);

    try {
      render(top);
    }
    catch (UnsupportedOperationException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("already set"));
      return;
    }

    fail("expecting exception");
  }

  public void testSetVarAfterCreation()
    throws Exception
  {
    // XXX: this is non-intuitve, the recursive call to url() happens in the
    // call to toString(), so the parameter is overwritten
    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      private VarDefinition MESSAGE = new VarDefinition("com.caucho.widget.test.message", String.class);

      public void setMessage(VarContext context, String value)
      {
        context.setVar(this, MESSAGE, value);
      }

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public String getId()
      {
        return "widget";
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        setMessage(request, "hello");
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        String message = getMessage(url);

        url.setParameter(message);

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        addState(url.toString());

        setMessage(url, "goodbye");

        addState(url.toString());

        super.response(response);
      }
    };

    render(widget);

    assertState(
      "http://mock:80/?widget=hello",
      "http://mock:80/?widget=goodbye"
    );
  }

  public void testSetVarAfterCreationContainment()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      private VarDefinition MESSAGE = new VarDefinition("com.caucho.widget.test.message", String.class);

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
        super.request(request);

        setMessage(request, "helloFrom:" + getId());
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        super.url(url);

        String message = getMessage(url);

        url.setParameter(message);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        addState(getId() + ":" + url.toString());

        setMessage(url, "goodbyeFrom:" + getId());

        addState(getId() + ":" + url.toString());

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      private VarDefinition MESSAGE = new VarDefinition("com.caucho.widget.test.message", String.class);

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
        super.request(request);

        setMessage(request, "helloFrom:" + getId());
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        super.url(url);

        String message = getMessage(url);

        url.setParameter(message);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        addState(getId() + ":" + url.toString());

        setMessage(url, "goodbyeFrom:" + getId());

        addState(getId() + ":" + url.toString());

        super.response(response);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer()
    {
      private VarDefinition MESSAGE = new VarDefinition("com.caucho.widget.test.message", String.class);

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
        setMessage(request, "helloFrom:" + getId());
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        super.url(url);

        String message = getMessage(url);

        url.setParameter(message);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        addState(getId() + ":" + url.toString());

        setMessage(url, "goodbyeFrom:" + getId());

        addState(getId() + ":" + url.toString());

        super.response(response);
      }
    };

    bottom.setId("bottom");

    middle.setId("middle");
    middle.setNamespace(true);
    middle.add(bottom);

    top.setId("top");
    top.add(middle);

    render(top);

    assertState(
      "top:http://mock:80/?middle-bottom=helloFrom:bottom&middle=helloFrom:middle&top=helloFrom:top",
      "top:http://mock:80/?middle-bottom=helloFrom:bottom&middle=helloFrom:middle&top=goodbyeFrom:top",
      "middle:http://mock:80/?middle-bottom=helloFrom:bottom&middle=helloFrom:middle&top=helloFrom:top",
      "middle:http://mock:80/?middle-bottom=helloFrom:bottom&middle=goodbyeFrom:middle&top=helloFrom:top",
      "bottom:http://mock:80/?middle-bottom=helloFrom:bottom&middle=helloFrom:middle&top=helloFrom:top",
      "bottom:http://mock:80/?middle-bottom=goodbyeFrom:bottom&middle=helloFrom:middle&top=helloFrom:top"
    );
  }

  public void testSetParameterAfterCreation()
    throws Exception
  {
    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public String getId()
      {
        return "widget";
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("helloWorld");
        url.setParameter("foo", "fooValue");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        url.setParameter("foo", "fooValueAfterCreation");

        addState(url.toString());

        super.response(response);
      }
    };

    try {
      render(widget);
    }
    catch (UnsupportedOperationException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("parameter"));
      return;
    }

    fail("expecting exception");
  }

  public void testSetSecure()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setSecure(true);

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    render(widget);

    assertState("https://mock:80/");
  }

  public void testSetSecureContainment()
    throws Exception
  {
    // once a widget calls setSecure(true), other widgets calling setSecure(false)
    // has no effect
    AbstractWidgetContainer top = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        addState(getId() + ": isSecure=" + url.isSecure());
        url.setSecure(true);
        addState(getId() + ": isSecure=" + url.isSecure());

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        addState(getId() + ": isSecure=" + url.isSecure());
        url.setSecure(false);
        addState(getId() + ": isSecure=" + url.isSecure());

        super.url(url);
      }
    };

    middle.setId("middle");

    top.setId("top");
    top.add(middle);

    render(top);

    assertState(
      "top: isSecure=false",
      "top: isSecure=true",
      "middle: isSecure=true",
      "middle: isSecure=true",
      "https://mock:80/");
  }

  public void testSetInsecureWithSecureConnection()
    throws Exception
  {
    // if the connection is secure, then calling setSecure(false) will make the
    // url insecure
    AbstractWidget widget = new AbstractWidget()
    {
      public void url(WidgetURL url)
        throws WidgetException
      {
        addState("isSecure=" + url.isSecure());
        url.setSecure(false);

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        addState(response.getUrl().toString());

        super.response(response);
      }
    };

    getExternalConnection().setSecure(true);
    render(widget);

    assertState(
      "isSecure=true",
      "http://mock:80/"
    );
  }

  public void testReconnect()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget()
    {
      public String getId()
      {
        return "widget";
      }

      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        String paramValue = invocation.getParameter();

        addState(paramValue);
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("hello");
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        response.getWriter().write(url.toString());
      }
    };

    render(widget);

    String output = getExternalConnection().getOutputAsString();

    assertEquals("http://mock:80/?widget=hello", output);

    setExternalConnection(output);

    render(widget);

    assertState(
      "<null>",
      "hello"
    );
  }

  public void testNamespace()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("top: " + invocation.getParameter());
        addState("top[topname]: " + invocation.getParameter("topname"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("topvalue");
        url.setParameter("topname", "topnamevalue");

        super.url(url);
      }

    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("middle: " + invocation.getParameter());
        addState("middle[middlename]: " + invocation.getParameter("middlename"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("middlevalue");
        url.setParameter("middlename", "middlenamevalue");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        String insecureUrlString = url.toString();

        addState(insecureUrlString);

        url.setSecure(true);

        String secureUrlString = url.toString();

        addState(secureUrlString);

        response.getWriter().print(insecureUrlString);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("bottom: " + invocation.getParameter());
        addState("bottom[bottomname]: " + invocation.getParameter("bottomname"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("bottomvalue");
        url.setParameter("bottomname", "bottomnamevalue");

        super.url(url);
      }

    };

    bottom.setId("bottom");

    middle.setId("middle");
    middle.setNamespace(true);
    middle.add(bottom);

    top.setId("top");
    top.add(middle);

    render(top);

    String output = getExternalConnection().getOutputAsString();

    setExternalConnection(output);

    render(top);

    assertState(
      "bottom: null",
      "bottom[bottomname]: null",
      "middle: null",
      "middle[middlename]: null",
      "top: null",
      "top[topname]: null",
      "http://mock:80/?top=topvalue&top.topname=topnamevalue&middle=middlevalue&middle.middlename=middlenamevalue&middle-bottom=bottomvalue&middle-bottom.bottomname=bottomnamevalue",
      "https://mock:80/?top=topvalue&top.topname=topnamevalue&middle=middlevalue&middle.middlename=middlenamevalue&middle-bottom=bottomvalue&middle-bottom.bottomname=bottomnamevalue",
      "bottom: bottomvalue",
      "bottom[bottomname]: bottomnamevalue",
      "middle: middlevalue",
      "middle[middlename]: middlenamevalue",
      "top: topvalue",
      "top[topname]: topnamevalue",
      "http://mock:80/?top=topvalue&top.topname=topnamevalue&middle=middlevalue&middle.middlename=middlenamevalue&middle-bottom=bottomvalue&middle-bottom.bottomname=bottomnamevalue",
      "https://mock:80/?top=topvalue&top.topname=topnamevalue&middle=middlevalue&middle.middlename=middlenamevalue&middle-bottom=bottomvalue&middle-bottom.bottomname=bottomnamevalue"
    );

  }

  // submit parameters come back as parameters for the children
  // of the widget that called createSubmitURL()
  public void testSubmit()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("top: " + invocation.getParameter());
        addState("top[topname]: " + invocation.getParameter("topname"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("topvalue");
        url.setParameter("topname", "topnamevalue");

        super.url(url);
      }

    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("middle: " + invocation.getParameter());
        addState("middle[middlename]: " + invocation.getParameter("middlename"));

      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("middlevalue");
        url.setParameter("middlename", "middlenamevalue");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getSubmitUrl();

        String urlString = url.toString();

        response.getWriter().print(urlString);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("bottom: " + invocation.getParameter());
        addState("bottom[bottomname]: " + invocation.getParameter("bottomname"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("bottomvalue");
        url.setParameter("bottomname", "bottomnamevalue");

        super.url(url);
      }

    };

    bottom.setId("bottom");

    middle.setId("middle");
    middle.add(bottom);

    top.setId("top");
    top.setNamespace(true);
    top.add(middle);

    render(top);

    String submitUrl = getExternalConnection().getOutputAsString();

    submitUrl =  submitUrl
      + "&middlename=middlenamesubmit"
      + "&bottom=bottomsubmit";

    setExternalConnection(submitUrl);

    render(top);

    assertState(
      "bottom: null",
      "bottom[bottomname]: null",
      "middle: null",
      "middle[middlename]: null",
      "top: null",
      "top[topname]: null",
      "bottom: bottomsubmit",
      "bottom[bottomname]: bottomnamevalue",
      "middle: middlevalue",
      "middle[middlename]: middlenamevalue",
      "top: topvalue",
      "top[topname]: topnamevalue"
    );

  }

  // paremeterSubmit parameters come back as named parameters for the
  // widget called createNamedSubmitURL()
  public void testParameterSubmit()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("top: " + invocation.getParameter());
        addState("top[topname]: " + invocation.getParameter("topname"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("topvalue");
        url.setParameter("topname", "topnamevalue");

        super.url(url);
      }

    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("middle: " + invocation.getParameter());
        addState("middle[middlename]: " + invocation.getParameter("middlename"));

      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("middlevalue");
        url.setParameter("middlename", "middlenamevalue");

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getParameterUrl();

        String urlString = url.toString();

        response.getWriter().print(urlString);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState("bottom: " + invocation.getParameter());
        addState("bottom[bottomname]: " + invocation.getParameter("bottomname"));
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("bottomvalue");
        url.setParameter("bottomname", "bottomnamevalue");

        super.url(url);
      }

    };

    bottom.setId("bottom");

    middle.setId("middle");
    middle.add(bottom);

    top.setId("top");
    top.setNamespace(true);
    top.add(middle);

    render(top);

    String submitUrl = getExternalConnection().getOutputAsString();

    submitUrl =  submitUrl
      + "&middlename=middlenamesubmit"
      + "&bottom=bottomsubmit";

    setExternalConnection(submitUrl);

    render(top);

    assertState(
      "bottom: null",
      "bottom[bottomname]: null",
      "middle: null",
      "middle[middlename]: null",
      "top: null",
      "top[topname]: null",
      "bottom: bottomvalue",
      "bottom[bottomname]: bottomnamevalue",
      "middle: middlevalue",
      "middle[middlename]: middlenamesubmit",
      "top: topvalue",
      "top[topname]: topnamevalue"
      );
  }

  public void testVar()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getId()
      {
        return "widget";
      }

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
        setMessage(request, "hello");
      }

      public void url(WidgetURL url)
      {
        String message = getMessage(url);

        url.setParameter(message);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();
        setMessage(url, "goodbye");

        addState(url.toString());

        url = response.getUrl();
        addState(url.toString());
      }
    };

    render(widget);

    assertState(
      "http://mock:80/?widget=goodbye",
      "http://mock:80/?widget=hello"
    );
  }

  public void testVarDefault()
    throws Exception
  {
    Widget widget = new AbstractWidget()
    {
      private String MESSAGE = "com.caucho.widget.TestWidget.message";

      public String getId()
      {
        return "widget";
      }

      public String getMessage(VarContext context)
      {
        return context.getVar(this, MESSAGE);
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        messageDef.setValue("hello");
        init.addVarDefinition(messageDef);
      }

      public void url(WidgetURL url)
      {
        url.setParameter(getMessage(url));
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        addState(url.toString());
      }
    };

    render(widget);

    assertState("http://mock:80/?widget=hello");
  }

  public void testVarInheritance()
    throws Exception
  {
    final String MESSAGE1 = "com.caucho.widget.TestWidget.message1";
    final String MESSAGE2 = "com.caucho.widget.TestWidget.message2";

    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public String getId()
      {
        return "top";
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        request.setVar(this, MESSAGE1, "value1");
        request.setVar(this, MESSAGE2, "value2");

        super.request(request);
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        url.setParameter("message1", (String) url.getVar(this, MESSAGE1));
        url.setParameter("message2", (String) url.getVar(this, MESSAGE2));

        super.url(url);
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        addState(url.toString());

        super.response(response);
      }

    };

    widget.add(
      new AbstractWidget() {
        public String getId()
        {
          return "bottom";
        }

        public void init(WidgetInit init)
          throws WidgetException
        {
          super.init(init);

          VarDefinition message1Def = new VarDefinition(MESSAGE1, String.class);
          init.addVarDefinition(message1Def);

          VarDefinition message2Def = new VarDefinition(MESSAGE2, String.class);
          message2Def.setInherited(true);
          init.addVarDefinition(message2Def);
        }

        public void response(WidgetResponse response)
          throws WidgetException, IOException
        {
          super.response(response);

          WidgetURL url = response.getUrl();

          addState(url.toString());
        }

        public void url(WidgetURL url)
        {
          url.setParameter("message1", (String) url.getVar(this, MESSAGE1));
          url.setParameter("message2", (String) url.getVar(this, MESSAGE2));
        }

      });

    render(widget);

    // value2 is inherited from it's parent
    assertState(
      "http://mock:80/?top.message1=value1&top.message2=value2&bottom.message2=value2",
      "http://mock:80/?top.message1=value1&top.message2=value2&bottom.message2=value2");
  }

  public void testVarDefaultInheritance()
    throws Exception
  {
    final String MESSAGE1 = "com.caucho.widget.TestWidget.message1";
    final String MESSAGE2 = "com.caucho.widget.TestWidget.message2";

    AbstractWidgetContainer widget = new AbstractWidgetContainer()
    {
      public String getId()
      {
        return "widget";
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition message1Def = new VarDefinition(MESSAGE1, String.class);
        message1Def.setValue("value1");
        init.addVarDefinition(message1Def);

        VarDefinition message2Def = new VarDefinition(MESSAGE2, String.class);
        message2Def.setValue("value2");
        init.addVarDefinition(message2Def);
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        super.url(url);

        url.setParameter("message1", (String) url.getVar(this, MESSAGE1));
        url.setParameter("message2", (String) url.getVar(this, MESSAGE2));
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();
        addState(url.toString());

        super.response(response);
      }
    };

    widget.add(new AbstractWidget() {
      public String getId()
      {
        return "bottom";
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        VarDefinition message1Def = new VarDefinition(MESSAGE1, String.class);
        init.addVarDefinition(message1Def);

        VarDefinition message2Def = new VarDefinition(MESSAGE2, String.class);
        message2Def.setInherited(true);
        init.addVarDefinition(message2Def);
      }

      public void url(WidgetURL url)
        throws WidgetException
      {
        super.url(url);

        url.setParameter("message1", (String) url.getVar(this, MESSAGE1));
        url.setParameter("message2", (String) url.getVar(this, MESSAGE2));
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();
        addState(url.toString());

        super.response(response);
      }
    });

    render(widget);

    assertState(
      "http://mock:80/?bottom.message2=value2&widget.message1=value1&widget.message2=value2",
      "http://mock:80/?bottom.message2=value2&widget.message1=value1&widget.message2=value2"
    );
  }

  public void testVarIncompatibleType()
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
        super.init(init);

        VarDefinition messageDef = new VarDefinition(MESSAGE, String.class);
        init.addVarDefinition(messageDef);
      }

      public void request(WidgetRequest request)
        throws WidgetException
      {
        setMessage(request, "hello, world");
      }

      public void url(WidgetURL url)
      {
        url.setParameter(getMessage(url));
      }

      public void response(WidgetResponse response)
        throws WidgetException, IOException
      {
        WidgetURL url = response.getUrl();

        url.setVar(this, MESSAGE, new Integer(1));

        addState(url.toString());
      }
    };

    try {
      render(widget);
    }
    catch (ClassCastException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("is incompatible with"));

      return;
    }

    fail("expecting exception");
  }
}
