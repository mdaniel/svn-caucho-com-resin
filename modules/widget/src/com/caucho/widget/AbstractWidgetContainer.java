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
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

abstract public class AbstractWidgetContainer
  extends AbstractWidget
{
  private static List<Widget> EMPTY_CHILDREN = Collections.emptyList();

  private ArrayList<Widget> _children;

  protected AbstractWidgetContainer()
  {
    super();
  }

  protected AbstractWidgetContainer(String id)
  {
    super(id);
  }

  public void add(Widget child)
  {
    if (_children == null)
      _children = new ArrayList<Widget>();

    _children.add(child);
  }

  public List<Widget> getChildren()
  {
    if (_children == null)
      return EMPTY_CHILDREN;
    else
      return _children;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation call's initChildren().
   */
  public void init(WidgetInit init)
    throws WidgetException
  {
    super.init(init);

    initChildren(init);
  }

  public void initChildren(WidgetInit init)
    throws WidgetException
  {
    if (_children != null) {
      WidgetInitChain next = init.getInitChain();

      for (Widget child : _children) {
        next.init(child);
      }
    }

  }

  /**
   * {@inheritDoc}
   *
   * This implementation call's invocationChildren().
   */
  public void invocation(WidgetInvocation invocation)
    throws WidgetException
  {
    super.invocation(invocation);

    invocationChildren(invocation);
  }

  public void invocationChildren(WidgetInvocation invocation)
    throws WidgetException
  {
    if (_children != null) {
      WidgetInvocationChain next = invocation.getInvocationChain();

      for (Widget child : _children) {
        next.invocation(child);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * This implementation call's requestChildren().
   */
  public void request(WidgetRequest request)
    throws WidgetException
  {
    super.request(request);

    requestChildren(request);
  }

  public void requestChildren(WidgetRequest request)
    throws WidgetException
  {
    if (_children != null) {
      WidgetRequestChain next = request.getRequestChain();

      for (Widget child : _children) {
        next.request(child);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * This implementation call's urlChildren().
   */
  public void url(WidgetURL url)
    throws WidgetException
  {
    super.url(url);

    urlChildren(url);
  }

  public void urlChildren(WidgetURL url)
    throws WidgetException
  {
    if (_children != null) {
      WidgetURLChain next = url.getURLChain();

      for (Widget child : _children) {
        next.url(child);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * This implementation call's responseChildren().
   */
  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    super.response(response);

    responseChildren(response);
  }

  public void responseChildren(WidgetResponse response)
    throws WidgetException, IOException
  {
    if (_children != null) {
      WidgetResponseChain next = response.getResponseChain();

      for (Widget child : _children) {
        next.response(child);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * This implementation call's destroyChildren().
   */
  public void destroy(WidgetDestroy destroy)
  {
    super.destroy(destroy);

    destroyChildren(destroy);
  }

  public void destroyChildren(WidgetDestroy destroy)
  {
    if (_children != null) {
      WidgetDestroyChain next = destroy.getDestroyChain();

      for (Widget child : _children) {
        next.destroy(child);
      }
    }
  }
}
