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

package com.caucho.iiop;

import com.caucho.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class SkeletonMethod {
  private static final Logger log = Log.open(SkeletonMethod.class);
  
  private IiopSkeleton _skeleton;
  private Method _method;
  
  private MarshallObject []_marshallArgs;
  private MarshallObject _marshallReturn;

  SkeletonMethod(IiopSkeleton skeleton, Method method, boolean isJava)
  {
    _skeleton = skeleton;
    _method = method;

    // System.out.println("M: " + method);
    
    Class []paramTypes = method.getParameterTypes();

    _marshallArgs = new MarshallObject[paramTypes.length];

    for (int i = 0; i < _marshallArgs.length; i++)
      _marshallArgs[i] = MarshallObject.create(paramTypes[i], isJava);

    _marshallReturn = MarshallObject.create(method.getReturnType(), isJava);
  }

  void service(Object object, IiopReader reader, IiopWriter writer)
    throws Throwable
  {
    Object []args = new Object[_marshallArgs.length];

    for (int i = 0; i < args.length; i++) {
      args[i] = _marshallArgs[i].unmarshall(reader);
    }

    try {
      Object result = _method.invoke(object, args);

      writer.startReplyOk(reader.getRequestId());

      _marshallReturn.marshall(result, writer);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
