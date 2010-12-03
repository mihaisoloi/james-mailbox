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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.mailbox.store.streaming.CountingInputStream;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;
import org.apache.james.mime4j.parser.RecursionMode;

public class MimeDescriptorImpl implements MimeDescriptor {

    public static MimeDescriptorImpl build(final Message document) throws IOException, MimeException {
        final MimeDescriptorImpl result;
        final String mediaType = document.getMediaType();
        if (isComposite(mediaType)) {
            result = build(ResultUtils.toInput(document));
        } else {
            final List<MessageResult.Header> headers = ResultUtils.createHeaders(document);
            final List<Property> properties = document.getProperties();
            final PropertyBuilder builder = new PropertyBuilder(properties);
            final Long textualLineCount = document.getTextualLineCount();
            result = new MimeDescriptorImpl(
                    document.getBodyOctets(), 
                    builder.getContentDescription(), 
                    builder.getContentID(), 
                    textualLineCount == null? -1: textualLineCount.longValue(), 
                    document.getSubType(),
                    mediaType, 
                    builder.getContentTransferEncoding(), 
                    headers, 
                    builder.getContentTypeParameters(),
                    builder.getContentLanguage(), 
                    builder.getContentDispositionType(), 
                    builder.getContentDispositionParameters(), 
                    null,
                    new ArrayList<MimeDescriptor>(), 
                    builder.getContentLocation(), 
                    builder.getContentMD5());
        }
        return result;
    }
    
    /**
     * Is this a composite media type (as per RFC2045)?
     * 
     * TODO: Move to Mime4j
     * @param mediaType, possibly null
     * @return true when the type is composite,
     * false otherwise
     */
    public static boolean isComposite(String mediaType) {
        return "message".equalsIgnoreCase(mediaType) || "multipart".equalsIgnoreCase(mediaType);
    }

    public static MimeDescriptorImpl build(final InputStream stream) throws IOException, MimeException {
        // Disable line length limit
        // See https://issues.apache.org/jira/browse/IMAP-132
        MimeEntityConfig config = new MimeEntityConfig();
        config.setMaximalBodyDescriptor(true);
        config.setMaxLineLen(-1);
        final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);

