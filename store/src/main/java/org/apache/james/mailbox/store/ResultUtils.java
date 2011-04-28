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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.text.Document;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.MessageResult.MimePath;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.streaming.InputStreamContent;
import org.apache.james.mailbox.store.streaming.InputStreamContent.Type;
import org.apache.james.mailbox.store.streaming.PartContentBuilder;
import org.apache.james.mailbox.util.MessageResultImpl;
import org.apache.james.mime4j.MimeException;

/**
 *
 */
public class ResultUtils {

    public static final byte[] BYTES_NEW_LINE = { 0x0D, 0x0A };

    public static final byte[] BYTES_HEADER_FIELD_VALUE_SEP = { 0x3A, 0x20 };

    static final Charset US_ASCII = Charset.forName("US-ASCII");

    public static List<MessageResult.Header> createHeaders(final Message<?> document) {
        final List<Header> headers = getSortedHeaders(document);

        final List<MessageResult.Header> results = new ArrayList<MessageResult.Header>(headers.size());
        for (Header header: headers) {
            final ResultHeader resultHeader = new ResultHeader(header);
            results.add(resultHeader);
        }
        return results;
    }

    private static List<Header> getSortedHeaders(final Message<?> document) {
        final List<Header> headers = new ArrayList<Header>(document.getHeaders());
        Collections.sort(headers);
        return headers;
    }

    /**
     * Return the {@link Content} which holds only the Body for the given {@link MailboxMembership}
     * 
     * @param membership
     * @return bodyContent
     * @throws IOException 
     */
    public static Content createBodyContent(Message<?> membership) throws IOException {
        final InputStreamContent result = new InputStreamContent(membership, Type.Body);
        return result;
    }

    /**
     * Return the {@link Content} which holds the full data for the given {@link MailboxMembership}
     * 
     * @param membership
     * @return content
     * @throws IOException 
     */
    public static Content createFullContent(final Message<?> membership) throws IOException {
        final InputStreamContent result = new InputStreamContent(membership, Type.Full);
        return result;
    }

    /**
     * Return the {@link MessageResult} for the given {@link MailboxMembership} and {@link FetchGroup}
     * 
     * @param message
     * @param fetchGroup
     * @return result
     * @throws MailboxException
     */
    public static MessageResult loadMessageResult(final Message<?> message, final FetchGroup fetchGroup) 
                throws MailboxException {

        MessageResultImpl messageResult = new MessageResultImpl();
        messageResult.setUid(message.getUid());
        if (fetchGroup != null) {
            int content = fetchGroup.content();
            messageResult.setFlags(message.createFlags());
            messageResult.setSize((int)message.getFullContentOctets());
            messageResult.setInternalDate(message.getInternalDate());

            try {

                if ((content & FetchGroup.HEADERS) > 0) {
                    addHeaders(message, messageResult);
                    content -= FetchGroup.HEADERS;
                }
                if ((content & FetchGroup.BODY_CONTENT) > 0) {
                    addBody(message, messageResult);
                    content -= FetchGroup.BODY_CONTENT;
                }
                if ((content & FetchGroup.FULL_CONTENT) > 0) {
                    addFullContent(message, messageResult);
                    content -= FetchGroup.FULL_CONTENT;
                }
                if ((content & FetchGroup.MIME_DESCRIPTOR) > 0) {
                    addMimeDescriptor(message, messageResult);
                    content -= FetchGroup.MIME_DESCRIPTOR;
                }
                if (content != 0) {
                    throw new UnsupportedOperationException("Unsupported result: " + content);
                }

                addPartContent(fetchGroup, message, messageResult);
            } catch (IOException e) {
                throw new MailboxException("Unable to parse message", e);
            } catch (MimeException e) {
                throw new MailboxException("Unable to parse message", e);
            }
        }
        return messageResult;
    }

    private static void addMimeDescriptor(Message<?> message, MessageResultImpl messageResult) throws IOException, MimeException {
            MimeDescriptor descriptor = MimeDescriptorImpl.build(message);
            messageResult.setMimeDescriptor(descriptor);
    }

    private static void addFullContent(final Message<?> messageRow, MessageResultImpl messageResult) throws IOException {
        Content content = createFullContent(messageRow);
        messageResult.setFullContent(content);

    }

    private static void addBody(final Message<?> message, MessageResultImpl messageResult)throws IOException {
        final Content content = createBodyContent(message);
        messageResult.setBody(content);

    }

    private static void addHeaders(final Message<?> message,
            MessageResultImpl messageResult) {
        final List<MessageResult.Header> headers = createHeaders(message);
        messageResult.setHeaders(headers);
    }

