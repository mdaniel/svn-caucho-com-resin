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

public class TestBoxWidget
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    BoxWidget box = new BoxWidget();
    LabelWidget label1 = new LabelWidget(null, "hello");
    LabelWidget label2 = new LabelWidget(null, "goodbye");

    box.add(label1);
    box.add(label2);

    render(box);

    assertOutputEquals(
      "<div class='Box horizontal'>"
      + "<span class='Label'>hello</span>"
      + "<span class='Label'>goodbye</span>"
      + "</div>"
    );
  }

  public void testVertical()
    throws Exception
  {
    BoxWidget box = new BoxWidget();
    box.setOrient(Orient.VERTICAL);

    LabelWidget label1 = new LabelWidget(null, "hello");
    LabelWidget label2 = new LabelWidget(null, "goodbye");

    box.add(label1);
    box.add(label2);

    render(box);

    assertOutputEquals(
      "<div class='Box vertical'>"
      + "<span class='Label'>hello</span>"
      + "<span class='Label'>goodbye</span>"
      + "</div>"
    );
  }
}

