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

public class TestXulWidget
  extends WidgetTestCase
{
  public void testCssClassDefault()
    throws Exception
  {
    Widget foo = new FooWidget();

    render(foo);

    assertState("Foo");
  }

  public void testCssClassOverride()
    throws Exception
  {
    Widget foo = new FooWidget()
    {
      {
        setCssClass("Bar");
      }

      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);
      }
    };

    render(foo);

    assertState("Bar");
  }

  public void testCssClassOverrideInInit()
    throws Exception
  {
    Widget foo = new FooWidget()
    {
      public void init(WidgetInit init)
        throws WidgetException
      {
        super.init(init);

        setCssClass(init, "Bar");
      }
    };

    render(foo);

    assertState("Bar");
  }

  private class FooWidget
    extends XulWidget
  {
    public void response(WidgetResponse response)
      throws WidgetException, IOException
    {
      addState(getCssClass(response));

      super.response(response);
    }
  }
}
