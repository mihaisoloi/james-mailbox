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

import java.util.*;
import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for HBaseMessageMapper.
 *
 * @author ieugen
 */
public class HBaseMessageMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseMailboxMapperTest.class);
    public static final HBaseClusterSingleton CLUSTER = HBaseClusterSingleton.build();
    private static HBaseUidProvider uidProvider;
    private static HBaseModSeqProvider modSeqProvider;
    private static HBaseMessageMapper messageMapper;
    private static final List<MailboxPath> MBOX_PATHS = new ArrayList<MailboxPath>();
    private static final List<Mailbox<UUID>> MBOXES = new ArrayList<Mailbox<UUID>>();
    private static final List<Message<UUID>> MESSAGES = new ArrayList<Message<UUID>>();
    private static final int COUNT = 5;
    private static Configuration conf;
    /*
     * we mock a simple message content
     */
    private static final byte[] messageTemplate = Bytes.toBytes(
            "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\n"
            + "From: Fred Foobar <foobar@Blurdybloop.COM>\n"
            + "Subject: Test 02\n"
            + "To: mooch@owatagu.siam.edu\n"
            + "Message-Id: <B27397-0100000@Blurdybloop.COM>\n"
            + "MIME-Version: 1.0\n"
            + "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\n"
            + "\n"
            + "Test\n"
            + "\n.");
    private static SharedInputStream content = new SharedByteArrayInputStream(messageTemplate);

    @Before
    public void setUp() throws Exception {
        CLUSTER.clearTables();
        conf = CLUSTER.getConf();
        uidProvider = new HBaseUidProvider(conf);
        modSeqProvider = new HBaseModSeqProvider(conf);
        generateTestData();
        final MailboxSession session = new MockMailboxSession("ieugen");
        messageMapper = new HBaseMessageMapper(session, uidProvider, modSeqProvider, conf);
        for (int i = 0; i < MESSAGES.size(); i++) {
            messageMapper.add(MBOXES.get(1), MESSAGES.get(i));
        }
    }

    public static void generateTestData() {
        final Random random = new Random();
        MailboxPath mboxPath;
        final PropertyBuilder propBuilder = new PropertyBuilder();

        for (int i = 0; i < COUNT; i++) {
            if (i % 2 == 0) {
                mboxPath = new MailboxPath("gsoc", "ieugen" + i, "INBOX");
            } else {
                mboxPath = new MailboxPath("gsoc", "ieugen" + i, "INBOX.box" + i);
            }
            MBOX_PATHS.add(mboxPath);
            MBOXES.add(new HBaseMailbox(MBOX_PATHS.get(i), random.nextLong()));
            propBuilder.setProperty("gsoc", "prop" + i, "value");
        }
        propBuilder.setMediaType("text");
        propBuilder.setSubType("html");
        propBuilder.setTextualLineCount(2L);

        SimpleMessage<UUID> myMsg;
        final Flags flags = new Flags(Flags.Flag.RECENT);
        final Date today = new Date();

        for (int i = 0; i < COUNT * 2; i++) {
            myMsg = new SimpleMessage<UUID>(today, messageTemplate.length,
                    messageTemplate.length - 20, content, flags, propBuilder,
                    MBOXES.get(1).getMailboxId());
            if (i == COUNT * 2 - 1) {
                flags.add(Flags.Flag.SEEN);
                flags.remove(Flags.Flag.RECENT);
                myMsg.setFlags(flags);
            }
            MESSAGES.add(myMsg);
        }
    }

    /**
     * Test of countMessagesInMailbox method, of class HBaseMessageMapper.
     */
    @Test
    public void testCountMessagesInMailbox() throws Exception {
        LOG.info("countMessagesInMailbox");
        long messageCount = messageMapper.countMessagesInMailbox(MBOXES.get(1));
        assertEquals(MESSAGES.size(), messageCount);
    }

    /**
     * Test of countUnseenMessagesInMailbox method, of class HBaseMessageMapper.
     */
    @Test
    public void testCountUnseenMessagesInMailbox() throws Exception {
        LOG.info("countUnseenMessagesInMailbox");
        long unseen = messageMapper.countUnseenMessagesInMailbox(MBOXES.get(1));
        assertEquals(MESSAGES.size() - 1, unseen);
    }

    /**
     * Test of findFirstUnseenMessageUid method, of class HBaseMessageMapper.
     */
    @Test
    public void testFindFirstUnseenMessageUid() throws Exception {
        LOG.info("findFirstUnseenMessageUid");
        final long uid = messageMapper.findFirstUnseenMessageUid(MBOXES.get(1));
        assertEquals(1, uid);
    }

    /**
     * Test of findRecentMessageUidsInMailbox method, of class
     * HBaseMessageMapper.
     */
    @Test
    public void testFindRecentMessageUidsInMailbox() throws Exception {
        LOG.info("findRecentMessageUidsInMailbox");
        List<Long> recentMessages = messageMapper.findRecentMessageUidsInMailbox(MBOXES.get(1));
        assertEquals(MESSAGES.size() - 1, recentMessages.size());
    }

    /**
     * Test of add method, of class HBaseMessageMapper.
     */
    @Test
    public void testAdd() throws Exception {
        LOG.info("add");
        // The tables should be deleted every time the tests run.
        long msgCount = messageMapper.countMessagesInMailbox(MBOXES.get(1));
        LOG.info(msgCount + " " + MESSAGES.size());
        assertEquals(MESSAGES.size(), msgCount);
    }

    /**
     * Test of getLastUid method, of class HBaseMessageMapper.
     */
    @Test
    public void testGetLastUid() throws Exception {
        LOG.info("getLastUid");
        long lastUid = messageMapper.getLastUid(MBOXES.get(1));
        assertEquals(MESSAGES.size(), lastUid);
    }

    /**
     * Test of getHighestModSeq method, of class HBaseMessageMapper.
     */
    @Test
    public void testGetHighestModSeq() throws Exception {
        LOG.info("getHighestModSeq");
        long highestModSeq = messageMapper.getHighestModSeq(MBOXES.get(1));
        assertEquals(MESSAGES.size(), highestModSeq);
    }
}
