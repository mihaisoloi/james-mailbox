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
package org.apache.james.mailbox.maildir.mail.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.mail.util.SharedFileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.maildir.MaildirMessageName;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.mailbox.store.streaming.CountingInputStream;
import org.apache.james.mailbox.store.streaming.LazySkippingInputStream;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;

public class LazyLoadingMaildirMessage extends AbstractMaildirMessage {

    private MaildirMessageName messageName;
    private int bodyStartOctet;
    private final PropertyBuilder propertyBuilder = new PropertyBuilder();

    private boolean parsed;

    public LazyLoadingMaildirMessage(Mailbox<Integer> mailbox, long uid, MaildirMessageName messageName) throws IOException {
        super(mailbox);
        setUid(uid);
        setModSeq(messageName.getFile().lastModified());
        Flags flags = messageName.getFlags();
        
        // Set the flags for the message and respect if its RECENT
        // See MAILBOX-84
        File file = messageName.getFile();
        if (!file.exists()) {
            throw new FileNotFoundException("Unable to read file " + file.getAbsolutePath() + " for the message");
        } else {
            // if the message resist in the new folder its RECENT
            if (file.getParentFile().getName().equals(MaildirFolder.NEW)) {
                flags.add(Flags.Flag.RECENT);
            }
        }
        setFlags(flags);
        this.messageName = messageName;
    }

    /**
     * Parse message if needed
     */
    private synchronized void parseMessage() {
        if (parsed)
            return;
        SharedFileInputStream tmpMsgIn = null;
        try {
            tmpMsgIn = new SharedFileInputStream(messageName.getFile());

            bodyStartOctet = bodyStartOctet(tmpMsgIn);

            // Disable line length... This should be handled by the smtp server
            // component and not the parser itself
            // https://issues.apache.org/jira/browse/IMAP-122
            MimeEntityConfig config = new MimeEntityConfig();
            config.setMaximalBodyDescriptor(true);
            config.setMaxLineLen(-1);
            final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);

            parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
            parser.parse(tmpMsgIn.newStream(0, -1));

            int next = parser.next();
            while (next != MimeTokenStream.T_BODY && next != MimeTokenStream.T_END_OF_STREAM && next != MimeTokenStream.T_START_MULTIPART) {
                next = parser.next();
            }
            final MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
            final String mediaType;
            final String mediaTypeFromHeader = descriptor.getMediaType();
            final String subType;
            if (mediaTypeFromHeader == null) {
                mediaType = "text";
                subType = "plain";
            } else {
                mediaType = mediaTypeFromHeader;
                subType = descriptor.getSubType();
            }
            propertyBuilder.setMediaType(mediaType);
            propertyBuilder.setSubType(subType);
            propertyBuilder.setContentID(descriptor.getContentId());
            propertyBuilder.setContentDescription(descriptor.getContentDescription());
            propertyBuilder.setContentLocation(descriptor.getContentLocation());
            propertyBuilder.setContentMD5(descriptor.getContentMD5Raw());
            propertyBuilder.setContentTransferEncoding(descriptor.getTransferEncoding());
            propertyBuilder.setContentLanguage(descriptor.getContentLanguage());
            propertyBuilder.setContentDispositionType(descriptor.getContentDispositionType());
            propertyBuilder.setContentDispositionParameters(descriptor.getContentDispositionParameters());
            propertyBuilder.setContentTypeParameters(descriptor.getContentTypeParameters());
            // Add missing types
            final String codeset = descriptor.getCharset();
            if (codeset == null) {
                if ("TEXT".equalsIgnoreCase(mediaType)) {
                    propertyBuilder.setCharset("us-ascii");
                }
            } else {
                propertyBuilder.setCharset(codeset);
            }

            final String boundary = descriptor.getBoundary();
            if (boundary != null) {
                propertyBuilder.setBoundary(boundary);
            }
            if ("text".equalsIgnoreCase(mediaType)) {
                long lines = -1;
                final CountingInputStream bodyStream = new CountingInputStream(parser.getInputStream());
                try {
                    bodyStream.readAll();
                    lines = bodyStream.getLineCount();
                } finally {
                    IOUtils.closeQuietly(bodyStream);
                }

                next = parser.next();
                if (next == MimeTokenStream.T_EPILOGUE) {
                    final CountingInputStream epilogueStream = new CountingInputStream(parser.getInputStream());
                    try {
                        epilogueStream.readAll();
                        lines += epilogueStream.getLineCount();
                    } finally {
                        IOUtils.closeQuietly(epilogueStream);
                    }
                }
                propertyBuilder.setTextualLineCount(lines);
            }
        } catch (IOException e) {
            // has successfully been parsen when appending, shouldn't give any
            // problems
        } catch (MimeException e) {
            // has successfully been parsen when appending, shouldn't give any
            // problems
        } finally {
            if (tmpMsgIn != null) {
                try {
                    tmpMsgIn.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
            parsed = true;
        }
    }

    /**
     * Return the position in the given {@link InputStream} at which the Body of
     * the Message starts
     * 
     * @param msgIn
     * @return bodyStartOctet
     * @throws IOException
     */
    private int bodyStartOctet(InputStream msgIn) throws IOException {
        // we need to pushback maximal 3 bytes
        PushbackInputStream in = new PushbackInputStream(msgIn, 3);
        int bodyStartOctet = in.available();
        int i = -1;
        int count = 0;
        while ((i = in.read()) != -1 && in.available() > 4) {
            if (i == 0x0D) {
                int a = in.read();
                if (a == 0x0A) {
                    int b = in.read();

                    if (b == 0x0D) {
                        int c = in.read();

                        if (c == 0x0A) {
                            bodyStartOctet = count + 4;
                            break;
                        }
                        in.unread(c);
                    }
                    in.unread(b);
                }
                in.unread(a);
            }
            count++;
        }
        return bodyStartOctet;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getMediaType()
     */
    public String getMediaType() {
        parseMessage();
        return propertyBuilder.getMediaType();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getSubType()
     */
    public String getSubType() {
        parseMessage();
        return propertyBuilder.getSubType();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getFullContentOctets()
     */
    public long getFullContentOctets() {
        return messageName.getSize();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getTextualLineCount()
     */
    public Long getTextualLineCount() {
        parseMessage();
        return propertyBuilder.getTextualLineCount();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getProperties()
     */
    public List<Property> getProperties() {
        parseMessage();
        return propertyBuilder.toProperties();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#getInternalDate()
     */
    public Date getInternalDate() {
        return messageName.getInternalDate();
    }

    private InputStream getFullContent() throws IOException {
        return new FileInputStream(messageName.getFile());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getBodyContent()
     */
    public InputStream getBodyContent() throws IOException {
        parseMessage();
        return new LazySkippingInputStream(getFullContent(), bodyStartOctet);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.AbstractMessage#getBodyStartOctet()
     */
    protected int getBodyStartOctet() {
        parseMessage();
        return bodyStartOctet;
    }

    @Override
    public InputStream getHeaderContent() throws IOException {
        parseMessage();
            
        return new BoundedInputStream(getFullContent(), bodyStartOctet - 2);

    }


}
