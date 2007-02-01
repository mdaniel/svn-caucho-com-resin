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

package javax.faces.webapp;

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.render.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public abstract class UIComponentTag extends UIComponentClassicTagBase
  implements Tag
{
  private String _binding;
  private String _rendered;

  public void setBinding(String binding)
    throws JspException
  {
    _binding = binding;
  }

  public boolean hasBinding()
  {
    return _binding != null;
  }

  public void setRendered(String rendered)
  {
    _rendered = rendered;
  }

  public boolean isSuppressed()
  {
    return false;
  }

  protected void setProperties(UIComponent component)
  {
    if (_rendered != null)
      component.getAttributes().put("rendered", _rendered);
  }

  protected UIComponent createComponent(FacesContext context,
					String newId)
  {
    throw new UnsupportedOperationException();
  }

  public static UIComponentTag getParentUIComponentTag(PageContext context)
  {
    return null;
  }

  public static boolean isValueReference(String value)
  {
    return value.startsWith("#{") && value.endsWith("}");
  }
}
