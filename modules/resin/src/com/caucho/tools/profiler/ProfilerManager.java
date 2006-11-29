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

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.LruCache;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

/**
 * The main entry point for profiling.  This class is used to obtain instances
 * of {@link ProfilerPoint}, which are then used during execution of code to
 * demarcate the code to be profiled.
 * <p/>
 * A {@link ProfilerManager} for the current {@link ClassLoader} is obtained
 * with {@link #getLocal()}.
 */
public class ProfilerManager {
  private static final EnvironmentLocal<ProfilerManager> _local = new EnvironmentLocal<ProfilerManager>();

  private final LruCache<String, ProfilerPoint> _profilerPointMap
    = new LruCache<String, ProfilerPoint>(1024);

  private boolean _isEnabled = false;

  public static ProfilerManager getLocal()
  {
    synchronized (_local) {
      ProfilerManager local = _local.get();

      if (local == null) {
        local = new ProfilerManager();
        _local.set(local);
      }

      return local;
    }
  }

  private ProfilerManager()
  {
    new ProfilerAdmin(this);
  }

  /**
   * Set to true to enable profiling, default false.
   */
  public void setEnabled(boolean isEnabled)
  {
    _isEnabled = isEnabled;
  }

  public boolean isEnabled()
  {
    return _isEnabled;
  }

  public void enable()
  {
    if (!_isEnabled) {
      reset();
      _isEnabled = true;
    }
  }

  public void disable()
  {
    _isEnabled = false;
  }

  public ProfilerPoint getProfilerPoint(String name)
  {
    synchronized (_profilerPointMap) {
      ProfilerPoint profilerPoint = _profilerPointMap.get(name);

      if (profilerPoint == null) {
        profilerPoint = new ProfilerPoint(this, name);
        _profilerPointMap.put(name, profilerPoint);
      }

      return profilerPoint;
    }
  }

  public CategorizingProfilerPoint getCategorizingProfilerPoint(String name)
  {
    synchronized (_profilerPointMap) {
      ProfilerPoint profilerPoint = _profilerPointMap.get(name);

      if (profilerPoint == null) {
        profilerPoint = new CategorizingProfilerPoint(this, name);
        _profilerPointMap.put(name, profilerPoint);
      }

      return (CategorizingProfilerPoint) profilerPoint;
    }
  }

  public Set<ProfilerPoint> getAllProfilerPoints()
  {
    synchronized (_profilerPointMap) {
      TreeSet<ProfilerPoint> allProfilerPoints = new TreeSet<ProfilerPoint>();

      Iterator<ProfilerPoint> existingValues = _profilerPointMap.values();

      while (existingValues.hasNext())
        allProfilerPoints.add(existingValues.next());

      return allProfilerPoints;
    }
  }

  /**
   * Returns a copy of all of the ProfilerNodes.
   */
  public Collection<ProfilerNode> getAllProfilerNodes()
  {
    LinkedList<ProfilerNode> profilerNodes = new LinkedList<ProfilerNode>();

    fillAllProfilerNodes(profilerNodes);

    return profilerNodes;
  }

  /**
   * Returns a copy of all of the ProfilerNodes, sorted with the given {@link
   * Comparator}.
   */
  public Collection<ProfilerNode> getAllProfilerNodes(Comparator<ProfilerNode> comparator)
  {
    TreeSet<ProfilerNode> profilerNodes = new TreeSet<ProfilerNode>(comparator);

    fillAllProfilerNodes(profilerNodes);

    return profilerNodes;
  }

  /**
   * Returns a copy of all of the ProfilerNodes that are children of the
   * parent.
   *
   * @param parent the parent, null to retrieve top-lvel nodes
   */
  public Collection<ProfilerNode> getChildProfilerNodes(ProfilerNode parent)
  {
    LinkedList<ProfilerNode> profilerNodes = new LinkedList<ProfilerNode>();

    fillChildProfilerNodes(parent, profilerNodes);

    return profilerNodes;
  }

  /**
   * Returns a copy of all of the ProfilerNodes that are children of the parent,
   * sorted with the given {@link Comparator}..
   *
   * @param parent the parent, null to retrieve top-lvel nodes
   */
  public Collection<ProfilerNode> getChildProfilerNodes(ProfilerNode parent,
                                                        Comparator<ProfilerNode> comparator)
  {
    TreeSet<ProfilerNode> profilerNodes = new TreeSet<ProfilerNode>(comparator);

    fillChildProfilerNodes(parent, profilerNodes);

    return profilerNodes;
  }

  private void fillAllProfilerNodes(Collection<ProfilerNode> profilerNodes)
  {
    synchronized (_profilerPointMap) {
      Iterator<ProfilerPoint> iter = _profilerPointMap.values();

      while (iter.hasNext()) {
        ProfilerPoint profilerPoint = iter.next();

        synchronized (profilerPoint) {
          profilerNodes.addAll(profilerPoint.getProfilerNodes());
        }
      }
    }
  }

  private void fillChildProfilerNodes(ProfilerNode parent,
                                      Collection<ProfilerNode> profilerNodes)
  {
    synchronized (_profilerPointMap) {
      Iterator<ProfilerPoint> iter = _profilerPointMap.values();

      while (iter.hasNext()) {
        ProfilerPoint profilerPoint = iter.next();

        synchronized (profilerPoint) {
          for (ProfilerNode profilerNode : profilerPoint.getProfilerNodes()) {
            if (profilerNode.getParent() == parent)
              profilerNodes.add(profilerNode);
          }
        }
      }
    }
  }

  /**
   * Clear all profiling information.
   */
  public void reset()
  {
    synchronized (_profilerPointMap) {
      Iterator<ProfilerPoint> iter = _profilerPointMap.values();

      while (iter.hasNext()) {
        ProfilerPoint profilerPoint = iter.next();

        profilerPoint.reset();
      }
    }
  }

  public String toString()
  {
    return "ProfilerManager[" + getClass().getClassLoader() + "]";
  }

}
