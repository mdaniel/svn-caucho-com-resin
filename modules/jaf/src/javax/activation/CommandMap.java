/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.util.*;

/**
 * Map of command objects.
 */
abstract public class CommandMap {

  private static WeakHashMap _commandMaps = new WeakHashMap();

  public CommandMap()
  {
  }

  /**
   * Returns the default command map.
   */
  public static CommandMap getDefaultCommandMap()
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl==null)
      cl = CommandMap.class.getClassLoader();

    CommandMap commandMap =
      (CommandMap)_commandMaps.get(cl);

    if (commandMap==null) {
      commandMap = new MailcapCommandMap();
      _commandMaps.put(cl, commandMap);
    }

    return commandMap;
  }

  /**
   * Sets the default command map.
   */
  public static void setDefaultCommandMap(CommandMap map)
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl==null)
      cl = CommandMap.class.getClassLoader();

    _commandMaps.put(cl, map);
  }

  /**
   * Returns the preferred command list.
   */
  public abstract CommandInfo []getPreferredCommands(String mimeType);

  /**                                                                      
   * Get the preferred command list from a MIME Type. The actual
   * semantics are determined by the implementation of the CommandMap.
   * The DataSource provides extra information, such as the file name,
   * that a CommandMap implementation may use to further refine the
   * list of commands that are returned.  The implementation in this
   * class simply calls the getPreferredCommands method that ignores
   * this argument.
   *                                                                       
   * @param mimeType the MIME type                                         
   * @param ds a DataSource for the data                                   
   * @return the CommandInfo classes that represent the command Beans.     
   * @since  JAF 1.1                                                       
   */
  public CommandInfo[] getPreferredCommands(String mimeType, DataSource ds)
  {
    return getPreferredCommands(mimeType);
  }

  /**
   * Returns all the command list.
   */
  public abstract CommandInfo []getAllCommands(String mimeType);

  /**                                                                      
   * Get all the available commands for this type. This method should
   * return all the possible commands for this MIME type.  The
   * DataSource provides extra information, such as the file name,
   * that a CommandMap implementation may use to further refine the
   * list of commands that are returned.  The implementation in this
   * class simply calls the getAllCommands method that ignores this
   * argument.
   *                                                                       
   * @param mimeType the MIME type                                         
   * @param ds a DataSource for the data                                   
   * @return the CommandInfo objects representing all the commands.        
   * @since  JAF 1.1                                                       
   */
  public CommandInfo[] getAllCommands(String mimeType, DataSource ds)
  {
    return getAllCommands(mimeType);
  }

  /**
   * Returns the default command.
   */
  public abstract CommandInfo getCommand(String mimeType, String cmdName);

  /**
   * Get the default command corresponding to the MIME type.  The
   * DataSource provides extra information, such as the file name,
   * that a CommandMap implementation may use to further refine the
   * command that is chosen.  The implementation in this class simply
   * calls the getCommand method that ignores this argument.
   *
   * @param mimeType the MIME type
   * @param cmdName the command name
   * @param ds a DataSource for the data 
   * @return the CommandInfo corresponding to the command. 
   * @since  JAF 1.1 
   */
  public CommandInfo getCommand(String mimeType, String cmdName, DataSource ds)
  {
    return getCommand(mimeType, cmdName);
  }

  /**
   * Returns the content handler
   */
  public abstract DataContentHandler createDataContentHandler(String mimeType);

  /**                                                                      
   * Locate a DataContentHandler that corresponds to the MIME type.
   * The mechanism and semantics for determining this are determined
   * by the implementation of the particular CommandMap.  The
   * DataSource provides extra information, such as the file name,
   * that a CommandMap implementation may use to further refine the
   * choice of DataContentHandler.  The implementation in this class
   * simply calls the createDataContentHandler method that ignores
   * this argument.
   *                                                                       
   * @param mimeType the MIME type                                         
   * @param ds a DataSource for the data                                   
   * @return the DataContentHandler for the MIME type                      
   * @since  JAF 1.1                                                       
   */
  public DataContentHandler createDataContentHandler(String mimeType,
						     DataSource ds)
  {
    return createDataContentHandler(mimeType);
  }


  /**                                                                      
   * Get all the MIME types known to this command map.  If the command
   * map doesn't support this operation, null is returned.
   *                                                                       
   * @return array of MIME types as strings, or null if not supported      
   * @since  JAF 1.1                                                       
   */
  public String[] getMimeTypes()
  {
    return null;
  }

}
