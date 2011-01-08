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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManagerTest;
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
        deleteMaildirTestDirectory();
    }
    
    /**
     * Delete Maildir directory after test.
     * 
     * @throws IOException 
     */
    @After
    public void tearDown() throws IOException {
        deleteMaildirTestDirectory();
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
            // doTestListWithMaildirStoreConfiguration("/%user");

            // TODO Tests fail with /%fulluser configuration
            // doTestListWithMaildirStoreConfiguration("/%fulluser");

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
        super.testList();
        try {
            deleteMaildirTestDirectory();
        } catch (IOException e) {
            Assert.fail();
            e.printStackTrace();
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
