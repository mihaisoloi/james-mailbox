package org.apache.james.mailbox.store.search.comparator;

import java.util.Comparator;
import java.util.List;

import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.Group;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;

public class HeaderMailboxComparator implements Comparator<Message<?>>{

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
        String mailbox1 = getMailbox(o1, headerName);
        String mailbox2 = getMailbox(o2, headerName);

        return mailbox1.compareTo(mailbox2);
    }
    
    
    private String getMailbox(Message<?> message, String headerName) {
        final List<Header> headers = message.getHeaders();
        for (Header header:headers) {
            final String name = header.getFieldName();
            if (headerName.equalsIgnoreCase(name)) {
                final String value = header.getValue();
                try {
                    AddressList aList = AddressList.parse(value);
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
            }
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

