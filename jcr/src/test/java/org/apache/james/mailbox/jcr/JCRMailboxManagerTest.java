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
package org.apache.james.mailbox.jcr;

import java.io.File;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jcr.mail.JCRCachingUidProvider;
import org.junit.After;
import org.junit.Before;
import org.xml.sax.InputSource;

/**
 * JCRMailboxManagerTest that extends the StoreMailboxManagerTest.
 */
public class JCRMailboxManagerTest extends MailboxManagerTest {
    
    private static final String JACKRABBIT_HOME = "target/jackrabbit";
    
    public static final String META_DATA_DIRECTORY = "target/user-meta-data";

    private static RepositoryImpl repository;
   
    /**
     * Setup the mailboxManager.
     * 
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
    
        new File(JACKRABBIT_HOME).delete();

        String user = "user";
        String pass = "pass";
        String workspace = null;
        RepositoryConfig config = RepositoryConfig.create(new InputSource(JCRMailboxManagerTest.class.getClassLoader().getResourceAsStream("test-repository.xml")), JACKRABBIT_HOME);
        repository = RepositoryImpl.create(config);

        // Register imap cnd file
        JCRUtils.registerCnd(repository, workspace, user, pass);
        MailboxSessionJCRRepository sessionRepos = new GlobalMailboxSessionJCRRepository(repository, workspace, user, pass);

        JCRCachingUidProvider uidProvider = new JCRCachingUidProvider(sessionRepos);

        JCRMailboxSessionMapperFactory mf = new JCRMailboxSessionMapperFactory(sessionRepos);
        setMailboxManager(new JCRMailboxManager(mf, null, uidProvider));

    }

    
    /**
     * Close system session and shutdown system repository.
     */
    @After
    public void tearDown() throws BadCredentialsException, MailboxException {
        MailboxSession session = getMailboxManager().createSystemSession("test", new SimpleLog("Test"));
        session.close();
        repository.shutdown();
        new File(JACKRABBIT_HOME).delete();
    }
    
    /* (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManagerTest#setMailboxManager(org.apache.james.mailbox.MailboxManager)
     */
    protected void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

}
