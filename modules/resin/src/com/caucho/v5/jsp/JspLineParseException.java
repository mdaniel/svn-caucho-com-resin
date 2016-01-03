/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.jsp;

import java.io.*;

import com.caucho.v5.config.DisplayableException;
import com.caucho.v5.config.UserMessageLocation;
import com.caucho.v5.javac.LineMapException;
import com.caucho.v5.util.*;

public class JspLineParseException extends JspParseException
  implements UserMessageLocation, LineMapException, DisplayableException {

  public JspLineParseException()
  {
  }

  public JspLineParseException(String msg)
  {
    super(msg);
  }

  public JspLineParseException(Throwable e)
  {
    super(e.getMessage(), e);
  }

  public JspLineParseException(String msg, Throwable e)
  {
    super(msg, e);
  }

  public void print(PrintWriter out)
  {
    out.println(Html.escapeHtml(getMessage()));
  }
}
