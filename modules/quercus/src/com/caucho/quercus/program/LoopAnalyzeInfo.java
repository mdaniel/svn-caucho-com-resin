/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
public class LoopAnalyzeInfo {
  private final LoopAnalyzeInfo _parentLoop;
  
  private final AnalyzeInfo _continueInfo;
  private final AnalyzeInfo _breakInfo;

  public LoopAnalyzeInfo(LoopAnalyzeInfo parentLoop,
			 AnalyzeInfo continueInfo,
			 AnalyzeInfo breakInfo)
  {
    _parentLoop = parentLoop;

    _continueInfo = continueInfo;
    _breakInfo = breakInfo;
  }

  /**
   * Returns the continue info.
   */
  public AnalyzeInfo getContinueInfo()
  {
    return _continueInfo;
  }

  /**
   * Returns the break info.
   */
  public AnalyzeInfo getBreakInfo()
  {
    return _breakInfo;
  }

  /**
   * Merges the continue info.
   */
  public void mergeContinueInfo(AnalyzeInfo info)
  {
    if (_continueInfo != null)
      _continueInfo.merge(info);
    else // XXX: for switch inside while
      _parentLoop.mergeContinueInfo(info);
  }

  /**
   * Merges the break info.
   */
  public void mergeBreakInfo(AnalyzeInfo info)
  {
    _breakInfo.merge(info);
  }
}

