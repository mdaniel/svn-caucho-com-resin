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
import java.util.Map;

import com.caucho.v5.health.action.ReportThreadDump;
import com.caucho.v5.json.io.JsonReader;
import com.caucho.v5.json.value.JsonArray;
import com.caucho.v5.json.value.JsonValue;
import com.caucho.v5.json.value.JsonValue.ValueType;
import com.caucho.v5.pdf.canvas.CanvasPdf;


/**
 * pdf object oriented API facade
 */
public class ThreadDumpPdf implements ContentAdminPdf
{
  @Override
  public void write(AdminPdf admin)
  {
    CanvasPdf canvas = admin.getCanvas();
    
    canvas.nextPage();
    canvas.section("Thread Dump", true);
    
    ReportThreadDump report = new ReportThreadDump();
    
    String json = report.executeJson();
    
    JsonValue top = (JsonValue) new JsonReader(new StringReader(json)).readObject(JsonValue.class);
    
    canvas.font(CanvasPdf.FontPdf.TEXT);
    canvas.newline();
    
    JsonValue keys = top.get("keys");
    JsonValue threadDumpJson = top.get("thread_dump");
    
    if (threadDumpJson == null) {
      return;
    }
    
    ArrayList<ThreadEntry> threads = fillThreadDump(threadDumpJson);
    
    for (ThreadEntry thread : threads) {
      int width = 20;
      
      canvas.newline();
      for (ThreadEntry peer : thread.getPeers()) {
        canvas.font(10, "Courier");
        canvas.textColumn(0, peer.getName());
        canvas.textColumn(20, peer.getMethodCall());
        canvas.textColumn(70, peer.getState());
        canvas.newline();
      }
      
      canvas.font(8, "Courier");
      for (StackEntry stack : thread.getStack()) {
        canvas.text("   ");
        canvas.text(stack.getClassName());
        canvas.text(".");
        canvas.text(stack.getMethod());
        //canvas.text(" (");
        canvas.newline();
      }
    }
    
    //canvas.text(json);
  }

  private ArrayList<ThreadEntry> fillThreadDump(JsonValue threadDumpJson)
  {
    ArrayList<ThreadEntry> threads = new ArrayList<>();
    
    for (Map.Entry<String,JsonValue> threadEntry : threadDumpJson.entrySet()) {
      JsonValue threadJson = threadEntry.getValue();
      
      long id = threadJson.get("id").longValue();
      String name = threadJson.get("name").getString();
      String state = threadJson.get("state").getString();
      boolean isNative = threadJson.get("native").booleanValue();
      
      ThreadEntry thread = new ThreadEntry(id, name, state);
      
      JsonValue stack = threadJson.get("stack");
      
      if (stack.getValueType() == ValueType.ARRAY) {
        threadFillStack(thread, (JsonArray) stack);
      }
      
      threads.add(thread);
    }
    
    ArrayList<ThreadEntry> threadPeers = new ArrayList<>();
    
    while (threads.size() > 0) {
      ThreadEntry thread = threads.get(0);
      
      threadPeers.add(thread);
      
      selectPeers(thread, threads);
      
    }
    
    return threadPeers;
  }
  
  private void selectPeers(ThreadEntry thread, 
                           ArrayList<ThreadEntry> threads)
  {
    for (int i = threads.size() - 1; i >= 0; i--) {
      ThreadEntry threadEntry = threads.get(i);
      
      if (thread.equalsStack(threadEntry)) {
        thread.addPeer(threadEntry);
        
        threads.remove(i);
      }
    }
  }
  
  private void threadFillStack(ThreadEntry thread, JsonArray stack)
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
  
  private class ThreadEntry {
    private long _id;
    private String _name;
    private String _state;
    
    private ArrayList<StackEntry> _stack = new ArrayList<>();
    private ArrayList<ThreadEntry> _peers = new ArrayList<>(); 
    
    ThreadEntry(long id, 
                String name, 
                String state)
    {
      _id = id;
      _name = name;
      _state = state;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public String getState()
    {
      return _state;
    }
    
    void addStack(StackEntry entry)
    {
      _stack.add(entry);
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
    
    void addPeer(ThreadEntry peer)
    {
      _peers.add(peer);
    }
    
    Iterable<ThreadEntry> getPeers()
    {
      return _peers;
    }
    
    boolean equalsStack(ThreadEntry thread)
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
