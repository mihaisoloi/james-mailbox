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
package org.apache.james.mailbox.functional.maildir;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.maildir.MaildirMailboxManager;
import org.apache.james.mailbox.maildir.MaildirMailboxSessionMapperFactory;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        FileUtils.deleteDirectory(new File(MAILDIR_HOME));
    }
    
    /**
     * Delete Maildir directory after test.
     * 
     * @throws IOException 
     */
    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(MAILDIR_HOME));
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
            
            // TODO Tests fail with /%user and /%fulluser configuration
//            doTestListWithMaildirStoreConfiguration("/%user");
//            doTestListWithMaildirStoreConfiguration("/%fulluser");

        }
            
    }
    
    private void doTestListWithMaildirStoreConfiguration(String maildirStoreConfiguration) throws MailboxException, UnsupportedEncodingException {
        MaildirStore store = new MaildirStore(MAILDIR_HOME + maildirStoreConfiguration);
        MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
        setMailboxManager(new MaildirMailboxManager(mf, null, store));
        super.testList();
        try {
            tearDown();
        } catch (IOException e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Override
    protected void createMailboxManager() {
        // Do nothing, the maildir mailboxManager is created in the test method.
    }

}
