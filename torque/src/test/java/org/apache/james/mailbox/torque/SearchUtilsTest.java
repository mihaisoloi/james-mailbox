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

package org.apache.james.mailbox.torque;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.torque.MessageSearches;
import org.apache.james.mailbox.torque.om.MessageFlags;
import org.apache.james.mailbox.torque.om.MessageHeader;
import org.apache.james.mailbox.torque.om.MessageRow;
import org.apache.torque.TorqueException;
import org.junit.Before;
import org.junit.Test;

public class SearchUtilsTest {

    private static final String RHUBARD = "Rhubard";

    private static final String CUSTARD = "Custard";

    private static final Date SUN_SEP_9TH_2001 = new Date(1000000000000L);

    private static final int SIZE = 1729;

    private static final String DATE_FIELD = ImapConstants.RFC822_DATE;

    private static final String SUBJECT_FIELD = ImapConstants.RFC822_SUBJECT;

    private static final String RFC822_SUN_SEP_9TH_2001 = "Sun, 9 Sep 2001 09:10:48 +0000 (GMT)";

    private static final String TEXT = RHUBARD + RHUBARD + RHUBARD;

    MessageRow row;

    MessageSearches searches;

    Collection recent;

    @Before
    public void setUp() throws Exception {
        recent = new ArrayList();
        row = new MessageRow();
        row.setUid(1009);
        searches = new MessageSearches();
    }

    @Test
    public void testMatchSizeLessThan() throws Exception {
        row.setSize(SIZE);
        assertFalse(searches.isMatch(SearchQuery.sizeLessThan(SIZE - 1), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeLessThan(SIZE), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.sizeLessThan(SIZE + 1), row,
                recent));
        assertTrue(searches.isMatch(
                SearchQuery.sizeLessThan(Integer.MAX_VALUE), row, recent));
    }

    @Test
    public void testMatchSizeMoreThan() throws Exception {
        row.setSize(SIZE);
        assertTrue(searches.isMatch(SearchQuery.sizeGreaterThan(SIZE - 1), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeGreaterThan(SIZE), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeGreaterThan(SIZE + 1),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .sizeGreaterThan(Integer.MAX_VALUE), row, recent));
    }

    @Test
    public void testMatchSizeEquals() throws Exception {
        row.setSize(SIZE);
        assertFalse(searches.isMatch(SearchQuery.sizeEquals(SIZE - 1), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.sizeEquals(SIZE), row, recent));
        assertFalse(searches.isMatch(SearchQuery.sizeEquals(SIZE + 1), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeEquals(Integer.MAX_VALUE),
                row, recent));
    }

