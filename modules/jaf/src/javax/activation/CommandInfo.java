/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package javax.activation;

import java.io.File;
import java.io.Externalizable;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.beans.Beans;

/**
 * Information for a command.
 */
public class CommandInfo {
  private final String _verb;
  private final String _className;
  
  /**
   * Creates the CommandInfo.
   */
  public CommandInfo(String verb, String className)
  {
    _verb = verb;
    _className = className;
  }

  /**
   * Return the command verb.
   */
  public String getCommandName()
  {
    return _verb;
  }

  /**
   * Return the command class.
   */
  public String getCommandClass()
  {
    return _className;
  }

  /**
   * Instantiate the command object.
   */
  public Object getCommandObject(DataHandler dataHandler,
				 ClassLoader loader)
    throws IOException, ClassNotFoundException
  {
    Object bean = Beans.instantiate(loader, getCommandClass());

    if (bean instanceof CommandObject) {
      CommandObject command = (CommandObject) bean;

      command.setCommandContext(getCommandName(), dataHandler);
    }
    else if (dataHandler == null) {
    }
    else if (bean instanceof Externalizable) {
      Externalizable ext = (Externalizable) bean;

      InputStream is = dataHandler.getInputStream();

      if (is != null) {
	try {
	  ObjectInputStream in = new ObjectInputStream(is);

	  ext.readExternal(in);
	} finally {
	  is.close();
	}
      }
    }

    return bean;
  }
}
