/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet.jsp.jstl.core;

import javax.el.*;

public final class IndexedValueExpression extends ValueExpression {
  protected final Integer i;
  protected final ValueExpression orig;

  public IndexedValueExpression(ValueExpression orig, int i)
  {
    this.orig = orig;
    this.i = Integer.valueOf(i);
  }

  @Override
  public String getExpressionString()
  {
    return this.orig.getExpressionString() + "[" + this.i + "]";
  }

  @Override
  public Class getExpectedType()
  {
    return Object.class;
  }

  @Override
  public Class getType(ELContext context)
  {
    Object value = getValue(context);

    if (value != null)
      return value.getClass();
    else
      return null;
  }

  @Override
  public boolean isLiteralText()
  {
    return false;
  }

  @Override
  public boolean isReadOnly(ELContext context)
  {
    return true;
  }

  @Override
  public Object getValue(ELContext context)
  {
    Object base = this.orig.getValue(context);

    return context.getELResolver().getValue(context, base, this.i);
  }

  @Override
  public void setValue(ELContext context, Object value)
  {
    Object base = this.orig.getValue(context);

    context.getELResolver().setValue(context, base, this.i, value);
  }

  public int hashCode()
  {
    return 65521 * this.orig.hashCode() + this.i.hashCode();
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof IndexedValueExpression))
      return false;

    IndexedValueExpression expr = (IndexedValueExpression) obj;

    return this.orig.equals(expr.orig) && this.i.equals(expr.i);
  }
}
