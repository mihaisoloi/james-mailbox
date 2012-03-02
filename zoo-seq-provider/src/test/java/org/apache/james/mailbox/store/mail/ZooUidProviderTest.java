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
package org.apache.james.mailbox.store.mail;

import java.util.UUID;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Test for UID provider.
 */
public class ZooUidProviderTest {

    /**
     * Test of nextUid method, of class ZooUidProvider.
     */
    @Test
    public void testNextUid() throws Exception {
        System.out.println("nextUid");
        MailboxSession session = null;
        Mailbox<UUID> mailbox = null;
        ZooUidProvider instance = null;
        long expResult = 0L;
        long result = instance.nextUid(session, mailbox);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of lastUid method, of class ZooUidProvider.
     */
    @Test
    public void testLastUid() throws Exception {
        System.out.println("lastUid");
        MailboxSession session = null;
        Mailbox<UUID> mailbox = null;
        ZooUidProvider instance = null;
        long expResult = 0L;
        long result = instance.lastUid(session, mailbox);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of close method, of class ZooUidProvider.
     */
    @Test
    public void testClose() throws Exception {
        System.out.println("close");
        ZooUidProvider instance = null;
        instance.close();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
