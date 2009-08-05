/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.db.store;

import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.OutputStreamWithBuffer;
import com.caucho.vfs.TempCharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the indexes for a BLOB or CLOB.
 *
 * The inode contains 16 long values
 * <pre>
 *  0) length of the saved file
 *  1-14) direct fragment addresses (to 112k)
 *  15) pointer to the indirect block
 * </pre>
 *
 * <h3>Inline storage (120)</h3>
 *
 * If the length of the blob is less than 120, the blob is stored directly
 * in the inode.
 *
 * <h3>mini fragment storage (3840)</h3>
 *
 * If the length of the blob is less than 3840, the blob is stored
 * in mini-fragments of size 256 pointed by the inode's addresses.
 *
 * The maximum wasted space for mini-fragment storage is 255 bytes.
 *
 * <h3>indirect storage</h3>
 *
 * The indirect block (an 8k fragment) itself is divided into sections:
 * <pre>
 *  0-511) single indirect fragment addresses (4M, 2^12)
 *  512-767) double indirect block addresses (128G, 2^37)
 *  768-1023) triple indirect block addresses (to 1P, 2^50)
 * </pre>
 */
public class Inode {
  private static final L10N L = new L10N(Inode.class);
  private static final Logger log
    = Logger.getLogger(Inode.class.getName());

  public static final int INODE_SIZE = 128;
  public static final int INLINE_BLOB_SIZE = 120;
  public static final int BLOCK_SIZE = Store.BLOCK_SIZE;
  public static final int INODE_BLOCK_SIZE = Store.FRAGMENT_SIZE;
  public static final int FRAGMENT_SIZE = Store.FRAGMENT_SIZE;

  public static final int MINI_FRAG_SIZE = Store.MINI_FRAG_SIZE;

  public static final int MINI_FRAG_BLOB_SIZE
    = (INLINE_BLOB_SIZE / 8) * MINI_FRAG_SIZE;

  public static final int INDIRECT_BLOCKS = INODE_BLOCK_SIZE / 8;

  // direct addresses are stored in the inode itself (112k of data).
  public static final int DIRECT_BLOCKS = 14;
  // single indirect addresses are stored in the indirect block (4M data)
  public static final int SINGLE_INDIRECT_BLOCKS = INDIRECT_BLOCKS / 2;
  // double indirect addresses (2^37 = 128G data)
  public static final int DOUBLE_INDIRECT_BLOCKS = INDIRECT_BLOCKS / 4;
  // triple indirect addresses (2^50 = 2P data)
  public static final int TRIPLE_INDIRECT_BLOCKS = INDIRECT_BLOCKS / 4;

  public static final long INLINE_MAX = 120;

  public static final int MINI_FRAG_MAX
    = (INLINE_BLOB_SIZE / 8) * MINI_FRAG_SIZE;

  public static final long DIRECT_MAX
    = FRAGMENT_SIZE * DIRECT_BLOCKS;

  public static final long SINGLE_INDIRECT_MAX
    = DIRECT_MAX + SINGLE_INDIRECT_BLOCKS * FRAGMENT_SIZE;

  public static final long FRAGMENT_MAX
    = SINGLE_INDIRECT_MAX;

  public static final long DOUBLE_INDIRECT_MAX
    = (SINGLE_INDIRECT_MAX
       + DOUBLE_INDIRECT_BLOCKS * (BLOCK_SIZE / 8L) * BLOCK_SIZE);

  private static final byte []NULL_BYTES = new byte[INODE_SIZE];

  private Store _store;
  private StoreTransaction _xa;

  private byte []_bytes = new byte[INODE_SIZE];

  public Inode()
  {
  }

  public Inode(Store store, StoreTransaction xa)
  {
    _store = store;
    _xa = xa;
  }

  public Inode(Store store)
  {
    this(store, RawTransaction.create());
  }

  /**
   * Returns the backing store.
   */
  public Store getStore()
  {
    return _store;
  }

  /**
   * Returns the buffer.
   */
  public byte []getBuffer()
  {
    return _bytes;
  }

  /**
   * Returns the length.
   */
  public long getLength()
  {
    return readLong(_bytes, 0);
  }

