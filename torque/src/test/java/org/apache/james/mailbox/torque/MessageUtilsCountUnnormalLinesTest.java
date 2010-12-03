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

import org.apache.james.mailbox.torque.MessageUtils;
import org.junit.Test;

public class MessageUtilsCountUnnormalLinesTest {

    @Test
    public void testEmpty() throws Exception {
        assertEquals("Check processing of empty array", 0, MessageUtils
                .countUnnormalLines("".getBytes()));
    }

    @Test
    public void testNormal() throws Exception {
        assertEquals("Check processing of normal data", 0, MessageUtils
                .countUnnormalLines("One\r\nTwo\r\nThree\r\n".getBytes()));
    }

    @Test
    public void testMissing() throws Exception {
        assertEquals("Check processing simple data containing unnormal lines",
                2, MessageUtils.countUnnormalLines("One\rTwo\nThree\r\n"
                        .getBytes()));
    }

    @Test
    public void testBoundaries() throws Exception {
        assertEquals("CR at end", 1, MessageUtils
                .countUnnormalLines("One\r\nTwo\r\nThree\r".getBytes()));
        assertEquals("LF at end", 1, MessageUtils
                .countUnnormalLines("One\r\nTwo\r\nThree\n".getBytes()));
        assertEquals("CR at start", 1, MessageUtils
                .countUnnormalLines("\rOne\r\nTwo\r\nThree".getBytes()));
        assertEquals("LF at start", 1, MessageUtils
                .countUnnormalLines("\nOne\r\nTwo\r\nThree".getBytes()));
    }

    @Test
    public void testSwitchOrder() throws Exception {
        assertEquals("Check processing simple data containing unnormal lines",
                8, MessageUtils
                        .countUnnormalLines("\n\rOne\n\rTwo\n\rThree\n\r"
                                .getBytes()));
    }
}
