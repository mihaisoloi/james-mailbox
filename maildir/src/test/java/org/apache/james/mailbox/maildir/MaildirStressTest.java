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

import org.apache.commons.io.FileUtils;
import org.apache.james.imap.functional.AbstractStressTest;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.maildir.MaildirMailboxManager;
import org.apache.james.mailbox.maildir.MaildirMailboxSessionMapperFactory;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.junit.After;
import org.junit.Before;

public class MaildirStressTest extends AbstractStressTest {

    private static final String MAILDIR_HOME = "target/Maildir";
    private MaildirMailboxManager mailboxManager;
    
    @Before
    public void setUp() throws MailboxException {
        MaildirStore store = new MaildirStore(MAILDIR_HOME + "/%user");

        MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
        mailboxManager = new MaildirMailboxManager(mf, null, store);
        mailboxManager.init();

    }
    
    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(MAILDIR_HOME));
    }

    @Override
    public void testStessTest() throws InterruptedException, MailboxException {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            super.testStessTest();
        }
    }

    @Override
    protected MailboxManager getMailboxManager() {
        return mailboxManager;
    }

}
