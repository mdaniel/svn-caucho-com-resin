/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.jsf.html;

import com.caucho.util.Base64;

import java.io.*;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.render.*;

public class ResponseStateManagerImpl extends ResponseStateManager
{
  public void writeState(FacesContext context,
			 Object state)
    throws IOException
  {
    if (Object [].class.isAssignableFrom(state.getClass())) {
      String value = encode(((Object [])state) [0]);
      
      ResponseWriter rw = context.getResponseWriter();
      
      rw.startElement("input", null);

      rw.writeAttribute("type", "hidden", null);
      rw.writeAttribute("name", VIEW_STATE_PARAM, null);
      rw.writeAttribute("value", value, null);
      
      rw.endElement("input");
      
      rw.write("\n");
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Deprecated
  public void writeState(FacesContext context,
			 StateManager.SerializedView state)
    throws IOException
  {
  }

  /**
   * @Since 1.2
   */
  public Object getState(FacesContext context,
			 String viewId)
  {
    ExternalContext extContext = context.getExternalContext();
    
    String data = extContext.getRequestParameterMap().get(VIEW_STATE_PARAM);
    
    if (data.charAt(0) == '!') {
      return new Object []{data.substring(1), null};
    }
    else {
      return new Object []{Base64.decodeToByteArray(data), null};
    }
  }

  @Deprecated
  public Object getTreeStructureToRestore(FacesContext context,
					  String viewId)
  {
    return null;
  }

  @Deprecated
  public Object getComponentStateToRestore(FacesContext context)
  {
    return null;
  }

  /**
   * Since 1.2
   */
  public boolean isPostback(FacesContext context)
  {
    ExternalContext extContext = context.getExternalContext();
    
    return extContext.getRequestParameterMap().containsKey(VIEW_STATE_PARAM);
  }

  private String encode(Object obj) {
    if (byte [].class.isAssignableFrom(obj.getClass())) {
      
      return Base64.encodeFromByteArray((byte []) obj);
    }
    else if (obj instanceof String) {

      return "!"+obj.toString();
    }
    else {
      throw new IllegalArgumentException();
    }
  }
}
