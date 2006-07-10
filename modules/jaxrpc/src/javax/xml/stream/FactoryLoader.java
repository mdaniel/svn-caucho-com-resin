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
 * @author Adam Megacz
 */

package javax.xml.stream;
import javax.xml.namespace.*;
import javax.xml.stream.events.*;
import java.util.*;
import java.util.logging.*;
import java.io.*;

class FactoryLoader {

  private static Logger log =
    Logger.getLogger("javax.xml.stream.FactoryLoader");

  public static Object newInstance(String factoryId,
				   ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    String className = null;

    className = System.getProperty(factoryId);

    if (className == null) {
      
      String fileName =
	System.getProperty("java.home") +
	File.separatorChar +
	"lib" +
	File.separatorChar +
	"stax.properties";

      FileInputStream is = null;
      try {
	is = new FileInputStream(new File(fileName));

	Properties props = new Properties();
	props.load(is);

	className = props.getProperty(factoryId);

      }
      catch (IOException e) {
	log.log(Level.FINER, "ignoring exception", e);

      }
      finally {
	if (is != null)
	  try {
	    is.close();
	  } catch (IOException e) {
	    log.log(Level.FINER, "ignoring exception", e);
	  }
      }
    }

    if (className == null) {
      try {
	InputStream is =
	  classLoader.getResourceAsStream("/META-INF/services/" + factoryId);
	
	BufferedReader br =
	  new BufferedReader(new InputStreamReader(is));
	className = br.readLine();
	  
	if (className.indexOf('#') != -1)
	  className = className.substring(0, className.indexOf('#'));
      } catch (Exception e) {
	throw new FactoryConfigurationError(e);
      }
    }

    if (className != null) {
	
      try {
	return classLoader.loadClass(className).newInstance();
      }
      catch (Exception e) {
	throw new FactoryConfigurationError(e);
      }
    }

    return null;
  }
}
