/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.store;

import static org.junit.Assert.*;

import org.apache.james.mailbox.store.ResultUtils;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.junit.Test;

public class MessageRowUtilsTest {    
    
    @Test
    public void testShouldReturnPositiveWhenFirstGreaterThanSecond()
            throws Exception {
        MailboxMembership<Long> one = buildMessage(100);
        MailboxMembership<Long> two = buildMessage(99);
        assertTrue(ResultUtils.getUidComparator().compare(one, two) > 0);
    }

    private MailboxMembership<Long> buildMessage(int uid) throws Exception {
        MessageBuilder builder = new MessageBuilder();
        builder.uid = uid;
        return builder.build();
    }

    @Test
    public void testShouldReturnNegativeWhenFirstLessThanSecond()
            throws Exception {
        MailboxMembership<Long> one = buildMessage(98);
        MailboxMembership<Long> two = buildMessage(99);
        assertTrue(ResultUtils.getUidComparator().compare(one, two) < 0);
    }

    @Test
    public void testShouldReturnZeroWhenFirstEqualsSecond() throws Exception {
        MailboxMembership<Long> one = buildMessage(90);
        MailboxMembership<Long> two = buildMessage(90);
        assertEquals(0, ResultUtils.getUidComparator().compare(one, two));
    }
}
