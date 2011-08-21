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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.Headers;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.streaming.InputStreamContent;
import org.apache.james.mailbox.store.streaming.InputStreamContent.Type;

/**
 * Bean based implementation.
 */
public class MessageResultImpl implements MessageResult {

    private final Map<MimePath, PartContent> partsByPath = new HashMap<MimePath, PartContent>();

    private MimeDescriptor mimeDescriptor;

	private final Message<?> message;

    private HeadersImpl headers;
    private Content fullContent;
    private Content bodyContent;

    
    public MessageResultImpl(Message<?> message) throws IOException {
        this.message = message;
        this.headers = new HeadersImpl(message);
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getUid()
     */
    public long getUid() {
        return message.getUid();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getInternalDate()
     */
    public Date getInternalDate() {
        return message.getInternalDate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getFlags()
     */
    public Flags getFlags() {
        return message.createFlags();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getSize()
     */
    public long getSize() {
        return message.getFullContentOctets();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MessageResult that) {
        if (getUid() > 0 && that.getUid() > 0) {
            // TODO: this seems inefficient
            return Long.valueOf(getUid()).compareTo(Long.valueOf(that.getUid()));
        } else {
            // TODO: throwing an undocumented untyped runtime seems wrong
            // TODO: if uids must be greater than zero then this should be
            // enforced
            // TODO: on the way in
            // TODO: probably an IllegalArgumentException would be better
            throw new RuntimeException("can't compare");
        }

    }
   
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getFullContent()
     */
    public synchronized final Content getFullContent() throws IOException {
        if (fullContent == null) {
            fullContent = new InputStreamContent(message, Type.Full);
        }
        return fullContent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getBody()
     */
    public synchronized final Content getBody() throws IOException {
        if (bodyContent == null) {
            bodyContent = new InputStreamContent(message, Type.Body);
        }
        return bodyContent;
    }


    /**
     * Renders suitably for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String TAB = " ";

        String retValue = "MessageResultImpl ( " + "uid = " + getUid() + TAB + "flags = " + getFlags() + TAB + "size = " + getSize() + TAB + "internalDate = " + getInternalDate()+ ")";

        return retValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MessageResult#getBody(org.apache.james.mailbox
     * .MessageResult.MimePath)
     */
    public Content getBody(MimePath path) throws MailboxException {
        final Content result;
        final PartContent partContent = getPartContent(path);
        if (partContent == null) {
            result = null;
        } else {
            result = partContent.getBody();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MessageResult#getMimeBody(org.apache.james.mailbox
     * .MessageResult.MimePath)
     */
    public Content getMimeBody(MimePath path) throws MailboxException {
        final Content result;
        final PartContent partContent = getPartContent(path);
        if (partContent == null) {
            result = null;
        } else {
            result = partContent.getMimeBody();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MessageResult#getFullContent(org.apache.james
     * .mailbox.MessageResult.MimePath)
     */
    public Content getFullContent(MimePath path) throws MailboxException {
        final Content result;
        final PartContent partContent = getPartContent(path);
        if (partContent == null) {
            result = null;
        } else {
            result = partContent.getFull();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MessageResult#iterateHeaders(org.apache.james
     * .mailbox.MessageResult.MimePath)
     */
    public Iterator<Header> iterateHeaders(MimePath path) throws MailboxException {
        final Iterator<Header> result;
        final PartContent partContent = getPartContent(path);
        if (partContent == null) {
            result = null;
        } else {
            result = partContent.getHeaders();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MessageResult#iterateMimeHeaders(org.apache.
     * james.mailbox.MessageResult.MimePath)
     */
    public Iterator<Header> iterateMimeHeaders(MimePath path) throws MailboxException {
        final Iterator<Header> result;
        final PartContent partContent = getPartContent(path);
        if (partContent == null) {
            result = null;
        } else {
            result = partContent.getMimeHeaders();
        }
        return result;
    }

    public void setBodyContent(MimePath path, Content content) {
        final PartContent partContent = getPartContent(path);
        partContent.setBody(content);
    }

    public void setMimeBodyContent(MimePath path, Content content) {
        final PartContent partContent = getPartContent(path);
        partContent.setMimeBody(content);
    }

    public void setFullContent(MimePath path, Content content) {
        final PartContent partContent = getPartContent(path);
        partContent.setFull(content);
    }

    public void setHeaders(MimePath path, Iterator<Header> headers) {
        final PartContent partContent = getPartContent(path);
        partContent.setHeaders(headers);
    }

    public void setMimeHeaders(MimePath path, Iterator<Header> headers) {
        final PartContent partContent = getPartContent(path);
        partContent.setMimeHeaders(headers);
    }

    private PartContent getPartContent(MimePath path) {
        PartContent result = (PartContent) partsByPath.get(path);
        if (result == null) {
            result = new PartContent();
            partsByPath.put(path, result);
        }
        return result;
    }

    private static final class PartContent {
        private Content body;

        private Content mimeBody;

        private Content full;

        private Iterator<Header> headers;

        private Iterator<Header> mimeHeaders;

        private int content;

        public Content getBody() {
            return body;
        }

        public void setBody(Content body) {
            content = content | FetchGroup.BODY_CONTENT;
            this.body = body;
        }

        public Content getMimeBody() {
            return mimeBody;
        }

        public void setMimeBody(Content mimeBody) {
            content = content | FetchGroup.MIME_CONTENT;
            this.mimeBody = mimeBody;
        }

        public Content getFull() {
            return full;
        }

        public void setFull(Content full) {
            content = content | FetchGroup.FULL_CONTENT;
            this.full = full;
        }

        public Iterator<Header> getHeaders() {
            return headers;
        }

        public void setHeaders(Iterator<Header> headers) {
            content = content | FetchGroup.HEADERS;
            this.headers = headers;
        }

        public Iterator<Header> getMimeHeaders() {
            return mimeHeaders;
        }

        public void setMimeHeaders(Iterator<Header> mimeHeaders) {
            content = content | FetchGroup.MIME_HEADERS;
            this.mimeHeaders = mimeHeaders;
        }
    }

    public void setMimeDescriptor(final MimeDescriptor mimeDescriptor) {
        this.mimeDescriptor = mimeDescriptor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getMimeDescriptor()
     */
    public MimeDescriptor getMimeDescriptor() {
        return mimeDescriptor;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MessageMetaData#getModSeq()
     */
    public long getModSeq() {
        return message.getModSeq();
    }
    
    @Override
    public synchronized Headers getHeaders() throws MailboxException {
        if (headers == null) {
            headers = new HeadersImpl(message);
        }
        return headers;
    }
    
    private final class HeadersImpl implements Headers {

        private Message<?> msg;
        private List<Header> headers;
        
        public HeadersImpl(Message<?> msg) {
            this.msg = msg;
        }
        @Override
        public InputStream getInputStream() throws IOException {
            return msg.getHeaderContent();
        }

        @Override
        public long size() {
            return msg.getFullContentOctets() - msg.getBodyOctets();
        }

        @Override
        public synchronized Iterator<Header> headers() throws MailboxException {
            if (headers == null) {
                try {
                    headers = ResultUtils.createHeaders(message);
                } catch (IOException e) {
                    throw new MailboxException("Unable to parse headers", e);
                }
            }
            return headers.iterator();
        }
        
    }
}
