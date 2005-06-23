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

/**
 * A disabled Widget is even more limited than a readonly Widget (See {@link ReadonlyProperty}).
 * The user cannot use the widget in any way,
 * not to select anything if it is a control, not to perform any actions with
 * it like submitting a form,
 * a disabled control is not even sent if a form is submitted.
 *
 * It is usually rendered in grey.
 */
public class DisabledProperty
  extends BooleanProperty
{
  public DisabledProperty(Widget widget)
  {
    super(widget, "disabled", true, false);
  }
}
