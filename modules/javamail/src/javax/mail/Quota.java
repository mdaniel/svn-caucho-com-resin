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

package javax.mail;

/**
 * This class represents a set of quotas for a given quota root.
 */
public class Quota {

  /**
   * The name of the quota root.
   */
  public String quotaRoot;

  /**
   * The set of resources associated with this quota root.
   */
  public Quota.Resource[] resources = new Resource[0];

  /**
   * Create a Quota object for the named quotaroot with no associated resources.
   * quotaRoot - the name of the quota root
   */
  public Quota(String quotaRoot)
  {
    this.quotaRoot = quotaRoot;
  }

  /**
   * Set a resource limit for this quota root.
   */
  public void setResourceLimit(String name, long limit)
  {
    for(int i=0; i<resources.length; i++)
      if (resources[i].name.equals(name)) {
	resources[i].limit = limit;
	return;
      }

    Quota.Resource[] resources =
      new Quota.Resource[this.resources.length+1];
    System.arraycopy(this.resources, 0, resources, 0, this.resources.length);
    resources[resources.length-1] = new Resource(name, 0, limit);
    this.resources = resources;
  }

  /**
   * An individual resource in a quota root.
   */
  public static class Resource {

    /**
     * The usage limit for the resource.
     */
    public long limit;

    /**
     * The name of the resource.
     */
    public String name;

    /**
     * The current usage of the resource.
     */
    public long usage;

    /**
     * Construct a Resource object with the given name, usage, and
     * limit.
     */
    public Resource(String name, long usage, long limit)
    {
      this.name = name;
      this.usage = usage;
      this.limit = limit;
    }

  }
}
