/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.repository;

import com.caucho.loader.ivy.IvyPattern;
import com.caucho.util.Hex;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The module repository holds the module jars for osgi and ivy.
 */
abstract public class Resolver
{
  private static final L10N L = new L10N(Resolver.class);

  private IvyPattern _artifactPattern;
  private IvyPattern _ivyPattern;

  public void setArtifactPattern(IvyPattern pattern)
  {
    _artifactPattern = pattern;
  }

  public IvyPattern getArtifactPattern()
  {
    return _artifactPattern;
  }

  public void setIvyPattern(IvyPattern pattern)
  {
    _ivyPattern = pattern;
  }

  public IvyPattern getIvyPattern()
  {
    return _ivyPattern;
  }

  public final DataSource resolveArtifact(String org,
					  String module,
					  String rev,
					  String ext)
  {
    return resolveArtifact(org, module, module, rev, ext);
  }
  
  abstract public DataSource resolveArtifact(String org,
					     String module,
					     String artifact,
					     String rev,
					     String ext);

  protected String resolveArtifactString(String org,
					 String module,
					 String artifact,
					 String rev,
					 String ext)
  {
    if (_artifactPattern == null)
      return null;

    HashMap<String,String> map = new HashMap<String,String>();

    if (artifact == null)
      artifact = module;

    map.put("organisation", org);
    map.put("org", org);
    map.put("module", module);
    map.put("artifact", artifact);
    map.put("revision", rev);
    map.put("ext", ext);

    return _artifactPattern.resolve(map);
  }

  protected void validateSignature(DataSource source,
				   String hash,
				   String algorithm)
    throws NoSuchAlgorithmException
  {
    if (hash == null)
      return;

    MessageDigest digest = MessageDigest.getInstance(algorithm);

    InputStream is = source.openInputStream();
    TempBuffer tempBuffer = TempBuffer.allocate();
    try {
      byte []buffer = tempBuffer.getBuffer();

      int len;

      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
	digest.update(buffer, 0, len);
      }

      byte []bytes = digest.digest();

      String hexBytes = Hex.toHex(bytes);

      if (! hash.equals(hexBytes))
	throw new ModuleNotFoundException(L.l("{0} signature for '{1}' does not properly match",
					      algorithm, source.getName()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      TempBuffer.free(tempBuffer);

      try {
	is.close();
      } catch (IOException e) {
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _artifactPattern + "]";
  }
}
