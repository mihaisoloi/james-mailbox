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
package org.apache.james.mailbox.copier;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.List;

import javax.mail.Flags;

import junit.framework.Assert;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.inmemory.mail.InMemoryCachingUidProvider;
import org.apache.james.mailbox.mock.MockMail;
import org.apache.james.mailbox.store.Authenticator;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link MailboxCopierImpl} implementation.
 * 
 * The InMemoryMailboxManager will be used as source and destination
 * Mailbox Manager.
 *
 */
public class MailboxCopierTest {
    
    /**
     * Number of Mailboxes to be created in the source Mailbox Manager.
     */
    private static final int MAILBOX_COUNT = 100;
    
    /**
     * Number of Messages per Mailbox to be created in the source Mailbox Manager.
     */
    private static final int MESSAGE_PER_MAILBOX_COUNT = 10;
    
    /**
     * The instance for the test mailboxCopier.
     */
    private MailboxCopierImpl mailboxCopier;
    
    /**
     * The instance for the source Mailbox Manager.
     */
    private MailboxManager srcMemMailboxManager;
    
    /**
     * The instance for the destination Mailbox Manager.
     */
    private MailboxManager dstMemMailboxManager;
    
    /**
     * Setup the mailboxCopier and the source and destination
     * Mailbox Manager.
     * 
     * We use a InMemoryMailboxManager implementation.
     * 
     * @throws BadCredentialsException
     * @throws MailboxException
     */
    @Before
    public void setup() throws BadCredentialsException, MailboxException {
        
        mailboxCopier = new MailboxCopierImpl();
        
        srcMemMailboxManager = newInMemoryMailboxManager();
        dstMemMailboxManager = newInMemoryMailboxManager();
        
    }
    
    /**
     * Feed the source MailboxManager with the number of mailboxes and
     * messages per mailbox.
     * 
     * Copy the mailboxes to the destination Mailbox Manager, and assert the number 
     * of mailboxes and messages per mailbox is the same as in the source
     * Mailbox Manager.
     * 
     * @throws MailboxException 
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void testMailboxCopy() throws MailboxException, UnsupportedEncodingException {
        
        feedSrcMailboxManager();

        assertMailboxManagerSize(srcMemMailboxManager);
        
        mailboxCopier.copyMailboxes(srcMemMailboxManager, dstMemMailboxManager);

        assertMailboxManagerSize(dstMemMailboxManager);
        
    }
    
    /**
     * Utility method to assert the number of mailboxes and messages per mailbox
     * are the ones expected.
     * 
     * @throws MailboxException 
     * @throws BadCredentialsException 
     */
    private void assertMailboxManagerSize(MailboxManager mailboxManager) throws BadCredentialsException, MailboxException {
        
        MailboxSession mailboxSession = mailboxManager.createSystemSession("manager", new SimpleLog("src-mailbox-copier"));        
        mailboxManager.startProcessingRequest(mailboxSession);
        List<MailboxPath> mailboxPathList = mailboxManager.list(mailboxSession);
        Assert.assertEquals(MAILBOX_COUNT, mailboxPathList.size());
        for (MailboxPath mailboxPath: mailboxPathList) {
            MessageManager messageManager = mailboxManager.getMailbox(mailboxPath, mailboxSession);
            Assert.assertEquals(MESSAGE_PER_MAILBOX_COUNT, messageManager.getMessageCount(mailboxSession));
        }
        mailboxManager.endProcessingRequest(mailboxSession);
        mailboxManager.logout(mailboxSession, true);
        
    }
    
    /**
     * Utility method to feed a Mailbox Manager with a number of 
     * mailboxes and messages per mailbox.
     * 
     * @throws MailboxException
     * @throws UnsupportedEncodingException
     */
    private void feedSrcMailboxManager() throws MailboxException, UnsupportedEncodingException {

        MessageManager messageManager = null;
        MailboxPath mailboxPath = null;
        
        for (int i=0; i < MAILBOX_COUNT; i++) {

            MailboxSession srcMailboxSession = srcMemMailboxManager.createSystemSession("user" + i, new SimpleLog("src-mailbox-copier"));        

            srcMemMailboxManager.startProcessingRequest(srcMailboxSession);
            mailboxPath = new MailboxPath("#private", "user" + i, "INBOX");
            srcMemMailboxManager.createMailbox(mailboxPath, srcMailboxSession);
            messageManager = srcMemMailboxManager.getMailbox(mailboxPath, srcMailboxSession);
            for (int j=0; j < MESSAGE_PER_MAILBOX_COUNT; j++) {
                messageManager.appendMessage(new ByteArrayInputStream(MockMail.MAIL_TEXT_PLAIN.getBytes("UTF-8")), 
                        Calendar.getInstance().getTime(), 
                        srcMailboxSession, 
                        true, 
                        new Flags(Flags.Flag.RECENT));
            }
            
            srcMemMailboxManager.endProcessingRequest(srcMailboxSession);
            srcMemMailboxManager.logout(srcMailboxSession, true);
        
        }
        
    }
    
    /**
     * Utility method to instanciate a new InMemoryMailboxManger with 
     * the needed MailboxSessionMapperFactory, Authenticator and UidProvider.
     * 
     * @return a new InMemoryMailboxManager
     */
    private MailboxManager newInMemoryMailboxManager() {
    
        return new InMemoryMailboxManager(
            new InMemoryMailboxSessionMapperFactory(), 
            new Authenticator() {
                public boolean isAuthentic(String userid, CharSequence passwd) {
                    return true;
                }
            }, 
            new InMemoryCachingUidProvider());
    
    }

}
