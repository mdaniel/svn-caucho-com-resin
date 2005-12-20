/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.tools.profiler;

import java.util.Collection;

/**
 * Usage of the a ProfilerPoint returned by {@link #createProfilerPoint(String)}
 * guarantees that this ProfilerPoint is an ancestor in the execution stack.
 */
public class CategorizingProfilerPoint
  extends ProfilerPoint
{
  CategorizingProfilerPoint(ProfilerManager profilerManager, String name)
  {
    super(profilerManager, name);
  }

  public ProfilerPoint createProfilerPoint(String name)
  {
    return new ProfilerPointParentSettingDelegate(this, getProfilerManager().getProfilerPoint(name));
  }

  private static final class ProfilerPointParentSettingDelegate
    extends ProfilerPoint
  {
    private final ProfilerPoint _parent;
    private final ProfilerPoint _profilerPoint;

    ProfilerPointParentSettingDelegate(ProfilerPoint parent, ProfilerPoint profilerPoint)
    {
      super(profilerPoint.getProfilerManager(), "ProfilerPointParentSettingDelegate{" + profilerPoint + "}");

      assert parent != null;
      assert profilerPoint != null;

      _parent = parent;
      _profilerPoint = profilerPoint;
    }

    public String getName()
    {
      return _profilerPoint.getName();
    }

    public Profiler start()
    {
      return _profilerPoint.start(_parent);
    }

    public Collection<ProfilerNode> getProfilerNodes()
    {
      return _profilerPoint.getProfilerNodes();
    }

    public ProfilerNode getProfilerNode(ProfilerNode parentNode)
    {
      return _profilerPoint.getProfilerNode(parentNode);
    }

    public void reset()
    {
      _profilerPoint.reset();
    }
  }
}
