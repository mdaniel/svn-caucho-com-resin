/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */

package com.caucho.portal.generic;

import java.util.*;
import java.io.IOException;
import javax.portlet.PortletRequest;

/**
 * Abstract base class for implementations that load and save preferences.
 */
abstract public class AbstractPreferencesStore 
  implements PreferencesStore
{
  protected static String[] DELETE = new String[] { "<delete>" };

  private LinkedHashMap<String, String[]> _defaultMap;
  private HashMap<String, String> _nameLinkMap;
  private Map<String, String> _reverseNameLinkMap;

  /**
   * Load the user preferences, called once for each connection.
   * Implementing classes use the <i>request</i> to determine the identity
   * of the user.
   *
   * @return a Map<String, String[]> of all the user preferences that are
   * available for the user identified by <i>request</i>, null
   * if no preferences are available.
   */
  abstract protected Map<String, String[]> load( PortletRequest request, 
                                                 String namespace )
    throws IOException;

  /**
   * Called just before unload() if the preferences have been updated.
   * The updateMap contains only entries that have changed.  Entries with a
   * value of DELETE are meant to be deleted from the store. 
   *
   * @param storeMap the map returned by load()
   *
   * @param updateMap the map containing the updates
   *
   * @throws UnsupportedOperationException if the implementation does not
   * support updates to preferences.
   */
  abstract protected void save( Map<String, String[]> storeMap, 
                                Map<String, String[]> updateMap )
    throws IOException;

  /**
   * Called when the connection is complete.
   * 
   * A call to unload() is not guaranteed to occur for every connection,
   * if an error occurs when processing a request unload() may never be called.
   *
   * @param isModified true if the map has been changed
   */
  abstract protected void unload(Map<String, String[]> map);

  public void addDefault(String name, String value)
  {
    if (_defaultMap == null)
      _defaultMap = new LinkedHashMap<String, String[]>();

    String[] values = _defaultMap.get(name);

    if (values == null)
      values = new String[] { value };
    else {
      String[] newValues = new String[values.length + 1];

      int i = 0;
      for (; i < values.length; i++) {
        newValues[i] = values[i];
      }

      newValues[i] = value;
    }

    _defaultMap.put(name, values);
  }

  public void addDefault(NameValuePair nameValuePair)
  {
    addDefault(nameValuePair.getName(), nameValuePair.getValue());
  }

  /**
   * Add a link such that when the portlet uses the preferences <i>name</i>, 
   * the name that is used with the store is <i>link</i>.
   */
  public void addNameLink(String name, String link)
  {
    if (_nameLinkMap == null)
      _nameLinkMap = new HashMap<String, String>();

    _nameLinkMap.put(name, link);
  }

  public void addNameLink(NameLink nameLink)
  {
    addNameLink(nameLink.getName(), nameLink.getLink());
  }

  public Map<String, String[]> getPreferencesMap( PortletRequest request,
                                                  String namespace )
    throws IOException
  {
    Map<String, String[]> storeMap = load(request, namespace);

    StoreUpdateMap<String, String[]> map 
      = new StoreUpdateMap<String, String[]>();
    
    if (_nameLinkMap != null) {
      synchronized (_nameLinkMap) {
        if (_reverseNameLinkMap == null)
          _reverseNameLinkMap 
            = KeyLinkMap.<String>getReverseKeyLinkMap(_nameLinkMap);
      }
    }

    map.start(_nameLinkMap, _reverseNameLinkMap, 
              _defaultMap, storeMap, null, DELETE);

    return map;
  }

  public void finish( Map<String, String[]> preferencesMap )
    throws IOException
  {
    StoreUpdateMap<String, String[]> map 
      = (StoreUpdateMap<String, String[]>) preferencesMap;

    Map<String, String[]> storeMap = map.getStoreMap();
    Map<String, String[]> updateMap = map.getUpdateMap();

    map.finish();

    if (updateMap != null)
      save(storeMap, updateMap);

    unload(storeMap);
  }
}

