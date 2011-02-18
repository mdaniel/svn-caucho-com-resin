/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */
package com.caucho.env.health;

/**
 * OK:          Health check reported healthy status.  This does not imply recovery.
 * WARNING:     Health check reported warning threshold reached or critical is possible.
 * CRITICAL:    Health check reported critical status; action should be taken.
 * UNKNOWN:     Health check failed to execute properly; status is inconclusive.
 */
// ordinal comparison would be easier if UNKNOWN was first, but it would 
// make ordinal values change which would not be backwards compatible
public enum HealthStatus
{
  OK,
  WARNING,
  CRITICAL,
  UNKNOWN 
}

