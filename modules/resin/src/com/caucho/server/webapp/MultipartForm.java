/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.config.types.Bytes;
import com.caucho.util.L10N;

/**
 * Configuration for the multipart form.
 */
public class MultipartForm {
  static L10N L = new L10N(MultipartForm.class);

  private boolean _enable = true;
  private long _uploadMax = -1;
  private long _parameterLengthMax = -1;

  public void setEnable(boolean enable)
  {
    _enable = enable;
  }

  public boolean isEnable()
  {
    return _enable;
  }

  public void setUploadMax(Bytes max)
  {
    _uploadMax = max.getBytes();
  }

  public long getUploadMax()
  {
    return _uploadMax;
  }

  public void setParameterLengthMax(Bytes max)
  {
    _parameterLengthMax = max.getBytes();
  }

  public long getParameterLengthMax()
  {
    return _parameterLengthMax;
  }
}
