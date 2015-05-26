/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.server.deploy;

import io.baratine.core.Result;

import java.util.Map;

import com.caucho.vfs.StreamSource;

public interface DeployService
{
  public String[] getCommitList(String[] commitList);

  public void sendFile(String sha1, StreamSource source,
                       Result<Boolean> callback);
  
  public StreamSource getFile(String tagName, String fileName);

  public String[] listFiles(String tagName, String fileName);
  
  //
  // tag methods
  //

  public boolean putTag(String tag, String contentHash,
                        Map<String, String> attributeCopy);

  public Boolean copyTag(String targetId, String sourceId,
                         Map<String, String> attributes);

  public DeployTagResult[] queryTags(String pattern);
  
  public boolean removeTag(String tag, Map<String, String> attributes);

  public DeployTagStateQuery getTagState(String tag);

  
  //
  // start/restart
  //

  public DeployControllerState start(String tag);
  
  public DeployControllerState restart(String tag);
  
  public DeployControllerState stop(String tag);
  
  void controllerRestart(String tag, Result<DeployControllerState> cb);
  /*
  public void
  restartCluster(String tag, ReplyCallback<ControllerStateActionQueryReply> cb);
*/
  public DeployControllerState restartCluster(String tag);
}
