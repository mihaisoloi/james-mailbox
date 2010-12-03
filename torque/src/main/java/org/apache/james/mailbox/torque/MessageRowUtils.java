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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.MessageResult.MimePath;
import org.apache.james.mailbox.store.MimeDescriptorImpl;
import org.apache.james.mailbox.store.ResultHeader;
import org.apache.james.mailbox.store.streaming.ByteContent;
import org.apache.james.mailbox.store.streaming.FullByteContent;
import org.apache.james.mailbox.store.streaming.PartContentBuilder;
import org.apache.james.mailbox.torque.om.MessageBody;
import org.apache.james.mailbox.torque.om.MessageHeader;
import org.apache.james.mailbox.torque.om.MessageRow;
import org.apache.james.mailbox.util.MessageResultImpl;
import org.apache.james.mime4j.MimeException;
import org.apache.torque.TorqueException;

/**
* @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class MessageRowUtils {

    public static final byte[] BYTES_NEW_LINE = { 0x0D, 0x0A };

    public static final byte[] BYTES_HEADER_FIELD_VALUE_SEP = { 0x3A, 0x20 };

    static final Charset US_ASCII = Charset.forName("US-ASCII");

    /**
     * Converts {@link MessageRow} to {@link MessageFlags}.
     * 
     * @param messageRows
     *            <code>Collectio</code> of {@link MessageRow}, not null
     * @return <code>MessageFlags</code>, not null
     * @throws TorqueException
     */
    public static MessageFlags[] toMessageFlags(final Collection messageRows)
            throws TorqueException {
        final MessageFlags[] results = new MessageFlags[messageRows.size()];
        int i = 0;
        for (final Iterator it = messageRows.iterator(); it.hasNext();) {
            MessageRow row = (MessageRow) it.next();
            final Flags flags = row.getMessageFlags().createFlags();
            final long uid = row.getUid();
            results[i++] = new MessageFlags(uid, flags);
        }
        return results;
    }

    public static List createHeaders(MessageRow messageRow)
            throws TorqueException {
        final List headers = getSortedHeaders(messageRow);

        final List results = new ArrayList(headers.size());
        for (Iterator it = headers.iterator(); it.hasNext();) {
            final MessageHeader messageHeader = (MessageHeader) it.next();
            final ResultHeader header = new ResultHeader(messageHeader);
            results.add(header);
        }
        return results;
    }

    private static List getSortedHeaders(MessageRow messageRow)
            throws TorqueException {
        final List<MessageHeader> headers = messageRow.getMessageHeaders();
        Collections.sort(headers, new Comparator<MessageHeader>() {

            public int compare(MessageHeader one, MessageHeader two) {
                return  one.getLineNumber()
                        - two.getLineNumber();
            }

        });
        return headers;
    }

    public static Content createBodyContent(MessageRow messageRow)
            throws TorqueException {
        final MessageBody body = (MessageBody) messageRow.getMessageBodys()
                .get(0);
        final byte[] bytes = body.getBody();
        final ByteContent result = new ByteContent(ByteBuffer.wrap(bytes));
        return result;
    }

    public static Content createFullContent(final MessageRow messageRow,
            List headers) throws TorqueException {
        if (headers == null) {
            headers = createHeaders(messageRow);
        }
        final MessageBody body = (MessageBody) messageRow.getMessageBodys()
                .get(0);
        final byte[] bytes = body.getBody();
        FullByteContent results;
        try {
            return new FullByteContent(ByteBuffer.wrap(bytes), headers);
        } catch (IOException e) {
            throw new TorqueException("Unable to parse message",e);
        }
    }

    public static MessageResult loadMessageResult(final MessageRow messageRow,
            final FetchGroup fetchGroup) throws TorqueException,
            MailboxException, MimeException {

        MessageResultImpl messageResult = new MessageResultImpl();
        messageResult.setUid(messageRow.getUid());
        if (fetchGroup != null) {
            int content = fetchGroup.content();
            org.apache.james.mailbox.torque.om.MessageFlags messageFlags = messageRow
            .getMessageFlags();
            if (messageFlags != null) {
                messageResult.setFlags(messageFlags.createFlags());
            }
            messageResult.setSize(messageRow.getSize());
            messageResult.setInternalDate(messageRow.getInternalDate());
            if ((content & FetchGroup.HEADERS) > 0) {
                addHeaders(messageRow, messageResult);
                content -= FetchGroup.HEADERS;
            }
            if ((content & FetchGroup.BODY_CONTENT) > 0) {
                addBody(messageRow, messageResult);
                content -= FetchGroup.BODY_CONTENT;
            }
            if ((content & FetchGroup.FULL_CONTENT) > 0) {
                addFullContent(messageRow, messageResult);
                content -= FetchGroup.FULL_CONTENT;
            }
            if ((content & FetchGroup.MIME_DESCRIPTOR) > 0) {
                addMimeDescriptor(messageRow, messageResult);
                content -= FetchGroup.MIME_DESCRIPTOR;
            }
            if (content != 0) {
                throw new TorqueException("Unsupported result: " + content);
            }
            try {
                addPartContent(fetchGroup, messageRow, messageResult);
            } catch (IOException e) {
                throw new TorqueException("Cannot parse message", e);
            } catch (MimeException e) {
                throw new TorqueException("Cannot parse message", e);
            }
        }
        return messageResult;
    }

    private static void addMimeDescriptor(MessageRow messageRow,
            MessageResultImpl messageResult) throws TorqueException, MimeException {
        try {
            MimeDescriptor descriptor = MimeDescriptorImpl
                    .build(toInput(messageRow));
            messageResult.setMimeDescriptor(descriptor);
        } catch (IOException e) {
            throw new TorqueException("Cannot parse message", e);
        }
    }

    private static void addFullContent(final MessageRow messageRow,
            MessageResultImpl messageResult) throws TorqueException,
            MailboxException {
        final List headers = messageResult.getHeaders();
        final Content content = createFullContent(messageRow, headers);
        messageResult.setFullContent(content);
    }

    private static void addBody(final MessageRow messageRow,
            MessageResultImpl messageResult) throws TorqueException {
        final Content content = createBodyContent(messageRow);
        messageResult.setBody(content);
    }

    private static void addHeaders(final MessageRow messageRow,
            MessageResultImpl messageResult) throws TorqueException {
        final List headers = createHeaders(messageRow);
        messageResult.setHeaders(headers);
    }

    private static void addPartContent(final FetchGroup fetchGroup,
            MessageRow row, MessageResultImpl messageResult)
            throws TorqueException, MailboxException, IOException,
            MimeException {
        Collection<FetchGroup.PartContentDescriptor> partContent = fetchGroup.getPartContentDescriptors();
        if (partContent != null) {
            for (FetchGroup.PartContentDescriptor descriptor:partContent) {
                addPartContent(descriptor, row, messageResult);
            }
        }
    }

    private static void addPartContent(
            FetchGroup.PartContentDescriptor descriptor, MessageRow row,
            MessageResultImpl messageResult) throws TorqueException,
            MailboxException, IOException, MimeException {
        final MimePath mimePath = descriptor.path();
        final int content = descriptor.content();
        if ((content & MessageResult.FetchGroup.FULL_CONTENT) > 0) {
            addFullContent(row, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.BODY_CONTENT) > 0) {
            addBodyContent(row, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.MIME_CONTENT) > 0) {
            addMimeBodyContent(row, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.HEADERS) > 0) {
            addHeaders(row, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.MIME_HEADERS) > 0) {
            addMimeHeaders(row, messageResult, mimePath);
        }
    }

    private static PartContentBuilder build(int[] path, final MessageRow row)
            throws IOException, MimeException, TorqueException {
        final InputStream stream = toInput(row);
        PartContentBuilder result = new PartContentBuilder();
        result.parse(stream);
        try {
            for (int i = 0; i < path.length; i++) {
                final int next = path[i];
                result.to(next);
            }
        } catch (PartContentBuilder.PartNotFoundException e) {
            // Missing parts should return zero sized content
            // See http://markmail.org/message/2jconrj7scvdi5dj
            result.markEmpty();
        }
        return result;
    }

    public static InputStream toInput(final MessageRow row)
            throws TorqueException {
        final List headers = getSortedHeaders(row);
        final StringBuffer headersToString = new StringBuffer(
                headers.size() * 50);
        for (Iterator it = headers.iterator(); it.hasNext();) {
            MessageHeader header = (MessageHeader) it.next();
            headersToString.append(header.getField());
            headersToString.append(": ");
            headersToString.append(header.getValue());
            headersToString.append("\r\n");
        }
        headersToString.append("\r\n");

        byte[] bodyContent = row.getBodyContent();
        final MessageInputStream stream = new MessageInputStream(
                headersToString, bodyContent);
        return stream;
    }

    private static final class MessageInputStream extends InputStream {
        private final StringBuffer headers;

        private final ByteBuffer bodyContent;

        private int headerPosition = 0;

        public MessageInputStream(final StringBuffer headers,
                final byte[] bodyContent) {
            super();
            this.headers = headers;
            this.bodyContent = ByteBuffer.wrap(bodyContent);
        }

        public int read() throws IOException {
            final int result;
            if (headerPosition < headers.length()) {
                result = headers.charAt(headerPosition++);
            } else if (bodyContent.hasRemaining()) {
                result = bodyContent.get();
            } else {
                result = -1;
            }
            return result;
        }

    }

    private static final int[] path(MimePath mimePath) {
        final int[] result;
        if (mimePath == null) {
            result = null;
        } else {
            result = mimePath.getPositions();
        }
        return result;
    }

    private static void addHeaders(MessageRow row,
            MessageResultImpl messageResult, MimePath mimePath)
            throws TorqueException, IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addHeaders(row, messageResult);
        } else {
            final PartContentBuilder builder = build(path, row);
            final List headers = builder.getMessageHeaders();
            messageResult.setHeaders(mimePath, headers.iterator());
        }
    }

    private static void addMimeHeaders(MessageRow row,
            MessageResultImpl messageResult, MimePath mimePath)
            throws TorqueException, IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addHeaders(row, messageResult);
        } else {
            final PartContentBuilder builder = build(path, row);
            final List headers = builder.getMimeHeaders();
            messageResult.setMimeHeaders(mimePath, headers.iterator());
        }
    }

    private static void addBodyContent(MessageRow row,
            MessageResultImpl messageResult, MimePath mimePath)
            throws TorqueException, IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addBody(row, messageResult);
        } else {
            final PartContentBuilder builder = build(path, row);
            final Content content = builder.getMessageBodyContent();
            messageResult.setBodyContent(mimePath, content);
        }
    }

    private static void addMimeBodyContent(MessageRow row,
            MessageResultImpl messageResult, MimePath mimePath)
            throws TorqueException, IOException, MimeException {
        final int[] path = path(mimePath);
        final PartContentBuilder builder = build(path, row);
        final Content content = builder.getMimeBodyContent();
        messageResult.setMimeBodyContent(mimePath, content);
    }

    private static void addFullContent(MessageRow row,
            MessageResultImpl messageResult, MimePath mimePath)
            throws TorqueException, MailboxException, IOException,
            MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addFullContent(row, messageResult);
        } else {
            final PartContentBuilder builder = build(path, row);
            final Content content = builder.getFullContent();
            messageResult.setFullContent(mimePath, content);
        }
    }

    /**
     * Gets a comparator that evaluates {@link MessageRow}'s on the basis of
     * their UIDs.
     * 
     * @return {@link Comparator}, not null
     */
    public static Comparator getUidComparator() {
        return UidComparator.INSTANCE;
    }

    private static final class UidComparator implements Comparator {
        private static final UidComparator INSTANCE = new UidComparator();

        public int compare(Object one, Object two) {
            final MessageRow rowOne = (MessageRow) one;
            final MessageRow rowTwo = (MessageRow) two;
            final int result = (int) (rowOne.getUid() - rowTwo.getUid());
            return result;
        }

    }
}
