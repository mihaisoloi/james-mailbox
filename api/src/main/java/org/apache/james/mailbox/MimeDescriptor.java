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
/**
 * 
 */
package org.apache.james.mailbox;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface MimeDescriptor extends Headers {

    /**
     * Gets the top level MIME content media type.
     * 
     * @return top level MIME content media type, or null if default
     */
    public String getMimeType();

    /**
     * Gets the MIME content subtype.
     * 
     * @return the MIME content subtype, or null if default
     */
    public String getMimeSubType();

    /**
     * Gets the MIME <code>Content-ID</code> header value.
     * 
     * @return MIME <code>Content-ID</code>, possibly null
     */
    public String getContentID();

    /**
     * Gets MIME <code>Content-Description</code> header value.
     * 
     * @return MIME <code>Content-Description</code>, possibly null
     */
    public String getContentDescription();

    /**
     * Gets MIME <code>Content-Location</code> header value.
     * 
     * @return parsed MIME <code>Content-Location</code>, possibly null
     */
    public String getContentLocation();

    /**
     * Gets MIME <code>Content-MD5</code> header value.
     * 
     * @return parsed MIME <code>Content-MD5</code>, possibly null
     */
    public String getContentMD5();

    /**
     * Gets the MIME content transfer encoding.
     * 
     * @return MIME <code>Content-Transfer-Encoding</code>, possibly null
     */
    public String getTransferContentEncoding();

    /**
     * Gets the languages, From the MIME <code>Content-Language</code>
     * header value.
     * 
     * @return <code>List</code> of <code>String</code> names
     */
    public List<String> getLanguages();

    /**
     * Gets MIME <code>Content-Disposition</code>.
     * 
     * @return <code>Content-Disposition</code>, or null if no
     *         disposition header exists
     */
    public String getDisposition();

    /**
     * Gets MIME <code>Content-Disposition</code> parameters.
     * 
     * @return <code>Content-Disposition</code> values indexed by names
     */
    public Map<String,String> getDispositionParams();

    /**
     * Gets the number of lines of text in a part of type <code>TEXT</code>
     * when transfer encoded.
     * 
     * @return <code>CRLF</code> count when a <code>TEXT</code> type,
     *         otherwise -1
     */
    public long getLines();

    /**
     * The number of octets contained in the body of this part.
     * 
     * @return number of octets
     */
    public long getBodyOctets();

    /**
     * Gets parts.
     * 
     * @return <code>MimeDescriptor</code> <code>Iterator</code> when a
     *         composite top level MIME media type, null otherwise
     */
    public Iterator<MimeDescriptor> parts();

    /**
     * Gets embedded message.
     * 
     * @return <code>MimeDescriptor</code> when top level MIME type is
     *         <code>message</code>, null otherwise
     */
    public MimeDescriptor embeddedMessage();


    /**
     * Gets MIME body parameters parsed from <code>Content-Type</code>.
     * 
     * @return <code>Header</code> <code>Iterator</code>, not null
     */
    public Map<String, String> contentTypeParameters();
}