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
package org.apache.james.mailbox.inmemory;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.imap.functional.InMemoryUserManager;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.inmemory.mail.InMemoryCachingUidProvider;
import org.junit.After;
import org.junit.Before;

/**
 * InMemoryMailboxManagerTest that extends the MailboxManagerTest.
 */
public class InMemoryMailboxManagerTest extends MailboxManagerTest {
    
    /**
     * Setup the mailboxManager.
     * 
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        createMailboxManager();
    }
    
    /**
     * Close the system session and entityManagerFactory
     * 
     * @throws MailboxException 
     * @throws BadCredentialsException 
     */
    @After
    public void tearDown() throws BadCredentialsException, MailboxException {
        MailboxSession session = getMailboxManager().createSystemSession("test", new SimpleLog("Test"));
        session.close();
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManagerTest#createMailboxManager()
     */
    @Override
    protected void createMailboxManager() throws MailboxException {
        
        InMemoryUserManager userManager = new InMemoryUserManager();
        InMemoryMailboxSessionMapperFactory factory = new InMemoryMailboxSessionMapperFactory();
        InMemoryCachingUidProvider uidProvider = new InMemoryCachingUidProvider();
        InMemoryMailboxManager mailboxManager = new InMemoryMailboxManager(factory, userManager, uidProvider);
        mailboxManager.init();
        
        setMailboxManager(mailboxManager);

    }
    
}
    