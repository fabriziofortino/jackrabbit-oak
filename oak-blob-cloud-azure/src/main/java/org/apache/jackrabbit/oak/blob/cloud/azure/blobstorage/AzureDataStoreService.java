/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.jackrabbit.oak.blob.cloud.azure.blobstorage;

import org.apache.jackrabbit.oak.stats.StatisticsProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, name = AzureDataStoreService.NAME)
public class AzureDataStoreService extends AbstractAzureDataStoreService {

    @Reference
    private StatisticsProvider statisticsProvider;

    public static final String NAME = "org.apache.jackrabbit.oak.plugins.blob.datastore.AzureDataStore";

    protected StatisticsProvider getStatisticsProvider(){
        return statisticsProvider;
    }

    protected void setStatisticsProvider(StatisticsProvider statisticsProvider) {
        this.statisticsProvider = statisticsProvider;
    }
}
