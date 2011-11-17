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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.boot;

import java.security.MessageDigest;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

/**
 * Command to stop Resin server
 * bin/resin.sh status -server a
 */
public class GeneratePasswordCommand extends AbstractBootCommand
{
  private static Logger _log;
  private static L10N _L;
  
  public GeneratePasswordCommand()
  {
    addValueKey("-user");
    addValueKey("-password");
  }

  @Override
  public String getName()
  {
    return "generate-password";
  }

  @Override
  public int doCommand(WatchdogArgs args, WatchdogClient client)
    throws BootArgumentException
  {
    // validateArgs(args.getArgv());
    
    String user = args.getArg("user");
    String password = args.getArg("password");
    
    if (args.getTailArgs().size() == 2) {
      user = args.getTailArgs().get(0);
      password = args.getTailArgs().get(1);
    }
    
    if (user == null)
      throw new ConfigException(L().l("generate-password requires a -user argument"));
    
    if (password == null)
      throw new ConfigException(L().l("generate-password requires a -password argument"));
    
    
    byte []salt = new byte[] { (byte) RandomUtil.getRandomLong(),
                               (byte) RandomUtil.getRandomLong(),
                               (byte) RandomUtil.getRandomLong(),
                               (byte) RandomUtil.getRandomLong() };
      
    CharBuffer cb = new CharBuffer();
    
    byte []digest = sha1(password, salt);
    byte []hash = new byte[salt.length + digest.length];
    
    System.arraycopy(digest, 0, hash, 0, digest.length);
    System.arraycopy(salt, 0, hash, digest.length, salt.length);
    
    Base64.encode(cb, hash);
    
    String sshaPassword = "{SSHA}" + cb;

    System.out.println("admin_user : " + user);
    System.out.println("admin_password : " + sshaPassword);

    return 0;
  }
  
  private byte []sha1(String password, byte []salt)
  {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA1");
      
      for (int i = 0; i < password.length(); i++) {
        digest.update((byte) password.charAt(i));
      }
      
      digest.update(salt);
      
      return digest.digest();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(GeneratePasswordCommand.class.getName());

    return _log;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(GeneratePasswordCommand.class);

    return _L;
  }

  @Override
  public void usage()
  {
    System.out.println("usage: bin/resin.sh [-options] generate-password");
    System.out.println();
    System.out.println("where options include:");
    System.out.println("   -user <name>         : the user name");
    System.out.println("   -password <password> : the password");
  }
}