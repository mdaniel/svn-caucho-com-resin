/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.management.server;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * Wrapper bean for results returned from a JdbcQueryMXBean.
 **/
public class JdbcQueryResult implements Serializable {
  private ArrayList<String> _rowNames;
  private ArrayList<ArrayList<String>> _resultData;

  public JdbcQueryResult()
  {
  }

  public JdbcQueryResult(ArrayList<String> rowNames,
                         ArrayList<ArrayList<String>> resultData)
  {
    _rowNames = rowNames;
    _resultData = resultData;
  }

  public void setRowNames(ArrayList<String> rowNames)
  {
    _rowNames = rowNames;
  }

  public ArrayList<String> getRowNames()
  {
    return _rowNames;
  }

  public void setResultData(ArrayList<ArrayList<String>> resultData)
  {
    _resultData = resultData;
  }

  public ArrayList<ArrayList<String>> getResultData()
  {
    return _resultData;
  }
}
