/*
 * COPIED FROM APACHE LUCENE 4.7.2
 *
 * Git URL: git@github.com:apache/lucene.git, tag: releases/lucene-solr/4.7.2, path: lucene/core/src/java
 *
 * (see https://issues.apache.org/jira/browse/OAK-10786 for details)
 */

package org.apache.lucene.util.packed;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.store.IndexInput;

final class DirectPacked64SingleBlockReader extends PackedInts.ReaderImpl {

  private final IndexInput in;
  private final long startPointer;
  private final int valuesPerBlock;
  private final long mask;

  DirectPacked64SingleBlockReader(int bitsPerValue, int valueCount,
      IndexInput in) {
    super(valueCount, bitsPerValue);
    this.in = in;
    startPointer = in.getFilePointer();
    valuesPerBlock = 64 / bitsPerValue;
    mask = ~(~0L << bitsPerValue);
  }

  @Override
  public long get(int index) {
    final int blockOffset = index / valuesPerBlock;
    final long skip = ((long) blockOffset) << 3;
    try {
      in.seek(startPointer + skip);

      long block = in.readLong();
      final int offsetInBlock = index % valuesPerBlock;
      return (block >>> (offsetInBlock * bitsPerValue)) & mask;
    } catch (IOException e) {
      throw new IllegalStateException("failed", e);
    }
  }

  @Override
  public long ramBytesUsed() {
    return 0;
  }
}
