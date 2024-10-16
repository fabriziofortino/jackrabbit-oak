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
package org.apache.jackrabbit.oak.spi.security.user.action;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.namepath.NamePathMapper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.mockito.Mockito.verifyNoInteractions;

public class AbstractGroupActionTest {

    private final GroupAction groupAction = new AbstractGroupAction() {};

    private final User user = Mockito.mock(User.class);
    private final Group group = Mockito.mock(Group.class);

    private final Root root = Mockito.mock(Root.class);
    private final NamePathMapper namePathMapper = Mockito.mock(NamePathMapper.class);

    @Test
    public void testMemberAdded() throws Exception {
        groupAction.onMemberAdded(group, user, root, namePathMapper);
        verifyNoInteractions(group, user, root, namePathMapper);
    }

    @Test
    public void testMemberRemoved() throws Exception {
        groupAction.onMemberRemoved(group, user, root, namePathMapper);
        verifyNoInteractions(group, user, root, namePathMapper);
    }

    @Test
    public void testMembersAdded() throws Exception {
        groupAction.onMembersAdded(group, Set.of("user1", "user2"), Set.of(), root, namePathMapper);
        verifyNoInteractions(group, user, root, namePathMapper);
    }

    @Test
    public void testMembersAddedContentId() throws Exception {
        groupAction.onMembersAddedContentId(group, Set.of("user1", "user2"), Set.of(), root, namePathMapper);
        verifyNoInteractions(group, user, root, namePathMapper);
    }

    @Test
    public void testMembersRemoved() throws Exception {
        groupAction.onMembersRemoved(group, Set.of("user1", "user2"), Set.of(), root, namePathMapper);
        verifyNoInteractions(group, user, root, namePathMapper);
    }
}
