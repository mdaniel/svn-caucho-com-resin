/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import java.util.ArrayList;
import java.util.HashMap;

import com.caucho.bootjni.JniProcess;
import com.caucho.inject.Module;
import com.caucho.vfs.Path;

/**
 * Resin's bootstrap class.
 */
@Module
public class JniBoot implements Boot {
  private JniProcess _jniProcess;
  
  public JniBoot()
  {
    _jniProcess = new JniProcess();
  }

  @Override
  public boolean isValid()
  {
    return _jniProcess != null && _jniProcess.isEnabled();
  }
  
  @Override
  public String getValidationMessage()
  {
    return _jniProcess != null ? _jniProcess.getTroubleshootMessage() : null;
  }

  @Override
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
  
  @Override
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
