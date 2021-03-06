/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.regionserver.compactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.regionserver.HStore;
import org.apache.hadoop.hbase.regionserver.StoreConfigInformation;

/**
 * Compaction configuration for a particular instance of HStore.
 * Takes into account both global settings and ones set on the column family/store.
 * Control knobs for default compaction algorithm:
 * <p/>
 * maxCompactSize - upper bound on file size to be included in minor compactions
 * minCompactSize - lower bound below which compaction is selected without ratio test
 * minFilesToCompact - lower bound on number of files in any minor compaction
 * maxFilesToCompact - upper bound on number of files in any minor compaction
 * compactionRatio - Ratio used for compaction
 * <p/>
 * Set parameter as "hbase.hstore.compaction.<attribute>"
 */

//TODO: revisit this class for online parameter updating (both in xml and on the CF)
@InterfaceAudience.Private
public class CompactionConfiguration {

  static final Log LOG = LogFactory.getLog(CompactionConfiguration.class);

  private static final String CONFIG_PREFIX = "hbase.hstore.compaction.";

  Configuration conf;
  StoreConfigInformation storeConfigInfo;

  long maxCompactSize;
  long minCompactSize;
  int minFilesToCompact;
  int maxFilesToCompact;
  double compactionRatio;
  double offPeekCompactionRatio;
  long throttlePoint;
  boolean shouldDeleteExpired;
  long majorCompactionPeriod;
  float majorCompactionJitter;
  int blockingStoreFileCount;

  CompactionConfiguration(Configuration conf, StoreConfigInformation storeConfigInfo) {
    this.conf = conf;
    this.storeConfigInfo = storeConfigInfo;

    maxCompactSize = conf.getLong(CONFIG_PREFIX + "max.size", Long.MAX_VALUE);
    minCompactSize = conf.getLong(CONFIG_PREFIX + "min.size",
        storeConfigInfo.getMemstoreFlushSize());
    minFilesToCompact = Math.max(2, conf.getInt(CONFIG_PREFIX + "min",
          /*old name*/ conf.getInt("hbase.hstore.compactionThreshold", 3)));
    maxFilesToCompact = conf.getInt(CONFIG_PREFIX + "max", 10);
    compactionRatio = conf.getFloat(CONFIG_PREFIX + "ratio", 1.2F);
    offPeekCompactionRatio = conf.getFloat(CONFIG_PREFIX + "ratio.offpeak", 5.0F);

    throttlePoint =  conf.getLong("hbase.regionserver.thread.compaction.throttle",
          2 * maxFilesToCompact * storeConfigInfo.getMemstoreFlushSize());
    shouldDeleteExpired = conf.getBoolean("hbase.store.delete.expired.storefile", true);
    majorCompactionPeriod = conf.getLong(HConstants.MAJOR_COMPACTION_PERIOD, 1000*60*60*24);
    majorCompactionJitter = conf.getFloat("hbase.hregion.majorcompaction.jitter", 0.20F);
    blockingStoreFileCount =
        conf.getInt("hbase.hstore.blockingStoreFiles", HStore.DEFAULT_BLOCKING_STOREFILE_COUNT);

    LOG.info("Compaction configuration " + this.toString());
  }

  @Override
  public String toString() {
    return String.format(
      "size [%d, %d); files [%d, %d); ratio %f; off-peak ratio %f; throttle point %d;"
      + "%s delete expired; major period %d, major jitter %f",
      minCompactSize,
      maxCompactSize,
      minFilesToCompact,
      maxFilesToCompact,
      compactionRatio,
      offPeekCompactionRatio,
      throttlePoint,
      shouldDeleteExpired ? "" : " don't",
      majorCompactionPeriod,
      majorCompactionJitter);
  }

  /**
   * @return store file count that will cause the memstore of this store to be blocked.
   */
  int getBlockingStorefileCount() {
    return this.blockingStoreFileCount;
  }

  /**
   * @return lower bound below which compaction is selected without ratio test
   */
  long getMinCompactSize() {
    return minCompactSize;
  }

  /**
   * @return upper bound on file size to be included in minor compactions
   */
  long getMaxCompactSize() {
    return maxCompactSize;
  }

  /**
   * @return upper bound on number of files to be included in minor compactions
   */
  int getMinFilesToCompact() {
    return minFilesToCompact;
  }

  /**
   * @return upper bound on number of files to be included in minor compactions
   */
  int getMaxFilesToCompact() {
    return maxFilesToCompact;
  }

  /**
   * @return Ratio used for compaction
   */
  double getCompactionRatio() {
    return compactionRatio;
  }

  /**
   * @return Off peak Ratio used for compaction
   */
  double getCompactionRatioOffPeak() {
    return offPeekCompactionRatio;
  }

  /**
   * @return ThrottlePoint used for classifying small and large compactions
   */
  long getThrottlePoint() {
    return throttlePoint;
  }

  /**
   * @return Major compaction period from compaction.
   * Major compactions are selected periodically according to this parameter plus jitter
   */
  long getMajorCompactionPeriod() {
    return majorCompactionPeriod;
  }

  /**
   * @return Major the jitter fraction, the fraction within which the major compaction
   *  period is randomly chosen from the majorCompactionPeriod in each store.
   */
  float getMajorCompactionJitter() {
    return majorCompactionJitter;
  }

  /**
   * @return Whether expired files should be deleted ASAP using compactions
   */
  boolean shouldDeleteExpired() {
    return shouldDeleteExpired;
  }
}
