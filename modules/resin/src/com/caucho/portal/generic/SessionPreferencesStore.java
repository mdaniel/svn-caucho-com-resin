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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SessionPreferencesFactory creates or returns an existing store that is in
 * the users's session.
 *
 * Each namespace is assigned an attribute name to use in the session,
 * the attribute value is an instance of {@link SessionPreferences}.
 */
public class SessionPreferencesStore 
  implements PreferencesStore 
{
  /** Attribute name to use in session */
  private final static String SESSION_PREFERENCES
    = "com.caucho.portal.generic.SessionPreferences";

  private boolean _alwaysCreateSession = true;

  /**
   * If true then a session is always created for the user if there is not one
   * already, default is true.
   */
  public void setAlwaysCreateSession(boolean alwaysCreateSession)
  {
    _alwaysCreateSession = alwaysCreateSession;
  }

  public Map<String, String[]> getPreferencesMap( PortletRequest request,
                                                  String namespace )
  {
    SessionPreferences pref = null;

    PortletSession session = request.getPortletSession(_alwaysCreateSession);

    if (session != null) {
      // If the user has a session, create or reuse a
      // SessionPreferences that is stored in the session.

      String attributeName = SESSION_PREFERENCES;

      if (namespace.length() > 0) {
        int len = SESSION_PREFERENCES.length() + namespace.length() + 2;
        StringBuffer buf = new StringBuffer(len);
        buf.append(SESSION_PREFERENCES);
        buf.append('.');
        buf.append(namespace);
        attributeName = buf.toString();
      }

      synchronized (session) {
        pref = (SessionPreferences) session.getAttribute(attributeName);

        if (pref == null) {
          pref = new SessionPreferences();
          session.setAttribute(attributeName, pref);
        }
      }
    }

    return pref;
  }

  public void finish(Map<String, String[]> preferences)
  {
    SessionPreferences pref = (SessionPreferences) preferences;
    pref.updateSessionIfNeeded();
  }
}

