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

package com.caucho.v5.server.admin;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.caucho.v5.json.io.JsonReader;
import com.caucho.v5.json.value.JsonArray;
import com.caucho.v5.json.value.JsonValue;
import com.caucho.v5.json.value.JsonValue.ValueType;
import com.caucho.v5.pdf.canvas.CanvasPdf;
import com.caucho.v5.profile.Profile;
import com.caucho.v5.profile.ProfileReport;


/**
 * pdf object oriented API facade
 */
public class ProfilePdf implements ContentAdminPdf
{
  @Override
  public void write(AdminPdf admin)
  {
    ProfileReport report = Profile.create().report();
    
    if (report == null) {
      return;
    }
    
    CanvasPdf canvas = admin.getCanvas();
    canvas.nextPage();
    
    canvas.section("Profile", true);
    
    String json = report.getJson();
    
    canvas.font(CanvasPdf.FontPdf.TEXT);
    canvas.newline();
    
    JsonValue top = (JsonValue) new JsonReader(new StringReader(json)).readObject(JsonValue.class);
    
    // JsonValue keys = top.get("keys");
    JsonValue profileJson = top.get("profile");
    
    if (profileJson == null) {
      return;
    }
    
    long ticks = top.get("ticks").longValue();
    long period = top.get("period").longValue();
    long gcTime = top.get("gc_time").longValue();
    double time = ticks * period;
    
    ArrayList<ProfileEntry> threads = fillProfile(profileJson);
    
    Collections.sort(threads);
    
    canvas.font(10, "Courier");
    canvas.getPage().hsb(20.0/360.0, 0.8, 0.8);
    canvas.text(String.format("Time: %.2fs, ", time * 1e-3));
    canvas.text(String.format("Period: %.1fms, ", (double) period));
    canvas.text(String.format("Ticks: %d, ", ticks));
    canvas.text(String.format("GC-time: %.1fms", (double) gcTime));
    canvas.newline();
    canvas.newline();
    
    canvas.getPage().gray(0);
    
    for (ProfileEntry thread : threads) {
      double timeEntry = thread.getTicks() * period;
      double timeEntryPer = timeEntry / time; 
      
      canvas.font(8, "Courier");
      canvas.textColumn(0, String.format("%7.1f%%", timeEntryPer * 100));
      canvas.textColumn(10, String.format("%.2fs", timeEntry * 1e-3));
      canvas.textColumnRight(20, 10, String.format("%d", thread.getTicks()));
      canvas.textColumn(30, thread.getName());
      canvas.newline();
      
      /*
      canvas.font(8, "Courier");
      for (StackEntry stack : thread.getStack()) {
        canvas.text("   ");
        canvas.text(stack.getClassName());
        canvas.text(".");
        canvas.text(stack.getMethod());
        //canvas.text(" (");
        canvas.newline();
      }
      */
    }

    canvas.newline();
    for (ProfileEntry thread : threads) {
      int width = 20;
      
      double timeEntry = thread.getTicks() * period;
      double timeEntryPer = timeEntry / time; 
      
      canvas.newline();
      canvas.newline();
      canvas.font(8, "Courier");
      canvas.textColumn(0, String.format("%7.1f%%", timeEntryPer * 100));
      canvas.textColumn(10, String.format("%.2fs", timeEntry * 1e-3));
      canvas.textColumn(20, String.format("%d", thread.getTicks()));
      canvas.textColumn(30, thread.getName());
      canvas.newline();
      
      for (StackEntry stack : thread.getStack()) {
        canvas.textColumn(10, stack.getClassName() + "." + stack.getMethod());
        canvas.newline();
      }
    }
    
    // canvas.text(json);
    
    //canvas.text(json);
  }

  private ArrayList<ProfileEntry> fillProfile(JsonValue threadDumpJson)
  {
    ArrayList<ProfileEntry> threads = new ArrayList<>();
    
    for (JsonValue threadJson : threadDumpJson.values()) {
      
      String name = threadJson.get("name").getString();
      long ticks = threadJson.get("ticks").longValue();
      String state = threadJson.get("state").getString();
      
      ProfileEntry thread = new ProfileEntry(name, ticks, state);
      
      JsonValue stack = threadJson.get("stack");
      
      if (stack.getValueType() == ValueType.ARRAY) {
        threadFillStack(thread, (JsonArray) stack);
      }
      
      threads.add(thread);
    }
    
    ArrayList<ProfileEntry> threadPeers = new ArrayList<>();
    
    while (threads.size() > 0) {
      ProfileEntry thread = threads.get(0);
      
      threadPeers.add(thread);
      
      selectPeers(thread, threads);
      
    }
    
    return threadPeers;
  }
  
