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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.mailbox.AbstractStressTest;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.jcr.mail.JCRModSeqProvider;
import org.apache.james.mailbox.jcr.mail.JCRUidProvider;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class JCRStressTest extends AbstractStressTest {
    
    private JCRMailboxManager mailboxManager;
    private RepositoryImpl repository;
    private static final String JACKRABBIT_HOME = "target/jackrabbit";
   
    @Before
    public void setUp() throws RepositoryException, MailboxException {

        new File(JACKRABBIT_HOME).delete();

        String user = "user";
        String pass = "pass";
        String workspace = null;
        RepositoryConfig config = RepositoryConfig.create(new InputSource(this.getClass().getClassLoader().getResourceAsStream("test-repository.xml")), JACKRABBIT_HOME);
        repository = RepositoryImpl.create(config);

        // Register imap cnd file
        JCRUtils.registerCnd(repository, workspace, user, pass);
        MailboxSessionJCRRepository sessionRepos = new GlobalMailboxSessionJCRRepository(repository, workspace, user, pass);
        JVMMailboxPathLocker locker = new JVMMailboxPathLocker();
        JCRUidProvider uidProvider = new JCRUidProvider(locker, sessionRepos);
        JCRModSeqProvider modSeqProvider= new JCRModSeqProvider(locker, sessionRepos);
        JCRMailboxSessionMapperFactory mf = new JCRMailboxSessionMapperFactory(sessionRepos, uidProvider, modSeqProvider);
        MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
        GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();

        mailboxManager = new JCRMailboxManager(mf, null, locker, aclResolver, groupMembershipResolver);
        mailboxManager.init();

    }
    
    @After
    public void tearDown() {
        MailboxSession session = mailboxManager.createSystemSession("test", LoggerFactory.getLogger("Test"));
        session.close();
        repository.shutdown();
        new File(JACKRABBIT_HOME).delete();
    }

    @Override
    protected MailboxManager getMailboxManager() {
        return mailboxManager;
    }
 
}
