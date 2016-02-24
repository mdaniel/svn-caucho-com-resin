/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.v5.http.webapp;

import java.util.logging.Logger;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

public class WebAppFragmentConfig extends WebAppConfig
{
  //implements XmlSchemaBean {

  private static final L10N L = new L10N(WebAppResinBase.class);
  private static final Logger log
    = Logger.getLogger(WebAppFragmentConfig.class.getName());

  //web-fragment name
  private String _name;

  //web-fragment metadata-complete
  private boolean _isMetadataComplete;

  //web-fragment jar
  private String _jarPath;

  private Ordering _ordering;
  private PathImpl _rootPath;

  public String getName()
  {
    return _name;
  }

  @Configurable
  public void setName(NameConfig nameConfig)
  {
    _name = nameConfig.getValue();
  }

  public boolean isMetadataComplete()
  {
    return _isMetadataComplete;
  }

  @Configurable
  public void setMetadataComplete(boolean metadataComplete)
  {
    _isMetadataComplete = metadataComplete;
  }

  // XXX: this will make tck tests with misspelled metadata-complete deploy.
  // tck test generally seems valid except for this problem
  @Configurable
  public void setMetaDataComplete(boolean metadataComplete)
  {
    _isMetadataComplete = metadataComplete;
  }

  public String getJarPath()
  {
    return _jarPath;
  }

  public void setJarPath(String jarPath)
  {
    _jarPath = jarPath;
  }

  public Ordering createOrdering()
  {
    if (_ordering != null)
      throw new IllegalStateException();

    _ordering = new Ordering();

    return _ordering;
  }

  public Ordering getOrdering()
  {
    return _ordering;
  }

  public Ordering createAbsoluteOrdering()
  {
    log.finer(L.l("'{0}' absolute-ordering tag should not be used inside web application descriptor.", this));

    return new Ordering();
  }

  /*
  @Override
  public String getSchema()
  {
    return "com/caucho/http/webapp/resin-web-xml.rnc";
  }
  */

  public void setRootPath(PathImpl rootPath)
  {
    _rootPath = rootPath;
  }

  public PathImpl getRootPath()
  {
    return _rootPath;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
  
  public static class NameConfig {
    private String _name;
    
    public void setId(String id)
    {
    }
    
    public void setValue(String value)
    {
      _name = value;
    }
    
    public String getValue()
    {
      return _name;
    }
  }
}
