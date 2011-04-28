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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.mail.Flags;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.store.mail.model.Message;

/**
 * {@link Iterator} implementation for {@link MessageResult}
 *
 */
public class ResultIterator<Id> implements Iterator<MessageResult> {

    private final Iterator<Message<Id>> messages;

    private final FetchGroup fetchGroup;

    public ResultIterator(final Iterator<Message<Id>> messages, final FetchGroup fetchGroup) {
        super();
        if (messages == null) {
            this.messages = new ArrayList<Message<Id>>().iterator();
        } else {
            this.messages = messages;
        }
        this.fetchGroup = fetchGroup;
    }
    

    /*
     * (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return messages.hasNext();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public MessageResult next() {
        if (hasNext() == false) {
            throw new NoSuchElementException("No such element.");
        }
        final Message<Id> message = messages.next();
        MessageResult result;
        try {

            result = ResultUtils.loadMessageResult(message, this.fetchGroup);
        } catch (MailboxException e) {
            result = new UnloadedMessageResult<Id>(message, e);
        }
        return result;
    }

    /**
     * Remove is not supported
     */
    public void remove() {
        throw new UnsupportedOperationException("Read only iteration.");
    }

    private static final class UnloadedMessageResult<Id> implements MessageResult {
        private final MailboxException exception;

        private final Date internalDate;

        private final long size;

        private final long uid;

        public UnloadedMessageResult(final Message<Id> message,
                final MailboxException exception) {
            super();
            internalDate = message.getInternalDate();
            size = message.getFullContentOctets();
            uid = message.getUid();
            this.exception = exception;
        }

        public Flags getFlags() throws MailboxException {
            throw exception;
        }

        public Content getFullContent() throws MailboxException {
            throw exception;
        }

        public Date getInternalDate() {
            return internalDate;
        }
        public Content getBody() throws MailboxException {
            throw exception;
        }

        public long getSize() {
            return size;
        }

        public long getUid() {
            return uid;
        }

        public Iterator<Header> headers() throws MailboxException {
            throw exception;
        }

        public int compareTo(MessageResult that) {
            // Java 1.5 return (int) Math.signum(uid - that.getUid());
            long diff = uid - that.getUid();
            return (int) diff == 0 ? 0 : diff > 0 ? 1 : -1;
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
