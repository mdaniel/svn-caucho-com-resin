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

package com.caucho.message.journal;

import java.io.IOException;

import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;

/**
 * Actor-processor for writing to the transaction log. The actor is a
 * single-threaded process.
 */
public class JournalWriteActor
  extends AbstractActorProcessor<JournalRingItem>
{
  private final JournalFile _journalFile;
  private String _threadName;
  
  public JournalWriteActor(JournalFile journalFile)
  {
    _journalFile = journalFile;
    
    _threadName = toString();
  }
  
  @Override
  public String getThreadName()
  {
    return _threadName;
  }

  @Override
  public final void process(JournalRingItem entry)
    throws IOException
  {
    if (entry.isData())
      processData(entry);
    else
      processCheckpoint(entry);
  }
  
  private final void processData(JournalRingItem entry)
    throws IOException
  {
    byte []buffer = entry.getBuffer();

    if (buffer == null) {
      //System.out.println("NULLB:" + sequence);
      return;
    }
    
    long code = entry.getCode();
    long xid = entry.getXid();
    long qid = entry.getQid();
    long mid = entry.getMid();
    JournalResult result = entry.getResult();

    _journalFile.write(code, entry.isInit(), entry.isFin(),
                       xid, qid, mid,
                       buffer, entry.getOffset(), entry.getLength(),
                       result);

    entry.freeTempBuffer();
  }
  
  private final void processCheckpoint(JournalRingItem entry)
    throws IOException
  {
    long blockAddr = entry.getBlockAddr();
    int offset = entry.getOffset();
    int length = entry.getLength();
      
    _journalFile.checkpoint(blockAddr, offset, length);
  }

  @Override
  public void onProcessComplete() throws Exception
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _journalFile + "]";
  }
}