        parser.parse(stream);
        parser.setRecursionMode(RecursionMode.M_NO_RECURSE);
        return createDescriptor(parser);
    }

    private static MimeDescriptorImpl createDescriptor(
            final MimeTokenStream parser) throws IOException, MimeException {
        int next = parser.next();
        final Collection<MessageResult.Header> headers = new ArrayList<MessageResult.Header>();
        while (next != MimeTokenStream.T_BODY
                && next != MimeTokenStream.T_END_OF_STREAM
                && next != MimeTokenStream.T_START_MULTIPART) {
            if (next == MimeTokenStream.T_FIELD) {
                headers.add(new ResultHeader(parser.getField().getName(), parser
                        .getField().getBody().trim()));
            }
            next = parser.next();
        }

        final MimeDescriptorImpl mimeDescriptorImpl;
        switch (next) {
            case MimeTokenStream.T_BODY:
                mimeDescriptorImpl = simplePartDescriptor(parser, headers);
                break;
            case MimeTokenStream.T_START_MULTIPART:
                mimeDescriptorImpl = compositePartDescriptor(parser, headers);
                break;
            case MimeTokenStream.T_END_OF_STREAM:
                throw new MimeException("Premature end of stream");
            default:
                throw new MimeException("Unexpected parse state");
        }
        return mimeDescriptorImpl;
    }

    private static MimeDescriptorImpl compositePartDescriptor(
            final MimeTokenStream parser, final Collection<MessageResult.Header> headers)
            throws IOException, MimeException {
        MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser
                .getBodyDescriptor();
        MimeDescriptorImpl mimeDescriptor = createDescriptor(0, 0, descriptor,
                null, headers);
        int next = parser.next();
        while (next != MimeTokenStream.T_END_MULTIPART
                && next != MimeTokenStream.T_END_OF_STREAM) {
            if (next == MimeTokenStream.T_START_BODYPART) {
                mimeDescriptor.addPart(createDescriptor(parser));
            }
            next = parser.next();
        }
        return mimeDescriptor;
    }

    private static MimeDescriptorImpl simplePartDescriptor(
            final MimeTokenStream parser, final Collection<MessageResult.Header> headers)
            throws IOException, MimeException {
        MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser
                .getBodyDescriptor();
        final MimeDescriptorImpl mimeDescriptorImpl;
        if ("message".equalsIgnoreCase(descriptor.getMediaType())
                && "rfc822".equalsIgnoreCase(descriptor.getSubType())) {
            final CountingInputStream messageStream = new CountingInputStream(
                    parser.getDecodedInputStream());
            MimeDescriptorImpl embeddedMessageDescriptor = build(messageStream);
            final int octetCount = messageStream.getOctetCount();
            final int lineCount = messageStream.getLineCount();

            mimeDescriptorImpl = createDescriptor(octetCount, lineCount,
                    descriptor, embeddedMessageDescriptor, headers);
        } else {
            final InputStream body = parser.getInputStream();
            long bodyOctets = 0;
            long lines = 0;
            for (int n = body.read(); n >= 0; n = body.read()) {
                if (n == '\r') {
                    lines++;
                }
                bodyOctets++;
            }

            mimeDescriptorImpl = createDescriptor(bodyOctets, lines,
                    descriptor, null, headers);
        }
        return mimeDescriptorImpl;
    }

    private static MimeDescriptorImpl createDescriptor(long bodyOctets,
            long lines, MaximalBodyDescriptor descriptor,
            MimeDescriptor embeddedMessage, final Collection<MessageResult.Header> headers) {
        final String contentDescription = descriptor.getContentDescription();
        final String contentId = descriptor.getContentId();

        final String subType = descriptor.getSubType();
        final String type = descriptor.getMediaType();
        final String transferEncoding = descriptor.getTransferEncoding();
        final Map<String, String> contentTypeParameters = new TreeMap<String, String>(descriptor.getContentTypeParameters());
        final String codeset = descriptor.getCharset();
        if (codeset == null) {
            if ("TEXT".equals(type)) {
                contentTypeParameters.put("charset", "us-ascii");
            }
        } else {
            contentTypeParameters.put("charset", codeset);
        }
        final String boundary = descriptor.getBoundary();
        if (boundary != null) {
            contentTypeParameters.put("boundary", boundary);
        }
        
        final List<String> languages = descriptor.getContentLanguage();
        final String disposition = descriptor.getContentDispositionType();
        final Map<String, String> dispositionParams = descriptor
                .getContentDispositionParameters();
        final Collection<MimeDescriptor> parts = new ArrayList<MimeDescriptor>();
        final String location = descriptor.getContentLocation();
        final String md5 = descriptor.getContentMD5Raw();
        final MimeDescriptorImpl mimeDescriptorImpl = new MimeDescriptorImpl(
                bodyOctets, contentDescription, contentId, lines, subType,
                type, transferEncoding, headers, contentTypeParameters,
                languages, disposition, dispositionParams, embeddedMessage,
                parts, location, md5);
        return mimeDescriptorImpl;
    }

    private final long bodyOctets;

    private final String contentDescription;

    private final String contentId;

    private final long lines;

    private final String subType;

    private final String type;

    private final String transferEncoding;

    private final List<String> languages;

    private final Collection<MessageResult.Header> headers;

    private final Map<String, String> contentTypeParameters;

    private final String disposition;

    private final Map<String, String> dispositionParams;

    private final MimeDescriptor embeddedMessage;

    private final Collection<MimeDescriptor> parts;

    private final String location;

    private final String md5;

    public MimeDescriptorImpl(final long bodyOctets,
            final String contentDescription, final String contentId,
            final long lines, final String subType, final String type,
            final String transferEncoding, final Collection<MessageResult.Header> headers,
            final Map<String, String> contentTypeParameters, final List<String> languages,
            String disposition, Map<String, String> dispositionParams,
            final MimeDescriptor embeddedMessage, final Collection<MimeDescriptor> parts,
            final String location, final String md5) {
        super();
        this.type = type;
        this.bodyOctets = bodyOctets;
        this.contentDescription = contentDescription;
        this.contentId = contentId;
        this.lines = lines;
        this.subType = subType;
        this.transferEncoding = transferEncoding;
        this.headers = headers;
        this.contentTypeParameters = contentTypeParameters;
        this.embeddedMessage = embeddedMessage;
        this.parts = parts;
        this.languages = languages;
        this.disposition = disposition;
        this.dispositionParams = dispositionParams;
        this.location = location;
        this.md5 = md5;
    }

    public Map<String, String> contentTypeParameters() {
        return contentTypeParameters;
    }

    public MimeDescriptor embeddedMessage() {
        return embeddedMessage;
    }

    public long getBodyOctets() {
        return bodyOctets;
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public String getContentID() {
        return contentId;
    }

    public long getLines() {
        return lines;
    }

    public String getMimeSubType() {
        return subType;
    }

    public String getMimeType() {
        return type;
    }

    public String getTransferContentEncoding() {
        return transferEncoding;
    }

    public Iterator<MessageResult.Header> headers() {
        return headers.iterator();
    }

    public Iterator<MimeDescriptor> parts() {
        return parts.iterator();
    }

    private void addPart(MimeDescriptor descriptor) {
        parts.add(descriptor);
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getDisposition() {
        return disposition;
    }

    public Map<String,String> getDispositionParams() {
        return dispositionParams;
    }

    public String getContentLocation() {
        return location;
    }

    public String getContentMD5() {
        return md5;
    }
}
