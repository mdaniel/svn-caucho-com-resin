/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.management.server;


/**
 * Information about a saved/pre-configured meter graph.
 */
public interface MeterGraphPageInfo {
  /**
   * Returns the meter graphs page name.
   */
  public String getName();
  
  /**
   * Returns the preferred number of columns.
   */
  public int getColumns();
  
  /**
   * Returns the preferred period in milliseconds
   */
  public long getPeriod();
  
  /**
   * Should we display summary?
   */
  public boolean isSummary();
  
  /**
   * Should we display log?
   */
  public boolean isLog();

  /**
   * Should we display heap dump?
   */
  public boolean isHeapDump();

  /**
   * Should we display profile?
   */
  public boolean isProfile();

  /**
   * Should we display thread-dump?
   */
  public boolean isThreadDump();

  /**
   * Should we display jmx-dump?
   */
  public boolean isJmxDump();
  
  
  /**
   * Returns the sections of the page.
   */
  public MeterGraphSectionInfo []getMeterSections();
  
  /**
   * Returns the graphs in the default section.
   */
  public MeterGraphInfo []getMeterGraphs();
}
