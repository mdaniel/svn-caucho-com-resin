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
 * @author Sam
 */

package com.caucho.server.resin;

import com.caucho.vfs.Path;

abstract public class ResinELContext
{
  private final JavaVar _javaVar = new JavaVar();
  private ResinVar _resinVar;

  public ResinELContext()
  {
  }

  public JavaVar getJavaVar()
  {
    return _javaVar;
  }

  public ResinVar getResinVar()
  {
    if (_resinVar == null) {
      _resinVar = new ResinVar(getServerId(),
                               getResinHome(),
                               getRootDirectory(),
                               getLogDirectory(),
                               getResinConf(),
                               isResinProfessional(),
                               null);
    }
    
    return _resinVar;
  }

  public ResinVar getServerVar()
  {
    return getResinVar();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  abstract public Path getResinHome();

  abstract public Path getRootDirectory();
  
  abstract public Path getLogDirectory();

  abstract public Path getResinConf();

  abstract public String getServerId();

  abstract public boolean isResinProfessional();
}
