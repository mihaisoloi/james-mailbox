package org.apache.james.mailbox.hbase.index;

import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.hbase.store.HBaseIndexStore;
import org.apache.james.mailbox.hbase.store.MessageBuilder;
import org.apache.james.mailbox.hbase.store.MessageFields;
import org.apache.james.mailbox.hbase.store.SimpleMailboxMembership;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.SimpleMailboxACL;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.junit.*;

import javax.mail.Flags;
import java.nio.charset.Charset;
import java.util.*;

import static org.apache.james.mailbox.hbase.index.MessageSearchIndexListener.*;
import static org.apache.james.mailbox.hbase.store.HBaseNames.COLUMN_FAMILY;
import static org.junit.Assert.*;

public class MessageSearchIndexListenerTest {
    private MessageSearchIndexListener index;
    private static HBaseIndexStore store;

    private SimpleMailbox mailbox = new SimpleMailbox(new UUID(0,0));
    private SimpleMailbox mailbox2 = new SimpleMailbox(new UUID(1,0));
    private SimpleMailbox mailbox3 = new SimpleMailbox(new UUID(2,0));


    private static final String FROM_ADDRESS = "Harry <harry@example.org>";

    private static final String SUBJECT_PART = "Mixed";

    private static final String CUSTARD = "CUSTARD";

    private static final String RHUBARD = "Rhubard";

    private static final long mailId = 10l;

    private static final String BODY = "This is a simple email\r\n "
            + "It has " + RHUBARD + ".\r\n" + "It has " + CUSTARD + ".\r\n"
            + "It needs naught else.\r\n";

    private static HBaseTestingUtility HTU = new HBaseTestingUtility();

    @BeforeClass
    public static void setUpEnvironment() throws Exception {
        HTU.startMiniCluster();
        store = HBaseIndexStore.getInstance(HTU.getConfiguration());
    }

    @Before
    public void setUp() throws Exception {
        index = new MessageSearchIndexListener(null,store);
        Map<String, String> headersSubject = new HashMap<String, String>();
        headersSubject.put("Subject", "test (fwd)");
        headersSubject.put("From", "test99 <test99@localhost>");
        headersSubject.put("To", "test2 <test2@localhost>, test3 <test3@localhost>");

        Map<String, String> headersTest = new HashMap<String, String>();
        headersTest.put("Test", "test");
        headersTest.put("From", "test1 <test1@localhost>");
        headersTest.put("To", "test3 <test3@localhost>, test4 <test4@localhost>");
        headersTest.put("Cc", "test21 <test21@localhost>, test6 <test6@foobar>");

        Map<String, String> headersTestSubject = new HashMap<String, String>();
        headersTestSubject.put("Test", "test");
        headersTestSubject.put("Subject", "test2");
        headersTestSubject.put("Date", "Thu, 14 Feb 1990 12:00:00 +0000 (GMT)");
        headersTestSubject.put("From", "test12 <test12@localhost>");
        headersTestSubject.put("Cc", "test211 <test21@localhost>, test6 <test6@foobar>");

        SimpleMailboxMembership m2 = new SimpleMailboxMembership(mailbox2.getMailboxId(), 1, 0, new Date(), 20, new Flags(Flags.Flag.ANSWERED), "My Body".getBytes(), headersSubject);
        index.add(null, mailbox2, m2);

        SimpleMailboxMembership m = new SimpleMailboxMembership(mailbox.getMailboxId(), 1, 0, new Date(), 200, new Flags(Flags.Flag.ANSWERED), "My Body".getBytes(), headersSubject);
        index.add(null, mailbox, m);

        Calendar cal = Calendar.getInstance();
        cal.set(1980, 2, 10);
        SimpleMailboxMembership m3 = new SimpleMailboxMembership(mailbox.getMailboxId(), 2, 0, cal.getTime(), 20, new Flags(Flags.Flag.DELETED), "My Otherbody".getBytes(), headersTest);
        index.add(null, mailbox, m3);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(8000, 2, 10);
        SimpleMailboxMembership m4 = new SimpleMailboxMembership(mailbox.getMailboxId(), 3, 0, cal2.getTime(), 20, new Flags(Flags.Flag.DELETED), "My Otherbody2".getBytes(), headersTestSubject);
        index.add(null, mailbox, m4);

        MessageBuilder builder = new MessageBuilder();
        builder.header("From", "test <user-from@domain.org>");
        builder.header("To", FROM_ADDRESS);
        builder.header("Subject", "A " + SUBJECT_PART + " Multipart Mail");
        builder.header("Date", "Thu, 14 Feb 2008 12:00:00 +0000 (GMT)");
        builder.body = Charset.forName("us-ascii").encode(BODY).array();
        builder.uid = mailId;
        builder.mailboxId = mailbox3.getMailboxId();

        index.add(null, mailbox3, builder.build());
    }

    @AfterClass
    public static void tearDownEnvironment() throws Exception {
        HTU.shutdownMiniCluster();
    }

