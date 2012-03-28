/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.gen;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.expr.VarExprPro;
import com.caucho.quercus.expr.VarState;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.FunctionInfo;
import com.caucho.quercus.program.QuercusProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Information about a function.
 */
public class AnalyzeInfo {
  private final QuercusProgram _program;
  private final FunctionInfo _function;

  private final LoopAnalyzeInfo _parentLoop;
  
  protected final HashMap<StringValue,VarExprPro> _varMap
    = new HashMap<StringValue,VarExprPro>();

  private boolean _isInitialBlock;

  public AnalyzeInfo(QuercusProgram program,
                     FunctionInfo function,
                     LoopAnalyzeInfo parentLoop)
  {
    _program = program;
    _function = function;
    _parentLoop = parentLoop;
  }

  public AnalyzeInfo(QuercusProgram program,
                     FunctionInfo function)
  {
    _program = program;
    _function = function;
    _parentLoop = null;
  }

  /**
   * Returns the function.
   */
  public FunctionInfo getFunction()
  {
    return _function;
  }
  
  public void setInitialBlock(boolean isInitialBlock)
  {
    _isInitialBlock = isInitialBlock;
  }
  
  /**
   * Returns true for the initial block.
   */
  public boolean isInitialBlock()
  {
    return _isInitialBlock;
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
  public VarExprPro getVar(StringValue name)
  {
    return _varMap.get(name);
  }

  /**
   * Adds the matching variable.
   */
  public void addVar(VarExprPro var)
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
   * Clears a var map to the unknown state.
   */
  public void setUnknown()
  {
    for (Map.Entry<StringValue,VarExprPro> entry : _varMap.entrySet()) {
      // php/3g11
      
      VarExprPro var = new VarExprPro(entry.getValue().getVarInfo());
      var.setVarState(VarState.UNKNOWN);
      entry.setValue(var);
    }
  }

  /**
   * Copies a var map.
   */
  public AnalyzeInfo copy()
  {
    AnalyzeInfo info = new AnalyzeInfo(_program, _function, _parentLoop);

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

    AnalyzeInfo info = new AnalyzeInfo(_program,
                                       _function,
                                       loopInfo);

    info._varMap.putAll(_varMap);

    return info;
  }

  /**
   * Merge with a joining var map.
   */
  public void merge(AnalyzeInfo info)
  {
    ArrayList<VarExprPro> vars = new ArrayList<VarExprPro>(_varMap.values());

    for (VarExprPro var : vars) {
      VarExprPro mergeVar = info.getVar(var.getName());

      if (mergeVar == null)
        var = var.analyzeVarState(VarState.UNKNOWN);
      else
        var = var.analyzeMerge(mergeVar);

      _varMap.put(var.getName(), var);
    }

    for (VarExprPro mergeVar : info._varMap.values()) {
      VarExprPro var = getVar(mergeVar.getName());

      if (var == null) {
        mergeVar = mergeVar.analyzeVarState(VarState.UNKNOWN);

        _varMap.put(mergeVar.getName(), mergeVar);
      }
    }
  }

  /**
   * Returns the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    QuercusContext quercus = _function.getQuercus();

    AbstractFunction fun = quercus.findFunction(name);

    if (fun != null)
      return fun;

    fun = _program.findFunction(name);

    if (fun != null)
      return fun;

    AbstractFunction []funList = _program.getRuntimeFunctionList();

    if (funList == null)
      return null;

    int id = quercus.getFunctionId(name);

    if (id < 0 || funList.length <= id)
      return null;

    fun = funList[id];

    return fun;
  }
}

