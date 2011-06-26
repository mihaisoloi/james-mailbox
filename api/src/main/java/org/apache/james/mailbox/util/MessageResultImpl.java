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

package org.apache.james.mailbox.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;

/**
 * Bean based implementation.
 */
public class MessageResultImpl implements MessageResult {
    private long uid;

    private Flags flags;

    private int size;

    private Date internalDate;

    private List<Header> headers;

    private Content body;

    private Content fullContent;

    private int includedResults = FetchGroup.MINIMAL;

    private Map<MimePath, PartContent> partsByPath = new HashMap<MimePath, PartContent>();

    private MimeDescriptor mimeDescriptor;

    public MessageResultImpl(long uid) {
        setUid(uid);
    }

    public MessageResultImpl() {
    }

    public MessageResultImpl(long uid, Flags flags) {
        setUid(uid);
        setFlags(flags);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getUid()
     */
    public long getUid() {
        return uid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getInternalDate()
     */
    public Date getInternalDate() {
        return internalDate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getFlags()
     */
    public Flags getFlags() {
        return flags;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getSize()
     */
    public long getSize() {
        return size;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MessageResult that) {
        if (this.uid > 0 && that.getUid() > 0) {
            // TODO: this seems inefficient
            return Long.valueOf(uid).compareTo(Long.valueOf(that.getUid()));
        } else {
            // TODO: throwing an undocumented untyped runtime seems wrong
            // TODO: if uids must be greater than zero then this should be
            // enforced
            // TODO: on the way in
            // TODO: probably an IllegalArgumentException would be better
            throw new RuntimeException("can't compare");
        }

    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setInternalDate(Date internalDate) {
        this.internalDate = internalDate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#headers()
     */
    public Iterator<Header> headers() {
        if (headers == null) {
            return null;
        }
        return headers.iterator();
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
        if (headers != null) {
            includedResults |= FetchGroup.HEADERS;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getFullContent()
     */
    public final Content getFullContent() {
        return fullContent;
    }

    public final void setFullContent(Content fullMessage) {
        this.fullContent = fullMessage;
        if (fullMessage != null) {
            includedResults |= FetchGroup.FULL_CONTENT;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MessageResult#getBody()
     */
    public final Content getBody() {
        return body;
    }

    public final void setBody(Content messageBody) {
        this.body = messageBody;
        if (messageBody != null) {
            includedResults |= FetchGroup.BODY_CONTENT;
        }
    }

    /**
     * Renders suitably for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String TAB = " ";

        String retValue = "MessageResultImpl ( " + "uid = " + this.uid + TAB + "flags = " + this.flags + TAB + "size = " + this.size + TAB + "internalDate = " + this.internalDate + TAB + "includedResults = " + this.includedResults + TAB + " )";

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
        includedResults |= FetchGroup.MIME_DESCRIPTOR;
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
}
