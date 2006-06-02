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

import java.awt.datatransfer.DataFlavor;
import java.io.*;
import java.util.logging.*;

/**
 * Supports the DataFlavor.
 */
public class ActivationDataFlavor extends DataFlavor  {

  private static Logger log =
    Logger.getLogger("javax.activation.ActivationDataFlavor");

  private Class _representationClass;

  /**
   * Creates the data flavor.
   */
  public ActivationDataFlavor(Class representationClass,
			      String mimeType,
			      String humanPresentableName)
  {
    super(mimeType, humanPresentableName);
    this._representationClass = representationClass;
  }
  
  /**
   * Creates the data flavor.
   */
  public ActivationDataFlavor(Class representationClass,
			      String humanPresentableName)
  {
    this(representationClass,
	 "application/x-java-serialized-object; class="+
	 representationClass.getName(),
	 humanPresentableName);
  }
  
  /**
   * Creates the data flavor.
   *        
   * NOTE: Sun's Javadoc claims "If the mimeType is
   *       "application/x-java-serialized-object; class=", the result
   *       is the same as calling new DataFlavor(Class.forName()) as
   *       above.", but their implementation DOES NOT DO THIS, so we
   *       don't do it either.
   */
  public ActivationDataFlavor(String mimeType, String humanPresentableName)
  {
    this(InputStream.class, mimeType, humanPresentableName);
  }

  /**
   * Compares the DataFlavor passed in with this DataFlavor; calls
   * the isMimeTypeEqual method.
   *
   * @param dataFlavor the DataFlavor to compare with 
   * @return true if the MIME type and representation class are the same 
   */
  public boolean equals(DataFlavor dataFlavor)
  {
    if (dataFlavor == null)
      return false;

    if (getRepresentationClass() != dataFlavor.getRepresentationClass())
      return false;

    return isMimeTypeEqual(dataFlavor.getMimeType());
  }

  /**
   * Return the Human Presentable name.
   *
   * @return the human presentable name 
   */
  public String getHumanPresentableName()
  {
    return super.getHumanPresentableName();
  }

  /**
   * Returns the mime-type.
   */
  public String getMimeType()
  {
    return super.getMimeType();
  }

  /**
   * Return the representation class.
   *
   * @return the representation class 
   */
  public Class getRepresentationClass()
  {
    return _representationClass;
  }

  /**
   * Is the string representation of the MIME type passed in
   * equivalent to the MIME type of this DataFlavor.
   * ActivationDataFlavor delegates the comparison of MIME types to
   * the MimeType class included as part of the JavaBeans Activation
   * Framework. This provides a more robust comparison than is
   * normally available in the DataFlavor class.
   *
   * @param mimeType the MIME type 
   * @return true if the same MIME type 
   */
  public boolean isMimeTypeEqual(String mimeType)
  {
    try {
      return new MimeType(getMimeType()).match(mimeType);
    }
    catch (Exception e) {
      // deliberately ignored
      log.log(Level.FINER, e.toString(), e);
      return false;
    }
  }

  /**
   * Deprecated.
   * @deprecated as of JAF 1.1
   */
  protected String normalizeMimeTypeParameter(String parameterName,
					      String parameterValue)
  {
    throw new UnsupportedOperationException("you should not be calling this");
  }

  /**
   * Deprecated.
   * @deprecated as of JAF 1.1
   */
  protected String normalizeMimeType(String mimeType)
  {
    throw new UnsupportedOperationException("you should not be calling this");
  }

  /**
   * Set the human presentable name.
   *
   * @param humanPresentableName the name to set
   */
  public void setHumanPresentableName(String humanPresentableName)
  {
    super.setHumanPresentableName(humanPresentableName);
  }

}
