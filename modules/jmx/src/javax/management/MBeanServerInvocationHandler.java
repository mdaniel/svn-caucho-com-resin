/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package javax.management;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy hander for mbeans.
 *
 * @since JMX 1.2
 */
public class MBeanServerInvocationHandler implements InvocationHandler {
  private MBeanServerConnection _connection;
  private ObjectName _name;

  /**
   * Creates the invocation handler.
   */
  public MBeanServerInvocationHandler(MBeanServerConnection connection,
				      ObjectName objectName)
  {
    _connection = connection;
    _name = objectName;
  }

  /**
   * Creates a new proxy instance.
   */
  public static Object newProxyInstance(MBeanServerConnection connection,
					ObjectName objectName,
					Class interfaceClass,
					boolean notificationBroadcaster)
  {
    Class []interfaces;

    if (notificationBroadcaster)
      interfaces = new Class[] { interfaceClass, NotificationEmitter.class };
    else
      interfaces = new Class[] { interfaceClass };
    
    return Proxy.newProxyInstance(interfaceClass.getClassLoader(),
				  interfaces,
				  new MBeanServerInvocationHandler(connection,
								   objectName));
  }

  /**
   * Handles the object invocation.
   *
   * @param proxy the proxy object to invoke
   * @param method the method to call
   * @param args the arguments to the proxy object
   */
  public Object invoke(Object proxy, Method method, Object []args)
    throws Throwable
  {
    String methodName = method.getName();
    Class []params = method.getParameterTypes();

    // equals and hashCode are special cased
    if (methodName.equals("equals") &&
        params.length == 1 && params[0].equals(Object.class)) {
      Object value = args[0];
      if (value == null || ! Proxy.isProxyClass(value.getClass()))
        return Boolean.FALSE;

      MBeanServerInvocationHandler handler;

      handler = (MBeanServerInvocationHandler) Proxy.getInvocationHandler(value);

      return new Boolean(_name.equals(handler._name));
    }
    else if (methodName.equals("hashCode") && params.length == 0)
      return new Integer(_name.hashCode());

    int len = methodName.length();
    String attrName;
    
    if (params.length == 0 && methodName.startsWith("get") && len > 3) {
      attrName = methodName.substring(3);

      return _connection.getAttribute(_name, attrName);
    }
    else if (params.length == 1 && method.getReturnType().equals(void.class) &&
	     methodName.startsWith("set") && len > 3) {
      attrName = methodName.substring(3);

      Attribute attr = new Attribute(attrName, args[0]);

      _connection.setAttribute(_name, attr);

      return null;
    }
    else if (methodName.equals("addNotificationListener")) {
      if (args.length != 3) {
      }
      else if (args[0] instanceof NotificationListener) {
	_connection.addNotificationListener(_name,
					    (NotificationListener) args[0],
					    (NotificationFilter) args[1],
					    args[2]);
	return null;
      }
      else if (args[0] instanceof ObjectName) {
	_connection.addNotificationListener(_name,
					    (ObjectName) args[0],
					    (NotificationFilter) args[1],
					    args[2]);
	return null;
      }
    }
    else if (methodName.equals("removeNotificationListener")) {
      if (args.length == 3) {
	if (args[0] instanceof NotificationListener) {
	  _connection.removeNotificationListener(_name,
					      (NotificationListener) args[0],
					      (NotificationFilter) args[1],
					      args[2]);
	  return null;
	}
	else if (args[0] instanceof ObjectName) {
	  _connection.removeNotificationListener(_name,
					      (ObjectName) args[0],
					      (NotificationFilter) args[1],
					      args[2]);
	  return null;
	}
      }
      else if (args.length == 1) {
	if (args[0] instanceof NotificationListener) {
	  _connection.removeNotificationListener(_name,
						 (NotificationListener) args[0]);
	  return null;
	}
	else if (args[0] instanceof ObjectName) {
	  _connection.removeNotificationListener(_name,
						 (ObjectName) args[0]);

	  return null;
	}
      }
    }

    String []sig = new String[params.length];

    for (int i = 0; i < sig.length; i++)
      sig[i] = params[i].getName();

    return _connection.invoke(_name, methodName, args, sig);

    /*
    else if (methodName.equals("toString") && params.length == 0)
      return "MBeanProxy[" + _name + "]";

    return null;
    */
  }
}
