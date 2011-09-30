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

import org.junit.Test;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.hbase.HBaseMailboxSessionMapperFactory;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 * Unit tests for HBaseMessageMapper.
 * @author ieugen
 */
public class HBaseMessageMapperTest {

    private static final Logger logger = Logger.getLogger("HBaseMailboxMapperTest");
    private static HBaseUidProvider uidProvider;
    private static HBaseModSeqProvider modSeqProvider;
    private static HBaseMailboxSessionMapperFactory mapperFactory;
    private static HBaseMailboxMapper mailboxMapper;
    private static HBaseMessageMapper messageMapper;
    private static final List<MailboxPath> mboxPaths = new ArrayList<MailboxPath>();
    private static final List<Mailbox<UUID>> mboxes = new ArrayList<Mailbox<UUID>>();
    private static final List<Message<UUID>> messages = new ArrayList<Message<UUID>>();
    private static final int pathAndMboxCount = 5;
    private static Configuration conf;
    /* we mock a simple message content*/
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

    @BeforeClass
    public static void setUpClass() throws Exception {
        conf = HBaseClusterSingleton.build();
        HBaseClusterSingleton.resetTables(conf);
        uidProvider = new HBaseUidProvider(conf);
        modSeqProvider = new HBaseModSeqProvider(conf);
        mapperFactory = new HBaseMailboxSessionMapperFactory(conf, uidProvider, modSeqProvider);
        generateTestData();
        mailboxMapper = new HBaseMailboxMapper(conf);
        MailboxSession session = new MockMailboxSession("ieugen");
        messageMapper = new HBaseMessageMapper(session, uidProvider, modSeqProvider, conf);
        for (int i = 0; i < messages.size(); i++) {
            messageMapper.add(mboxes.get(1), messages.get(i));
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public static void generateTestData() {
        Random random = new Random();
        MailboxPath mboxPath;
        PropertyBuilder propBuilder = new PropertyBuilder();

        for (int i = 0; i < pathAndMboxCount; i++) {
            if (i % 2 == 0) {
                mboxPath = new MailboxPath("gsoc", "ieugen" + i, "INBOX");
            } else {
                mboxPath = new MailboxPath("gsoc", "ieugen" + i, "INBOX.box" + i);
            }
            mboxPaths.add(mboxPath);
            mboxes.add(new HBaseMailbox(mboxPaths.get(i), random.nextLong()));
            propBuilder.setProperty("gsoc", "prop" + i, "value");
        }
        propBuilder.setMediaType("text");
        propBuilder.setSubType("html");
        propBuilder.setTextualLineCount(2L);

        SimpleMessage<UUID> myMsg;
        Flags flags = new Flags(Flags.Flag.RECENT);
        Date today = new Date();

        for (int i = 0; i < pathAndMboxCount * 2; i++) {
            myMsg = new SimpleMessage<UUID>(today, messageTemplate.length,
                    messageTemplate.length - 20, content, flags, propBuilder,
                    mboxes.get(1).getMailboxId());
            if (i == pathAndMboxCount * 2 - 1) {
                flags.add(Flags.Flag.SEEN);
                flags.remove(Flags.Flag.RECENT);
                myMsg.setFlags(flags);
            }
            messages.add(myMsg);
        }
    }


    /**
     * Test of countMessagesInMailbox method, of class HBaseMessageMapper.
     */
    @Test
    public void testCountMessagesInMailbox() throws Exception {
        System.out.println("countMessagesInMailbox");
        long messageCount = messageMapper.countMessagesInMailbox(mboxes.get(1));
        assertEquals(messages.size(), messageCount);
    }

    /**
     * Test of countUnseenMessagesInMailbox method, of class HBaseMessageMapper.
     */
    @Test
    public void testCountUnseenMessagesInMailbox() throws Exception {
        System.out.println("countUnseenMessagesInMailbox");
        long unseen = messageMapper.countUnseenMessagesInMailbox(mboxes.get(1));
        assertEquals(messages.size() - 1, unseen);
    }

    /**
     * Test of findFirstUnseenMessageUid method, of class HBaseMessageMapper.
     */
    @Test
    public void testFindFirstUnseenMessageUid() throws Exception {
        System.out.println("findFirstUnseenMessageUid");
        long uid = messageMapper.findFirstUnseenMessageUid(mboxes.get(1));
        assertEquals(1, uid);
    }

    /**
     * Test of findRecentMessageUidsInMailbox method, of class HBaseMessageMapper.
     */
    @Test
    public void testFindRecentMessageUidsInMailbox() throws Exception {
        System.out.println("findRecentMessageUidsInMailbox");
        List<Long> recentMessages = messageMapper.findRecentMessageUidsInMailbox(mboxes.get(1));
        assertEquals(messages.size() - 1, recentMessages.size());
    }

    /**
     * Test of add method, of class HBaseMessageMapper.
     */
    @Test
    public void testAdd() throws Exception {
        System.out.println("add");
        // The tables should be deleted every time the tests run.
        long msgCount = messageMapper.countMessagesInMailbox(mboxes.get(1));
        System.out.println(msgCount + " " + messages.size());
        assertEquals(messages.size(), msgCount);
    }

    /**
     * Test of getLastUid method, of class HBaseMessageMapper.
     */
    @Test
    public void testGetLastUid() throws Exception {
        System.out.println("getLastUid");
        long lastUid = messageMapper.getLastUid(mboxes.get(1));
        assertEquals(messages.size(), lastUid);
    }

    /**
     * Test of getHighestModSeq method, of class HBaseMessageMapper.
     */
    @Test
    public void testGetHighestModSeq() throws Exception {
        System.out.println("getHighestModSeq");
        long highestModSeq = messageMapper.getHighestModSeq(mboxes.get(1));
        assertEquals(messages.size(), highestModSeq);
    }
}
