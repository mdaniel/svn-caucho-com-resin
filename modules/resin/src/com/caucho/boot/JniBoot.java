/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.boot;

import java.util.ArrayList;
import java.util.HashMap;

import com.caucho.config.ConfigException;
import com.caucho.vfs.*;

/**
 * Resin's bootstrap class.
 */
public class JniBoot implements Boot {
  private JniProcessAPI _jniProcess;
  
  public JniBoot()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      Class cl = Class.forName("com.caucho.bootjni.JniProcess", false, loader);
      
      _jniProcess = (JniProcessAPI) cl.newInstance();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public boolean isValid()
  {
    return _jniProcess != null && _jniProcess.isValid();
  }
  
  public void clearSaveOnExec()
  {
    if (_jniProcess != null)
      _jniProcess.clearSaveOnExec();
  }

  public void chown(Path path, String user, String group)
  {
    if (_jniProcess != null)
      _jniProcess.chown(path.getNativePath(), user, group);
  }
  
  public Process exec(ArrayList<String> argv,
		      HashMap<String,String> env,
		      String chroot,
		      String pwd,
		      String user,
		      String group)
  {
    if (_jniProcess != null)
      return _jniProcess.create(argv, env, chroot, pwd, user, group);
    else
      return null;
  }
}
