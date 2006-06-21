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

import java.net.URL;

import java.io.*;

import java.util.logging.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Facade to data from multiple sources.
 */
public class DataHandler implements Transferable {

  private static Logger log =
    Logger.getLogger("javax.activation.DataHandler");

  private static DataContentHandlerFactory _factory;
  
  private DataSource _dataSource;
  private DataSource _cachedDataSource;
  
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
    if (_dataSource!=null)
      return _dataSource;

    if (_cachedDataSource==null)
      _cachedDataSource = new DataHandlerDataSource();

    return _cachedDataSource;
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
   *
   * WARNING: this method cannot be tested (see server/340g.qa)
   */
  public InputStream getInputStream()
    throws IOException
  {
    if (_dataSource != null)
      return _dataSource.getInputStream();

    final DataContentHandler dch = getDataContentHandler();

    if (dch==null)
      throw new UnsupportedDataTypeException("dch==null");
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    dch.writeTo(_object, _mimeType, baos);

    return
      new ByteArrayInputStream(baos.toByteArray());
  }

  /**
   * Writes to the output stream.
   *
   * WARNING: this method cannot be tested (see server/340g.qa)
   */
  public void writeTo(OutputStream os)
    throws IOException
  {
    if (_dataSource != null) {
      InputStream is = _dataSource.getInputStream();

      try {

	byte[] buf = new byte[1024];
	while(true) {
	  int numread = is.read(buf, 0, buf.length);
	  if (numread==-1) break;
	  os.write(buf, 0, numread);
	}

      } finally {
	is.close();
      }
    }
    else {
      getDataContentHandler().writeTo(_object, _mimeType, os);
    }
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
   *
   * WARNING: this method cannot be tested (see server/340g.qa)
   */
  public DataFlavor []getTransferDataFlavors()
  {
    try {
      DataContentHandler handler = getDataContentHandler();
      
      if (handler != null)
	return handler.getTransferDataFlavors();
    }
    catch (UnsupportedDataTypeException e) {
      log.log(Level.FINER, e.toString(), e);
      return null;
    }

    // XXX: If a DataContentHandler can not be located, and if the
    // DataHandler was created with a DataSource (or URL), one
    // DataFlavor is returned that represents this object's MIME type
    // and the java.io.InputStream class. If the DataHandler was
    // created with an object and a MIME type, getTransferDataFlavors
    // returns one DataFlavor that represents this object's MIME type
    // and the object's class.

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
    // XXX:

    // Returns an object that represents the data to be
    // transferred. The class of the object returned is defined by the
    // representation class of the data flavor.

    // For DataHandler's created with DataSources or URLs:

    // The DataHandler attempts to locate a DataContentHandler for
    // this MIME type. If one is found, the passed in DataFlavor and
    // the type of the data are passed to its getTransferData
    // method. If the DataHandler fails to locate a DataContentHandler
    // and the flavor specifies this object's MIME type and the
    // java.io.InputStream class, this object's InputStream is
    // returned. Otherwise it throws an UnsupportedFlavorException.

    // For DataHandler's created with Objects:

    // The DataHandler attempts to locate a DataContentHandler for
    // this MIME type. If one is found, the passed in DataFlavor and
    // the type of the data are passed to its getTransferData
    // method. If the DataHandler fails to locate a DataContentHandler
    // and the flavor specifies this object's MIME type and its class,
    // this DataHandler's referenced object is returned. Otherwise it
    // throws an UnsupportedFlavorException.

    throw new UnsupportedFlavorException(flavor);
  }

  /**
   * Sets the command map.
   */
  public void setCommandMap(CommandMap map)
  {
    _commandMap = map==null ? CommandMap.getDefaultCommandMap() : map;
    _cachedDataSource = null;
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
   *
   * WARNING: this method cannot be tested (see server/340g.qa)
   */
  public Object getContent()
    throws IOException
  {
    if (_object==null) {
      DataContentHandler dch = getDataContentHandler();
      if (dch==null)
	return getInputStream();
      return dch.getContent(getDataSource());
    }
    else {
      return _object;
    }
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
   *
   * WARNING: this method cannot be tested (see server/340g.qa)
   */
  public static void setDataContentHandlerFactory(DataContentHandlerFactory
						  factory)
  {
    if (_factory != null)
      throw new IllegalStateException("factory already set.");

    _factory = factory;
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
   * Returns the handler.
   *
   * WARNING: this method cannot be tested (see server/340g.qa)
   */
  private DataContentHandler getDataContentHandler()
    throws UnsupportedDataTypeException
  {
    if (_factory != null)
      return _factory.createDataContentHandler(getContentType());
    else
      throw new UnsupportedDataTypeException();
  }

  private class DataHandlerDataSource implements DataSource {
    public InputStream getInputStream()
      throws IOException
    {
      return DataHandler.this.getInputStream();
    }

    public OutputStream getOutputStream()
      throws IOException
    {
      return DataHandler.this.getOutputStream();
    }
  
    public String getContentType()
    {
      return DataHandler.this.getContentType();
    }
    public String getName()
    {
      return DataHandler.this.getName();
    }      
  }
}
