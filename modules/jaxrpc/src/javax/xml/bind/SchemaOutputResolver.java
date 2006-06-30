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

package javax.xml.bind;
import java.io.*;
import javax.xml.transform.*;

/**
 * Controls where a JAXB implementation puts the generates schema files. An
 * implementation of this abstract class has to be provided by the calling
 * application to generate schemas. This is a class, not an interface so as to
 * allow future versions to evolve without breaking the compatibility. Author:
 * Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public abstract class SchemaOutputResolver {
  public SchemaOutputResolver()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Decides where the schema file (of the given namespace URI) will be
   * written, and return it as a object. This method is called only once for
   * any given namespace. IOW, all the components in one namespace is always
   * written into the same schema document.
   */
  public abstract Result createOutput(String namespaceUri, String suggestedFileName) throws IOException;

}

