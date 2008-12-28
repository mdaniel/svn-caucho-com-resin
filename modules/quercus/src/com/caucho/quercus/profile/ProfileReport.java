/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.profile;

import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

/**
 * Report of profile entries
 */
public class ProfileReport
{
  private String _url;
  private long _time;

  private ArrayList<ProfileItem> _itemList = new ArrayList<ProfileItem>();

  private HashMap<String,ProfileMethod> _methodMap
    = new HashMap<String,ProfileMethod>();

  public ProfileReport(String url, long time)
  {
    _url = url;
    _time = time;
  }

  /**
   * Returns the list of profile items.
   */
  public ArrayList<ProfileItem> getItemList()
  {
    return _itemList;
  }

  /**
   * Adds a profile item.
   */
  public void addItem(String name, String parent, long count, long micros)
  {
    ProfileItem item = new ProfileItem(name, parent, count, micros);

    _itemList.add(item);

    ProfileMethod method = getMethod(name);
    method.addParent(item);

    if (! "__undefined__".equals(parent)) {
      ProfileMethod parentMethod = getMethod(parent);
      parentMethod.addChild(item);
    }
  }

  /**
   * Printable flat report
   */
  public void printHotSpotReport(WriteStream out)
    throws IOException
  {
    ArrayList<ProfileMethod> methodList
      = new ArrayList<ProfileMethod>(_methodMap.values());

    Collections.sort(methodList, new SelfMicrosComparator());

    double totalMicros = 0;
    int maxNameLength = 0;

    for (ProfileMethod method : methodList) {
      int len = method.getName().length();

      if (maxNameLength < len)
	maxNameLength = len;
      
      totalMicros += method.getSelfMicros();
    }

    out.println();
    out.println("Hot Spot Profile: " + _url + " at " + new Date(_time));
    out.println();
    out.println(" self(us)  total(us)  count   %time     %sum   name");
    out.println("----------------------------------------------------");

    double sumMicros = 0;

    for (ProfileMethod method : methodList) {
      String name = method.getName();
      long selfMicros = method.getSelfMicros();
      sumMicros += selfMicros;

      out.print(String.format("%7dus", selfMicros));
      out.print(String.format(" %8dus", method.getTotalMicros()));
      out.print(String.format(" %6d", method.getCount()));
      out.print(String.format(" %6.2f%%", 100.0 * selfMicros / totalMicros));
      out.print(String.format("  %6.2f%%", 100.0 * sumMicros / totalMicros));
      out.print("   " + name);

      out.println();
    }

    out.println();
  }

  /**
   * Printable hierarchy report
   */
  public void printHierarchyReport(WriteStream out)
    throws IOException
  {
    ArrayList<ProfileMethod> methodList
      = new ArrayList<ProfileMethod>(_methodMap.values());

    Collections.sort(methodList, new TotalMicrosComparator());

    double totalMicros = methodList.get(0).getTotalMicros();
    int maxNameLength = 0;

    out.println();
    out.println("Hierarchy: " + _url + " at " + new Date(_time));
    out.println();
    out.println(" total(us)  self(us)  count   %time     %sum   name");
    out.println("----------------------------------------------------");

    double sumMicros = 0;

    for (ProfileMethod method : methodList) {
      String name = method.getName();
      long ownTotalMicros = method.getTotalMicros();
      long selfMicros = method.getSelfMicros();
      sumMicros += selfMicros;

      out.println();

      ArrayList<ProfileItem> parentList
	= new ArrayList<ProfileItem>(method.getParentItems());

      for (ProfileItem item : parentList) {
	out.print("        ");
	out.print(String.format(" %7dus", item.getMicros()));
	out.print(String.format(" %6d", item.getCount()));
	
	out.print(String.format("     %-19s", item.getParent()));
	out.print(String.format("%6.2f%%",
				100.0 * item.getMicros() / ownTotalMicros));
	out.println();
      }
      
      out.print(String.format(" %6.2f%%", 100.0 * ownTotalMicros / totalMicros));
      out.print(String.format(" %7dus", method.getTotalMicros()));
      out.print(String.format(" %6d", method.getCount()));
      out.print(String.format("  %-22s", name));
      out.print(String.format("%6.2f%%",
			      100.0 * selfMicros / ownTotalMicros));
      out.print(String.format(" %7dus", method.getSelfMicros()));
      out.println();

      ArrayList<ProfileItem> childList
	= new ArrayList<ProfileItem>(method.getChildItems());

      for (ProfileItem item : childList) {
	out.print("        ");
	out.print(String.format(" %7dus", item.getMicros()));
	out.print(String.format(" %6d", item.getCount()));
	
	out.print(String.format("     %-19s", item.getName()));
	out.print(String.format("%6.2f%%",
				100.0 * item.getMicros() / ownTotalMicros));
	out.println();
      }
    }

    out.println();
  }

  /**
   * Returns the ProfileMethod for the given method name
   */
  protected ProfileMethod getMethod(String name)
  {
    ProfileMethod method = _methodMap.get(name);

    if (method == null) {
      method = new ProfileMethod(name);
      _methodMap.put(name, method);
    }

    return method;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class SelfMicrosComparator implements Comparator<ProfileMethod> {
    public int compare(ProfileMethod a, ProfileMethod b)
    {
      long delta = b.getSelfMicros() - a.getSelfMicros();

      if (delta == 0)
	return 0;
      else if (delta < 0)
	return -1;
      else
	return 1;
    }
  }

  static class TotalMicrosComparator implements Comparator<ProfileMethod> {
    public int compare(ProfileMethod a, ProfileMethod b)
    {
      long delta = b.getTotalMicros() - a.getTotalMicros();

      if (delta == 0)
	return 0;
      else if (delta < 0)
	return -1;
      else
	return 1;
    }
  }
}

