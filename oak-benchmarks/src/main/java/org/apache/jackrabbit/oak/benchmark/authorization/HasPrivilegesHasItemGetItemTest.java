/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.benchmark.authorization;

import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

public class HasPrivilegesHasItemGetItemTest extends AbstractHasItemGetItemTest {

    public HasPrivilegesHasItemGetItemTest(int itemsToRead, int numberOfACEs, int numberOfGroups, boolean doReport) {
        super(itemsToRead, numberOfACEs, numberOfGroups, doReport);
    }

    @NotNull
    @Override
    String additionalMethodName() {
        return "hasPrivileges";
    }

    @Override
    void additionalOperations(@NotNull String path, @NotNull Session s, @NotNull AccessControlManager acMgr) {
        try {
            acMgr.hasPrivileges(getAccessControlledPath(path), (Privilege[]) Utils.getRandom(allPrivileges, 3).toArray(new Privilege[0]));
        } catch (RepositoryException e) {
            if (doReport) {
                e.printStackTrace(System.out);
            }
        }
    }

    @NotNull
    @Override
    protected String getTestNodeName() {
        return "HasPrivilegesHasItemGetItemTest";
    }
}