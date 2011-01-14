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
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.Flags;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.junit.Test;

public abstract class AbstractStressTest {

    private final static int APPEND_OPERATIONS = 200;
    
    
    protected abstract MailboxManager getMailboxManager();
    
    @Test
    public void testStessTest() throws InterruptedException, MailboxException {
       
        final CountDownLatch latch = new CountDownLatch(APPEND_OPERATIONS);
        final ExecutorService pool = Executors.newFixedThreadPool(APPEND_OPERATIONS/2);
        
        MailboxSession session = getMailboxManager().createSystemSession("test", new SimpleLog("Test"));
        getMailboxManager().startProcessingRequest(session);
        final MailboxPath path = new MailboxPath(MailboxConstants.USER_NAMESPACE, "username", "INBOX");
        getMailboxManager().createMailbox(path, session);
        getMailboxManager().endProcessingRequest(session);
        getMailboxManager().logout(session, false);
        final AtomicBoolean fail = new AtomicBoolean(false);
        
        // fire of 1000 append operations
        for (int i = 0 ; i < APPEND_OPERATIONS; i++) {
            pool.execute(new Runnable() {
                
                public void run() {
                    if (fail.get()){
                        latch.countDown();
                        return;
                    }
                    

                    try {
                        MailboxSession session = getMailboxManager().createSystemSession("test", new SimpleLog("Test"));

                        getMailboxManager().startProcessingRequest(session);
                        MessageManager m = getMailboxManager().getMailbox(path, session);
                        
                        System.out.println("Append message with uid=" + m.appendMessage(new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), session, false, new Flags()));
                        getMailboxManager().endProcessingRequest(session);
                        getMailboxManager().logout(session,false);
                    } catch (MailboxException e) {
                        e.printStackTrace();
                        fail.set(true);
                    } finally {
                        latch.countDown();
                    }
                    
                    
                }
            });
        }
        
        latch.await();
        
        org.junit.Assert.assertFalse("Unable to append all messages",fail.get());
        pool.shutdown();

        
    }
}
