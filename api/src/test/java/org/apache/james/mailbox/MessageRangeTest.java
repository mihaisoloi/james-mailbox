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
package org.apache.james.mailbox;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

public class MessageRangeTest {

    @Test
    public void testToRanges() {
        List<MessageRange> ranges = MessageRange.toRanges(Arrays.asList(1L,2L,3L,5L,6L,9L));
        assertEquals(3, ranges.size());
        checkRange(1, 3, ranges.get(0));
        checkRange(5, 6, ranges.get(1));
        checkRange(9, 9, ranges.get(2));

    }
    
    @Test
    public void testOneUidToRange() {
        List<MessageRange> ranges = MessageRange.toRanges(Arrays.asList(1L));
        assertEquals(1, ranges.size());
        checkRange(1, 1, ranges.get(0));
    }
    
    private void checkRange(long from, long to, MessageRange range) {
        assertEquals(from, range.getUidFrom());
        assertEquals(to, range.getUidTo());
    }
}
