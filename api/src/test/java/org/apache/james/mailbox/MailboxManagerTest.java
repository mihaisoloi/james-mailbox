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

import java.io.UnsupportedEncodingException;

import junit.framework.Assert;

import org.apache.james.mailbox.mock.MockMailboxManager;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link StoreMailboxManager} methods that 
 * are not covered by the protocol-tester suite.
 * 
 * This class needs to be extended by the different mailbox 
 * implementations which are responsible to setup and 
 * implement the test methods.
 * 
 */
public abstract class MailboxManagerTest {
    
    /**
     * The mailboxManager that needs to get instanciated
     * by the mailbox implementations.
     */
    protected MailboxManager mailboxManager;
    
    /**
     * Create some INBOXes and their sub mailboxes and assert list() method.
     * 
     * @throws UnsupportedEncodingException 
     * @throws MailboxException 
     */
    @Test
    public void testList() throws MailboxException, UnsupportedEncodingException {

        setMailboxManager(new MockMailboxManager(getMailboxManager()).getMockMailboxManager());

        MailboxSession mailboxSession = getMailboxManager().createSystemSession("manager", LoggerFactory.getLogger("testList"));
        getMailboxManager().startProcessingRequest(mailboxSession);
        Assert.assertEquals(MockMailboxManager.EXPECTED_MAILBOXES_COUNT, getMailboxManager().list(mailboxSession).size());

    }
    
    /**
     * Implement this method to create the mailboxManager.
     * 
     * @return
     * @throws MailboxException 
     */
    protected abstract void createMailboxManager() throws MailboxException;
    
    /**
     * Setter to inject the mailboxManager.
     */
    protected void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    /**
     * Accessor to the mailboxManager.
     * 
     * @return the mailboxManager instance.
     * @throws IllegalStateException in case of null mailboxManager
     */
    protected MailboxManager getMailboxManager() {
        if (mailboxManager == null) {
            throw new IllegalStateException("Please setMailboxManager with a non null value before requesting getMailboxManager()");
        }
        return mailboxManager;
    }

}
