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
package org.apache.james.mailbox.store.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.DateResolution;
import org.apache.james.mailbox.store.MessageBuilder;
import org.apache.james.mailbox.store.SimpleHeader;
import org.apache.james.mailbox.store.SimpleMailboxMembership;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.search.lucene.LuceneMessageSearchIndex;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;

public class LuceneMessageSearchIndexTest {

    private LuceneMessageSearchIndex<Long> index;

    private SimpleMailbox mailbox = new SimpleMailbox(0);
    private SimpleMailbox mailbox2 = new SimpleMailbox(1);
    private SimpleMailbox mailbox3 = new SimpleMailbox(2);


    private static final String FROM_ADDRESS = "Harry <harry@example.org";

    private static final String SUBJECT_PART = "Mixed";

    private static final String CUSTARD = "CUSTARD";

    private static final String RHUBARD = "Rhubard";

    private static final String BODY = "This is a simple email\r\n "
            + "It has " + RHUBARD + ".\r\n" + "It has " + CUSTARD + ".\r\n"
            + "It needs naught else.\r\n";

    Message<Long> row;

    protected boolean useLenient() {
        return true;
    }
    
    @Before
    public void setUp() throws Exception {
        index = new LuceneMessageSearchIndex<Long>(null, new RAMDirectory(), useLenient());
        index.setEnableSuffixMatch(true);
        List<org.apache.james.mailbox.store.SimpleHeader> headersSubject = new ArrayList<org.apache.james.mailbox.store.SimpleHeader>();
        headersSubject.add(new SimpleHeader("Subject", 1, "test"));
       
        List<org.apache.james.mailbox.store.SimpleHeader> headersTest = new ArrayList<org.apache.james.mailbox.store.SimpleHeader>();
        headersSubject.add(new SimpleHeader("Test", 1, "test"));
        
        List<org.apache.james.mailbox.store.SimpleHeader> headersTestSubject = new ArrayList<org.apache.james.mailbox.store.SimpleHeader>();
        headersTestSubject.add(new SimpleHeader("Test", 1, "test"));
        headersTestSubject.add(new SimpleHeader("Subject", 2, "test2"));


        
        SimpleMailboxMembership m2 = new SimpleMailboxMembership(mailbox2.getMailboxId(),1, 0, new Date(), 20, new Flags(Flag.ANSWERED), "My Body".getBytes(), headersSubject);
        index.add(null, mailbox2, m2);

        SimpleMailboxMembership m = new SimpleMailboxMembership(mailbox.getMailboxId(),1, 0, new Date(), 200, new Flags(Flag.ANSWERED), "My Body".getBytes(), headersSubject);
        index.add(null, mailbox, m);
        
        Calendar cal = Calendar.getInstance();
        cal.set(1980, 2, 10);
        SimpleMailboxMembership m3 = new SimpleMailboxMembership(mailbox.getMailboxId(),2, 0, cal.getTime(), 20, new Flags(Flag.DELETED), "My Otherbody".getBytes(), headersTest);
        index.add(null, mailbox, m3);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(8000, 2, 10);
        SimpleMailboxMembership m4 = new SimpleMailboxMembership(mailbox.getMailboxId(),3, 0, cal2.getTime(), 20, new Flags(Flag.DELETED), "My Otherbody2".getBytes(), headersTestSubject);
        index.add(null, mailbox, m4);
        
        MessageBuilder builder = new MessageBuilder();
        builder.header("From", "Alex <alex@example.org");
        builder.header("To", FROM_ADDRESS);
        builder.header("Subject", "A " + SUBJECT_PART + " Multipart Mail");
        builder.header("Date", "Thu, 14 Feb 2008 12:00:00 +0000 (GMT)");
        builder.body = Charset.forName("us-ascii").encode(BODY).array();
        builder.uid = 10;
        builder.mailboxId = mailbox3.getMailboxId();
        
        index.add(null, mailbox3, builder.build());
                
    }
    