    @Test
    public void testUUIDTransform() throws Exception{
        UUID uuid = new UUID(11,22);
        assertEquals(uuid, rowToUUID(uuidToBytes(uuid)));
    }

    @Test
    public void testReadMailFromStore() throws Exception{
        for(Result result: store.retrieveMails(uuidToBytes(mailbox3.getMailboxId()),mailId)){
            NavigableMap<byte[],byte[]> family = result.getFamilyMap(COLUMN_FAMILY.name);
            assertEquals(mailId, Bytes.toLong(family.firstEntry().getKey()));
            byte[] row = result.getRow();
            UUID mailboxUUID = rowToUUID(row);
            assertEquals(mailbox3.getMailboxId(),mailboxUUID);
        }
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
    public void testSearchAddress() throws Exception {

        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.address(SearchQuery.AddressType.To,FROM_ADDRESS));
        Iterator<Long> result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());

        query = new SearchQuery();
        query.andCriteria(SearchQuery.address(SearchQuery.AddressType.To,"Harry"));
        result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());

        query = new SearchQuery();
        query.andCriteria(SearchQuery.address(SearchQuery.AddressType.To,"Harry@example.org"));
        result = index.search(null, mailbox3, query);
        assertEquals(10L, result.next().longValue());
        assertFalse(result.hasNext());
    }

    @Test
    public void testSearchAddressFrom() throws Exception {

        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.address(SearchQuery.AddressType.From,"ser-from@domain.or"));
        Iterator<Long> result = index.search(null, mailbox3, query);
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
        q.andCriteria(SearchQuery.flagIsSet(Flags.Flag.DELETED));
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
        q2.andCriteria(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT));
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
        q2.andCriteria(SearchQuery.internalDateBefore(cal.getTime(), SearchQuery.DateResolution.Day));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Test
    public void testSearchInternalDateAfter() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.internalDateAfter(cal.getTime(), SearchQuery.DateResolution.Day));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(3L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Test
    public void testSearchInternalDateOn() throws Exception {
        SearchQuery q2 = new SearchQuery();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        q2.andCriteria(SearchQuery.internalDateOn(cal.getTime(), SearchQuery.DateResolution.Day));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
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

    @Ignore("unsupported operation")
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

    @Ignore("unsupported operation")
    @Test
    public void testSearchSizeEquals() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.sizeEquals(200));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSearchSizeLessThan() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.sizeLessThan(200));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
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

    @Ignore("unsupported operation")
    @Test
    public void testSortUid() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortUidReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.Uid, true)));
        q2.andCriteria(SearchQuery.all());
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(3L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortSentDate() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.SentDate, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortSentDateReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.SentDate, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortBaseSubject() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.BaseSubject, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortBaseSubjectReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.BaseSubject, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortMailboxFrom() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.MailboxFrom, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void  testSortMailboxFromReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.MailboxFrom, true)));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortMailboxCc() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.MailboxCc, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void  testSortMailboxCcReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.MailboxCc, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortMailboxTo() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.MailboxTo, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);

        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void  testSortMailboxToReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.MailboxTo, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortDisplayTo() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.DisplayTo, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);

        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void  testSortDisplayToReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.DisplayTo, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortDisplayFrom() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.DisplayFrom, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);

        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void  testSortDisplayFromReverse() throws Exception {

        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.DisplayFrom, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortArrival() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.Arrival, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortArrivalReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.Arrival, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortSize() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.Size, false)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertEquals(1L, it4.next().longValue());

        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testSortSizeReverse() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.all());
        q2.setSorts(Arrays.asList(new SearchQuery.Sort(SearchQuery.Sort.SortClause.Size, true)));

        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(1L, it4.next().longValue());
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    @Ignore("unsupported operation")
    @Test
    public void testNot() throws Exception {
        SearchQuery q2 = new SearchQuery();
        q2.andCriteria(SearchQuery.not(SearchQuery.uid(new SearchQuery.NumericRange[] { new SearchQuery.NumericRange(1)})));
        Iterator<Long> it4 = index.search(null, mailbox, q2);
        assertEquals(2L, it4.next().longValue());
        assertEquals(3L, it4.next().longValue());
        assertFalse(it4.hasNext());
    }

    private final class SimpleMailbox implements Mailbox<UUID> {
        private UUID id;

        public SimpleMailbox(UUID id) {
            this.id = id;
        }

        public UUID getMailboxId() {
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
            return id.toString();
        }

        public void setName(String name) {
            throw new UnsupportedOperationException("Not supported");

        }

        public long getUidValidity() {
            return 0;
        }

        /* (non-Javadoc)
         * @see org.apache.james.mailbox.store.mail.model.Mailbox#getACL()
         */
        @Override
        public MailboxACL getACL() {
            return SimpleMailboxACL.OWNER_FULL_ACL;
        }

        /* (non-Javadoc)
         * @see org.apache.james.mailbox.store.mail.model.Mailbox#setACL(org.apache.james.mailbox.MailboxACL)
         */
        @Override
        public void setACL(MailboxACL acl) {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
