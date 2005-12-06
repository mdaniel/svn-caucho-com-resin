/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.program;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;

import com.caucho.quercus.expr.VarInfo;
import com.caucho.quercus.expr.VarState;
import com.caucho.quercus.expr.VarExpr;

/**
 * Information about a function.
 */
public class AnalyzeInfo {
  private final LoopAnalyzeInfo _parentLoop;
  
  private final FunctionInfo _function;

  protected final HashMap<String,VarExpr> _varMap
    = new HashMap<String,VarExpr>();

  public AnalyzeInfo(FunctionInfo function, LoopAnalyzeInfo parentLoop)
  {
    _parentLoop = parentLoop;
    _function = function;
  }

  public AnalyzeInfo(FunctionInfo function)
  {
    _parentLoop = null;
    _function = function;
  }

  /**
   * Returns the function.
   */
  public FunctionInfo getFunction()
  {
    return _function;
  }

  /**
   * Returns the continue info for the containing loop.
   */
  public void mergeLoopContinueInfo()
  {
    if (_parentLoop != null)
      _parentLoop.mergeContinueInfo(this);
  }

  /**
   * Returns the break info for the containing loop.
   */
  public void mergeLoopBreakInfo()
  {
    if (_parentLoop != null)
      _parentLoop.mergeBreakInfo(this);
  }

  /**
   * Returns the matching variable.
   */
  public VarExpr getVar(String name)
  {
    return _varMap.get(name);
  }

  /**
   * Adds the matching variable.
   */
  public void addVar(VarExpr var)
  {
    _varMap.put(var.getName(), var);
  }

  /**
   * Clears a var map.
   */
  public void clear()
  {
    _varMap.clear();
  }

  /**
   * Copies a var map.
   */
  public AnalyzeInfo copy()
  {
    AnalyzeInfo info = new AnalyzeInfo(_function, _parentLoop);

    info._varMap.putAll(_varMap);

    return info;
  }

  /**
   * Creates a loop
   */
  public AnalyzeInfo createLoop(AnalyzeInfo contInfo,
				AnalyzeInfo breakInfo)
  {
    LoopAnalyzeInfo loopInfo = new LoopAnalyzeInfo(_parentLoop,
						   contInfo, breakInfo);

    AnalyzeInfo info = new AnalyzeInfo(_function, loopInfo);

    info._varMap.putAll(_varMap);

    return info;
  }

  /**
   * Merge with a joining var map.
   */
  public void merge(AnalyzeInfo info)
  {
    ArrayList<VarExpr> vars = new ArrayList<VarExpr>(_varMap.values());

    for (VarExpr var : vars) {
      VarExpr mergeVar = info.getVar(var.getName());

      if (mergeVar == null)
	var = var.analyzeVarState(VarState.UNKNOWN);
      else
	var = var.analyzeMerge(mergeVar);

      _varMap.put(var.getName(), var);
    }

    for (VarExpr mergeVar : info._varMap.values()) {
      VarExpr var = getVar(mergeVar.getName());

      if (var == null) {
	mergeVar = mergeVar.analyzeVarState(VarState.UNKNOWN);
	
	_varMap.put(mergeVar.getName(), mergeVar);
      }
    }
  }
}

