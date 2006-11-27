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

package javax.faces.render;

import java.io.*;

import javax.faces.application.*;
import javax.faces.context.*;

public abstract class ResponseStateManager
{
  public static final String RENDER_KIT_ID_PARAM = "javax.faces.RenderKitId";

  /**
   * @Since 1.2
   */
  public static final String VIEW_STATE_PARAM = "javax.faces.ViewState";

  public void writeState(FacesContext context,
			 Object state)
    throws IOException
  {
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
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }
}
