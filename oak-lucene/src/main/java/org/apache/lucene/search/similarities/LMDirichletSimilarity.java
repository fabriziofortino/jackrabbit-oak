/*
 * COPIED FROM APACHE LUCENE 4.7.2
 *
 * Git URL: git@github.com:apache/lucene.git, tag: releases/lucene-solr/4.7.2, path: lucene/core/src/java
 *
 * (see https://issues.apache.org/jira/browse/OAK-10786 for details)
 */

package org.apache.lucene.search.similarities;

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

import java.util.Locale;

import org.apache.lucene.search.Explanation;

/**
 * Bayesian smoothing using Dirichlet priors. From Chengxiang Zhai and John
 * Lafferty. 2001. A study of smoothing methods for language models applied to
 * Ad Hoc information retrieval. In Proceedings of the 24th annual international
 * ACM SIGIR conference on Research and development in information retrieval
 * (SIGIR '01). ACM, New York, NY, USA, 334-342.
 * <p>
 * The formula as defined the paper assigns a negative score to documents that
 * contain the term, but with fewer occurrences than predicted by the collection
 * language model. The Lucene implementation returns {@code 0} for such
 * documents.
 * </p>
 * 
 * @lucene.experimental
 */
public class LMDirichletSimilarity extends LMSimilarity {
  /** The &mu; parameter. */
  private final float mu;
  
  /** Instantiates the similarity with the provided &mu; parameter. */
  public LMDirichletSimilarity(CollectionModel collectionModel, float mu) {
    super(collectionModel);
    this.mu = mu;
  }
  
  /** Instantiates the similarity with the provided &mu; parameter. */
  public LMDirichletSimilarity(float mu) {
    this.mu = mu;
  }

  /** Instantiates the similarity with the default &mu; value of 2000. */
  public LMDirichletSimilarity(CollectionModel collectionModel) {
    this(collectionModel, 2000);
  }
  
  /** Instantiates the similarity with the default &mu; value of 2000. */
  public LMDirichletSimilarity() {
    this(2000);
  }
  
  @Override
  protected float score(BasicStats stats, float freq, float docLen) {
    float score = stats.getTotalBoost() * (float)(Math.log(1 + freq /
        (mu * ((LMStats)stats).getCollectionProbability())) +
        Math.log(mu / (docLen + mu)));
    return score > 0.0f ? score : 0.0f;
  }
  
  @Override
  protected void explain(Explanation expl, BasicStats stats, int doc,
      float freq, float docLen) {
    if (stats.getTotalBoost() != 1.0f) {
      expl.addDetail(new Explanation(stats.getTotalBoost(), "boost"));
    }

    expl.addDetail(new Explanation(mu, "mu"));
    Explanation weightExpl = new Explanation();
    weightExpl.setValue((float)Math.log(1 + freq /
        (mu * ((LMStats)stats).getCollectionProbability())));
    weightExpl.setDescription("term weight");
    expl.addDetail(weightExpl);
    expl.addDetail(new Explanation(
        (float)Math.log(mu / (docLen + mu)), "document norm"));
    super.explain(expl, stats, doc, freq, docLen);
  }

  /** Returns the &mu; parameter. */
  public float getMu() {
    return mu;
  }
  
  @Override
  public String getName() {
    return String.format(Locale.ROOT, "Dirichlet(%f)", getMu());
  }
}
