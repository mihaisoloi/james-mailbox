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

import org.apache.james.mailbox.store.mail.model.AbstractComparableProperty;
import org.apache.james.mailbox.store.mail.model.Property;

public class MaildirProperty extends AbstractComparableProperty<MaildirProperty> {

    private int order;
    private String namespace;
    private String localName;
    private String value;
    
    public MaildirProperty(final String namespace, final String localName, final String value, final int order) {
        this.namespace = namespace;
        this.localName = localName;
        this.value = value;
        this.order = order;
    }
    
    public MaildirProperty(final Property property, final int order) {
        this(property.getNamespace(), property.getLocalName(), property.getValue(), order);
    }
    
    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.AbstractComparableProperty#getOrder()
     */
    @Override
    public int getOrder() {
        return order;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Property#getLocalName()
     */
    public String getLocalName() {
        return localName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getValue() {
        return value;
    }

}
