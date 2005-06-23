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

public class TestWidgetInterceptor
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    StateReportingWidgetInterceptor interceptor = new StateReportingWidgetInterceptor("1");
    StateReportingWidget widget = new StateReportingWidget("a");

    widget.addInterceptor(interceptor);

    render(widget);

    assertState(
      "a: init",

      "1: invocation intercept before next",
      "a: invocation",
      "1: invocation intercept after next",

      "1: request intercept before next",
      "a: request",
      "1: request intercept after next",

      "1: response intercept before next",
      "a: response",
      "1: url intercept before next",
      "a: url",
      "1: url intercept after next",
      "1: response intercept after next",


      "1: destroy intercept before next",
      "a: destroy",
      "1: destroy intercept after next"
    );
  }

  public void testMultiple()
    throws Exception
  {
    StateReportingWidget widget = new StateReportingWidget("a");

    StateReportingWidgetInterceptor interceptor1 = new StateReportingWidgetInterceptor("1");
    StateReportingWidgetInterceptor interceptor2 = new StateReportingWidgetInterceptor("2");

    widget.addInterceptor(interceptor1);
    widget.addInterceptor(interceptor2);

    render(widget);

    assertState(
      "a: init",

      "1: invocation intercept before next",
      "2: invocation intercept before next",
      "a: invocation",
      "2: invocation intercept after next",
      "1: invocation intercept after next",

      "1: request intercept before next",
      "2: request intercept before next",
      "a: request",
      "2: request intercept after next",
      "1: request intercept after next",

      "1: response intercept before next",
      "2: response intercept before next",
      "a: response",
      "1: url intercept before next",
      "2: url intercept before next",
      "a: url",
      "2: url intercept after next",
      "1: url intercept after next",
      "2: response intercept after next",
      "1: response intercept after next",

      "1: destroy intercept before next",
      "2: destroy intercept before next",
      "a: destroy",
      "2: destroy intercept after next",
      "1: destroy intercept after next"
      );
  }

  public void testContainment()
    throws Exception
  {
    StateReportingWidget widgetA = new StateReportingWidget("a");

    StateReportingWidgetInterceptor interceptor1a = new StateReportingWidgetInterceptor("1a");
    StateReportingWidgetInterceptor interceptor2a = new StateReportingWidgetInterceptor("2a");

    widgetA.addInterceptor(interceptor1a);
    widgetA.addInterceptor(interceptor2a);

    StateReportingWidget widgetB = new StateReportingWidget("b");

    StateReportingWidgetInterceptor interceptor1b = new StateReportingWidgetInterceptor("1b");
    StateReportingWidgetInterceptor interceptor2b = new StateReportingWidgetInterceptor("2b");

    widgetB.addInterceptor(interceptor1b);
    widgetB.addInterceptor(interceptor2b);

    widgetA.add(widgetB);

    render(widgetA);

    assertState(
      "a: init",
      "b: init",

      "1a: invocation intercept before next",
      "2a: invocation intercept before next",
      "a: invocation",
      "1b: invocation intercept before next",
      "2b: invocation intercept before next",
      "b: invocation",
      "2b: invocation intercept after next",
      "1b: invocation intercept after next",
      "2a: invocation intercept after next",
      "1a: invocation intercept after next",

      "1a: request intercept before next",
      "2a: request intercept before next",
      "a: request",
      "1b: request intercept before next",
      "2b: request intercept before next",
      "b: request",
      "2b: request intercept after next",
      "1b: request intercept after next",
      "2a: request intercept after next",
      "1a: request intercept after next",

      "1a: response intercept before next",
      "2a: response intercept before next",
      "a: response",
      "1b: response intercept before next",
      "2b: response intercept before next",
      "b: response",
      "1a: url intercept before next",
      "2a: url intercept before next",
      "a: url",
      "1b: url intercept before next",
      "2b: url intercept before next",
      "b: url",
      "2b: url intercept after next",
      "1b: url intercept after next",
      "2a: url intercept after next",
      "1a: url intercept after next",
      "2b: response intercept after next",
      "1b: response intercept after next",
      "1a: url intercept before next",
      "2a: url intercept before next",
      "a: url",
      "1b: url intercept before next",
      "2b: url intercept before next",
      "b: url",
      "2b: url intercept after next",
      "1b: url intercept after next",
      "2a: url intercept after next",
      "1a: url intercept after next",
      "2a: response intercept after next",
      "1a: response intercept after next",

      "1a: destroy intercept before next",
      "2a: destroy intercept before next",
      "a: destroy",
      "1b: destroy intercept before next",
      "2b: destroy intercept before next",
      "b: destroy",
      "2b: destroy intercept after next",
      "1b: destroy intercept after next",
      "2a: destroy intercept after next",
      "1a: destroy intercept after next"
      );
  }

  private class StateReportingWidgetInterceptor
    implements WidgetInterceptor
  {
    String _testId;

    public StateReportingWidgetInterceptor(String testId)
    {
      _testId = testId;
    }

    public void invocation(WidgetInvocation invocation,
                             WidgetInterceptorChain next)
      throws WidgetException
    {
      addState(_testId + ": invocation intercept before next");
      next.invocation(invocation);
      addState(_testId + ": invocation intercept after next");
    }

    public void request(WidgetRequest request, WidgetInterceptorChain next)
      throws WidgetException
    {
      addState(_testId + ": request intercept before next");
      next.request(request);
      addState(_testId + ": request intercept after next");
    }

    public void url(WidgetURL url, WidgetInterceptorChain next)
      throws WidgetException
    {
      addState(_testId + ": url intercept before next");
      next.url(url);
      addState(_testId + ": url intercept after next");
    }

    public void response(WidgetResponse response, WidgetInterceptorChain next)
      throws WidgetException, IOException
    {
      addState(_testId + ": response intercept before next");
      next.response(response);
      addState(_testId + ": response intercept after next");
    }

    public void destroy(WidgetDestroy destroy, WidgetInterceptorChain next)
    {
      addState(_testId + ": destroy intercept before next");
      next.destroy(destroy);
      addState(_testId + ": destroy intercept after next");
    }
  };

  public class StateReportingWidget
    extends AbstractWidgetContainer
  {
    public StateReportingWidget(String id)
    {
      setId(id);
    }

    public void init(WidgetInit init)
      throws WidgetException
    {
      addState(getId() + ": init");

      super.init(init);

    }

    public void invocation(WidgetInvocation invocation)
      throws WidgetException
    {
      addState(getId() + ": invocation");

      super.invocation(invocation);
    }

    public void request(WidgetRequest request)
      throws WidgetException
    {
      addState(getId() + ": request");

      super.request(request);
    }

    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      addState(getId() + ": response");

      super.response(response);

      response.getUrl().toString();
    }

    public void url(WidgetURL url)
      throws WidgetException
    {
      addState(getId() + ": url");

      super.url(url);
    }

    public void destroy(WidgetDestroy destroy)
    {
      addState(getId() + ": destroy");

      super.destroy(destroy);
    }
  };

}
