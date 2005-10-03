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

package com.caucho.jmx;

import javax.management.MBeanOperationInfo;

public class AdminOperationInfo
  extends AdminFeatureInfo
{
  private int _impact = MBeanOperationInfo.UNKNOWN;
  private Closure<Boolean> _enabled;

  AdminOperationInfo(String name)
  {
    super(name);
  }

  /**
   * Set the impact that is returned by
   * {@link javax.management.MBeanOperationInfo#getImpact()},
   * default is
   * {@link javax.management.MBeanOperationInfo#UNKNOWN}.
   */
  public AdminOperationInfo setImpact(int impact)
  {
    _impact = impact;

    return this;
  }

  public int getImpact()
  {
    return _impact;
  }

  /**
   * If true, the operation can be performed.  If false, the oepration cannot
   * be performed.
   *
   * <p>
   * This is a hint to the client, clients can still perform the operation
   * even if this is false.
   * </p>
   *
   * <p>The corresponding descriptor field is "enabled".</p>
   */
  public AdminOperationInfo setEnabled(Closure<Boolean> closure)
  {
    _enabled = closure;

    return this;
  }

  public boolean isEnabled()
  {
    return _enabled.eval();
  }

  public interface Closure<T> {
    public T eval()
      throws RuntimeException;
  }
}
