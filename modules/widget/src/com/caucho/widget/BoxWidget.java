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

import com.caucho.util.L10N;
import com.caucho.vfs.XmlWriter;

import java.io.IOException;

/**
 * <a href="http://www.xulplanet.com/references/elemref/ref_box.html">XUL reference</a>
 */
public class BoxWidget
  extends XulWidgetContainer
{
  private static final L10N L = new L10N(BoxWidget.class);

  private Orient _orient = Orient.HORIZONTAL;

  public BoxWidget()
  {
  }

  public BoxWidget(String id)
  {
    setId(id);
  }

  public BoxWidget(String id, Orient orient)
  {
    setId(id);
    setOrient(orient);
  }

  /**
   * Set the orientation, default is {@link Orient.HORIZONTAL}.
   */
  public void setOrient(Orient orient)
  {
    _orient = orient;
  }

  /**
   * Set the orientation, either "horizontal" or "vertical".
   */
  public void setOrient(String orient)
  {
    if ("horizontal".equals(orient))
      setOrient(Orient.HORIZONTAL);
    else if ("vertical".equals(orient))
      setOrient(Orient.VERTICAL);
    else
      throw new IllegalArgumentException("" + orient);
  }

  public Orient getOrient()
  {
    return _orient;
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    super.init(init);
  }

  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    Orient orient = getOrient();

    String id = getId();
    String cssClass = getCssClass(response);

    XmlWriter out = response.getWriter();

    out.startElement("div");
    out.writeAttribute("id", id);

    String orientAttribute = orient == Orient.HORIZONTAL ? "horizontal" : "vertical";

    out.writeAttribute("class", cssClass, orientAttribute);

    super.response(response);

    out.endElement("div");
  }
}