  public void init(Store store, StoreTransaction xa,
                   byte []buffer, int offset)
  {
    _store = store;
    _xa = xa;

    System.arraycopy(buffer, offset, _bytes, 0, _bytes.length);
  }

  /**
   * Opens a read stream to the inode.
   */
  public InputStream openInputStream()
  {
    return new BlobInputStream(this);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToStream(OutputStreamWithBuffer os)
    throws IOException
  {
    writeToStream(os, 0, Long.MAX_VALUE / 2);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToStream(OutputStreamWithBuffer os,
                            long offset, long length)
    throws IOException
  {
    byte []buffer = os.getBuffer();
    int writeLength = buffer.length;
    int writeOffset = os.getBufferOffset();

    while (length > 0) {
      int sublen = writeLength - writeOffset;

      if (sublen == 0) {
        buffer = os.nextBuffer(writeOffset);
        writeOffset = os.getBufferOffset();
        sublen = writeLength - writeOffset;
      }

      if (length < sublen)
        sublen = (int) length;

      int len = read(_bytes, 0, _store,
                     offset,
                     buffer, writeOffset, sublen);

      if (len <= 0)
        break;

      writeOffset += len;
      offset += len;
      length -= len;
    }

    os.setBufferOffset(writeOffset);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToWriter(Writer writer)
    throws IOException
  {
    TempCharBuffer tempBuffer = TempCharBuffer.allocate();

    char []buffer = tempBuffer.getBuffer();
    int writeLength = buffer.length;
    long offset = 0;

    while (true) {
      int sublen = writeLength;

      int len = read(_bytes, 0, _store,
                     offset,
                     buffer, 0, sublen);

      if (len <= 0)
        break;

      writer.write(buffer, 0, len);

      offset += 2 * len;
    }

    TempCharBuffer.free(tempBuffer);
  }

  /**
   * Reads into a buffer.
   *
   * @param inode the inode buffer
   * @param inodeOffset the offset of the inode data in the buffer
   * @param store the owning store
   * @param fileOffset the offset into the file to read
   * @param buffer the buffer receiving the data
   * @param bufferOffset the offset into the receiving buffer
   * @param bufferLength the maximum number of bytes to read
   *
   * @return the number of bytes read
   */
  static int read(byte []inode, int inodeOffset,
                  Store store,
                  long fileOffset,
                  byte []buffer, int bufferOffset, int bufferLength)
    throws IOException
  {
    long fileLength = readLong(inode, inodeOffset);

    int sublen = bufferLength;
    if (fileLength - fileOffset < sublen)
      sublen = (int) (fileLength - fileOffset);

    if (sublen <= 0)
      return -1;

    if (fileLength <= INLINE_MAX) {
      System.arraycopy(inode, inodeOffset + 8 + (int) fileOffset,
                       buffer, bufferOffset, sublen);

      return sublen;
    }
    else if (fileLength <= MINI_FRAG_MAX) {
      long fragAddr = readMiniFragAddr(inode, inodeOffset, store, fileOffset);
      int fragOffset = (int) (fileOffset % MINI_FRAG_SIZE);

      if (MINI_FRAG_SIZE - fragOffset < sublen)
        sublen = MINI_FRAG_SIZE - fragOffset;

      store.readMiniFragment(fragAddr, fragOffset,
                             buffer, bufferOffset, sublen);

      return sublen;
    }
    else if (fileOffset < FRAGMENT_MAX) {
      long fragAddr = readFragmentAddr(inode, inodeOffset, store, fileOffset);
      int fragOffset = (int) (fileOffset % FRAGMENT_SIZE);

      if (FRAGMENT_SIZE - fragOffset < sublen)
        sublen = FRAGMENT_SIZE - fragOffset;

      store.readFragment(fragAddr, fragOffset, buffer, bufferOffset, sublen);

      return sublen;
    }
    else {
      long addr = readBlockAddr(inode, inodeOffset, store, fileOffset);
      int offset = (int) ((fileOffset - FRAGMENT_MAX) % BLOCK_SIZE);

      if (BLOCK_SIZE - offset < sublen)
        sublen = BLOCK_SIZE - offset;

      store.readBlock(addr, offset, buffer, bufferOffset, sublen);

      return sublen;
    }
  }

  /**
   * Updates the buffer.  Called only from the blob classes.
   */
  static void append(byte []inode, int inodeOffset,
                     Store store, StoreTransaction xa,
                     byte []buffer, int offset, int length)
    throws IOException
  {
    long currentLength = readLong(inode, inodeOffset);
    long newLength = currentLength + length;

    writeLong(inode, inodeOffset, newLength);

    if (newLength <= INLINE_MAX) {
      System.arraycopy(buffer, offset,
                       inode, (int) (inodeOffset + 8 + currentLength),
                       length);
    }
    else if (newLength <= MINI_FRAG_MAX) {
      while (length > 0) {
        int sublen = length;

        if (MINI_FRAG_SIZE < sublen)
          sublen = MINI_FRAG_SIZE;

        long miniFragAddr = store.allocateMiniFragment(xa);

        if (miniFragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0} illegal mini fragment",
                                              store));
        }

        writeMiniFragAddr(inode, inodeOffset,
                          store, xa,
                          currentLength, miniFragAddr);

        store.writeMiniFragment(xa, miniFragAddr, 0, buffer, offset, sublen);

        offset += sublen;
        length -= sublen;
        currentLength += sublen;
      }
    }
    else {
      if (currentLength > 0 && currentLength < MINI_FRAG_MAX)
        throw new IllegalStateException(L.l("illegal length transition {0} to {1}",
                                            currentLength, newLength));

      if (currentLength < FRAGMENT_MAX) {
        int sublen = length;

        if (FRAGMENT_MAX - currentLength < sublen)
          sublen = (int) (FRAGMENT_MAX - currentLength);

        appendFragment(inode, inodeOffset, store, xa,
                       buffer, offset, length, currentLength);

        offset += sublen;
        length -= sublen;
        currentLength += sublen;
      }

      if (length > 0) {
        appendBlock(inode, inodeOffset, store, xa,
                    buffer, offset, length, currentLength);
      }
    }
  }

