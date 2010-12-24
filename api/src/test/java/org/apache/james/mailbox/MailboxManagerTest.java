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
import org.apache.james.mailbox.mock.MockMail;
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
     * Number of Domains to be created in the Mailbox Manager.
     */
    private static final int DOMAIN_COUNT = 5;
    
    /**
     * Number of Users (with INBOX) to be created in the Mailbox Manager.
     */
    private static final int USER_COUNT = 5;
    
    /**
     * Number of Sub Mailboxes (mailbox in another mailbox) to be created in the Mailbox Manager.
     */
    private static final int SUB_MAILBOXES_COUNT = 5;
    
    /**
     * Number of Messages per Mailbox to be created in the Mailbox Manager.
     */
    private static final int MESSAGE_PER_MAILBOX_COUNT = 5;
    
    /**
     * Create some INBOXes and their sub mailboxes and assert list() method.
     * 
     * @throws UnsupportedEncodingException 
     * @throws MailboxException 
     */
    @Test
    public void testList() throws MailboxException, UnsupportedEncodingException {

        feedMailboxManager();

        MailboxSession mailboxSession = getMailboxManager().createSystemSession("manager", new SimpleLog("testList"));
        getMailboxManager().startProcessingRequest(mailboxSession);
        Assert.assertEquals(DOMAIN_COUNT * 
                  (USER_COUNT + // INBOX
                  USER_COUNT * MESSAGE_PER_MAILBOX_COUNT + // INBOX.SUB_FOLDER
                  USER_COUNT * MESSAGE_PER_MAILBOX_COUNT * MESSAGE_PER_MAILBOX_COUNT),  // INBOX.SUB_FOLDER.SUBSUB_FOLDER
                getMailboxManager().list(mailboxSession).size());

    }
    
    /**
     * Utility method to feed the Mailbox Manager with a number of 
     * mailboxes and messages per mailbox.
     * 
     * @throws MailboxException
     * @throws UnsupportedEncodingException
     */
    private void feedMailboxManager() throws MailboxException, UnsupportedEncodingException {

        MailboxPath mailboxPath = null;
        
        for (int i=0; i < DOMAIN_COUNT; i++) {

            for (int j=0; j < USER_COUNT; j++) {
                
                String user = "user" + j + "@localhost" + i;
                
                String folderName = "INBOX";

                MailboxSession mailboxSession = getMailboxManager().createSystemSession(user, new SimpleLog("mailboxmanager-test"));
                mailboxPath = new MailboxPath("#private", user, folderName);
                createMailbox(mailboxSession, mailboxPath);
                
                for (int k=0; k < SUB_MAILBOXES_COUNT; k++) {
                    
                    folderName = folderName + ".SUB_FOLDER_" + k;
                    mailboxPath = new MailboxPath("#private", user, folderName);
                    createMailbox(mailboxSession, mailboxPath);
                    
                    for (int l=0; l < SUB_MAILBOXES_COUNT; l++) {

                        folderName = folderName + ".SUBSUB_FOLDER_" + l;
                        mailboxPath = new MailboxPath("#private", user, folderName);
                        createMailbox(mailboxSession, mailboxPath);

                    }
                        
                }

                getMailboxManager().logout(mailboxSession, true);
        
            }
            
        }
        
    }
    
    /**
     * 
     * @param mailboxPath
     * @throws MailboxException
     * @throws UnsupportedEncodingException 
     */
    private void createMailbox(MailboxSession mailboxSession, MailboxPath mailboxPath) throws MailboxException, UnsupportedEncodingException {
        getMailboxManager().startProcessingRequest(mailboxSession);
        getMailboxManager().createMailbox(mailboxPath, mailboxSession);
        MessageManager messageManager = getMailboxManager().getMailbox(mailboxPath, mailboxSession);
        for (int j=0; j < MESSAGE_PER_MAILBOX_COUNT; j++) {
            messageManager.appendMessage(new ByteArrayInputStream(MockMail.MAIL_TEXT_PLAIN.getBytes("UTF-8")), 
                    Calendar.getInstance().getTime(), 
                    mailboxSession, 
                    true, 
                    new Flags(Flags.Flag.RECENT));
        }
        getMailboxManager().endProcessingRequest(mailboxSession);
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
