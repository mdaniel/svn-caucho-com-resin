/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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

package com.caucho.v5.cli.boot;

import java.io.Console;
import java.util.List;

import com.caucho.v5.admin.Password;
import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.server.ServerCommandBase;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.util.L10N;

/**
 * Command to encrypt a password for resin:Password
 */
public class PasswordEncryptCommand extends ServerCommandBase<ArgsDaemon>
{
  private static final L10N L = new L10N(PasswordEncryptCommand.class);
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("password", "password", "the password to be encrypted");
    addValueOption("salt", "salt", "optional salt for the password");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "encrypts a password";
  }

  @Override
  public ExitCode doCommand(ArgsDaemon args, 
                            ConfigBoot boot)
    throws BootArgumentException
  {
    // validateArgs(args.getArgv());
    
    String salt = args.getArg("-salt");
    String password = args.getArg("-password");
    
    List<String> tailArgs = args.getTailArgs();
    
    if (tailArgs.size() == 1 && 
        ! tailArgs.get(0).startsWith("-")) {
      password = tailArgs.get(0);
    }
    
    if (password == null)
      password = readPasswordFromConsole();
      
    if (password == null)
      throw new ConfigException(L.l("password-encrypt requires a -password argument"));
    
    String value = encrypt(password, salt);
    
    if (value != null)
      System.out.println("password: {RESIN}" + value);
    else
      System.out.println("password: " + password);

    return ExitCode.OK;
  }
  
  private static String readPasswordFromConsole()
  {
    try {
      
      Console console = System.console();
      if (console == null) {
        System.out.println(L.l("Warning: interactive console is not available"));
        return null;
      }
      
      char[] passwordChars = console.readPassword("Enter password: ");
      
      if (passwordChars == null || passwordChars.length == 0)
        return null;
      
      return new String(passwordChars);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  private String encrypt(String password, String salt)
  {
    try {
      Class<?> cl = Class.forName("com.caucho.admin.PasswordImpl");
      Password passwordApi = (Password) cl.newInstance();

      return passwordApi.encrypt(password, salt);
    } catch (Exception e) {
      throw ConfigException.create("password-encrypt requires Resin Pro\n",
                                   e);
      //return null;
    }
  }
  
  @Override
  public String getUsageTailArgs()
  {
    return " [<password>]";
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }
}