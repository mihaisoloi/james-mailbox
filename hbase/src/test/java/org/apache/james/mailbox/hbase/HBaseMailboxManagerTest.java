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

package org.apache.james.mailbox.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.james.mailbox.AbstractMailboxManagerTest;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxACLResolver;
import org.apache.james.mailbox.MailboxACLResolver.GroupMembershipResolver;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.UnionMailboxACLResolver;
import org.apache.james.mailbox.hbase.mail.HBaseModSeqProvider;
import org.apache.james.mailbox.hbase.mail.HBaseUidProvider;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 * HBaseMailboxManagerTest that extends the StoreMailboxManagerTest.
 *  
 */
public class HBaseMailboxManagerTest extends AbstractMailboxManagerTest {

    private Configuration conf;
    /**
     * Setup the mailboxManager.
     
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        conf = HBaseClusterSingleton.build();
        HBaseClusterSingleton.resetTables(conf);
        createMailboxManager();
    }

    /**
     * Close the system session and entityManagerFactory
     * 
     * @throws MailboxException 
     * @throws BadCredentialsException 
     */
    @After
    public void tearDown() throws Exception {
        deleteAllMailboxes();
        MailboxSession session = getMailboxManager().createSystemSession("test", LoggerFactory.getLogger("Test"));
        session.close();
        HBaseClusterSingleton.resetTables(conf);
    }

    /* (non-Javadoc)i deve
     * @see org.apache.james.mailbox.MailboxManagerTest#createMailboxManager()
     */
    @Override
    protected void createMailboxManager() throws MailboxException{
        HBaseUidProvider uidProvider = new HBaseUidProvider(conf);
        HBaseModSeqProvider modSeqProvider = new HBaseModSeqProvider(conf);
        HBaseMailboxSessionMapperFactory mf = new HBaseMailboxSessionMapperFactory(conf, uidProvider, modSeqProvider);
        
        MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
        GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();
        
        HBaseMailboxManager mailboxManagerLocal = new HBaseMailboxManager(mf, null, aclResolver, groupMembershipResolver);
        mailboxManagerLocal.init();

        setMailboxManager(mailboxManagerLocal);

        deleteAllMailboxes();
    }

    private void deleteAllMailboxes() throws BadCredentialsException, MailboxException {
        MailboxSession session = getMailboxManager().createSystemSession("test", LoggerFactory.getLogger("Test"));
        try {
            ((HBaseMailboxManager) mailboxManager).deleteEverything(session);
        } catch (MailboxException e) {
            e.printStackTrace();
        }
        session.close();
    }
}
