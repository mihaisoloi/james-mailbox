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

import org.apache.james.mailbox.store.mail.model.AbstractComparableHeader;
import org.apache.james.mailbox.store.mail.model.Header;

public class MaildirHeader extends AbstractComparableHeader {

    private int lineNumber;
    private String field;
    private String value;

    /**
     * Copies the content of an existing header.
     * 
     * @param header
     */
    public MaildirHeader(Header header) {
        this(header.getLineNumber(), header.getFieldName(), header.getValue());
    }
    
    public MaildirHeader(int lineNumber, String field, String value) {
        super();
        this.lineNumber = lineNumber;
        this.field = field;
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.mail.model.Header#getFieldName()
     */
    public String getFieldName() {
        return field;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.mail.model.Header#getLineNumber()
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.mail.model.Header#getValue()
     */
    public String getValue() {
        return value;
    }

}
