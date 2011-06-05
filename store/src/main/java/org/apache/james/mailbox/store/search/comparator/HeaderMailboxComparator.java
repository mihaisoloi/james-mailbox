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
package org.apache.james.mailbox.store.search.comparator;

import java.util.Comparator;

import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.Group;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;

public class HeaderMailboxComparator extends AbstractHeaderComparator{

    private final String headerName;

    private final static Comparator<Message<?>> FROM = new HeaderMailboxComparator("from");
    private final static Comparator<Message<?>> REVERSE_FROM = new ReverseComparator(FROM);


    private final static Comparator<Message<?>> TO = new HeaderMailboxComparator("to");
    private final static Comparator<Message<?>> REVERSE_TO = new ReverseComparator(TO);


    private final static Comparator<Message<?>> CC = new HeaderMailboxComparator("cc");
    private final static Comparator<Message<?>> REVERSE_CC = new ReverseComparator(CC);

    
    public HeaderMailboxComparator(String headerName) {
        this.headerName = headerName;
    }
    
    @Override
    public int compare(Message<?> o1, Message<?> o2) {
        String mailbox1 = getMailbox(headerName, o1);
        String mailbox2 = getMailbox(headerName, o2);

        return mailbox1.compareTo(mailbox2);
    }
    
    
    private String getMailbox(String headerName, Message<?> message) {
        try {
            AddressList aList = AddressList.parse(getHeaderValue(headerName, message));
            for (int i = 0; i < aList.size(); i++) {
                Address address = aList.get(i);
                if (address instanceof Mailbox) {
                    Mailbox m = (Mailbox) address;
                    String mailboxName = m.getName();
                    if (mailboxName == null) {
                        mailboxName ="";
                    }
                    return mailboxName;
                } else if (address instanceof Group) {
                    MailboxList mList = ((Group) address).getMailboxes();
                    for (int a = 0; a < mList.size(); ) {
                        String mailboxName = mList.get(a).getName();
                        if (mailboxName == null) {
                            mailboxName ="";
                        }
                        return mailboxName;                         
                    }
                }
            }

        } catch (org.apache.james.mime4j.field.address.parser.ParseException e) {
            return "";
        }
        return "";
    }
    
    public static Comparator<Message<?>> from(boolean reverse) {
        if (reverse) {
            return REVERSE_FROM;
        } else {
            return FROM;
        }
    }
    
    public static Comparator<Message<?>> cc(boolean reverse) {
        if (reverse) {
            return REVERSE_CC;
        } else {
            return CC;
        }
    }
    
    public static Comparator<Message<?>> to(boolean reverse) {
        if (reverse) {
            return REVERSE_TO;
        } else {
            return TO;
        }
    }
}