    @Test
    public void testBodyShouldMatchPhraseInBody() throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.bodyContains(CUSTARD));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
        
        
        query = new SearchQuery();
        query.andCriteria(SearchQuery.bodyContains(CUSTARD + CUSTARD));
        result = index.search(null, mailbox3, query);
        assertFalse(result.hasNext());
    }

    @Test
    public void testBodyMatchShouldBeCaseInsensitive() throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.bodyContains(RHUBARD));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
    }

    @Test
    public void testBodyShouldNotMatchPhraseOnlyInHeader() throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.bodyContains(FROM_ADDRESS));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertFalse(result.hasNext());
        
        query = new SearchQuery();
        query.andCriteria(SearchQuery.bodyContains(SUBJECT_PART));
        result = index.search(null, mailbox3, query);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTextShouldMatchPhraseInBody() throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.mailContains(CUSTARD));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
        
        query = new SearchQuery();
        query.andCriteria(SearchQuery.mailContains(CUSTARD + CUSTARD));
        result = index.search(null, mailbox3, query);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTextMatchShouldBeCaseInsensitive() throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.mailContains(RHUBARD));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
        
        query.andCriteria(SearchQuery.mailContains(RHUBARD.toLowerCase(Locale.US)));
        result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
    }

    @Test
    public void testBodyShouldMatchPhraseOnlyInHeader() throws Exception {
        
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.mailContains(FROM_ADDRESS));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
        
        query.andCriteria(SearchQuery.mailContains(SUBJECT_PART));
        result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
    }
    
    @Test
    public void testSearchAll() throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.all());
        Iterator<Long> it2 = index.search(null, mailbox2, query);
        assertTrue(it2.hasNext());
        assertEquals(1L, it2.next().longValue());
        assertFalse(it2.hasNext());
    }
    
    @Test
    public void testSearchFlag() throws Exception {

        SearchQuery q = new SearchQuery();
        q.andCriteria(SearchQuery.flagIsSet(Flag.DELETED));
        Iterator<Long> it3 = index.search(null, mailbox, q);
        assertEquals(2L, it3.next().longValue());
        assertEquals(3L, it3.next().longValue());
        assertFalse(it3.hasNext());
    }
    
    @Test
    public void testSearchBody() throws Exception {    
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.bodyContains("body"));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testSearchMail() throws Exception {    
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.mailContains("body"));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testSearchHeaderContains() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.headerContains("Subject", "test"));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testSearchHeaderExists() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.headerExists("Subject"));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testSearchFlagUnset() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.flagIsUnSet(Flag.DRAFT));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    
    @Test
    public void testSearchInternalDateBefore() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.internalDateBefore(cal.getTime(), DateResolution.Day));
        
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }
    
    
    @Test
    public void testSearchInternalDateAfter() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.internalDateAfter(cal.getTime(), DateResolution.Day));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(3L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }
    
    
    
    @Test
    public void testSearchInternalDateOn() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.internalDateOn(cal.getTime(), DateResolution.Day));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testSearchUidMatch() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.uid(new SearchQuery.NumericRange[] {new SearchQuery.NumericRange(1)}));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }
    
    
    @Test
    public void testSearchUidRange() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.uid(new SearchQuery.NumericRange[] {new SearchQuery.NumericRange(1), new SearchQuery.NumericRange(2,3)}));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    
    
    @Test
    public void testSearchSizeEquals() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.sizeEquals(200));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testSearchSizeLessThan() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.sizeLessThan(200));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    
    @Test
    public void testSearchSizeGreaterThan() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.sizeGreaterThan(6));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }
    
    @Test
    public void testNot() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.not(SearchQuery.uid(new SearchQuery.NumericRange[] { new SearchQuery.NumericRange(1)})));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }
    
    private final class SimpleMailbox implements Mailbox<Long> {
        private long id;

        public SimpleMailbox(long id) {
            this.id = id;
        }

        public Long getMailboxId() {
            return id;
        }

        public String getNamespace() {
            throw new UnsupportedOperationException("Not supported");
        }

        public void setNamespace(String namespace) {
            throw new UnsupportedOperationException("Not supported");
        }

        public String getUser() {
            throw new UnsupportedOperationException("Not supported");
        }

        public void setUser(String user) {
            throw new UnsupportedOperationException("Not supported");
        }

        public String getName() {
            return Long.toString(id);
        }

        public void setName(String name) {
            throw new UnsupportedOperationException("Not supported");

        }

        public long getUidValidity() {
            return 0;
        }

        @Override
        public long getLastKnownUid() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getHighestKnownModSeq() {
            // TODO Auto-generated method stub
            return 0;
        }
    }
}
