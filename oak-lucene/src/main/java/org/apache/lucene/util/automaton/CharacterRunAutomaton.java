/*
 * COPIED FROM APACHE LUCENE 4.7.2
 *
 * Git URL: git@github.com:apache/lucene.git, tag: releases/lucene-solr/4.7.2, path: lucene/core/src/java
 *
 * (see https://issues.apache.org/jira/browse/OAK-10786 for details)
 */

package org.apache.lucene.util.automaton;

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

/**
 * Automaton representation for matching char[].
 */
public class CharacterRunAutomaton extends RunAutomaton {

  public CharacterRunAutomaton(Automaton a) {
    super(a, Character.MAX_CODE_POINT, false);
  }

  /**
   * Returns true if the given string is accepted by this automaton.
   */
  public boolean run(String s) {
    int p = initial;
    int l = s.length();
    for (int i = 0, cp = 0; i < l; i += Character.charCount(cp)) {
      p = step(p, cp = s.codePointAt(i));
      if (p == -1) return false;
    }
    return accept[p];
  }
  
  /**
   * Returns true if the given string is accepted by this automaton
   */
  public boolean run(char[] s, int offset, int length) {
    int p = initial;
    int l = offset + length;
    for (int i = offset, cp = 0; i < l; i += Character.charCount(cp)) {
      p = step(p, cp = Character.codePointAt(s, i, l));
      if (p == -1) return false;
    }
    return accept[p];
  }
}
