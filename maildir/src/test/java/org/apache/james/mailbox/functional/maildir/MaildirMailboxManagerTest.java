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
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.maildir.MaildirMailboxManager;
import org.apache.james.mailbox.maildir.MaildirMailboxSessionMapperFactory;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
    @BeforeClass
    public static void setup() throws Exception {
        FileUtils.deleteDirectory(new File(MAILDIR_HOME));
        MaildirStore store = new MaildirStore(MAILDIR_HOME + "/%domain/%user");
        MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
        setMailboxManager(new MaildirMailboxManager(mf, null, store));
    }
    
    /**
     * Close the system session and entityManagerFactory
     * 
     * @throws MailboxException 
     * @throws BadCredentialsException 
     */
    @AfterClass
    public static void tearDown() throws BadCredentialsException, MailboxException {
//        FileUtils.deleteDirectory(new File(MAILDIR_HOME));
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManagerTest#testList()
     */
    @Test
    public void testList() throws MailboxException, UnsupportedEncodingException {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            super.testList();
        }
    }
    
}
