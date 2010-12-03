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
package org.apache.james.mailbox.jpa.mail.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.james.mailbox.store.mail.model.AbstractComparableHeader;
import org.apache.james.mailbox.store.mail.model.Header;

@Entity(name="Header")
public class JPAHeader extends AbstractComparableHeader {
    
    private static final String TOSTRING_SEP = " ";

    @Id @GeneratedValue private long id;

    /** The value for the lineNumber field */
    @Basic(optional=false) private int lineNumber;

    /** The value for the field field */
    /** Use a max of 1024 which could happen on very freaky header field names*/
    @Column(length=1024)
    @Basic(optional=false) private String field;

    /** The value for the value field */
    /** We use 10240 as max which is mostly overkill for most emails but better waste a bit of space then loose headers**/
    @Column(length=10240)
    @Basic(optional=false) private String value;
    
    /**
     * For JPA use only.
     */
    @Deprecated
    public JPAHeader() {}
    
    /**
     * Copies the content of an existing header.
     * @param header
     */
    public JPAHeader(Header header) {
        this(header.getLineNumber(), header.getFieldName(), header.getValue());
    }
    
    public JPAHeader(int lineNumber, String field, String value) {
        super();
        this.lineNumber = lineNumber;
        this.field = field;
        this.value = value;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Header#getFieldName()
     */
    public String getFieldName() {
        return field;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Header#getLineNumber()
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Header#getValue()
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final JPAHeader other = (JPAHeader) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public String toString()
    {
        final String retValue =  "Header ( "
            + "id = " + this.id + TOSTRING_SEP
            + "lineNumber = " + this.lineNumber + TOSTRING_SEP
            + "field = " + this.field + TOSTRING_SEP
            + "value = " + this.value + TOSTRING_SEP
            + " )";
        return retValue;
    }

}