  private void selectPeers(ProfileEntry thread, 
                           ArrayList<ProfileEntry> threads)
  {
    for (int i = threads.size() - 1; i >= 0; i--) {
      ProfileEntry threadEntry = threads.get(i);
      
      if (thread.equalsStack(threadEntry)) {
        thread.addPeer(threadEntry);
        
        threads.remove(i);
      }
    }
  }
  
  private void threadFillStack(ProfileEntry thread, JsonArray stack)
  {
    for (int i = 0; i < stack.size(); i++) {
      JsonValue entryJson = stack.get(i);
      
      String className = entryJson.get("class").getString();
      String method = entryJson.get("method").getString();
      String file = entryJson.get("file").getString();
      int line = entryJson.get("line").intValue();
      
      StackEntry entry = new StackEntry(className, method, file, line);
      
      thread.addStack(entry);
    }
  }
  
  private class ProfileEntry implements Comparable<ProfileEntry>
  {
    private String _name;
    private long _ticks;
    private String _state;
    
    private ArrayList<StackEntry> _stack = new ArrayList<>();
    private ArrayList<ProfileEntry> _peers = new ArrayList<>(); 
    
    ProfileEntry(String name,
                long ticks,
                String state)
    {
      _name = name;
      _ticks = ticks;
      _state = state;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public long getTicks()
    {
      return _ticks;
    }
    
    public String getState()
    {
      return _state;
    }
    
    void addStack(StackEntry entry)
    {
      _stack.add(entry);
    }
    
    public int compareTo(ProfileEntry entry)
    {
      long cmp = entry._ticks - _ticks;
      
      if (cmp != 0) {
        return Long.signum(cmp);
      }
      
      return _name.compareTo(entry._name);
    }
    
    String getMethodCall()
    {
      int i = 0;
      String prefix = null;
      String methodName = "<unknown>";
      
      for (; i < _stack.size(); i++) { 
        StackEntry entry = _stack.get(i);
        String className = entry.getClassName();
        String method = entry.getMethod();
      
        int p = className.lastIndexOf('.');
      
        if (p > 0) {
          methodName = className.substring(p + 1) + "." + method;
        }
        else {
          methodName = className + "." + method;
        }
        
        if (methodName.equals("Object.wait")) {
          prefix = "wait";
          continue;
        }
        else if (methodName.equals("Unsafe.park")) {
          prefix = "park";
          continue;
        }
        else if (methodName.equals("LockSupport.park")) {
          prefix = "park";
          continue;
        }
        else if (methodName.equals("LockSupport.parkUntil")) {
          prefix = "parkUtil";
          continue;
        }
        else if (methodName.equals("SocketInputStream.socketRead0")) {
          prefix = "socketRead";
          continue;
        }
        else if (methodName.equals("SocketInputStream.read")) {
          prefix = "socketRead";
          continue;
        }
        
        if (prefix != null) {
          return prefix + " (" + methodName + ")";
        }
        else {
          return methodName;
        }
      }
      
      return methodName;
    }
    
    Iterable<StackEntry> getStack()
    {
      return _stack;
    }
    
    void addPeer(ProfileEntry peer)
    {
      _peers.add(peer);
    }
    
    Iterable<ProfileEntry> getPeers()
    {
      return _peers;
    }
    
    boolean equalsStack(ProfileEntry thread)
    {
      if (_stack.size() != thread._stack.size()) {
        return false;
      }
      
      for (int i = 0; i < _stack.size(); i++) {
        StackEntry entryA = _stack.get(i);
        StackEntry entryB = thread._stack.get(i);
        
        if (! entryA.equals(entryB)) {
          return false;
        }
      }
      
      return true;
    }
  }
  
  private class StackEntry {
    private String _className;
    private String _method;
    private String _file;
    private int _line;
    
    StackEntry(String className,
               String method, 
               String file,
               int line)
    {
      _className = className;
      _method = method;
      _file = file;
      _line = line;
    }
    
    public String getClassName()
    {
      return _className;
    }
    
    public String getMethod()
    {
      return _method;
    }
    
    @Override
    public int hashCode()
    {
      return _className.hashCode() * 65521 + _method.hashCode();
    }
    
    public boolean equals(Object o)
    {
      if (! (o instanceof StackEntry)) {
        return false;
      }
      
      StackEntry entry = (StackEntry) o;
      
      if (! _className.equals(entry._className)) {
        return false;
      }
      else {
        return true;
      }
    }
  }
}
