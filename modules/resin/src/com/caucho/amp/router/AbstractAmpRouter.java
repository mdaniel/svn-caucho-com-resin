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

package com.caucho.amp.router;

import java.util.logging.Logger;

import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpHeaders;

/**
 * AmpRouter routes messages to mailboxes.
 */
public class AbstractAmpRouter implements AmpBroker
{
  private static final Logger log
    = Logger.getLogger(AbstractAmpRouter.class.getName());
  
  @Override
  public void send(String to, 
                   String from, 
                   AmpHeaders headers,
                   AmpEncoder encoder, 
                   String methodName, 
                   Object... args)
  {
    AmpMailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.send(to, from, headers, encoder, methodName, args);
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.stream.AmpStream#error(java.lang.String, java.lang.String, com.caucho.amp.stream.AmpHeaders, com.caucho.amp.stream.AmpEncoder, com.caucho.amp.stream.AmpError)
   */
  @Override
  public void error(String to, String from, AmpHeaders headers,
                    AmpEncoder encoder, AmpError error)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void query(long id, 
                    String to, 
                    String from, 
                    AmpHeaders headers,
                    AmpEncoder encoder, 
                    String methodName, 
                    Object... args)
  {
    AmpMailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.query(id, to, from, headers, encoder, methodName, args);
    }
    else {
      queryError(id, from, to, headers, encoder, new AmpError());
    }
  }

  @Override
  public void queryResult(long id, String to, String from, AmpHeaders headers,
                          AmpEncoder encoder, Object result)
  {
    AmpMailbox mailbox = getMailbox(to);
    
    if (mailbox != null) {
      mailbox.queryResult(id, to, from, headers, encoder, result);
    }
    else {
      log.warning(this + " queryResult to=" + to + " is an unknown mailbox");
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.stream.AmpStream#queryError(long, java.lang.String, java.lang.String, com.caucho.amp.stream.AmpHeaders, com.caucho.amp.stream.AmpEncoder, com.caucho.amp.stream.AmpError)
   */
  @Override
  public void queryError(long id, String to, String from, AmpHeaders headers,
                         AmpEncoder encoder, AmpError error)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.router.AmpRouter#getRouterMailbox()
   */
  @Override
  public AmpMailbox getRouterMailbox()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.router.AmpRouter#getMailbox(java.lang.String)
   */
  @Override
  public AmpMailbox getMailbox(String address)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.router.AmpRouter#addMailbox(com.caucho.amp.mailbox.AmpMailbox)
   */
  @Override
  public void addMailbox(AmpMailbox mailbox)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.router.AmpRouter#removeMailbox(com.caucho.amp.mailbox.AmpMailbox)
   */
  @Override
  public void removeMailbox(AmpMailbox mailbox)
  {
    // TODO Auto-generated method stub
    
  }
  @Override
  public String getAddress()
  {
    return null;
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }


  /* (non-Javadoc)
   * @see com.caucho.amp.router.AmpRouter#close()
   */
  @Override
  public void close()
  {
    // TODO Auto-generated method stub
    
  }
}