    @Test
    public void testMatchInternalDateEquals() throws Exception {
        row.setInternalDate(SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(9, 9, 2000),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(8, 9, 2001),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateOn(9, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(10, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(9, 9, 2002),
                row, recent));
    }

    @Test
    public void testMatchInternalDateBefore() throws Exception {
        row.setInternalDate(SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.internalDateBefore(10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateBefore(9, 9, 2002),
                row, recent));
    }

    @Test
    public void testMatchInternalDateAfter() throws Exception {
        row.setInternalDate(SUN_SEP_9TH_2001);
        assertTrue(searches.isMatch(SearchQuery.internalDateAfter(9, 9, 2000),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateAfter(8, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateAfter(9, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateAfter(10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateAfter(9, 9, 2002),
                row, recent));
    }

    @Test
    public void testMatchHeaderDateAfter() throws Exception {
        addHeader(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2000), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 8,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", 9, 9,
                2001), row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderDateAfter() throws Exception {
        addHeader(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2000), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 8,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", 9, 9,
                2001), row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderDateAfter() throws Exception {
        addHeader(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2000), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 8,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", 9, 9,
                2001), row, recent));
    }

    @Test
    public void testMatchHeaderDateOn() throws Exception {
        addHeader(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 8, 9,
                2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 10,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", 9, 9,
                2001), row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderDateOn() throws Exception {
        addHeader(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 8, 9,
                2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 10,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", 9, 9,
                2001), row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderDateOn() throws Exception {
        addHeader(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 8, 9,
                2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 10,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", 9, 9,
                2001), row, recent));
    }

    @Test
    public void testMatchHeaderDateBefore() throws Exception {
        addHeader(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", 9,
                9, 2001), row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderDateBefore() throws Exception {
        addHeader(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", 9,
                9, 2001), row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderDateBefore() throws Exception {
        addHeader(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", 9,
                9, 2001), row, recent));
    }

    @Test
    public void testMatchHeaderContainsCaps() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT.toUpperCase());
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    @Test
    public void testMatchHeaderContainsLowers() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT.toLowerCase());
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    @Test
    public void testMatchHeaderContains() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT);
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    @Test
    public void testShouldMatchLowerHeaderContains() throws Exception {
        addHeader(SUBJECT_FIELD.toLowerCase(), TEXT);
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderContains() throws Exception {
        addHeader(SUBJECT_FIELD.toUpperCase(), TEXT);
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    @Test
    public void testMatchHeaderExists() throws Exception {
        addHeader(SUBJECT_FIELD, TEXT);
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderExists() throws Exception {
        addHeader(SUBJECT_FIELD.toLowerCase(), TEXT);
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    @Test
    public void testShouldMatchUppersHeaderExists() throws Exception {
        addHeader(SUBJECT_FIELD.toUpperCase(), TEXT);
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    @Test
    public void testShouldMatchUidRange() throws Exception {
        row.setPrimaryKey(1, 1729);
        assertFalse(searches.isMatch(SearchQuery.uid(range(1, 1)), row, recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1728, 1728)), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1729, 1729)), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1730, 1730)), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1, 1728)), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1, 1729)), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1729, 1800)), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery
                .uid(range(1730, Long.MAX_VALUE)), row, recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1730,
                Long.MAX_VALUE, 1, 1728)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1730, Long.MAX_VALUE,
                1, 1729)), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .uid(range(1, 1728, 1800, 1810)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1, 1, 1729, 1729)),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1, 1, 1800, 1800)),
                row, recent));
    }

    @Test
    public void testShouldMatchSeenFlagSet() throws Exception {
        setFlags(true, false, false, false, false, false);
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchAnsweredFlagSet() throws Exception {
        setFlags(false, false, true, false, false, false);
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.ANSWERED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchFlaggedFlagSet() throws Exception {
        setFlags(false, true, false, false, false, false);
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchDraftFlagSet() throws Exception {
        setFlags(false, false, false, true, false, false);
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchDeletedFlagSet() throws Exception {
        setFlags(false, false, false, false, true, false);
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchSeenRecentSet() throws Exception {
        setFlags(false, false, false, false, false, false);
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchSeenFlagUnSet() throws Exception {
        setFlags(false, true, true, true, true, true);
        recent.add(new Long(row.getUid()));
        assertTrue(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    @Test
    public void testShouldMatchAnsweredFlagUnSet() throws Exception {
        setFlags(true, true, false, true, true, true);
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertTrue(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    @Test
    public void testShouldMatchFlaggedFlagUnSet() throws Exception {
        setFlags(true, false, true, true, true, true);
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    @Test
    public void testShouldMatchDraftFlagUnSet() throws Exception {
        setFlags(true, true, true, false, true, true);
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    @Test
    public void testShouldMatchDeletedFlagUnSet() throws Exception {
        setFlags(true, true, true, true, false, true);
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    @Test
    public void testShouldMatchSeenRecentUnSet() throws Exception {
        setFlags(true, true, true, true, true, true);
        recent.add(new Long(row.getUid() + 1));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.RECENT),
                row, recent));
    }

    @Test
    public void testShouldMatchAll() throws Exception {
        assertTrue(searches.isMatch(SearchQuery.all(), row, recent));
    }

    @Test
    public void testShouldMatchNot() throws Exception {
        assertFalse(searches.isMatch(SearchQuery.not(SearchQuery.all()), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.not(SearchQuery
                .headerExists(DATE_FIELD)), row, recent));
    }

    @Test
    public void testShouldMatchOr() throws Exception {
        assertTrue(searches.isMatch(SearchQuery.or(SearchQuery.all(),
                SearchQuery.headerExists(DATE_FIELD)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.or(SearchQuery
                .headerExists(DATE_FIELD), SearchQuery.all()), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .or(SearchQuery.headerExists(DATE_FIELD), SearchQuery
                        .headerExists(DATE_FIELD)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.or(SearchQuery.all(),
                SearchQuery.all()), row, recent));
    }

    @Test
    public void testShouldMatchAnd() throws Exception {
        assertFalse(searches.isMatch(SearchQuery.and(SearchQuery.all(),
                SearchQuery.headerExists(DATE_FIELD)), row, recent));
        assertFalse(searches.isMatch(SearchQuery.and(SearchQuery
                .headerExists(DATE_FIELD), SearchQuery.all()), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .and(SearchQuery.headerExists(DATE_FIELD), SearchQuery
                        .headerExists(DATE_FIELD)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.and(SearchQuery.all(),
                SearchQuery.all()), row, recent));
    }

    private void setFlags(boolean seen, boolean flagged, boolean answered,
            boolean draft, boolean deleted, boolean recent)
            throws TorqueException {
        final MessageFlags messageFlags = new MessageFlags();
        messageFlags.setSeen(seen);
        messageFlags.setFlagged(flagged);
        messageFlags.setAnswered(answered);
        messageFlags.setDraft(draft);
        messageFlags.setDeleted(deleted);
        messageFlags.setRecent(recent);
        row.addMessageFlags(messageFlags);
    }

    private SearchQuery.NumericRange[] range(long low, long high) {
        SearchQuery.NumericRange[] results = { new SearchQuery.NumericRange(
                low, high) };
        return results;
    }

    private SearchQuery.NumericRange[] range(long lowOne, long highOne,
            long lowTwo, long highTwo) {
        SearchQuery.NumericRange[] results = {
                new SearchQuery.NumericRange(lowOne, highOne),
                new SearchQuery.NumericRange(lowTwo, highTwo) };
        return results;
    }

    private void addHeader(String fieldName, String value)
            throws TorqueException {
        final MessageHeader messageHeader = new MessageHeader();
        messageHeader.setField(fieldName);
        messageHeader.setValue(value);
        row.addMessageHeader(messageHeader);
    }
}
