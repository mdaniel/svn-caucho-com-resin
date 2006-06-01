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

import java.net.URL;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Facade to data from multiple sources.
 */
public class DataHandler implements Transferable {
  private static DataContentHandlerFactory _factory;
  
  private DataSource _dataSource;
  
  private Object _object;
  private String _mimeType;

  private CommandMap _commandMap;

  /**
   * Creates the handler with the given source.
   */
  public DataHandler(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Creates the handler with the given object and mime type.
   */
  public DataHandler(Object obj, String mimeType)
  {
    _object = obj;
    _mimeType = mimeType;
  }

  /**
   * Creates the handler with the given object and mime type.
   */
  public DataHandler(URL url)
  {
    _dataSource = new URLDataSource(url);
  }

  /**
   *  Returns the data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   *  Returns the name of the data.
   */
  public String getName()
  {
    if (_dataSource != null)
      return _dataSource.getName();
    else
      return null;
  }

  /**
   * Returns the object's mime-type
   */
  public String getContentType()
  {
    if (_dataSource != null)
      return _dataSource.getContentType();
    else
      return _mimeType;
  }

  /**
   * Returns the input stream to the object.
   */
  public InputStream getInputStream()
    throws IOException
  {
    if (_dataSource != null)
      return _dataSource.getInputStream();
    else
      throw new UnsupportedOperationException();
  }

  /**
   * Writes to the output stream.
   */
  public void writeTo(OutputStream os)
    throws IOException
  {
    // XXX: ?
    String text = (String) _object;
    byte []bytes = text.getBytes();

    os.write(bytes, 0, bytes.length);
  }

  /**
   * Returns the input stream to the data source.
   */
  public OutputStream getOutputStream()
    throws IOException
  {
    if (_dataSource != null)
      return _dataSource.getOutputStream();
    else
      return null;
  }

  /**
   * Returns the data transfer flavors.
   */
  public DataFlavor []getTransferDataFlavors()
  {
    DataContentHandler handler = getDataContentHandler();
    
    if (handler != null)
      return handler.getTransferDataFlavors();
    throw new UnsupportedOperationException();
  }

  /**
   * Checks to see if the specified flavor is supported.
   */
  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    DataFlavor []flavors = getTransferDataFlavors();

    if (flavors == null)
      return false;

    for (int i = 0; i < flavors.length; i++) {
      DataFlavor supportedFlavor = flavors[i];

      if (flavor.equals(supportedFlavor))
	return true;
    }

    return false;
  }

  /**
   * Returns the object for the data to be transferred.
   */
  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException, IOException
  {
    throw new UnsupportedFlavorException(flavor);
  }

  /**
   * Sets the command map.
   */
  public void setCommandMap(CommandMap map)
  {
    _commandMap = map;
  }

  /**
   * Gets the command map.
   */
  private CommandMap getCommandMap()
  {
    if (_commandMap != null)
      return _commandMap;
    else
      return CommandMap.getDefaultCommandMap();
  }

  /**
   * Returns the preferred commands.
   */
  public CommandInfo []getPreferredCommands()
  {
    return getCommandMap().getPreferredCommands(getContentType());
  }

  /**
   * Returns all the commands.
   */
  public CommandInfo []getAllCommands()
  {
    return getCommandMap().getAllCommands(getContentType());
  }

  /**
   * Returns all the commands.
   */
  public CommandInfo getCommand(String commandName)
  {
    return getCommandMap().getCommand(getContentType(), commandName);
  }

  /**
   * Returns the object.
   */
  public Object getContent()
    throws IOException
  {
    return _object;
  }

  /**
   * Returns the bean.
   */
  public Object getBean(CommandInfo cmdInfo)
    throws IOException, ClassNotFoundException
  {
    return cmdInfo.getCommandObject(this, getClass().getClassLoader());
  }

  /**
   * Sets the DataContentHandlerFactory
   */
  public static void setDataContentHandlerFactory(DataContentHandlerFactory factory)
  {
    if (_factory != null)
      throw new Error("factory already set.");

    _factory = factory;
  }

  /**
   * Returns the handler.
   */
  private DataContentHandler getDataContentHandler()
  {
    if (_factory != null) {
      return _factory.createDataContentHandler(getContentType());
    }
    else
      throw new UnsupportedOperationException();
  }
}
