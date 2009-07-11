/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.bootjni;

import com.caucho.boot.*;
import com.caucho.config.ConfigException;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resin's bootstrap class.
 */
public class JniProcess extends Process
 implements JniProcessAPI
{
  private static final L10N L
    = new L10N(JniProcess.class);
  private static final Logger log
    = Logger.getLogger(JniProcess.class.getName());

  private static RuntimeException _jniLoadException;
  
  private static boolean _hasJni;

  private int _stdoutFd = -1;
  private int _pid = -1;
  private int _exitValue = -1;

  private int _status = -1;

  private ReadStream _is;

  public JniProcess()
  {
    if (_jniLoadException == null)
      _hasJni = isNativeBootAvailable();
  }

  private JniProcess(ArrayList<String> args,
		     HashMap<String,String> env,
		     String chroot,
		     String pwd,
		     String user,
		     String group)
  {
    if (! _hasJni)
      throw new UnsupportedOperationException("No JNI available");

    String []argv = new String[args.size()];
    args.toArray(argv);

    String []envp = new String[env.size()];

    int i = 0;
    for (Map.Entry<String,String> entry : env.entrySet()) {
      envp[i++] = entry.getKey() + '=' + entry.getValue();
    }

    setFdMax();

    if (! exec(argv, envp, chroot, pwd, user, group))
      throw new IllegalStateException("exec failed");

    int stdoutFd = _stdoutFd;
    _stdoutFd = -1;

    try {
      StreamImpl stream;

      Class cl = Class.forName("com.caucho.vfs.JniFileStream",
			       false, getClass().getClassLoader());

      Constructor ctor = cl.getConstructor(new Class[] { int.class, boolean.class, boolean.class });
      
      stream = (StreamImpl) ctor.newInstance(stdoutFd, true, false);
      
      _is = new ReadStream(stream);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public JniProcess create(ArrayList<String> args,
                           HashMap<String,String> env,
                           String chroot,
                           String pwd,
                           String user,
                           String group)
  {
    if (_hasJni)
      return new JniProcess(args, env, chroot, pwd, user, group);
    else
      throw new UnsupportedOperationException("No JNI available");
  }

  public boolean isValid()
  {
    return _hasJni;
  }
  
  public OutputStream getOutputStream()
  {
    return new NullOutputStream();
  }

  public InputStream getInputStream()
  {
    return _is;
  }

  public InputStream getErrorStream()
  {
    return getInputStream();
  }

  public int getPid()
  {
    return _pid;
  }

  public void chown(String path, String user, String group)
  {
    if (_jniLoadException != null)
      throw _jniLoadException;
    
    byte []name = path.getBytes();
    int len = name.length;

    nativeChown(name, len, user, group);
  }

  public int waitFor()
  {
    int pid = _pid;
    _pid = 0;
    
    if (pid > 0) {
      _status = waitpid(pid, true);
    }

    return _status;
  }

  public int exitValue()
  {
    if (_status >= 0)
      return _status;

    if (_pid > 0) {
      int result = waitpid(_pid, false);

      if (result < 0)
	throw new IllegalThreadStateException("Pid " + _pid + " not yet closed");
      _pid = 0;
      _status = result;
    }

    return _status;
  }

  public void destroy()
  {
  }

  static class NullOutputStream extends OutputStream {
    public void write(int ch)
    {
    }
    
    public void flush()
    {
    }
    
    public void close()
    {
    }
  }
  
  public native boolean isNativeBootAvailable();
  
  public native boolean clearSaveOnExec();

  public native static int getFdMax();
  public native static int setFdMax();
  
  private native boolean exec(String []argv,
			      String []envp,
			      String chroot,
			      String pwd,
			      String user,
			      String group);
  
  private native void nativeChown(byte []name, int length,
				  String user, String group);
  
  private native int waitpid(int pid, boolean isBlock);

  static {
    try {
      System.loadLibrary("resin_os");
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      if (e instanceof RuntimeException)
	_jniLoadException = (RuntimeException) e;
      else
	_jniLoadException = new RuntimeException(e);
    }
  }
}