  private static void appendFragment(byte []inode, int inodeOffset,
                                     Store store, StoreTransaction xa,
                                     byte []buffer, int offset, int length,
                                     long currentLength)
    throws IOException
  {
    // XXX: theoretically deal with case of appending to inline, although
    // the blobs are the only writers and will avoid that case.

    while (length > 0 && currentLength < FRAGMENT_MAX) {
      if (currentLength % FRAGMENT_SIZE != 0) {
        long fragAddr = readFragmentAddr(inode, inodeOffset,
                                         store,
                                         currentLength);

        if (fragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(store + " inode: illegal fragment at " + currentLength);
        }

        int fragOffset = (int) (currentLength % FRAGMENT_SIZE);
        int sublen = length;

        if (FRAGMENT_SIZE - fragOffset < sublen)
          sublen = FRAGMENT_SIZE - fragOffset;

        store.writeFragment(xa, fragAddr, fragOffset, buffer, offset, sublen);

        offset += sublen;
        length -= sublen;

        currentLength += sublen;
      }
      else {
        int sublen = length;

        if (FRAGMENT_SIZE < sublen)
          sublen = FRAGMENT_SIZE;

        long fragAddr = store.allocateFragment(xa);

        if (fragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0} illegal fragment",
                                              store));
        }

        writeFragmentAddr(inode, inodeOffset,
                          store, xa,
                          currentLength, fragAddr);

        store.writeFragment(xa, fragAddr, 0, buffer, offset, sublen);

        offset += sublen;
        length -= sublen;

