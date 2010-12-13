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
import org.apache.james.mailbox.store.Authenticator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MailboxCopierTest {
    
    private static final int MAILBOX_COUNT = 100;
    
    private static final int MESSAGE_PER_MAILBOX_COUNT = 10;
    
    private MailboxCopierImpl mailboxCopier;
    
    private MailboxManager srcMemMailboxManager;
    
    private MailboxManager dstMemMailboxManager;
    
    @Before
    public void setup() throws BadCredentialsException, MailboxException {
        
        mailboxCopier = new MailboxCopierImpl();
        
        srcMemMailboxManager = newInMemoryMailboxManager();
        dstMemMailboxManager = newInMemoryMailboxManager();
        
        mailboxCopier.setSrcMailboxManager(srcMemMailboxManager);
        mailboxCopier.setDstMailboxManager(dstMemMailboxManager);
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * @param args
     * @throws MailboxException 
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void testMailboxCopy() throws MailboxException, UnsupportedEncodingException {
        
        feedSrcMailboxManager();

        assertMailboxManagerSize(srcMemMailboxManager);
        
        mailboxCopier.copyMailboxes();

        assertMailboxManagerSize(dstMemMailboxManager);
        
    }
    
    /**
     * @throws MailboxException 
     * @throws BadCredentialsException 
     * 
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
                messageManager.appendMessage(new ByteArrayInputStream(new String("fake message" + i).getBytes("UTF-8")), 
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
     * @return
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
