
package com.caucho.widget;

import java.io.IOException;
import java.util.ArrayList;

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

public class AbstractWidget
  implements Widget
{
  private String _id;
  private String _typeName;
  private boolean _isNamespace;

  private ArrayList<WidgetInterceptor> _interceptorList;

  protected AbstractWidget()
  {
    initTypeName();
  }

  protected AbstractWidget(String id)
  {
    initTypeName();
    setId(id);
  }

  private void initTypeName()
  {
    if (_typeName == null) {
      String typeName = getClass().getName();

      int begin;
      int end = typeName.length();

      if (typeName.endsWith("Widget"))
        end -= 6;

      int lastDot = typeName.lastIndexOf('.');
      int lastDollar = typeName.lastIndexOf('$');

      begin = Math.max(lastDot, lastDollar) + 1;

      StringBuilder buf = new StringBuilder(typeName.substring(begin, end));

      buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));

      _typeName = buf.toString();
    }
  }

  /**
   * XXX: allowed to be null, unless isNamespace
   *
   * @param id
   */
  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  /**
   * XXX: doc default
   */
  public void setTypeName(String typeName)
  {
    _typeName = typeName;
  }

  public String getTypeName()
  {
    return _typeName;
  }

  /**
   * Default is false.
   */
  public void setNamespace(boolean isNamespace)
  {
    _isNamespace = isNamespace;
  }

  public boolean isNamespace()
  {
    return _isNamespace;
  }


  /**
   * {@inheritDoc}
   *
   * This implementation throws UnsupportedOperationException.
   */
  public void add(Widget widget)
    throws UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }

  public void addInterceptor(WidgetInterceptor interceptor)
  {
    if (_interceptorList == null)
      _interceptorList = new ArrayList<WidgetInterceptor>();

    _interceptorList.add(interceptor);
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    if (_interceptorList != null) {
      for (int i = 0; i < _interceptorList.size(); i++) {
        init.addInterceptor(_interceptorList.get(i));
      }

      _interceptorList.clear();
      _interceptorList = null;
    }
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

  public String toString()
  {
    return getTypeName() + "[" + "id=" + (getId() == null ? "" : getId()) + "]";
  }
}
