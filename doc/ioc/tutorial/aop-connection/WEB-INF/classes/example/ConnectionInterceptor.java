package example;

import java.lang.reflect.Method;

import java.sql.Connection;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * The require connection interceptor.
 */
public class ConnectionInterceptor implements MethodInterceptor {
  private DataSource _dataSource;
  private int _connParamIndex = -1;

  /**
   * Configures the data source based on the annotation value.
   */
  public void setValue(String jndiName)
    throws NamingException
  {
    String name = jndiName;

    if (name.indexOf(':') < 0)
      name = "java:comp/env/" + jndiName;
    
    _dataSource = (DataSource) new InitialContext().lookup(name);
  }
  
  public Object invoke(MethodInvocation inv) throws Throwable
  {
    if (_connParamIndex < 0)
      _connParamIndex = getParamIndex(inv.getMethod());

    Object []args = inv.getArguments();
      
    Connection oldConn = (Connection) args[_connParamIndex];
    Connection conn = oldConn;

    try {
      if (conn == null) {
	conn = _dataSource.getConnection();
	
	args[_connParamIndex] = conn;
      }
      
      return inv.proceed();
    } finally {
      if (oldConn == null && conn != null)
	conn.close();
    }
  }

  /**
   * Returns the parameter index of the Connection parameter.
   *
   * @param method the calling method.
   */
  private int getParamIndex(Method method)
  {
    Class []paramTypes = method.getParameterTypes();

    for (int i = 0; i < paramTypes.length; i++) {
      if (Connection.class.equals(paramTypes[i]))
	return i;
    }

    throw new IllegalArgumentException(method.toString());
  }
}
