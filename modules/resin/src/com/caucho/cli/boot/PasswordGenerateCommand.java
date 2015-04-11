/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.cli.boot;

import java.io.Console;
import java.security.MessageDigest;
import java.util.List;
import java.util.logging.Logger;

import com.caucho.cli.baratine.ArgsCli;
import com.caucho.cli.server.BootArgumentException;
import com.caucho.cli.server.ServerCommandBase;
import com.caucho.config.ConfigException;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.config.ConfigBoot;
import com.caucho.util.Base64Util;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

/**
 * Command to stop Resin server
 * bin/resin.sh status -server a
 */
public class PasswordGenerateCommand extends ServerCommandBase<ArgsCli>
{
  private static Logger _log;
  private static L10N _L;
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("user", "user", "the user name to generate a password hash");
    addValueOption("password", "password", "the password for the password hash (leave empty for prompt)");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "generates an administrator user and password";
  }

  @Override
  public ExitCode doCommand(ArgsCli args, 
                            ConfigBoot boot)
    throws BootArgumentException
  {
    // validateArgs(args.getArgv());
    
    String user = args.getArg("-user");
    String password = args.getArg("-password");
    
    List<String> tailArgs = args.getTailArgs();
    
    if (tailArgs.size() == 2
        && ! tailArgs.get(0).startsWith("-")
        && ! tailArgs.get(1).startsWith("-")) {
      user = tailArgs.get(0);
      password = tailArgs.get(1);
    }
    
    if (user == null)
      throw new ConfigException(L().l("generate-password requires a --user argument"));
    
    if (password == null) {
      password = readPasswordFromConsole("Enter");
      
      String password2 = readPasswordFromConsole("Verify");
      
      if (password == null || ! password.equals(password2)) {
        throw new ConfigException(L().l("password must match"));
      }
    }
      
    if (password == null) {
      throw new ConfigException(L().l("generate-password requires a -password argument"));
    }
    
    
    byte []salt = new byte[] { (byte) RandomUtil.getRandomLong(),
                               (byte) RandomUtil.getRandomLong(),
                               (byte) RandomUtil.getRandomLong(),
                               (byte) RandomUtil.getRandomLong() };
      
    CharBuffer cb = new CharBuffer();
    
    byte []digest = sha1(password, salt);
    byte []hash = new byte[salt.length + digest.length];
    
    System.arraycopy(digest, 0, hash, 0, digest.length);
    System.arraycopy(salt, 0, hash, digest.length, salt.length);
    
    Base64Util.encode(cb, hash);
    
    String sshaPassword = "{SSHA}" + cb;

    System.out.println("admin_user : " + user);
    System.out.println("admin_password : " + sshaPassword);

    return ExitCode.OK;
  }
  
  private static String readPasswordFromConsole(String msg)
  {
    try {
      
      Console console = System.console();
      if (console == null) {
        System.out.println(L().l("Warning: interactive console is not available"));
        return null;
      }
      
      char[] passwordChars = console.readPassword(msg + " password: ");
      
      if (passwordChars == null || passwordChars.length == 0)
        return null;
      
      return new String(passwordChars);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
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
      _log = Logger.getLogger(PasswordGenerateCommand.class.getName());

    return _log;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(PasswordGenerateCommand.class);

    return _L;
  }
  
  @Override
  public String getUsageTailArgs()
  {
    return " [<user>] [<password>]";
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }

  /*
  @Override
  public void usage()
  {
    System.out.println("usage: bin/resin.sh [-options] generate-password");
    System.out.println();
    System.out.println("where options include:");
    System.out.println("   -user <name>         : the user name");
    System.out.println("   -password <password> : the password");
  }
  */
}