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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mailbox.store.MailboxMetaData;
import org.apache.james.mailbox.store.streaming.CRLFOutputStream;
import org.apache.james.mailbox.torque.om.MailboxRow;
import org.apache.james.mailbox.torque.om.MailboxRowPeer;
import org.apache.james.mailbox.torque.om.MessageBody;
import org.apache.james.mailbox.torque.om.MessageFlags;
import org.apache.james.mailbox.torque.om.MessageFlagsPeer;
import org.apache.james.mailbox.torque.om.MessageHeader;
import org.apache.james.mailbox.torque.om.MessageRow;
import org.apache.james.mailbox.torque.om.MessageRowPeer;
import org.apache.james.mailbox.util.FetchGroupImpl;
import org.apache.james.mime4j.MimeException;
import org.apache.torque.NoRowsException;
import org.apache.torque.TooManyRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.workingdogs.village.DataSetException;

/**
 * 
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class TorqueMailbox implements MessageManager {

    private static final int LOCK_TIMEOUT = 10;

    private boolean open = true;

    private MailboxRow mailboxRow;

    private final UidChangeTracker tracker;

    private final ReentrantReadWriteLock lock;

    private final MessageSearches searches;

    TorqueMailbox(final MailboxRow mailboxRow, final ReentrantReadWriteLock lock) {
        this.searches = new MessageSearches();
        this.mailboxRow = mailboxRow;
        this.tracker = new UidChangeTracker(mailboxRow.getLastUid(), getMailboxPath(mailboxRow.getName()));
        this.lock = lock;
    }

    public synchronized String getName() {
        checkAccess();
        return mailboxRow.getName();
    }

    public long getMessageCount(MailboxSession mailboxSession)
    throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            try {
                return getMailboxRow().countMessages();
            } catch (Exception e) {
                throw new MailboxException("Count of messages in mailbox " + getName() + " failed", e);
            }
        } finally {
            unlockAfterReading();
        }
    }

    private void lockForWriting() throws LockException {
        if (!lock.isWriteLockedByCurrentThread()) {
            LockException.tryLock(this.lock.writeLock(), LOCK_TIMEOUT);
        }
    }

    private void lockForReading() throws LockException {
        LockException.tryLock(lock.readLock(), LOCK_TIMEOUT);
    }

    public long appendMessage(InputStream messageIn, Date internalDate,
            MailboxSession mailboxSession, boolean isRecent, Flags flags)
    throws MailboxException {
        try {
            checkAccess();

            final MailboxRow myMailboxRow = reserveNextUid();

            if (myMailboxRow != null) {
                File file = null;
                try {
                    
                    // To be thread safe, we first get our own copy and the
                    // exclusive
                    // Uid
                    // TODO create own message_id and assign uid later
                    // at the moment it could lead to the situation that uid 5
                    // is
                    // inserted long before 4, when
                    // mail 4 is big and comes over a slow connection.
                    long uid = myMailboxRow.getLastUid();
                    this.mailboxRow = myMailboxRow;

                    MessageRow messageRow = new MessageRow();
                    messageRow.setMailboxId(getMailboxRow().getMailboxId());
                    messageRow.setUid(uid);
                    messageRow.setInternalDate(internalDate);
                    // Create a temporary file and copy the message to it. We will work with the file as
                    // source for the InputStream
                    file = File.createTempFile("imap", ".msg");
                    FileOutputStream out = new FileOutputStream(file);
                    
                    byte[] buf = new byte[1024];
                    int i = 0;
                    while ((i = messageIn.read(buf)) != -1) {
                        out.write(buf, 0, i);
                    }
                    out.flush();
                    out.close();
                    
                    final MimeMessage mimeMessage = new MimeMessage(null, new FileInputStream(file));

                    if (isRecent) {
                        mimeMessage.setFlag(Flags.Flag.RECENT, true);
                    }
                    if (flags != null) {
                        for (final Flags.Flag flag:flags.getSystemFlags()) {
                            mimeMessage.setFlag(flag, true);
                        }
                    }
                    final int size = size(mimeMessage);
                    messageRow.setSize(size);
                    populateFlags(mimeMessage, messageRow);

                    addHeaders(mimeMessage, messageRow);

                    MessageBody mb = populateBody(mimeMessage);
                    messageRow.addMessageBody(mb);

                    save(messageRow);
                    MessageResult messageResult = fillMessageResult(messageRow,
                            FetchGroupImpl.MINIMAL);
                    getUidChangeTracker().found(messageResult.getUid(), messageResult.getFlags(), mailboxSession.getSessionId());
                    return messageResult.getUid();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MailboxException("Parsing of message failed", e);
                } finally {
                    if (file != null) {
                        file.delete();
                    }
                }
            } else {
                // mailboxRow==null
                throw new MailboxNotFoundException(getName());
            }
        } catch (InterruptedException e) {
            throw new LockException(e);
        }
    }

    private void populateFlags(MimeMessage message, MessageRow messageRow)
    throws MessagingException, TorqueException {
        final Flags flags = message.getFlags();
        buildFlags(messageRow, flags);
    }

    private void buildFlags(MessageRow messageRow, final Flags flags)
    throws TorqueException {
        MessageFlags messageFlags = new MessageFlags();
        messageFlags.setFlags(flags);
        messageRow.addMessageFlags(messageFlags);
    }

    private MessageBody populateBody(MimeMessage message) throws IOException,
    MessagingException {
        MessageBody mb = new MessageBody();

        InputStream is = message.getInputStream();

        final byte[] bytes = MessageUtils.toByteArray(is);
        mb.setBody(bytes);
        return mb;
    }

    private void addHeaders(MimeMessage message, MessageRow messageRow)
    throws MessagingException, TorqueException {
        int line_number = 0;

        for (Enumeration lines = message.getAllHeaderLines(); lines
        .hasMoreElements();) {
            String line = (String) lines.nextElement();
            int colon = line.indexOf(": ");
            if (colon > 0) {
                line_number++;
                MessageHeader mh = new MessageHeader();
                mh.setLineNumber(line_number);
                mh.setField(line.substring(0, colon));
                // TODO avoid unlikely IOOB Exception
                mh.setValue(line.substring(colon + 2));
                messageRow.addMessageHeader(mh);
            }
        }
    }

    private int size(MimeMessage message) throws IOException,
    MessagingException {
        // TODO very ugly size mesurement
        ByteArrayOutputStream sizeBos = new ByteArrayOutputStream();
        message.writeTo(new CRLFOutputStream(sizeBos));
        final int size = sizeBos.size();
        return size;
    }

    private void save(MessageRow messageRow) throws TorqueException, LockException {
        try {
            lockForWriting();
            messageRow.save();
        } finally {
            unlockAfterWriting();
        }
    }

    private MailboxRow reserveNextUid() throws InterruptedException,
    MailboxException {
        final MailboxRow myMailboxRow;
        try {
            lockForWriting();
            myMailboxRow = getMailboxRow().consumeNextUid();
        } catch (TorqueException e) {
            throw new MailboxException("Consume next uid failed", e);
        } catch (SQLException e) {
            throw new MailboxException("Consume next uid failed", e);
        } finally {
            unlockAfterWriting();
        }
        return myMailboxRow;
    }

    private void unlockAfterWriting() {
        if (lock.isWriteLockedByCurrentThread()) {
            lock.writeLock().unlock();
        }
    }

    private Criteria criteriaForMessageSet(MessageRange set)
    throws MailboxException {
        Criteria criteria = new Criteria();
        criteria.addAscendingOrderByColumn(MessageRowPeer.UID);
        switch (set.getType()) {
            default:
            case ALL:
//              empty Criteria = everythin
                break;
            case ONE:
                criteria.add(MessageRowPeer.UID, set.getUidFrom());
                break;
            case FROM:
            case RANGE:
                Criteria.Criterion criterion1 = criteria.getNewCriterion(
                        MessageRowPeer.UID, new Long(set.getUidFrom()),
                        Criteria.GREATER_EQUAL);
                if (set.getUidTo() > 0) {
                    Criteria.Criterion criterion2 = criteria.getNewCriterion(
                            MessageRowPeer.UID, new Long(set.getUidTo()),
                            Criteria.LESS_EQUAL);
                    criterion1.and(criterion2);
                }
                criteria.add(criterion1);                
        }
        return criteria;
    }

    public Iterator getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            try {
                Criteria c = criteriaForMessageSet(set);
                c.add(MessageFlagsPeer.MAILBOX_ID, getMailboxRow()
                        .getMailboxId());
                return getMessages(fetchGroup, set, c, mailboxSession);
            } catch (TorqueException e) {
                throw new MailboxException("Search failed", e);
            } catch (MessagingException e) {
                throw new MailboxException("Parsing of message failed", e);
            }
        } finally {
            unlockAfterReading();
        }
    }

    @SuppressWarnings("unchecked")
    private TorqueResultIterator getMessages(FetchGroup result, MessageRange range,
            Criteria c, MailboxSession session) throws TorqueException, MessagingException,
            MailboxException {
        List<MessageRow> rows = MessageRowPeer.doSelectJoinMessageFlags(c);
        final Map<Long, Flags> flagsByIndex = new HashMap<Long, Flags>();
        for (MessageRow row:rows) {
            flagsByIndex.put(row.getUid(), row.getMessageFlags().createFlags());
        }
        final TorqueResultIterator results = getResults(result, rows);
        getUidChangeTracker().found(range, flagsByIndex, session.getSessionId());
        return results;
    }

    private TorqueResultIterator getResults(FetchGroup result, List rows)
    throws TorqueException {
        Collections.sort(rows, MessageRowUtils.getUidComparator());
        final TorqueResultIterator results = new TorqueResultIterator(rows,
                result);
        return results;
    }

    public MessageResult fillMessageResult(MessageRow messageRow,
            FetchGroup result) throws TorqueException, MessagingException,
            MailboxException, MimeException {
        return MessageRowUtils.loadMessageResult(messageRow, result);
    }

    public synchronized Flags getPermanentFlags() {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        return permanentFlags;
    }

    protected List<Long> recent(boolean reset, MailboxSession mailboxSession)
    throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            final Criteria criterion = queryRecentFlagSet();
            final List messageRows = getMailboxRow().getMessageRows(
                    criterion);
            final List<Long> results = new ArrayList<Long>(messageRows.size());
            for (Iterator it = messageRows.iterator(); it.hasNext();) {
                final MessageRow row = (MessageRow) it.next();
                results.add(row.getUid());
            }

            if (reset) {
                getMailboxRow().resetRecent();
            }
            return results;
        } catch (TorqueException e) {
            throw new MailboxException("Search failed", e);
        } finally {
            unlockAfterReading();
        }
    }

    private Criteria queryRecentFlagSet() {
        final Criteria criterion = new Criteria();
        criterion.addJoin(MessageFlagsPeer.MAILBOX_ID,
                MessageRowPeer.MAILBOX_ID);
        criterion.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);

        MessageFlagsPeer.addFlagsToCriteria(new Flags(Flags.Flag.RECENT), true,
                criterion);
        return criterion;
    }

    public Long getFirstUnseen(MailboxSession mailboxSession) throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            Criteria c = new Criteria();
            c.addAscendingOrderByColumn(MessageRowPeer.UID);
            c.setLimit(1);
            c.setSingleRecord(true);

            c.addJoin(MessageFlagsPeer.MAILBOX_ID,
                    MessageRowPeer.MAILBOX_ID);
            c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);

            MessageFlagsPeer.addFlagsToCriteria(new Flags(Flags.Flag.SEEN),
                    false, c);

            try {
                List messageRows = getMailboxRow().getMessageRows(c);
                if (messageRows.size() > 0) {
                    MessageResult messageResult = fillMessageResult(
                            (MessageRow) messageRows.get(0), FetchGroupImpl.MINIMAL);
                    if (messageResult != null) {
                        getUidChangeTracker().found(messageResult.getUid(), messageResult.getFlags(), mailboxSession.getSessionId());
                    }

                    return messageResult.getUid();
                } else {
                    return null;
                }
            } catch (TorqueException e) {
                throw new MailboxException("Search failed", e);
            } catch (MessagingException e) {
                throw new MailboxException("Parsing of message failed", e);
            } catch (MimeException e) {
                throw new MailboxException("Parsing of message failed", e);
            }
        } finally {
            unlockAfterReading();
        }
    }

    public int getUnseenCount(MailboxSession mailboxSession)
    throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            try {
                final int count = getMailboxRow().countMessages(
                        new Flags(Flags.Flag.SEEN), false);
                return count;
            } catch (TorqueException e) {
                throw new MailboxException("count failed", e);
            } catch (DataSetException e) {
                throw new MailboxException("count failed", e);
            }
        } finally {
            unlockAfterReading();
        }
    }

    public Iterator<Long> expunge(MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        lockForWriting();
        try {
            return doExpunge(set);
        } finally {
            unlockAfterWriting();
        }
    }

    private Iterator<Long> doExpunge(final MessageRange set)
    throws MailboxException {
        checkAccess();
        try {
            // TODO put this into a serializable transaction
            final Criteria c = criteriaForMessageSet(set);
            c.addJoin(MessageRowPeer.MAILBOX_ID, MessageFlagsPeer.MAILBOX_ID);
            c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
            c.add(MessageRowPeer.MAILBOX_ID, getMailboxRow().getMailboxId());
            c.add(MessageFlagsPeer.DELETED, true);

            final List messageRows = getMailboxRow().getMessageRows(c);
            final Collection<Long> uids = new TreeSet<Long>();

            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                MessageRow messageRow = (MessageRow) iter.next();
                uids.add(messageRow.getUid());
                Criteria todelc = new Criteria();
                todelc
                .add(MessageRowPeer.MAILBOX_ID, messageRow
                        .getMailboxId());
                todelc.add(MessageRowPeer.UID, messageRow.getUid());
                MessageRowPeer.doDelete(todelc);
            }
            getUidChangeTracker().expunged(uids);
            return uids.iterator();
        } catch (TorqueException e) {
            throw new MailboxException("delete failed", e);
        }
    }

    public Map<Long, Flags> setFlags(Flags flags, boolean value, boolean replace,
            MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        lockForWriting();
        try {
            return doSetFlags(flags, value, replace, set, mailboxSession);
        } finally {
            unlockAfterWriting();
        }
    }

    private Map<Long, Flags> doSetFlags(Flags flags, boolean value, boolean replace,
            final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        checkAccess();
        try {
            // TODO put this into a serializeable transaction
            final List messageRows = getMailboxRow().getMessageRows(criteriaForMessageSet(set));
            SortedMap<Long, Flags> newFlagsByUid = new TreeMap<Long, Flags>();
            Map<Long, Flags> originalFlagsByUid = new HashMap<Long, Flags>(32);
            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                final MessageRow messageRow = (MessageRow) iter.next();
                final MessageFlags messageFlags = messageRow.getMessageFlags();
                if (messageFlags != null) {
                    originalFlagsByUid.put(messageRow.getUid(), messageFlags.createFlags());
                    if (replace) {
                        messageFlags.setFlags(flags);
                    } else {
                        Flags current = messageFlags.createFlags();
                        if (value) {
                            current.add(flags);
                        } else {
                            current.remove(flags);
                        }
                        messageFlags.setFlags(current);
                    }
                    newFlagsByUid.put(messageRow.getUid(), messageFlags.createFlags());
                    messageFlags.save();
                }
            }
            tracker.flagsUpdated(newFlagsByUid, originalFlagsByUid, mailboxSession.getSessionId());
            return newFlagsByUid;
        } catch (TorqueException e) {
            throw new MailboxException("save failed", e);
        }
    }

    public void addListener(MailboxListener listener)
    throws MailboxException {
        checkAccess();
        tracker.addMailboxListener(listener);
    }

    public long getUidValidity(MailboxSession mailboxSession)
    throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            final long result = getMailboxRow().getUidValidity();
            return result;
        } finally {
            unlockAfterReading();
        }
    }

    public long getUidNext(MailboxSession mailboxSession)
    throws MailboxException {
        lockForReading();
        try {
            checkAccess();
            try {
                MailboxRow myMailboxRow = MailboxRowPeer
                .retrieveByPK(mailboxRow.getPrimaryKey());
                if (myMailboxRow != null) {
                    mailboxRow = myMailboxRow;
                    final long lastUid = mailboxRow.getLastUid();
                    return lastUid + 1;
                } else {
                    throw new MailboxException("consume failed");
                }
            } catch (NoRowsException e) {
                throw new MailboxException("consume failed");
            } catch (TooManyRowsException e) {
                throw new MailboxException("consume failed");
            } catch (TorqueException e) {
                throw new MailboxException("consume failed");
            }
        } finally {
            unlockAfterReading();
        }
    }

    private void checkAccess() {
        if (!open) {
            throw new RuntimeException("mailbox is closed");
        }
    }

    protected UidChangeTracker getUidChangeTracker() {
        return tracker;
    }

    protected MailboxRow getMailboxRow() {
        return mailboxRow;
    }

    protected void setMailboxRow(MailboxRow mailboxRow) {
        this.mailboxRow = mailboxRow;
    }

    public Iterator<Long> search(SearchQuery query, MailboxSession mailboxSession) throws MailboxException {
        lockForReading();
        try {
            checkAccess();

            final Criteria criterion = preSelect(query);
            final List rows = MessageRowPeer
            .doSelectJoinMessageFlags(criterion);
            final Set<Long> uids = new TreeSet<Long>();
            for (Iterator it = rows.iterator(); it.hasNext();) {
                final MessageRow row = (MessageRow) it.next();
                try {
                    if (searches.isMatch(query, row)) {
                        uids.add(row.getUid());
                    }
                } catch (TorqueException e) {
                    mailboxSession.getLog()
                    .info(
                            "Cannot test message against search criteria. Will continue to test other messages.",
                            e);
                    if (mailboxSession.getLog().isDebugEnabled())
                        mailboxSession.getLog().debug("UID: " + row.getUid());
                }
            }

            return uids.iterator();
        } catch (TorqueException e) {
            throw new MailboxException("search failed");
        } catch (MimeException e) {
            throw new MailboxException("Parsing of message failed");
        } finally {
            unlockAfterReading();
        }
    }

    private Criteria preSelect(SearchQuery query) {
        final Criteria results = new Criteria();
        final List criteria = query.getCriterias();
        if (criteria.size() == 1) {
            final Criterion criterion = (Criterion) criteria.get(0);
            if (criterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) criterion;
                preSelectUid(results, uidCriterion);
            }
        }
        results.and(MessageRowPeer.MAILBOX_ID, mailboxRow.getMailboxId());
        return results;
    }

    private void preSelectUid(final Criteria results,
            final SearchQuery.UidCriterion uidCriterion) {
        final NumericRange[] ranges = uidCriterion.getOperator().getRange();
        for (int i = 0; i < ranges.length; i++) {
            final long low = ranges[i].getLowValue();
            final long high = ranges[i].getHighValue();
            if (low == Long.MAX_VALUE) {
                results.add(MessageRowPeer.UID, high, Criteria.LESS_EQUAL);
            } else if (low == high) {
                results.add(MessageRowPeer.UID, low);
            } else {
                final Criteria.Criterion fromCriterion = results
                .getNewCriterion(MessageRowPeer.UID, new Long(low),
                        Criteria.GREATER_EQUAL);
                if (high > 0 && high < Long.MAX_VALUE) {
                    final Criteria.Criterion toCriterion = results
                    .getNewCriterion(MessageRowPeer.UID,
                            new Long(high), Criteria.LESS_EQUAL);
                    fromCriterion.and(toCriterion);
                }
                results.add(fromCriterion);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#isWriteable(org.apache.james.mailbox.MailboxSession)
     */
    public boolean isWriteable(MailboxSession session) {
        return true;
    }

    public void copyTo(MessageRange set, TorqueMailbox toMailbox,
            MailboxSession session) throws MailboxException {
        final List rows;
        lockForReading();
        try {
            checkAccess();
            try {
                final Criteria c = criteriaForMessageSet(set);
                c.add(MessageFlagsPeer.MAILBOX_ID, getMailboxRow()
                        .getMailboxId());
                rows = MessageRowPeer.doSelectJoinMessageFlags(c);
                
            } catch (TorqueException e) {
                throw new MailboxException("save failed");
            } catch (MailboxException e) {
                throw new MailboxException("parsing of message failed");
            }
        } finally {
            unlockAfterReading();
        }
        // Release read lock before copying
        toMailbox.copy(rows, session);
    }

    private void unlockAfterReading() {
        try {
            lock.readLock().unlock();
        } catch (RuntimeException e) {
            // Swallow
        }
    }

    private void copy(List rows, MailboxSession session)
    throws MailboxException {
        try {
            for (Iterator iter = rows.iterator(); iter.hasNext();) {
                MessageRow fromRow = (MessageRow) iter.next();
                final MailboxRow mailbox = reserveNextUid();

                if (mailbox != null) {
                    // To be thread safe, we first get our own copy and the
                    // exclusive
                    // Uid
                    // TODO create own message_id and assign uid later
                    // at the moment it could lead to the situation that uid 5
                    // is
                    // inserted long before 4, when
                    // mail 4 is big and comes over a slow connection.
                    long uid = mailbox.getLastUid();
                    this.mailboxRow = mailbox;

                    MessageRow newRow = new MessageRow();
                    newRow.setMailboxId(getMailboxRow().getMailboxId());
                    newRow.setUid(uid);
                    newRow.setInternalDate(fromRow.getInternalDate());
                    newRow.setSize(fromRow.getSize());
                    buildFlags(newRow, fromRow.getMessageFlags()
                            .createFlags());

                    final List headers = fromRow.getMessageHeaders();
                    for (Iterator iterator = headers.iterator(); iterator
                    .hasNext();) {
                        final MessageHeader fromHeader = (MessageHeader) iterator
                        .next();
                        final MessageHeader newHeader = new MessageHeader(
                                fromHeader.getField(), fromHeader.getValue(),
                                fromHeader.getLineNumber());
                        newRow.addMessageHeader(newHeader);
                    }

                    MessageBody mb = new MessageBody(fromRow.getBodyContent());
                    newRow.addMessageBody(mb);

                    save(newRow);
                    MessageResult messageResult = fillMessageResult(newRow,
                            FetchGroupImpl.MINIMAL);
                    getUidChangeTracker().found(messageResult.getUid(), messageResult.getFlags(), session.getSessionId());
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException("save failed");
        } catch (InterruptedException e) {
            throw new LockException();
        } catch (MessagingException e) {
            throw new MailboxException("parsing of message failed");
        } catch (MimeException e) {
            throw new MailboxException("parsing of message failed");

        }
    }

    public void deleted(MailboxSession session) {
        tracker.mailboxDeleted(session.getSessionId());
    }

    public void reportRenamed(String from, MailboxRow mailboxRow, MailboxSession session) {
        tracker.reportRenamed(getMailboxPath(mailboxRow.getName()), session.getSessionId());
        this.mailboxRow = mailboxRow;
    }

    public MailboxPath getMailboxPath(String name) {
        String nameParts[] = name.split("\\" +MailboxConstants.DEFAULT_DELIMITER_STRING,3);
        if (nameParts.length < 3) {
            return new MailboxPath(nameParts[0], null, nameParts[1]);
        }
        return new MailboxPath(nameParts[0], nameParts[1], nameParts[2]);

    }
    /**
     * @see org.apache.james.mailbox.MessageManager#getMetaData(boolean, MailboxSession, org.apache.james.mailbox.MessageManager.MetaData.FetchGroup)
     */
    public MetaData getMetaData(boolean resetRecent, MailboxSession mailboxSession, MessageManager.MetaData.FetchGroup fetchGroup) throws MailboxException {
        final List<Long> recent = recent(resetRecent, mailboxSession);
        final Flags permanentFlags = getPermanentFlags();
        final long uidValidity = getUidValidity(mailboxSession);
        final long uidNext = getUidNext(mailboxSession);
        final long messageCount = getMessageCount(mailboxSession);
        final int unseenCount;
        final Long firstUnseen;
        switch (fetchGroup) {
            case UNSEEN_COUNT:
                unseenCount = getUnseenCount(mailboxSession);
                firstUnseen = null;
                break;
            case FIRST_UNSEEN:
                firstUnseen = getFirstUnseen(mailboxSession);
                unseenCount = 0;
                break;
            default:
                firstUnseen = null;
            unseenCount = 0;
            break;
        }

        return new MailboxMetaData(recent, permanentFlags, uidValidity, uidNext, messageCount, unseenCount, firstUnseen, isWriteable(mailboxSession));
    }
}
