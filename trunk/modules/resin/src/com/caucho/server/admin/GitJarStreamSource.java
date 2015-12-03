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

package com.caucho.server.admin;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.env.git.GitCommitJar;
import com.caucho.vfs.StreamSource;

/**
 * Stream source for Hessian serialization of large data
 */
public class GitJarStreamSource extends StreamSource
{
  private String _sha1;
  private GitCommitJar _gitJar;

  /**
   * Create a new GitJarStreamSource for the named file
   */
  public GitJarStreamSource(String sha1, GitCommitJar gitJar)
  {
    _sha1 = sha1;
    _gitJar = gitJar;
  }

  /**
   * Returns an input stream, freeing the results
   */
  @Override
  public InputStream getInputStream()
    throws IOException
  {
    return openInputStream();
  }

  /**
   * Returns an input stream, without freeing the results
   */
  @Override
  public InputStream openInputStream()
    throws IOException
  {
    return _gitJar.openFile(_sha1);
  }
}
