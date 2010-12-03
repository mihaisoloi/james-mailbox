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
package org.apache.james.mailbox.store.mail.model;

/**
 * 
 * Email Header 
 *
 */
public interface Header extends Comparable<Header> {

    /**
     * Return the name of the Header field
     * 
     * @return fieldName
     */
    public abstract String getFieldName();

    /**
     * Return the line number of the Header
     * 
     * @return lineNumber
     */
    public abstract int getLineNumber();

    /**
     * Return the value of the Header
     * 
     * @return value
     */
    public abstract String getValue();

}