        currentLength += sublen;
      }
    }
  }

  private static void appendBlock(byte []inode, int inodeOffset,
                                  Store store, StoreTransaction xa,
                                  byte []buffer, int offset, int length,
                                  long currentLength)
    throws IOException
  {
    // XXX: theoretically deal with case of appending to inline, although
    // the blobs are the only writers and will avoid that case.

    while (length > 0) {
      if ((currentLength - FRAGMENT_MAX) % BLOCK_SIZE != 0) {
        long addr = readBlockAddr(inode, inodeOffset,
                                  store,
                                  currentLength);

        if (addr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(store + " inode: illegal block at " + currentLength);
        }

        int blockOffset = (int) ((currentLength - FRAGMENT_MAX) % BLOCK_SIZE);
        int sublen = length;

        if (BLOCK_SIZE - blockOffset < sublen)
          sublen = BLOCK_SIZE - blockOffset;

        store.writeBlock(xa, addr, blockOffset, buffer, offset, sublen);

        offset += sublen;
        length -= sublen;

        currentLength += sublen;
      }
      else {
        int sublen = length;

        if (BLOCK_SIZE < sublen)
          sublen = BLOCK_SIZE;

        Block block = store.allocateBlock();

        long blockAddr = Store.blockIdToAddress(block.getBlockId());

        block.free();

        if (blockAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0}: illegal block",
                                              store));
        }

        writeBlockAddr(inode, inodeOffset,
                       store, xa,
                       currentLength, blockAddr);

        store.writeBlock(xa, blockAddr, 0, buffer, offset, sublen);

        offset += sublen;
        length -= sublen;

        currentLength += sublen;
      }
    }
  }

  /**
   * Reads into a buffer.
   *
   * @param inode the inode buffer
   * @param inodeOffset the offset of the inode data in the buffer
   * @param store the owning store
   * @param fileOffset the offset into the file to read
   * @param buffer the buffer receiving the data
   * @param bufferOffset the offset into the receiving buffer
   * @param bufferLength the maximum number of chars to read
   *
   * @return the number of characters read
   */
  static int read(byte []inode, int inodeOffset, Store store,
                  long fileOffset,
                  char []buffer, int bufferOffset, int bufferLength)
    throws IOException
  {
    long fileLength = readLong(inode, inodeOffset);

    int sublen = (int) (fileLength - fileOffset) / 2;
    if (bufferLength < sublen)
      sublen = bufferLength;

    if (sublen <= 0)
      return -1;

    if (fileLength <= INLINE_MAX) {
      int baseOffset = inodeOffset + 8 + (int) fileOffset;

      for (int i = 0; i < sublen; i++) {
        char ch = (char) (((inode[baseOffset] & 0xff) << 8) +
                          ((inode[baseOffset + 1] & 0xff)));

        buffer[bufferOffset + i] = ch;

        baseOffset += 2;
      }

      return sublen;
    }
    else if (fileLength <= MINI_FRAG_MAX) {
      long fragAddr = readMiniFragAddr(inode, inodeOffset, store, fileOffset);
      int fragOffset = (int) (fileOffset % Inode.MINI_FRAG_SIZE);

      if (MINI_FRAG_SIZE - fragOffset < 2 * sublen)
        sublen = (MINI_FRAG_SIZE - fragOffset) / 2;

      store.readMiniFragment(fragAddr, fragOffset,
                             buffer, bufferOffset, sublen);

      return sublen;
    }
    else if (fileOffset < FRAGMENT_MAX) {
      long fragAddr = readFragmentAddr(inode, inodeOffset, store, fileOffset);
      int fragOffset = (int) (fileOffset % Inode.INODE_BLOCK_SIZE);

      if (FRAGMENT_SIZE - fragOffset < 2 * sublen)
        sublen = (FRAGMENT_SIZE - fragOffset) / 2;

      store.readFragment(fragAddr, fragOffset, buffer, bufferOffset, sublen);

      return sublen;
    }
    else {
      long addr = readBlockAddr(inode, inodeOffset, store, fileOffset);
      int offset = (int) ((fileOffset - FRAGMENT_MAX) % BLOCK_SIZE);

      if (BLOCK_SIZE - offset < sublen)
        sublen = BLOCK_SIZE - offset;

      store.readBlock(addr, offset, buffer, bufferOffset, sublen);

      return sublen;
    }
  }

  /**
   * Updates the buffer.  Called only from the clob classes.
   */
  static void append(byte []inode, int inodeOffset,
                     Store store, StoreTransaction xa,
                     char []buffer, int offset, int length)
    throws IOException
  {
    long currentLength = readLong(inode, inodeOffset);
    long newLength = currentLength + length;

    writeLong(inode, inodeOffset, newLength);

    if (newLength <= INLINE_BLOB_SIZE) {
      int writeOffset = (int) (inodeOffset + 8 + currentLength);

      for (int i = 0; i < length; i++) {
        char ch = buffer[offset + i];

        inode[writeOffset++] = (byte) (ch >> 8);
        inode[writeOffset++] = (byte) (ch);
      }
    }
    else {
      // XXX: theoretically deal with case of appending to inline, although
      // the blobs are the only writers and will avoid that case.

      if (currentLength % FRAGMENT_SIZE != 0) {
        long fragAddr = readFragmentAddr(inode, inodeOffset,
                                         store,
                                         currentLength);

        int fragOffset = (int) (currentLength % FRAGMENT_SIZE);
        int sublen = 2 * length;

        if (INODE_BLOCK_SIZE - fragOffset < sublen)
          sublen = INODE_BLOCK_SIZE - fragOffset;

        store.writeFragment(xa, fragAddr, fragOffset, buffer, offset, sublen);

        offset += sublen / 2;
        length -= sublen / 2;

        currentLength += sublen;
      }

      while (length > 0) {
        int sublen = 2 * length;

        if (INODE_BLOCK_SIZE < sublen)
          sublen = INODE_BLOCK_SIZE;

        long fragAddr = store.allocateFragment(xa);

        writeFragmentAddr(inode, inodeOffset,
                          store, xa,
                          currentLength, fragAddr);

        store.writeFragment(xa, fragAddr, 0, buffer, offset, sublen);

        offset += sublen / 2;
        length -= sublen / 2;

        currentLength += sublen;
      }
    }
  }

  private void appendFragment(byte []inode, int inodeOffset,
                              Store store, StoreTransaction xa,
                              char []buffer, int offset, int length,
                              long currentLength)
    throws IOException
  {
    // XXX: theoretically deal with case of appending to inline, although
    // the blobs are the only writers and will avoid that case.

    while (length > 0 && currentLength <= FRAGMENT_MAX) {
      if (currentLength % FRAGMENT_SIZE != 0) {
        long fragAddr = readFragmentAddr(inode, inodeOffset,
                                         store,
                                         currentLength);

        if (fragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(store + " inode: illegal fragment at " + currentLength);
        }

        int fragOffset = (int) (currentLength % INODE_BLOCK_SIZE);
        int sublen = 2 * length;

        if (INODE_BLOCK_SIZE - fragOffset < sublen)
          sublen = INODE_BLOCK_SIZE - fragOffset;

        store.writeFragment(xa, fragAddr, fragOffset, buffer, offset, sublen);

        offset += sublen / 2;
        length -= sublen / 2;

        currentLength += sublen;
      }
      else {
        int sublen = 2 * length;

        if (FRAGMENT_SIZE < sublen)
          sublen = FRAGMENT_SIZE;

        long fragAddr = store.allocateFragment(xa);

        if (fragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0}: illegal fragment",
                                              store));
        }

        writeFragmentAddr(inode, inodeOffset,
                          store, xa,
                          currentLength, fragAddr);

        store.writeFragment(xa, fragAddr, 0, buffer, offset, sublen);

        offset += sublen / 2;
        length -= sublen / 2;

        currentLength += sublen;
      }
    }
  }

  private void appendBlock(byte []inode, int inodeOffset,
                           Store store, StoreTransaction xa,
                           char []buffer, int offset, int length,
                           long currentLength)
    throws IOException
  {
    // XXX: theoretically deal with case of appending to inline, although
    // the blobs are the only writers and will avoid that case.

    while (length > 0) {
      if ((currentLength - FRAGMENT_MAX) % BLOCK_SIZE != 0) {
        long addr = readBlockAddr(inode, inodeOffset,
                                  store,
                                  currentLength);

        if (addr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(store + " inode: illegal block at " + currentLength);
        }

        int blockOffset = (int) ((currentLength - FRAGMENT_MAX) % BLOCK_SIZE);
        int sublen = 2 * length;

        if (BLOCK_SIZE - blockOffset < sublen)
          sublen = BLOCK_SIZE - blockOffset;

        store.writeBlock(xa, addr, blockOffset, buffer, offset, sublen);

        offset += sublen / 2;
        length -= sublen / 2;

        currentLength += sublen;
      }
      else {
        int sublen = 2 * length;

        if (BLOCK_SIZE < sublen)
          sublen = BLOCK_SIZE;

        long blockAddr = store.allocateFragment(xa);

        if (blockAddr == 0) {
          store.setCorrupted(true);
          throw new IllegalStateException(L.l("{0}: illegal fragment",
                                              store));
        }

        writeBlockAddr(inode, inodeOffset,
                       store, xa,
                       currentLength, blockAddr);

        store.writeBlock(xa, blockAddr, 0, buffer, offset, sublen);

        offset += sublen / 2;
        length -= sublen / 2;

        currentLength += sublen;
      }
    }
  }

  /**
   * Opens a byte output stream to the inode.
   */
  public OutputStream openOutputStream()
  {
    return new BlobOutputStream(this);
  }

  /**
   * Closes the output stream.
   */
  void closeOutputStream()
  {
    try {
      _store.saveAllocation();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Opens a char reader to the inode.
   */
  public Reader openReader()
  {
    return new ClobReader(this);
  }

  /**
   * Opens a char writer to the inode.
   */
  public Writer openWriter()
  {
    return new ClobWriter(this);
  }

  /**
   * Deletes the inode
   */
  public void remove()
  {
    synchronized (_bytes) {
      long length = readLong(_bytes, 0);

      byte []bytes = _bytes;

      try {
        if (length <= INLINE_BLOB_SIZE || bytes == null)
          return;
        else if (length <= MINI_FRAG_BLOB_SIZE) {
          for (; length > 0; length -= MINI_FRAG_SIZE) {
            long fragAddr = readMiniFragAddr(bytes, 0, _store, length - 1);

            if ((fragAddr & Store.BLOCK_MASK) == 0) {
              _store.setCorrupted(true);

              String msg = _store + ": inode block " + Long.toHexString(length) + " has 0 fragment";

              throw stateError(msg);
            }
            else if (fragAddr < 0) {
              String msg = _store + ": inode block " + Long.toHexString(length) + " has invalid fragment " + Long.toHexString(fragAddr);

              _store.setCorrupted(true);

              throw stateError(msg);
            }

            _store.deleteMiniFragment(_xa, fragAddr);
          }
        }
        else {
          long initLength = length;
          for (; length > 0; length -= INODE_BLOCK_SIZE) {
            long fragAddr = readFragmentAddr(bytes, 0, _store, length - 1);

            if ((fragAddr & Store.BLOCK_MASK) == 0) {
              String msg = _store + ": inode block " + Long.toHexString(length) + " has 0 fragment";
              log.warning(msg);
              _store.setCorrupted(true);
              continue;
            }
            else if (fragAddr < 0) {
              String msg = _store + ": inode block " + Long.toHexString(length) + " has invalid fragment " + Long.toHexString(fragAddr);

              log.warning(msg);
              _store.setCorrupted(true);
              continue;
            }

            _store.deleteFragment(_xa, fragAddr);

            int fragCount = (int) ((length - 1) / INODE_BLOCK_SIZE);

            int dblFragCount = fragCount - DIRECT_BLOCKS - SINGLE_INDIRECT_BLOCKS;

            // remove the double indirect blocks
            if (dblFragCount >= 0
                && dblFragCount % INDIRECT_BLOCKS == 0) {
              fragAddr = readLong(bytes, (DIRECT_BLOCKS + 1) * 8);

              int dblIndex = (int) (fragCount / INDIRECT_BLOCKS);

              fragAddr = _store.readFragmentLong(fragAddr, dblIndex);

              if (fragAddr != 0)
                _store.deleteFragment(_xa, fragAddr);
            }

            // remove the indirect blocks
            if (fragCount == DIRECT_BLOCKS) {
              fragAddr = readLong(bytes, (DIRECT_BLOCKS + 1) * 8);

              if (fragAddr != 0) {
                _store.deleteFragment(_xa, fragAddr);
              }
            }
          }
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        System.arraycopy(NULL_BYTES, 0, _bytes, 0, NULL_BYTES.length);

        try {
          _store.saveAllocation();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  /**
   * Clears the inode.
   */
  static void clear(byte []inode, int inodeOffset)
  {
    int end = inodeOffset + INODE_SIZE;

    for (; inodeOffset < end; inodeOffset++)
      inode[inodeOffset] = 0;
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readMiniFragAddr(byte []inode, int inodeOffset,
                                   Store store, long fileOffset)
    throws IOException
  {
    long fragCount = fileOffset / MINI_FRAG_SIZE;

    return readLong(inode, (int) (inodeOffset + 8 + 8 * fragCount));
  }

  /**
   * Writes the block id into the inode.
   */
  private static void writeMiniFragAddr(byte []inode, int offset,
                                        Store store, StoreTransaction xa,
                                        long fragLength, long fragAddr)
    throws IOException
  {
    int fragCount = (int) (fragLength / MINI_FRAG_SIZE);

    if ((fragAddr & Store.BLOCK_MASK) == 0) {
      store.setCorrupted(true);

      throw new IllegalStateException(store + ": inode block " + fragLength + " has zero value " + fragAddr);
    }

    writeLong(inode, offset + (fragCount + 1) * 8, fragAddr);
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readFragmentAddr(byte []inode, int inodeOffset,
                               Store store,
                               long fileOffset)
    throws IOException
  {
    int fragCount = (int) (fileOffset / FRAGMENT_SIZE);

    if (fragCount < DIRECT_BLOCKS)
      return readLong(inode, inodeOffset + 8 * (fragCount + 1));
    else if (fragCount < DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS) {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("{0} null block id", store));
      }

      int fragOffset = 8 * (fragCount - DIRECT_BLOCKS);

      long fragAddr = store.readFragmentLong(indAddr, fragOffset);

      return fragAddr;
    }
    else {
      store.setCorrupted(true);

      throw new IllegalStateException(L.l("{0} fragment address is over 64M ({1}), internal error",
                                          store, fragCount));

    }
  }

  /**
   * Writes the block id into the inode.
   */
  private static void writeFragmentAddr(byte []inode, int inodeOffset,
                                        Store store, StoreTransaction xa,
                                        long fileOffset, long fragAddr)
    throws IOException
  {
    if (FRAGMENT_MAX <= fileOffset) {
      store.setCorrupted(true);

      throw new IllegalStateException(L.l("{0} fragment address is over {1}M ({2}), internal error",
                                          store, FRAGMENT_MAX / (1024 * 1024),
                                          fileOffset));

    }

    int fragCount = (int) (fileOffset / FRAGMENT_SIZE);

    // XXX: not sure if correct, needs XA?
    if ((fragAddr & Store.BLOCK_MASK) == 0) {
      store.setCorrupted(true);

      String msg = store + ": inode block " + fragCount + " writing 0 fragment";
      throw stateError(msg);
    }

    if (fragCount < DIRECT_BLOCKS) {
      writeLong(inode, inodeOffset + 8 * (fragCount + 1), fragAddr);
    }
    else if (fragCount < DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS) {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        indAddr = store.allocateFragment(xa);

        writeLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8, indAddr);
      }

      int fragOffset = 8 * (fragCount - DIRECT_BLOCKS);

      store.writeFragmentLong(xa, indAddr, fragOffset, fragAddr);
    }
    else {
      store.setCorrupted(true);

      throw new IllegalStateException(L.l("{0}: can't yet support data over 64M (count={1})",
                                          store, fragCount));
    }
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readBlockAddr(byte []inode, int inodeOffset,
                            Store store,
                            long fileOffset)
    throws IOException
  {
    if (fileOffset < FRAGMENT_MAX) {
      store.setCorrupted(true);

      throw new IllegalStateException(store + " block/fragment mixup at " + fileOffset);
    }

    if (fileOffset < DOUBLE_INDIRECT_MAX) {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("{0} null block id", store));
      }

      int blockCount = (int) ((fileOffset - FRAGMENT_MAX) / BLOCK_SIZE);

      int dblBlockCount = blockCount / (BLOCK_SIZE / 8);

      int dblBlockIndex = 8 * (SINGLE_INDIRECT_BLOCKS + dblBlockCount);

      long dblIndAddr = store.readFragmentLong(indAddr, dblBlockIndex);

      if (dblIndAddr == 0) {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("null indirect block id"));
      }

      int blockOffset = 8 * (blockCount % (BLOCK_SIZE / 8));

      return store.readBlockLong(dblIndAddr, blockOffset);
    }
    else {
      store.setCorrupted(true);

      throw new IllegalStateException(L.l("{0} size over {1}M not supported",
                                          store,
                                          (DOUBLE_INDIRECT_MAX / (1024 * 1024))));
    }
  }

  /**
   * Writes the block id into the inode.
   */
  private static void writeBlockAddr(byte []inode, int inodeOffset,
                                     Store store, StoreTransaction xa,
                                     long fileOffset, long blockAddr)
    throws IOException
  {
    if (fileOffset < FRAGMENT_MAX) {
      store.setCorrupted(true);

      throw new IllegalStateException(store + " block/fragment mixup at " + fileOffset);
    }

    if (fileOffset < DOUBLE_INDIRECT_MAX) {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("{0} null block id", store));
      }

      int blockCount = (int) ((fileOffset - FRAGMENT_MAX) / BLOCK_SIZE);

      int dblBlockCount = blockCount / (BLOCK_SIZE / 8);
      int dblBlockIndex = 8 * (SINGLE_INDIRECT_BLOCKS + dblBlockCount);

      long dblIndAddr = store.readFragmentLong(indAddr, dblBlockIndex);

      if (dblIndAddr == 0) {
        Block block = store.allocateBlock();

        dblIndAddr = Store.blockIdToAddress(block.getBlockId());

        block.free();

        store.writeFragmentLong(xa, indAddr, dblBlockIndex, dblIndAddr);
      }

      int blockOffset = 8 * (blockCount % (BLOCK_SIZE / 8));

      store.writeBlockLong(xa, dblIndAddr, blockOffset, blockAddr);
    }
    else {
      store.setCorrupted(true);

      throw new IllegalStateException(L.l("{0} size over {1}M not supported",
                                          store,
                                          (DOUBLE_INDIRECT_MAX / (1024 * 1024))));
    }
  }

  /**
   * Reads the long.
   */
  public static long readLong(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56)
            + ((buffer[offset + 1] & 0xffL) << 48)
            + ((buffer[offset + 2] & 0xffL) << 40)
            + ((buffer[offset + 3] & 0xffL) << 32)
            + ((buffer[offset + 4] & 0xffL) << 24)
            + ((buffer[offset + 5] & 0xffL) << 16)
            + ((buffer[offset + 6] & 0xffL) << 8)
            + ((buffer[offset + 7] & 0xffL)));
  }

  /**
   * Writes the long.
   */
  public static void writeLong(byte []buffer, int offset, long v)
  {
    buffer[offset + 0] = (byte) (v >> 56);
    buffer[offset + 1] = (byte) (v >> 48);
    buffer[offset + 2] = (byte) (v >> 40);
    buffer[offset + 3] = (byte) (v >> 32);

    buffer[offset + 4] = (byte) (v >> 24);
    buffer[offset + 5] = (byte) (v >> 16);
    buffer[offset + 6] = (byte) (v >> 8);
    buffer[offset + 7] = (byte) (v);
  }

  /**
   * Reads the short.
   */
  private static int readShort(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 8)
            + ((buffer[offset + 1] & 0xff)));
  }

  /**
   * Writes the short.
   */
  private static void writeShort(byte []buffer, int offset, int v)
  {
    buffer[offset + 0] = (byte) (v >> 8);
    buffer[offset + 1] = (byte) v;
  }

  private static IllegalStateException stateError(String msg)
  {
    IllegalStateException e = new IllegalStateException(msg);
    e.fillInStackTrace();
    log.log(Level.WARNING, e.toString(), e);
    return e;
  }
}
