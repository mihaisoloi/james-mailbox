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
package org.apache.james.mailbox.maildir;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.mail.Flags;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * MaildirMailboxManagerTest that extends the StoreMailboxManagerTest.
 */
public class MaildirMailboxManagerTest extends MailboxManagerTest {
    
    private static final String MAILDIR_HOME = "target/Maildir";

    /**
     * Setup the mailboxManager.
     * 
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
          deleteMaildirTestDirectory();
        }
    }
    
    /**
     * Delete Maildir directory after test.
     * 
     * @throws IOException 
     */
    @After
    public void tearDown() throws IOException {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
          deleteMaildirTestDirectory();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManagerTest#testList()
     */
    @Test
    public void testList() throws MailboxException, UnsupportedEncodingException {
        
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {

            doTestListWithMaildirStoreConfiguration("/%domain/%user");

            // TODO Tests fail with /%user configuration
            try {
                doTestListWithMaildirStoreConfiguration("/%user");
                fail();
            } catch (MailboxExistsException e) {
                // This is expected as the there are many users which have the same localpart
            }
            // TODO Tests fail with /%fulluser configuration
            doTestListWithMaildirStoreConfiguration("/%fulluser");

        }
            
    }
    
    // See MAILBOX-31
    @Test
    public void testCreateSubFolder() throws MailboxException {

        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {

            MaildirStore store = new MaildirStore(MAILDIR_HOME + "/%domain/%user");
            MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
            MaildirMailboxManager manager = new MaildirMailboxManager(mf, null, store);
    
            manager.init();
    
            String user = "test@localhost";
            MailboxSession session = manager.createSystemSession(user, LoggerFactory.getLogger("Test"));
            manager.createMailbox(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "Trash"), session);
            manager.createMailbox(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "INBOX.testfolder"), session);
            
            // this threw NPE
            manager.getMailbox(MailboxPath.inbox(user), session).appendMessage(new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), session, false, new Flags());

        }

    }

    /**
     * Create the maildirStore with the provided configuration and executes the list() tests.
     * Cleans the generated artifacts.
     * 
     * @param maildirStoreConfiguration
     * @throws MailboxException
     * @throws UnsupportedEncodingException
     */
    private void doTestListWithMaildirStoreConfiguration(String maildirStoreConfiguration) throws MailboxException, UnsupportedEncodingException {
        MaildirStore store = new MaildirStore(MAILDIR_HOME + maildirStoreConfiguration);
        MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
        MaildirMailboxManager manager = new MaildirMailboxManager(mf, null, store);
        manager.init();
        setMailboxManager(manager);
        try {
            super.testList();
        } finally {
            try {
                deleteMaildirTestDirectory();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManagerTest#createMailboxManager()
     */
    @Override
    protected void createMailboxManager() {
        // Do nothing, the maildir mailboxManager is created in the test method.
    }
   
    /**
     * Utility method to delete the test Maildir Directory.
     * 
     * @throws IOException
     */
    private void deleteMaildirTestDirectory() throws IOException {
        FileUtils.deleteDirectory(new File(MAILDIR_HOME));
    }
    
}