    private static void addPartContent(final FetchGroup fetchGroup,
            Message<?> message, MessageResultImpl messageResult)
            throws MailboxException, IOException,
            MimeException {
        Collection<FetchGroup.PartContentDescriptor> partContent = fetchGroup.getPartContentDescriptors();
        if (partContent != null) {
            for (FetchGroup.PartContentDescriptor descriptor: partContent) {
                addPartContent(descriptor, message, messageResult);
            }
        }
    }

    private static void addPartContent(
            FetchGroup.PartContentDescriptor descriptor, Message<?> message,
            MessageResultImpl messageResult) throws 
            MailboxException, IOException, MimeException {
        final MimePath mimePath = descriptor.path();
        final int content = descriptor.content();
        if ((content & MessageResult.FetchGroup.FULL_CONTENT) > 0) {
            addFullContent(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.BODY_CONTENT) > 0) {
            addBodyContent(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.MIME_CONTENT) > 0) {
            addMimeBodyContent(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.HEADERS) > 0) {
            addHeaders(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.MIME_HEADERS) > 0) {
            addMimeHeaders(message, messageResult, mimePath);
        }
    }

    private static PartContentBuilder build(int[] path, final Message<?> message)
            throws IOException, MimeException {
        final InputStream stream = toInput(message);
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
   

    /**
     * Return an {@link InputStream} which holds the content of the given {@link org.apache.james.mailbox.store.mail.model.Message}
     * 
     * @param document
     * @return stream
     * @throws IOException 
     */
    public static InputStream toInput(final Message<?> document) throws IOException {
        final List<Header> headers = getSortedHeaders(document);
        final StringBuffer headersToString = new StringBuffer(headers.size() * 50);
        for (Header header: headers) {
            headersToString.append(header.getFieldName());
            headersToString.append(": ");
            headersToString.append(header.getValue());
            headersToString.append("\r\n");
        }
        headersToString.append("\r\n");
        final InputStream bodyContent = document.getBodyContent();
        final MessageInputStream stream = new MessageInputStream(headersToString, bodyContent);
        return stream;
    }


    private static final class MessageInputStream extends FilterInputStream {
        private final StringBuffer headers;
        private int headerPosition = 0;

        public MessageInputStream(final StringBuffer headers,
                final InputStream bodyContent) throws IOException{
            super(bodyContent);
            
            this.headers = headers;
        }

        public int read() throws IOException {
            final int result;
            if (headerPosition < headers.length()) {
                result = headers.charAt(headerPosition++);
            } else  { 
                result = super.read();
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (headerPosition < headers.length()) {
                int headersLeft = headers.length() - headerPosition;
                if (len > headersLeft) {
                    int i;
                    for (i = 0; i < headersLeft; i++) {
                        int a =  read();
                        if (a == -1) {
                            return i;
                        } 
                        b[off +i] = (byte) a;
                    }
                    int bytesLeft = len - headersLeft;
                    return i + super.read(b, off +i, bytesLeft);
                } else {

                    for (int i = 0 ; i < len; i++) {
                        int a =  read();
                        if (a == -1) {
                            return -1;
                        } 
                        b[off +i] = (byte) a;
                    }
                    return len;
                }
            }
            return super.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
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

    private static void addHeaders(Message<?> message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addHeaders(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final List<MessageResult.Header> headers = builder.getMessageHeaders();
            messageResult.setHeaders(mimePath, headers.iterator());
        }
    }

    private static void addMimeHeaders(Message<?> message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addHeaders(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final List<MessageResult.Header> headers = builder.getMimeHeaders();
            messageResult.setMimeHeaders(mimePath, headers.iterator());
        }
    }

    private static void addBodyContent(Message<?> message,
            MessageResultImpl messageResult, MimePath mimePath) throws IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addBody(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final Content content = builder.getMessageBodyContent();
            messageResult.setBodyContent(mimePath, content);
        }
    }

    private static void addMimeBodyContent(Message<?> message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws IOException, MimeException {
        final int[] path = path(mimePath);
        final PartContentBuilder builder = build(path, message);
        final Content content = builder.getMimeBodyContent();
        messageResult.setMimeBodyContent(mimePath, content);
    }

    private static void addFullContent(Message<?> message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws MailboxException, IOException,
            MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addFullContent(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final Content content = builder.getFullContent();
            messageResult.setFullContent(mimePath, content);
        }
    }

    /**
     * Gets a comparator that evaluates {@link Document}'s on the basis of
     * their UIDs.
     * 
     * @return {@link Comparator}, not null
     */
    public static Comparator<Message<?>> getUidComparator() {
        return UidComparator.INSTANCE;
    }

    private static final class UidComparator implements Comparator<Message<?>> {
        private static final UidComparator INSTANCE = new UidComparator();

        public int compare(Message<?> one, Message<?> two) {
            final int result = (int) (one.getUid() - two.getUid());
            return result;
        }

    }
}
