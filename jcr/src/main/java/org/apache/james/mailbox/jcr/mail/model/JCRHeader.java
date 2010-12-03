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
package org.apache.james.mailbox.jcr.mail.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.james.mailbox.jcr.JCRImapConstants;
import org.apache.james.mailbox.jcr.Persistent;
import org.apache.james.mailbox.store.mail.model.AbstractComparableHeader;
import org.apache.james.mailbox.store.mail.model.Header;

/**
 * JCR implementation of a {@link Header}
 * 
 *
 */
public class JCRHeader extends AbstractComparableHeader implements JCRImapConstants, Persistent{
    private static final String TOSTRING_SEP = " ";

    public final static String FIELDNAME_PROPERTY = "jamesMailbox:headerFieldName";
    public final static String VALUE_PROPERTY = "jamesMailbox:headerValue";
    public final static String LINENUMBER_PROPERTY = "jamesMailbox:headerLineNumber";

    private String fieldName;
    private String value;
    private int lineNumber;
    private Log logger;
    private Node node;
    
    /**
     * Copies the content of an existing header.
     * @param header
     */
    public JCRHeader(Header header, Log logger) {
        this(header.getLineNumber(), header.getFieldName(), header.getValue(), logger);
    }
    
    public JCRHeader(Node node, Log logger) {
        this.node = node;
        this.logger = logger;
    }
    
    
    
    public JCRHeader(final int lineNumber, final String fieldName, final String value, Log logger) {
        this.fieldName = fieldName;
        this.value = value;
        this.lineNumber = lineNumber;
        this.logger = logger;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Header#getFieldName()
     */
    public String getFieldName() {
        if (isPersistent()) {
            try {
                return node.getProperty(FIELDNAME_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + FIELDNAME_PROPERTY, e);
            }
        }
        return fieldName;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Header#getLineNumber()
     */
    public int getLineNumber() {
        if (isPersistent()) {
            try {
                return new Long(node.getProperty(LINENUMBER_PROPERTY).getLong()).intValue();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + FIELDNAME_PROPERTY, e);
            }
        }
        return lineNumber;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Header#getValue()
     */
    public String getValue() {
        if (isPersistent()) {
            try {
                return node.getProperty(VALUE_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + FIELDNAME_PROPERTY, e);
            }
        }
        return value;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.jcr.IsPersistent#getNode()
     */
    public Node getNode() {
        return node;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.jcr.IsPersistent#isPersistent()
     */
    public boolean isPersistent() {
        return node != null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.jcr.IsPersistent#merge(javax.jcr.Node)
     */
    public void merge(Node node) throws RepositoryException {
        node.setProperty(FIELDNAME_PROPERTY, getFieldName());
        node.setProperty(LINENUMBER_PROPERTY, getLineNumber());
        node.setProperty(VALUE_PROPERTY, getValue());
        
        /*
        this.node = node;
        
        this.fieldName = null;
        this.lineNumber = 0;
        this.value = null;
        */
    }
   

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + getFieldName().hashCode();
        result = PRIME * result + getValue().hashCode();

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
        final JCRHeader other = (JCRHeader) obj;
        if (getValue() != other.getValue() || getFieldName() != other.getFieldName())
            return false;
        return true;
    }

    public String toString() {
        final String retValue =  "Header ( "
            + "lineNumber = " + this.getLineNumber() + TOSTRING_SEP
            + "field = " + this.getFieldName() + TOSTRING_SEP
            + "value = " + this.getValue() + TOSTRING_SEP
            + " )";
        return retValue;
    }

}
