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

/**
 * Gathers statistics for a {@link ProfilerPoint} with a particular parentage.
 */
final public class ProfilerNode {
  private final ProfilerNode _parent;
  private final ProfilerPoint _profilerPoint;
  private long _longHashCode;

  private long _time;
  private long _invocationCount;

  private long _minTime = Long.MAX_VALUE;
  private long _maxTime = Long.MIN_VALUE;

  ProfilerNode(ProfilerNode parent, ProfilerPoint profilerPoint)
  {
    _parent = parent;
    _profilerPoint = profilerPoint;
  }

  public ProfilerNode getParent()
  {
    return _parent;
  }

  public ProfilerPoint getProfilerPoint()
  {
    return _profilerPoint;
  }

  public String getName()
  {
    return _profilerPoint.getName();
  }

  /**
   * Increment the invocation count and add time.
   *
   * @param totalTime
   */
  void update(long totalTime)
  {
    System.out.println("UPDATE: " + _invocationCount + " " + this);
    
    synchronized (this) {
      _invocationCount++;

      if (_invocationCount > 0) {
        _time += totalTime;
      }

      if (totalTime < _minTime)
	_minTime = totalTime;

      if (_maxTime < totalTime)
	_maxTime = totalTime;
    }
  }

  /**
   * Time for this node in nanoseconds, does not include the time for child
   * nodes.
   */
  public long getTime()
  {
    return _time;
  }

  /**
   * Minimum time for this node in nanoseconds, does not include
   * the time for child nodes.
   */
  public long getMinTime()
  {
    return _minTime;
  }

  /**
   * Minimum time for this node in nanoseconds, does not include
   * the time for child nodes.
   */
  public long getMaxTime()
  {
    return _maxTime;
  }

  void incrementInvocationCount()
  {
    synchronized (this) {
      _invocationCount++;
    }
  }

  public long getInvocationCount()
  {
    return _invocationCount;
  }

  public long longHashCode()
  {
    if (_longHashCode == 0) {
      long longHashCode = _parent == null ? 7 : _parent.longHashCode();

      longHashCode = longHashCode * 33 ^ _profilerPoint.longHashCode();

      _longHashCode = longHashCode;
    }

    return _longHashCode;
  }

  public int hashCode()
  {
    return (int) longHashCode();
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;

    // if (!(o instanceof ProfilerNode))  return false;

    ProfilerNode other = (ProfilerNode) o;

    if (other._parent != _parent)
      return false;

    if (other._profilerPoint != _profilerPoint)
      return false;

    return true;
  }

  public String toString()
  {
    StringBuilder builder = new StringBuilder();

    ProfilerNode node = this;

    do {
      if (builder.length() != 0)
        builder.insert(0, " --> ");

      builder.insert(0, node._profilerPoint.getName());

      node = node.getParent();

    } while (node != null);

    builder.insert(0, "ProfilerNode[");
    builder.append(']');

    return builder.toString();
  }
}
