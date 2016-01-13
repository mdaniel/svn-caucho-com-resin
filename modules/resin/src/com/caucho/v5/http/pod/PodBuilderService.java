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

package com.caucho.v5.http.pod;

import io.baratine.service.Result;

import java.util.List;

import com.caucho.v5.baratine.ServiceServer.PodBuilder;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.deploy.DeployHandle;


/**
 * The pod deployment service manages the deployed pods for the current server.
 */
public interface PodBuilderService
{
  public static final String ADDRESS = "/pods-service";
  public static final String CONFIG_DIR = "/config/pods";
  
  void buildPod(PodBuilder podBuilder, Result<Void> result);

  void findPod(String name, Result<PodBartender> result);

  void onPodAppStart(String id, String tag);
  void onPodAppStop(String id, String tag);
  
  void getActiveHandles(Result<List<DeployHandle<?>>> result);
}
