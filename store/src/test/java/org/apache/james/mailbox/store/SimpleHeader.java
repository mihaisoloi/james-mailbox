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

import org.apache.james.mailbox.store.mail.model.Header;

public class SimpleHeader implements Header {

    public String field;
    public int lineNumber;
    public String value;
    
    public SimpleHeader() {}
    
    public SimpleHeader(SimpleHeader header) {
        this.field = header.field;
        this.lineNumber = header.lineNumber;
        this.value = header.value;
    }
    
    public SimpleHeader(String field, int lineNumber, String value) {
        super();
        this.field = field;
        this.lineNumber = lineNumber;
        this.value = value;
    }

    public String getFieldName() {
        return field;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getValue() {
        return value;
    }

    public int compareTo(Header o) {
        return getLineNumber() - o.getLineNumber();
    }
}
