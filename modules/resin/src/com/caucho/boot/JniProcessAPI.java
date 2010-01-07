/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.boot;

import com.caucho.config.ConfigException;
import com.caucho.license.LicenseCheck;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resin's bootstrap class.
 */
public interface JniProcessAPI {
  public boolean clearSaveOnExec();
  
  public boolean isValid();

  public Process create(ArrayList<String> args,
			HashMap<String,String> env,
			String chroot,
			String pwd,
			String user,
			String group);

  public void chown(String path, String user, String group);
}
