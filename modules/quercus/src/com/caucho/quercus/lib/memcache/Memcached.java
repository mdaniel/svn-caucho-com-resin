/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.memcache;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

public class Memcached
{
  public static final int OPT_COMPRESSION = 0;
  public static final int OPT_SERIALIZER = 1;
  public static final int SERIALIZER_PHP = 2;
  public static final int SERIALIZER_IGBINARY = 3;
  public static final int SERIALIZER_JSON = 4;
  public static final int OPT_PREFIX_KEY = 5;
  public static final int OPT_HASH = 6;
  public static final int HASH_DEFAULT = 7;
  public static final int HASH_MD5 = 8;
  public static final int HASH_CRC = 9;
  public static final int HASH_FNV1_64 = 10;
  public static final int HASH_FNV1A_64 = 11;
  public static final int HASH_FNV1_32 = 12;
  public static final int HASH_FNV1A_32 = 13;
  public static final int HASH_HSIEH = 14;
  public static final int HASH_MURMUR = 15;
  public static final int OPT_DISTRIBUTION = 16;
  public static final int DISTRIBUTION_MODULA = 17;
  public static final int DISTRIBUTION_CONSISTENT = 18;
  public static final int OPT_LIBKETAMA_COMPATIBLE = 19;
  public static final int OPT_BUFFER_WRITES = 20;
  public static final int OPT_BINARY_PROTOCOL = 21;
  public static final int OPT_NO_BLOCK = 22;
  public static final int OPT_TCP_NODELAY = 23;
  public static final int OPT_SOCKET_SEND_SIZE = 24;
  public static final int OPT_SOCKET_RECV_SIZE = 25;
  public static final int OPT_CONNECT_TIMEOUT = 26;
  public static final int OPT_RETRY_TIMEOUT = 27;
  public static final int OPT_SEND_TIMEOUT = 28;
  public static final int OPT_RECV_TIMEOUT = 29;
  public static final int OPT_POLL_TIMEOUT = 30;
  public static final int OPT_CACHE_LOOKUPS = 31;
  public static final int OPT_SERVER_FAILURE_LIMIT = 32;
  public static final int HAVE_IGBINARY = 33;
  public static final int HAVE_JSON = 34;
  public static final int GET_PRESERVE_ORDER = 35;
  public static final int RES_SUCCESS = 36;
  public static final int RES_FAILURE = 37;
  public static final int RES_HOST_LOOKUP_FAILURE = 38;
  public static final int RES_UNKNOWN_READ_FAILURE = 39;
  public static final int RES_PROTOCOL_ERROR = 40;
  public static final int RES_CLIENT_ERROR = 41;
  public static final int RES_SERVER_ERROR = 42;
  public static final int RES_WRITE_FAILURE = 43;
  public static final int RES_DATA_EXISTS = 44;
  public static final int RES_NOTSTORED = 45;
  public static final int RES_NOTFOUND = 46;
  public static final int RES_PARTIAL_READ = 47;
  public static final int RES_SOME_ERRORS = 48;
  public static final int RES_NO_SERVERS = 49;
  public static final int RES_END = 50;
  public static final int RES_ERRNO = 51;
  public static final int RES_BUFFERED = 52;
  public static final int RES_TIMEOUT = 53;
  public static final int RES_BAD_KEY_PROVIDED = 54;
  public static final int RES_CONNECTION_SOCKET_CREATE_FAILURE = 55;
  public static final int RES_PAYLOAD_FAILURE = 56;

  public boolean setOption(Env env, int option, Value value)
  {
    return false;
  }

  public boolean setOptions(Env env, ArrayValue array)
  {
    return false;
  }
}
