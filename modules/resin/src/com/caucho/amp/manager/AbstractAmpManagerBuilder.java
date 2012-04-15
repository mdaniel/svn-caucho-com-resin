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

package com.caucho.amp.manager;

import com.caucho.amp.AmpManager;
import com.caucho.amp.AmpManagerBuilder;
import com.caucho.amp.broker.AmpBrokerFactory;
import com.caucho.amp.mailbox.AmpMailboxBuilderFactory;

/**
 * Factory for creating an AMP manager.
 */
abstract public class AbstractAmpManagerBuilder implements AmpManagerBuilder
{
  private AmpBrokerFactory _brokerFactory;
  private AmpMailboxBuilderFactory _mailboxBuilderFactory;

  @Override
  public AmpBrokerFactory getBrokerFactory()
  {
    return _brokerFactory;
  }

  @Override
  public AmpManagerBuilder setBrokerFactory(AmpBrokerFactory factory)
  {
    _brokerFactory = factory;

    return this;
  }

  @Override
  public AmpMailboxBuilderFactory getMailboxBuilderFactory()
  {
    return _mailboxBuilderFactory;
  }

  @Override
  public AmpManagerBuilder setMailboxBuilderFactory(AmpMailboxBuilderFactory factory)
  {
    _mailboxBuilderFactory = factory;

    return this;
  }

  @Override
  abstract public AmpManager create();
}
