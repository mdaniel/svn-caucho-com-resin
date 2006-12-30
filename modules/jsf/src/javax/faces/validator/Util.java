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

package javax.faces.validator;

import java.util.*;

import javax.faces.application.*;
import javax.faces.context.*;

class Util
{
  public static ValidatorException validationExn(FacesContext context,
						  String messageId,
						  Object []values)
  {
    throw new ValidatorException(facesMessage(context, messageId, values));
  }
  
  public static FacesMessage facesMessage(FacesContext context,
					String messageId,
					Object []values)
  {
    Application app = context.getApplication();

    String bundleName = app.getMessageBundle();

    if (bundleName == null)
      return new FacesMessage(messageId);
    
    ResourceBundle bundle = app.getResourceBundle(context, bundleName);

    if (bundle == null)
      return new FacesMessage(messageId);

    return new FacesMessage(bundle.getString(messageId));
  }
}
