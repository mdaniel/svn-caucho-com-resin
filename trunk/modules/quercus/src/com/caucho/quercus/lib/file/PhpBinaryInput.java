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
 * @author Nam Nguyen
 */
package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StringStream;
import com.caucho.vfs.VfsStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * php://input
 */
public class PhpBinaryInput extends AbstractBinaryInput
{
  private static final Logger log
    = Logger.getLogger(PhpBinaryInput.class.getName());

  public PhpBinaryInput(Env env)
  {
    super(env);
    
    StringValue inputData = env.getInputData();
    
    if (inputData == null)
      inputData = env.getEmptyString();
    
    init(new ReadStream(new VfsStream(inputData.toInputStream(), null)));
  }
  
  public String toString()
  {
    return "PhpBinaryInput[]";
  }
}
