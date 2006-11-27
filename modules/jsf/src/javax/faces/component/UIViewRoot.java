/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.faces.component;

public class UIViewRoot extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.ViewRoot";
  public static final String COMPONENT_TYPE = "javax.faces.ViewRoot";
  public static final String UNIQUE_ID_PREFIX = "_id";

  private String _renderKitId = "HTML_BASIC";

  private String _viewId;
  private int _unique;

  public UIViewRoot()
  {
  }

  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }
  
  public String getRenderKitId()
  {
    return _renderKitId;
  }
  
  public void setRenderKitId(String renderKitId)
  {
    _renderKitId = renderKitId;
  }
  
  public String getViewId()
  {
    return _viewId;
  }
  
  public void setViewId(String value)
  {
    _viewId = value;
  }

  public String createUniqueId()
  {
    return UNIQUE_ID_PREFIX + _unique++;
  }
}
