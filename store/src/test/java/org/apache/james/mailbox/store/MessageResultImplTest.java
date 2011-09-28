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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.james.mailbox.store.mail.model.Message;
import org.junit.Before;
import org.junit.Test;

public class MessageResultImplTest {
    private MessageResultImpl nameA;
    private MessageResultImpl nameACopy;
    private MessageResultImpl nameB;
    private MessageResultImpl nameC;

    /**
     * Initialize name instances
     */
    @Before
    public void initNames() throws Exception
    {
        Message<Long> msgA = buildMessage(100);
        Message<Long> msgB = buildMessage(100);
        Message<Long> msgC = buildMessage(200);
        
        nameA = new MessageResultImpl(msgA);
        nameACopy = new MessageResultImpl(msgA);
        nameB = new MessageResultImpl(msgB);
        nameC = new MessageResultImpl(msgC);
    }


    private Message<Long> buildMessage(int uid) throws Exception {
        MessageBuilder builder = new MessageBuilder();
        builder.uid = uid;
        return builder.build();
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse(nameA.equals(null));
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals(nameA, nameA);
    }


    @Test
    public void testCompareToReflexive() throws Exception
    {
        assertEquals(0, nameA.compareTo(nameA));
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals(nameA.hashCode(), nameA.hashCode());
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals(nameA, nameACopy);
        assertEquals(nameACopy, nameA);
    }


    @Test
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals(nameA.hashCode(), nameACopy.hashCode());
        assertEquals(nameACopy.hashCode(), nameA.hashCode());
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals(nameA, nameACopy);
        assertEquals(nameACopy, nameB);
        assertEquals(nameA, nameB);
    }


    @Test
    public void testCompareToTransitive() throws Exception
    {
        assertEquals(0, nameA.compareTo(nameACopy));
        assertEquals(0, nameACopy.compareTo(nameB));
        assertEquals(0, nameA.compareTo(nameB));
    }


    @Test
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals(nameA.hashCode(), nameACopy.hashCode());
        assertEquals(nameACopy.hashCode(), nameB.hashCode());
        assertEquals(nameA.hashCode(), nameB.hashCode());
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse(nameA.equals(nameC));
        assertFalse(nameC.equals(nameA));
    }

    @Test
    public void testShouldReturnPositiveWhenFirstGreaterThanSecond()
            throws Exception {
        assertTrue( nameC.compareTo(nameB) > 0);
    }

    @Test
    public void testShouldReturnNegativeWhenFirstLessThanSecond()
            throws Exception {
        assertTrue( nameB.compareTo(nameC) < 0);
    }
}