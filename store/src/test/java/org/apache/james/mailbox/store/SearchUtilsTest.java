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

package org.apache.james.mailbox.store;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.Flags;

import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.DateResolution;
import org.apache.james.mailbox.store.MessageSearches;
import org.apache.james.mailbox.store.mail.model.Message;
import org.junit.Before;
import org.junit.Test;

public class SearchUtilsTest {

    private static final String RHUBARD = "Rhubard";

    private static final String CUSTARD = "Custard";

    private static final Date SUN_SEP_9TH_2001 = new Date(1000000000000L);

    private static final int SIZE = 1729;

    private static final String DATE_FIELD = "Date";

    private static final String SUBJECT_FIELD = "Subject";

    private static final String RFC822_SUN_SEP_9TH_2001 = "Sun, 9 Sep 2001 09:10:48 +0000 (GMT)";

    private static final String TEXT = RHUBARD + RHUBARD + RHUBARD;

    MessageBuilder builder;

    MessageSearches searches;

    Collection<Long> recent;

    private Calendar getGMT() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.UK);
    }
    
    private Date getDate(int day, int month, int year) {
        Calendar cal = getGMT();
        cal.set(year, month -1, day);
        return cal.getTime();
    }
    @Before
    public void setUp() throws Exception {
        recent = new ArrayList<Long>();
        builder = new MessageBuilder();
        builder.uid = 1009;
        searches = new MessageSearches();
    }
    
    @Test
    public void testMatchSizeLessThan() throws Exception {
        builder.size = SIZE;
        Message<Long> row = builder.build();
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
        builder.size = SIZE;
        Message<Long> row = builder.build();
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
        builder.size = SIZE;
        Message<Long> row = builder.build();
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
        builder.internalDate = SUN_SEP_9TH_2001;
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(getDate(9, 9, 2000), DateResolution.Day),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(getDate(8, 9, 2001), DateResolution.Day),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateOn(getDate(9, 9, 2001), DateResolution.Day),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(getDate(10, 9, 2001), DateResolution.Day),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(getDate(9, 9, 2002), DateResolution.Day),
                row, recent));
    }

    
    @Test
    public void testMatchInternalDateBefore() throws Exception {
        builder.internalDate = SUN_SEP_9TH_2001;
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(getDate(9, 9, 2000), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(getDate(8, 9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(getDate(9, 9, 2001), DateResolution.Day), row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.internalDateBefore(getDate(10, 9, 2001), DateResolution.Day), row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateBefore(getDate(9, 9, 2002), DateResolution.Day),
                row, recent));
    }

    @Test
    public void testMatchInternalDateAfter() throws Exception {
        builder.internalDate = SUN_SEP_9TH_2001;
        Message<Long> row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.internalDateAfter(getDate(9, 9, 2000), DateResolution.Day),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateAfter(getDate(8, 9, 2001), DateResolution.Day),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateAfter(getDate(9, 9, 2001), DateResolution.Day),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateAfter(getDate(10, 9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateAfter(getDate(9, 9, 2002), DateResolution.Day),
                row, recent));
    }

    @Test
    public void testMatchHeaderDateAfter() throws Exception {
        builder.header(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2000), DateResolution.Day), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(8,
                9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                getDate(10, 9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2002), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", getDate(9, 9,
                2001), DateResolution.Day), row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderDateAfter() throws Exception {
        builder.header(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2000), DateResolution.Day), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(8,
                9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                getDate(10, 9, 2001), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2002), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", getDate(9, 9,
                2001), DateResolution.Day), row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderDateAfter() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2000), DateResolution.Day), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(8,
                9, 2001),DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                getDate(10, 9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(9,
                9, 2002), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", getDate(9, 9,
                2001), DateResolution.Day),row, recent));
    }

    @Test
    public void testMatchHeaderDateOn() throws Exception {
        builder.header(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2000), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(8, 9,
                2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(10,
                9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2002), DateResolution.Day), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", getDate(9, 9,
                2001), DateResolution.Day), row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderDateOn() throws Exception {
        builder.header(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2000), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(8, 9,
                2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(10,
                9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2002), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", getDate(9, 9,
                2001), DateResolution.Day),row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderDateOn() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2000), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(8, 9,
                2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(10,
                9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(9, 9,
                2002), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", getDate(9, 9,
                2001), DateResolution.Day),row, recent));
    }

    @Test
    public void testMatchHeaderDateBefore() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(9, 9, 2000), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(8, 9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(9, 9, 2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(10, 9, 2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, getDate(9,
                9, 2002), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", getDate(9,
                9, 2001), DateResolution.Day),row, recent));
    }

    @Test
    public void testShouldMatchCapsHeaderDateBefore() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(9, 9, 2000), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(8, 9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(9, 9, 2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(10, 9, 2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, getDate(9,
                9, 2002), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", getDate(9,
                9, 2001), DateResolution.Day),row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderDateBefore() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(9, 9, 2000), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(8, 9, 2001), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(9, 9, 2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                getDate(10, 9, 2001), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, getDate(9,
                9, 2002), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", getDate(9,
                9, 2001), DateResolution.Day),row, recent));
    }

    @Test
    public void testMatchHeaderContainsCaps() throws Exception {
        builder.header(SUBJECT_FIELD, TEXT.toUpperCase());
        Message<Long> row = builder.build();
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
        builder.header(SUBJECT_FIELD, TEXT.toUpperCase());
        Message<Long> row = builder.build();
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
        builder.header(SUBJECT_FIELD, TEXT.toUpperCase());
        Message<Long> row = builder.build();
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
        builder.header(SUBJECT_FIELD.toLowerCase(), TEXT);
        Message<Long> row = builder.build();
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
        builder.header(SUBJECT_FIELD.toUpperCase(), TEXT);
        Message<Long> row = builder.build();
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
        builder.header(SUBJECT_FIELD, TEXT);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    @Test
    public void testShouldMatchLowersHeaderExists() throws Exception {
        builder.header(SUBJECT_FIELD.toLowerCase(), TEXT);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    @Test
    public void testShouldMatchUppersHeaderExists() throws Exception {
        builder.header(SUBJECT_FIELD.toLowerCase(), TEXT);
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    @Test
    public void testShouldMatchUidRange() throws Exception {
        builder.setKey(1, 1729);
        Message<Long> row = builder.build();
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
        builder.setFlags(true, false, false, false, false, false);
        Message<Long> row = builder.build();
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
        builder.setFlags(false, false, true, false, false, false);
        Message<Long> row = builder.build();
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
        builder.setFlags(false, true, false, false, false, false);
        Message<Long> row = builder.build();
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
        builder.setFlags(false, false, false, true, false, false);
        Message<Long> row = builder.build();
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
        builder.setFlags(false, false, false, false, true, false);
        Message<Long> row = builder.build();
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
        builder.setFlags(false, false, false, false, false, false);
        Message<Long> row = builder.build();
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
        builder.setFlags(false, true, true, true, true, true);
        Message<Long> row = builder.build();
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
        builder.setFlags(true, true, false, true, true, true);
        Message<Long> row = builder.build();
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
        builder.setFlags(true, false, true, true, true, true);
        Message<Long> row = builder.build();
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
        builder.setFlags(true, true, true, false, true, true);
        Message<Long> row = builder.build();
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
        builder.setFlags(true, true, true, true, false, true);
        Message<Long> row = builder.build();
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
        builder.setFlags(true, true, true, true, true, true);
        Message<Long> row = builder.build();
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
        Message<Long> row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.all(), row, recent));
    }

    @Test
    public void testShouldMatchNot() throws Exception {
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.not(SearchQuery.all()), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.not(SearchQuery
                .headerExists(DATE_FIELD)), row, recent));
    }

    @Test
    public void testShouldMatchOr() throws Exception {
        Message<Long> row = builder.build();
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
        Message<Long> row = builder.build();
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
    
    
    @Test
    public void testMatchHeaderDateOnWithOffset() throws Exception {
        builder.header(DATE_FIELD, "Mon, 26 Mar 2007 00:00:00 +0300");
        Message<Long> row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(26, 3,
                2007), DateResolution.Day),row, recent));
        
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(25, 3,
                2007), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, getDate(27, 3,
                2007), DateResolution.Day),row, recent));
    }
    

    @Test
    public void testShouldMatchHeaderDateBeforeWithOffset() throws Exception {
        builder.header(DATE_FIELD, "Mon, 26 Mar 2007 00:00:00 +0300");
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, getDate(26, 3,
                2007), DateResolution.Day),row, recent));
        
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, getDate(27, 3,
                2007), DateResolution.Day),row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, getDate(25, 3,
                2007), DateResolution.Day),row, recent));
    }

    @Test
    public void testShouldMatchHeaderDateAfterWithOffset() throws Exception {
        builder.header(DATE_FIELD, "Mon, 26 Mar 2007 00:00:00 +0300");
        Message<Long> row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(26, 3,
                2007), DateResolution.Day),row, recent));
        
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(27, 3,
                2007), DateResolution.Day),row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, getDate(25, 3,
                2007), DateResolution.Day),row, recent));
    }
}
