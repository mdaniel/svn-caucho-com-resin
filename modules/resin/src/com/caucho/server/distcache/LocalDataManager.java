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

package com.caucho.server.distcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.db.blob.BlobInputStream;
import com.caucho.distcache.CacheSerializer;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.server.distcache.CacheStoreManager.DataItem;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.NullOutputStream;
import com.caucho.vfs.Crc64InputStream;
import com.caucho.vfs.Crc64OutputStream;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Manages the distributed cache
 */
final class LocalDataManager
{
  private static final L10N L = new L10N(LocalDataManager.class);
  private static final Logger log
    = Logger.getLogger(LocalDataManager.class.getName());
  
  private final CacheStoreManager _storeManager;
  
  LocalDataManager(CacheStoreManager storeManager)
  {
    _storeManager = storeManager;
  }
  
  private CacheStoreManager getStoreManager()
  {
    return _storeManager;
  }
  
  private CacheDataBacking getDataBacking()
  {
    return getStoreManager().getDataBacking();
  }

  MnodeUpdate writeData(MnodeValue update, 
                       long version,
                       StreamSource source)
  {
    long valueHash = update.getValueHash();
    long valueLength = update.getValueLength();
    long valueDataId = getDataBacking().saveData(source, (int) valueLength);
    
    return new MnodeUpdate(valueHash,
                           valueDataId,
                           valueLength,
                           version,
                           update);
  }

  final public MnodeUpdate writeValue(MnodeEntry mnodeValue,
                                      Object value,
                                      CacheConfig config)
  {
    TempOutputStream os = null;
    CacheSerializer serializer = config.getValueSerializer();
    
    try {
      os = new TempOutputStream();

      long newValueHash = writeDataStream(os, value, serializer);

      int length = os.getLength();

      StreamSource source = new StreamSource(os);
      long valueDataId = getDataBacking().saveData(source, length);
      
      if (valueDataId <= 0) {
        throw new IllegalStateException(L.l("Can't save the data '{0}'",
                                            newValueHash));
      }
      
      long newVersion = getNewVersion(mnodeValue);
      
      // XXX: request owner?

      return new MnodeUpdate(newValueHash,
                             valueDataId,
                             length, 
                             newVersion,
                             config,
                             mnodeValue.getLeaseOwner(),
                             mnodeValue.getLeaseTimeout());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
        os.destroy();
    }
  }

  final protected Object readData(HashKey key,
                                  long valueHash,
                                  long valueDataId,
                                  CacheSerializer serializer,
                                  CacheConfig config)
  {
    if (valueHash == 0)
      return null;

    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      WriteStream out = Vfs.openWrite(os);

      if (! getDataBacking().loadData(valueDataId, out)) {
        log.warning(this + " cannot load data for key=" + key + " from triad");
        
        out.close();
        
        return null;
        
        /*
        if (! loadClusterData(key, valueKey, valueIndex, config)) {
          log.warning(this + " cannot load data for " + valueKey + " from triad");
          
          out.close();
        
          return null;
        }

        if (! getDataBacking().loadData(valueKey, out)) {
          out.close();
        
          return null;
        }
        */
      }

      out.close();

      InputStream is = os.openInputStream();

      try {
        // InflaterInputStream gzIn = new InflaterInputStream(is);

        // Object value = serializer.deserialize(gzIn);
        Object value = serializer.deserialize(is);

        // gzIn.close();

        return value;
      } finally {
        is.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    } finally {
      if (os != null)
        os.destroy();
    }
  }

  final protected boolean readData(HashKey key,
                                   MnodeEntry mnodeValue,
                                   OutputStream os,
                                   CacheConfig config)
    throws IOException
  {
    long valueDataId = mnodeValue.getValueDataId();
    
    if (valueDataId <= 0) {
      throw new IllegalStateException(L.l("readData may not be called with a null value"));
    }

    WriteStream out = Vfs.openWrite(os);

    try {
      Blob blob = mnodeValue.getBlob();
      
      if (blob == null) {
        blob = getDataBacking().loadBlob(valueDataId);
        
        if (blob != null)
          mnodeValue.setBlob(blob);
      }

      if (blob != null) {
        loadData(blob, out);

        return true;
      }

      /*
      if (! loadClusterData(key, valueKey, valueIndex, config)) {
        log.warning(this + " cannot load cluster value " + valueKey);

        // XXX: error?  since we have the value key, it should exist

        // server/0180
        // return false;
      }

      if (getDataBacking().loadData(valueKey, valueIndex, out)) {
        return true;
      }
      */

      log.warning(this + " unexpected load failure in readValue key=" + key);

      // XXX: error?  since we have the value key, it should exist

      return false;
    } finally {
      if (out != os)
        out.close();
    }
  }
  
  private void loadData(Blob blob, WriteStream out)
    throws IOException
  {
    try {
      InputStream is = blob.getBinaryStream();
      
      if (is instanceof BlobInputStream) {
        BlobInputStream blobIs = (BlobInputStream) is;
        
        blobIs.readToOutput(out);
      }
      else {
        out.writeStream(blob.getBinaryStream());
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
  
  private long getNewVersion(MnodeEntry mnodeValue)
  {
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;
    
    return getNewVersion(version);
  }
  
  private long getNewVersion(long version)
  {
    long newVersion = version + 1;

    long now = CurrentTime.getCurrentTime();
  
    if (newVersion < now)
      return now;
    else
      return newVersion;
  }

  /**
   * Used by QA
   */
  final public long calculateValueHash(Object value,
                                       CacheConfig config)
  {
    // TempOutputStream os = null;
    
    try {
      NullOutputStream os = NullOutputStream.NULL;

      return writeDataStream(os, value, config.getValueSerializer());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private long writeDataStream(OutputStream os, 
                               Object value, 
                               CacheSerializer serializer)
    throws IOException
  {
    Crc64OutputStream mOut = new Crc64OutputStream(os);
    //DeflaterOutputStream gzOut = new DeflaterOutputStream(mOut);
    //ResinDeflaterOutputStream gzOut = new ResinDeflaterOutputStream(mOut);

    //serializer.serialize(value, gzOut);
    serializer.serialize(value, mOut);

    //gzOut.finish();
    //gzOut.close();
    mOut.close();
    
    long hash = mOut.getDigest();
    
    return hash;
  }

  final public DataItem writeData(InputStream is)
    throws IOException
  {
    TempOutputStream os = null;

    try {
      Crc64InputStream mIn = new Crc64InputStream(is);
      
      long valueIndex = getDataBacking().saveData(mIn, -1);

      long valueHash = mIn.getDigest();

      long length = mIn.getLength();
      
      return new DataItem(valueHash, valueIndex, length);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
        os.destroy();
    }
  }
}
