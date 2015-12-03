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

package com.google.appengine.api.files;

/**
 * Factory for creating file services.
 */
public class GSFileOptions
{
  private final String _acl;
  private final String _bucket;
  private final String _key;
  private final String _mimeType;
  
  private GSFileOptions(GSFileOptionsBuilder builder)
  {
    _bucket = builder._bucket;
    _mimeType = builder._mimeType;
    _acl = builder._acl;
    _key = builder._key;
  }
  
  public String testGetAcl()
  {
    return _acl;
  }
  
  public String testGetBucket()
  {
    return _bucket;
  }
  
  public String testGetKey()
  {
    return _key;
  }
  
  public String testGetMimeType()
  {
    return _mimeType;
  }
  
  public static class GSFileOptionsBuilder {
    private String _acl;
    private String _bucket;
    private String _key;
    private String _mimeType;
    
    public GSFileOptionsBuilder setAcl(String acl)
    {
      _acl = acl;
      
      return this;
    }
    
    public GSFileOptionsBuilder setKey(String key)
    {
      _key = key;
      
      return this;
    }
    
    public GSFileOptionsBuilder setBucket(String bucket)
    {
      _bucket = bucket;
      
      return this;
    }
    
    public GSFileOptionsBuilder setMimeType(String mimeType)
    {
      _mimeType = mimeType;
      
      return this;
    }
    
    public GSFileOptions build()
    {
      return new GSFileOptions(this);
    }
  }
}
