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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import java.io.*;
import java.rmi.*;

import com.caucho.util.*;
import com.caucho.xml.*;
import com.caucho.vfs.*;

public class SerializedExceptionWrapper extends RemoteException {
  private Throwable cause;
  private String trace;
  
  public SerializedExceptionWrapper() {}
  
  public SerializedExceptionWrapper(Throwable cause, String trace)
  {
    this.cause = cause;
    this.trace = trace;
  }

  public Throwable getRootCause()
  {
    return cause;
  }

  public String getMessage()
  {
    return "remote: " + cause.getMessage();
  }

  public String toString()
  {
    return "SerializedExceptionWrapper: " + cause.toString();
  }

  public void printStackTrace()
  {
    if (trace != null) {
      System.out.print("remote exception: ");
      System.out.print(trace);
    }
    
    System.out.print("local exception: ");
    super.printStackTrace();
  }

  public void printStackTrace(PrintWriter pw)
  {
    if (trace != null) {
      pw.print("remote exception: ");
      pw.print(trace);
    }
    
    pw.print("local exception: ");
    super.printStackTrace(pw);
  }

  public void printStackTrace(PrintStream os)
  {
    if (trace != null) {
      os.print("remote exception: ");
      os.print(trace);
    }

    os.print("local exception: ");
    super.printStackTrace(os);
  }
}
