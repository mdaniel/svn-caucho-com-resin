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

package com.caucho.env.vote;

import com.caucho.env.service.*;

/**
 * Manages the distributed lock
 */
public class VoteService extends AbstractResinSubSystem 
{
  private AbstractVoteManager _voteManager;
  
  public VoteService(AbstractVoteManager voteManager)
  {
    _voteManager = voteManager;
  }
  
  public static VoteService
      createAndAddService(AbstractVoteManager voteManager)
  {
    ResinSystem system = preCreate(VoteService.class);

    VoteService service = new VoteService(voteManager);
    system.addService(VoteService.class, service);

    return service;
  }
  
  public static VoteService getCurrent()
  {
    return ResinSystem.getCurrentService(VoteService.class);
  }
  
  public AbstractVoteManager getManager()
  {
    return _voteManager;
  }
  
  //
  // lifecycle/
  //
  
  @Override
  public void start()
  {
    // _voteManager.start();
  }
  
  @Override
  public void stop()
  {
    // _voteManager.close();
  }
  
}
