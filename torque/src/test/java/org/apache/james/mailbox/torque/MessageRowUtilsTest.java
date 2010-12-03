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

package org.apache.james.mailbox.torque;

import static org.junit.Assert.*;

import org.apache.james.mailbox.torque.MessageRowUtils;
import org.apache.james.mailbox.torque.om.MessageRow;
import org.junit.Test;

public class MessageRowUtilsTest  {


    @Test
    public void testShouldReturnPositiveWhenFirstGreaterThanSecond()
            throws Exception {
        MessageRow one = new MessageRow();
        one.setUid(100);
        MessageRow two = new MessageRow();
        two.setUid(99);
        assertTrue(MessageRowUtils.getUidComparator().compare(one, two) > 0);
    }

    @Test
    public void testShouldReturnNegativeWhenFirstLessThanSecond()
            throws Exception {
        MessageRow one = new MessageRow();
        one.setUid(98);
        MessageRow two = new MessageRow();
        two.setUid(99);
        assertTrue(MessageRowUtils.getUidComparator().compare(one, two) < 0);
    }

    @Test
    public void testShouldReturnZeroWhenFirstEqualsSecond() throws Exception {
        MessageRow one = new MessageRow();
        one.setUid(90);
        MessageRow two = new MessageRow();
        two.setUid(90);
        assertEquals(0, MessageRowUtils.getUidComparator().compare(one, two));
    }
}
