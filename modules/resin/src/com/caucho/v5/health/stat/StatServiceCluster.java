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
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.util.ArrayList;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;

/**
 * Service for handling the distributed cache
 */
public class StatServiceCluster
  implements AlarmListener
{
  private StatServiceLocalImpl _statService;

  private ArrayList<AddSampleMetadata> _metadataRetryList
    = new ArrayList<>();

  private Alarm _alarm = new Alarm(this);

  // private ClusterStatServiceApi _triadAll;

  //private TriadShard<ClusterStatServiceApi> _triadFirst;
  
  StatServiceCluster(StatServiceLocalImpl statService)
  {
    _statService = statService;
    
    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    
    rampManager.newService(this).address("public:///stat").ref();
    
    ServiceManagerAmp champManager = AmpSystem.getCurrentManager();
    
    /* XXX:
    _triadAll = champManager.createTriadAllRemoteProxy("/stat", ClusterStatServiceApi.class);
    _triadFirst = champManager.triadFirstRemote("/stat", 1000, 
                                                ClusterStatServiceApi.class);
                                                */
  }

  //
  // methods used by StatServiceImpl
  //

  void sendSampleMetadata(long id, String name)
  {
    // _triadAll.addSampleMetadata(id, name);
  }

  void sendSample(long now, long []ids, double []values)
  {
    long deltaTime = CurrentTime.getCurrentTime() - now;
    
    //_triadFirst.get(TriadOwner.A_B).addSample(deltaTime, ids, values);
  }

  //
  // messages
  //

  public void addSampleMetadata(long id, String name)
  {
    _statService.addSampleDatabaseMetadata(id, name);
  }

  /*
  @MessageError
  public void sampleMetadataError(String to, String from,
                                  AddSampleMetadata msg,
                                  BamError error)
  {
    synchronized (_metadataRetryList) {
      if (! _metadataRetryList.contains(msg)) {
        _metadataRetryList.add(msg);
        _alarm.queue(600 * 1000);
      }
    }
  }
  */

  /**
   * Message handler to add a new sample from a remote machine.
   */
  public void addSample(long deltaTime, long []ids, double []values)
  {
    long now = CurrentTime.getCurrentTime() + deltaTime;
    
    _statService.addSampleDatabase(now, ids, values);
  }

  @Override
  public void handleAlarm(Alarm alarm)
  {
    ArrayList<AddSampleMetadata> list = new ArrayList<AddSampleMetadata>();
    
    synchronized (_metadataRetryList) {
      list.addAll(_metadataRetryList);
      _metadataRetryList.clear();
    }

    for (AddSampleMetadata msg : list) {
      sendSampleMetadata(msg.getId(), msg.getName());
    }
  }
}
