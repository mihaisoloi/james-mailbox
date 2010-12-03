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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.torque.om.MessageRow;
import org.apache.james.mailbox.util.FetchGroupImpl;
import org.apache.james.mime4j.MimeException;
import org.apache.torque.TorqueException;

/**
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class TorqueResultIterator implements Iterator {

    private final Buffer messageRows;

    private final FetchGroup fetchGroup;

    public TorqueResultIterator(final Collection messageRows,
            final FetchGroup fetchGroup) {
        super();
        if (messageRows == null || messageRows.isEmpty()) {
            this.messageRows = BufferUtils.EMPTY_BUFFER;
        } else {
            this.messageRows = new BoundedFifoBuffer(messageRows);
        }
        this.fetchGroup = fetchGroup;
    }

    public MessageFlags[] getMessageFlags() throws TorqueException {
        final MessageFlags[] results = MessageRowUtils
                .toMessageFlags(messageRows);
        return results;
    }

    /**
     * Iterates over the contained rows.
     * 
     * @return <code>Iterator</code> for message rows
     */
    public final Iterator iterateRows() {
        return messageRows.iterator();
    }

    public boolean hasNext() {
        return !messageRows.isEmpty();
    }

    public Object next() {
        if (messageRows.isEmpty()) {
            throw new NoSuchElementException("No such element.");
        }
        final MessageRow messageRow = (MessageRow) messageRows.remove();
        MessageResult result;
        try {

            result = MessageRowUtils.loadMessageResult(messageRow,
                    this.fetchGroup);
        } catch (TorqueException e) {
            result = new UnloadedMessageResult(messageRow,
                    new MailboxException("Search failed", e));
        } catch (MailboxException e) {
            result = new UnloadedMessageResult(messageRow, e);
        } catch (MimeException e) {
            result = new UnloadedMessageResult(messageRow, new MailboxException("Parsing message failed", e));
        }
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("Read only iteration.");
    }

    private static final class UnloadedMessageResult implements MessageResult {
        private static final FetchGroup FETCH_GROUP = FetchGroupImpl.MINIMAL;

        private final MailboxException exception;

        private final Date internalDate;

        private final int size;

        private final long uid;

        public UnloadedMessageResult(final MessageRow row,
                final MailboxException exception) {
            super();
            internalDate = row.getInternalDate();
            size = row.getSize();
            uid = row.getUid();
            this.exception = exception;
        }

        public Flags getFlags() throws MailboxException {
            throw exception;
        }

        public Content getFullContent() throws MailboxException {
            throw exception;
        }

        public FetchGroup getIncludedResults() {
            return FETCH_GROUP;
        }

        public Date getInternalDate() {
            return internalDate;
        }

        public String getKey() {
            return null;
        }

        public Content getBody() throws MailboxException {
            throw exception;
        }

        public MimeMessage getMimeMessage() throws MailboxException {
            throw exception;
        }

        public int getMsn() {
            return 0;
        }

        public long getSize() {
            return size;
        }

        public long getUid() {
            return uid;
        }

        public long getUidValidity() {
            return 0;
        }

        public Iterator<Header> headers() throws MailboxException {
            throw exception;
        }

        public int compareTo(MessageResult that) {
            // Java 1.5 return (int) Math.signum(uid - that.getUid());
            long diff = uid - that.getUid();
            return (int) diff == 0 ? 0 : diff > 0 ? 1 : -1;
        }

        public Content getMessageBody(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Content getFullContent(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Iterator<Header> iterateHeaders(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Iterator<Header> iterateMimeHeaders(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Content getBody(MimePath path) throws MailboxException {
            throw exception;
        }

        public Content getMimeBody(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public MimeDescriptor getMimeDescriptor()
                throws MailboxException {
            throw exception;
        }

    }
}
