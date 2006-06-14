/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package javax.mail;

import java.net.InetAddress;

/**
 * Represents a mail authenticator.
 */
public abstract class Authenticator {

  private InetAddress _requestingSite = null;
  private int         _requestingPort = 1;
  private String      _requestingProtocol = null;
  private String      _requestingPrompt = null;
  private String      _defaultUserName = null;

  public Authenticator()
  {
  }

  /**
   * Returns the requesting site.
   */
  protected final InetAddress getRequestingSite()
  {
    return _requestingSite;
  }

  /**
   * Returns the requested connectin port.
   */
  protected final int getRequestingPort()
  {
    return _requestingPort;
  }

  /**
   * Returns the protocol.
   */
  protected final String getRequestingProtocol()
  {
    return _requestingProtocol;
  }

  /**
   * Returns the prompt string.
   */
  protected final String getRequestingPrompt()
  {
    return _requestingPrompt;
  }

  /**
   * Returns the default user name.
   */
  protected final String getDefaultUserName()
  {
    return _defaultUserName;
  }
  
  /**
   * Override for the passwor dauth.
   */
  protected PasswordAuthentication getPasswordAuthentication()
  {
    return null;
  }
}
