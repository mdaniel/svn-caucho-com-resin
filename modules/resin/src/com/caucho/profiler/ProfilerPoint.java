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


package com.caucho.profiler;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Represents a unique point at which profiling is performed. Obtained from a
 * {@link ProfilerManager}. Equality between two instances of ProfilerPoint is
 * based on the name only.
 */
public class ProfilerPoint
  implements Comparable<ProfilerPoint>
{
  private static final Profiler NOOP_PROFILER = new Profiler() {
    public void finish()
    {
    }

    public String toString()
    {
      return "NoopProfiler[]";
    }
  };

  private final ProfilerManager _profilerManager;
  private final String _name;

  private long _longHashCode;

  private Map<ProfilerNode, ProfilerNode> _childProfilerNodesMap;

  ProfilerPoint(ProfilerManager profilerManager, String name)
  {
    assert profilerManager != null;
    assert name != null;

    _profilerManager = profilerManager;
    _name = name;
  }

  protected ProfilerManager getProfilerManager()
  {
    return _profilerManager;
  }

  public String getName()
  {
    return _name;
  }

  public ProfilerPoint createProfilerPoint(String name)
  {
    return getProfilerManager().getProfilerPoint(name);
  }

  public Profiler start()
  {
    if (!_profilerManager.isEnabled())
      return NOOP_PROFILER;

    ThreadProfiler profiler = ThreadProfiler.current();

    profiler.start(this);

    return profiler;
  }

  protected Profiler start(ProfilerPoint parent)
  {
    if (!getProfilerManager().isEnabled())
      return NOOP_PROFILER;

    ThreadProfiler profiler = ThreadProfiler.current();

    profiler.start(parent, this);

    return profiler;
  }

  /**
   * Caller must synchronize on this ProfilerPoint while it uses the returned
   * map.
   */
  Collection<ProfilerNode> getProfilerNodes()
  {
    if (_childProfilerNodesMap == null)
      return Collections.emptyList();
    else
      return _childProfilerNodesMap.values();
  }

  ProfilerNode getProfilerNode(ProfilerNode parentNode)
  {
    synchronized (this) {
      ProfilerNode node;

      if (_childProfilerNodesMap == null) {
        _childProfilerNodesMap = new IdentityHashMap<ProfilerNode, ProfilerNode>();
        node = null;
      }
      else
        node = _childProfilerNodesMap.get(parentNode);

      if (node == null) {
        node = new ProfilerNode(parentNode, this);

        _childProfilerNodesMap.put(parentNode, node);
      }

      return node;
    }
  }

  /**
   * Drop all of the ProfilerNode's.
   */
  void reset()
  {
    synchronized (this) {
      if (_childProfilerNodesMap != null)
        _childProfilerNodesMap.clear();
    }
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;

    if (o == null)
      return false;

    if (!(o instanceof ProfilerPoint))
      return false;

    final ProfilerPoint other = (ProfilerPoint) o;

    if (longHashCode() != other.longHashCode())
      return false;

    return getName().equals(other.getName());
  }

  public int compareTo(ProfilerPoint other)
  {
    return getName().compareTo(other.getName());
  }

  public long longHashCode()
  {
    if (_longHashCode == 0) {
      long longHashCode = 7;

      final int len = _name.length();

      for (int i = 0; i < len; i++)
        longHashCode = longHashCode * 33 ^ _name.charAt(i);

      _longHashCode = longHashCode;
    }

    return _longHashCode;
  }

  public int hashCode()
  {
    return (int) longHashCode();
  }

  public String toString()
  {
    return "ProfilerPoint[" + getName() + "]";
  }
}
