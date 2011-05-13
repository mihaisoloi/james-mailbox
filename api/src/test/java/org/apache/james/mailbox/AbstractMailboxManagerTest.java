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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;


/**
 * Abstract base class for {@link MailboxManager} tests
 * 
 *
 */
public abstract class AbstractMailboxManagerTest {

    protected abstract MailboxManager createMailboxManager();
    private final static String USER1 = "USER1";
    
    @Test
    public void testBasicOperations() throws BadCredentialsException, MailboxException {
        MailboxManager manager = createMailboxManager();
        
        MailboxSession session = manager.createSystemSession(USER1, LoggerFactory.getLogger("Mock"));
        Assert.assertEquals(USER1, session.getUser().getUserName());
        
        manager.startProcessingRequest(session);
        
        
        MailboxPath inbox = MailboxPath.inbox(session);
        Assert.assertFalse(manager.mailboxExists(inbox, session));
        
        manager.createMailbox(inbox, session);
        Assert.assertTrue(manager.mailboxExists(inbox, session));
        
        try {
            manager.createMailbox(inbox, session);
            Assert.fail();
        } catch (MailboxException e) {
            // mailbox already exists!
        }
        
        MailboxPath inboxSubMailbox = new MailboxPath(inbox, "INBOX.Test");
        Assert.assertFalse(manager.mailboxExists(inboxSubMailbox, session));
        
        manager.createMailbox(inboxSubMailbox, session);
        Assert.assertTrue(manager.mailboxExists(inboxSubMailbox, session));
        
        manager.deleteMailbox(inbox, session);
        Assert.assertFalse(manager.mailboxExists(inbox, session));
        Assert.assertTrue(manager.mailboxExists(inboxSubMailbox, session));
        
        manager.deleteMailbox(inboxSubMailbox, session);
        Assert.assertFalse(manager.mailboxExists(inboxSubMailbox, session));

        manager.logout(session, false);
        manager.endProcessingRequest(session);

        Assert.assertFalse(session.isOpen());
    }
}
