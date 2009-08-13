/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus;

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.Path;
import com.caucho.vfs.StdoutStream;
import com.caucho.vfs.WriteStream;

/**
 * Command line interface for Quercus
 */
public class QuercusEngine
{
  private Quercus _quercus;
  private String _fileName;
  private String []_args;
  
  public static void main(String []args)
    throws IOException
  {
    QuercusEngine engine = new QuercusEngine();
    
    engine.parseArgs(args);
    
    if (engine.getFileName() != null) {
      engine.exec();
    }
    else {
      throw new RuntimeException("input file not specified");
    }
  }
  
  public QuercusEngine()
  {
    _quercus = createQuercus();
    
    _quercus.init();
  }
  
  protected Quercus createQuercus()
  {
    return new CliQuercus();
  }
  
  public String getFileName()
  {
    return _fileName;
  }
  
  public Quercus getQuercus()
  {
    return _quercus;
  }
  
  public void parseArgs(String []args)
  {
    ArrayList<String> phpArgList = new ArrayList<String>();
    
    int i = 0;
    for (; i < args.length; i++) {
      if ("-d".equals(args[i])) {
        int eqIndex = args[i + 1].indexOf('=');
        
        String name = "";
        String value = "";
        
        if (eqIndex >= 0) {
          name = args[i + 1].substring(0, eqIndex);
          value = args[i + 1].substring(eqIndex + 1);
        }
        else {
          name = args[i + 1];
        }
        
        i++;
        _quercus.setIni(name, value);
      }
      else if ("-f".equals(args[i])) {
        _fileName = args[++i];
      }
      else if ("--".equals(args[i])) {
        break;
      }
      else if (args[i].startsWith("-")) {
        System.out.println("unknown option: " + args[i]);
        throw new RuntimeException("unknown option: " + args[i]);
      }
      else {
        phpArgList.add(args[i]);
      }
    }
    
    for (; i < args.length; i++) {
      phpArgList.add(args[i]);
    }
    
    _args = phpArgList.toArray(new String[phpArgList.size()]);
    
    if (_fileName == null && _args.length > 0)
      _fileName = _args[0];
  }
  
  public void exec()
    throws IOException
  {
    Path path = _quercus.getPwd().lookup(_fileName);
    
    QuercusPage page = _quercus.parse(path);
    
    WriteStream os = new WriteStream(StdoutStream.create());
      
    os.setNewlineString("\n");
    os.setEncoding("iso-8859-1");
    
    Env env = _quercus.createEnv(page, os, null, null);
    env.start();
    
    if (_args.length > 0)
      env.setArgs(_args);
    
    try {
      env.execute();
    } catch (QuercusDieException e) {
    } catch (QuercusExitException e) {
    }
    
    env.close();
    
    os.flush();
  }
}
