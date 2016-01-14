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

import com.caucho.v5.deploy2.DeployHandle2;
import com.caucho.v5.vfs.PathImpl;


/**
 * Deploy pod-node controller.
 * 
 * Manages the activation of a pod node.
 */
class DeployNodeLoader extends DeployNodePod<PodLoader>
{
  DeployNodeLoader(String id,
                      PodDeploy pod,
                      DeployHandle2<PodLoader> handle)
  {
    super(id, pod, handle);
  }
  
  @Override
  public boolean isPodNodeActive()
  {
    return true;
  }

  /*
  @Override
  protected PodLoaderController createController(String id, PathImpl root)
  {
    return new PodLoaderController(id, root, getPod(), getPodContainer());
  }
  */
}
