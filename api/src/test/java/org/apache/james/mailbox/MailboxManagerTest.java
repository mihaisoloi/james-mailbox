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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.mail.Flags;

import junit.framework.Assert;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.junit.Test;

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
    private static MailboxManager mailboxManager;
    
    /**
     * Number of Mailboxes to be created in the Mailbox Manager.
     */
    private static final int MAILBOX_COUNT = 10;
    
    /**
     * Number of Messages per Mailbox to be created in the Mailbox Manager.
     */
    private static final int MESSAGE_PER_MAILBOX_COUNT = 10;
    
    /**
     * @throws UnsupportedEncodingException 
     * @throws MailboxException 
     */
    @Test
    public void testList() throws MailboxException, UnsupportedEncodingException {

        feedMailboxManager();

        MailboxSession mailboxSession = getMailboxManager().createSystemSession("manager", new SimpleLog("testList"));
        getMailboxManager().startProcessingRequest(mailboxSession);
        Assert.assertEquals(getMailboxManager().list(mailboxSession).size(), MAILBOX_COUNT);

    }
    
    /**
     * Utility method to feed the Mailbox Manager with a number of 
     * mailboxes and messages per mailbox.
     * 
     * @throws MailboxException
     * @throws UnsupportedEncodingException
     */
    private void feedMailboxManager() throws MailboxException, UnsupportedEncodingException {

        MessageManager messageManager = null;
        MailboxPath mailboxPath = null;
        
        for (int i=0; i < MAILBOX_COUNT; i++) {

            MailboxSession mailboxSession = getMailboxManager().createSystemSession("user" + i, new SimpleLog("testList-feeder"));        

            getMailboxManager().startProcessingRequest(mailboxSession);
            mailboxPath = new MailboxPath("#private", "user" + i, "INBOX");
            getMailboxManager().createMailbox(mailboxPath, mailboxSession);
            messageManager = getMailboxManager().getMailbox(mailboxPath, mailboxSession);
            for (int j=0; j < MESSAGE_PER_MAILBOX_COUNT; j++) {
                messageManager.appendMessage(new ByteArrayInputStream(new String("fake message" + i).getBytes("UTF-8")), 
                        Calendar.getInstance().getTime(), 
                        mailboxSession, 
                        true, 
                        new Flags(Flags.Flag.RECENT));
            }
            
            getMailboxManager().endProcessingRequest(mailboxSession);
            getMailboxManager().logout(mailboxSession, true);
        
        }
        
    }
    
    /**
     * Setter to inject the mailboxManager.
     */
    protected static void setMailboxManager(MailboxManager mailboxManager) {
        MailboxManagerTest.mailboxManager = mailboxManager;
    }

    /**
     * Accessor to the mailboxManager.
     * 
     * @return the mailboxManager instance.
     * @throws IllegalStateException in case of null mailboxManager
     */
    protected static MailboxManager getMailboxManager() {
        if (mailboxManager == null) {
            throw new IllegalStateException("Please setMailboxManager with a non null value before requesting getMailboxManager()");
        }
        return mailboxManager;
    }

}
