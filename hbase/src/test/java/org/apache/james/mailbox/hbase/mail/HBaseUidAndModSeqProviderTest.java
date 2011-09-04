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
package org.apache.james.mailbox.hbase.mail;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.hadoop.conf.Configuration;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.hbase.HBaseMailboxSessionMapperFactory;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for UidProvider. 
 * @author ieugen
 */
public class HBaseUidAndModSeqProviderTest {

    private static final Logger logger = Logger.getLogger("HBaseUidProviderTest");
    /** testing mini-cluster */
    private static Configuration conf;
    private static HBaseUidProvider uidProvider;
    private static HBaseModSeqProvider modSeqProvider;
    private static HBaseMailboxMapper mapper;
    private static HBaseMailboxSessionMapperFactory mapperFactory;
    private static List<HBaseMailbox> mailboxList;
    private static List<MailboxPath> pathsList;
    private static final int NAMESPACES = 5;
    private static final int USERS = 5;
    private static final int MAILBOXES = 5;
    private static final char SEPARATOR = '%';

    @BeforeClass
    public static void setUpClass() throws Exception {
        // start the test cluster 
        conf = HBaseClusterSingleton.build();
        HBaseClusterSingleton.resetTables(conf);
        uidProvider = new HBaseUidProvider(conf);
        modSeqProvider = new HBaseModSeqProvider(conf);
        mapperFactory = new HBaseMailboxSessionMapperFactory(conf, uidProvider, modSeqProvider);
        mapper = new HBaseMailboxMapper(conf);
        fillMailboxList();
        for (HBaseMailbox mailbox : mailboxList) {
            mapper.save(mailbox);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    private static void fillMailboxList() {
        mailboxList = new ArrayList<HBaseMailbox>();
        pathsList = new ArrayList<MailboxPath>();
        MailboxPath path;
        String name;
        for (int i = 0; i < NAMESPACES; i++) {
            for (int j = 0; j < USERS; j++) {
                for (int k = 0; k < MAILBOXES; k++) {
                    if (j == 3) {
                        name = "test" + SEPARATOR + "subbox" + k;
                    } else {
                        name = "mailbox" + k;
                    }
                    path = new MailboxPath("namespace" + i, "user" + j, name);
                    pathsList.add(path);
                    mailboxList.add(new HBaseMailbox(path, 13));
                }
            }
        }

        logger.log(Level.INFO, "Created test case with {0} mailboxes and {1} paths", new Object[]{mailboxList.size(), pathsList.size()});
    }

    /**
     * Test of lastUid method, of class HBaseUidProvider.
     */
    @Test
    public void testLastUid() throws Exception {
        System.out.println("lastUid");
        HBaseMailbox mailbox = mailboxList.get(mailboxList.size() / 2);
        MailboxPath path = new MailboxPath("gsoc", "ieugen", "Trash");
        HBaseMailbox newBox = new HBaseMailbox(path, 1234);
        mapper.save(newBox);
        mailboxList.add(newBox);
        pathsList.add(path);

        long result = uidProvider.lastUid(null, newBox);
        assertEquals(0, result);
        for (int i = 1; i < 10; i++) {
            long uid = uidProvider.nextUid(null, newBox);
            assertEquals(uid, uidProvider.lastUid(null, newBox));
        }
    }

    /**
     * Test of nextUid method, of class HBaseUidProvider.
     */
    @Test
    public void testNextUid() throws Exception {
        System.out.println("nextUid");
        HBaseMailbox mailbox = mailboxList.get(mailboxList.size() / 2);
        long lastUid = uidProvider.lastUid(null, mailbox);
        for (int i = (int) lastUid + 1; i < (lastUid + 10); i++) {
            long result = uidProvider.nextUid(null, mailbox);
            assertEquals(i, result);
        }
    }

    /**
     * Test of highestModSeq method, of class HBaseModSeqProvider.
     */
    @Test
    public void testHighestModSeq() throws Exception {
        System.out.println("highestModSeq");
        System.out.println("lastUid");
        HBaseMailbox mailbox = mailboxList.get(mailboxList.size() / 2);
        MailboxPath path = new MailboxPath("gsoc", "ieugen", "Trash");
        HBaseMailbox newBox = new HBaseMailbox(path, 1234);
        mapper.save(newBox);
        mailboxList.add(newBox);
        pathsList.add(path);

        long result = modSeqProvider.highestModSeq(null, newBox);
        assertEquals(0, result);
        for (int i = 1; i < 10; i++) {
            long uid = modSeqProvider.nextModSeq(null, newBox);
            assertEquals(uid, modSeqProvider.highestModSeq(null, newBox));
        }
    }

    /**
     * Test of nextModSeq method, of class HBaseModSeqProvider.
     */
    @Test
    public void testNextModSeq() throws Exception {
        System.out.println("nextModSeq");
        HBaseMailbox mailbox = mailboxList.get(mailboxList.size() / 2);
        long lastUid = modSeqProvider.highestModSeq(null, mailbox);
        for (int i = (int) lastUid + 1; i < (lastUid + 10); i++) {
            long result = modSeqProvider.nextModSeq(null, mailbox);
            assertEquals(i, result);
        }
    }
}
