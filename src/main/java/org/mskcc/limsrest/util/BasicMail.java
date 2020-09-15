package org.mskcc.limsrest.util;

import java.util.Properties;
import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.Message;

public class BasicMail {

    public static void main(String[] args) throws MessagingException {
        BasicMail mail = new BasicMail();
        mail.send("sharmaa1@mskcc.org", "sharmaa1@mskcc.org", "tango.mskcc.org", "Hello", "This does not have an attachment");
    }

    public void send(String from, String to, String host, String subject, String text) throws MessagingException {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", host);
        Session mailSession = Session.getDefaultInstance(props, null);

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));

        String[] recipients = to.split(",");
        InternetAddress[] addresses = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addresses[i] = new InternetAddress(recipients[i]);
        }
        message.addRecipients(Message.RecipientType.TO, addresses);

        message.setSubject(subject);
        message.setText(text);
        message.setSentDate(new Date());

        Transport.send(message);
    }

    //This send will attach files to the email you are senidng
    public void send(String from, String to, String host, String subject, String text, String files) throws AddressException, MessagingException {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", host);
        Session mailSession = Session.getDefaultInstance(props, null);

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        String[] recipients = to.split(",");
        InternetAddress[] addresses = new InternetAddress[recipients.length];

        for (int i = 0; i < recipients.length; i++) {
            // message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipients[i]));
            addresses[i] = new InternetAddress(recipients[i]);
        }
        message.addRecipients(Message.RecipientType.TO, addresses);
        message.setSubject(subject);
        //message.setText(text);
        // This is a multipart message, so create Multipart (message) and add the BodyParts
        Multipart multipart = new MimeMultipart();
        BodyPart messageParts = new MimeBodyPart();

        // First add the body text
        messageParts.setText(text + "\n");
        multipart.addBodyPart(messageParts);

        //Next add files
        String[] filenames = files.split(",");

        for (int x = 0; x < filenames.length; x++) {
            messageParts = new MimeBodyPart();
            DataSource source = new FileDataSource(filenames[x]);
            messageParts.setDataHandler(new DataHandler(source));
            String[] nameParts = filenames[x].split("/");
            String name = nameParts[nameParts.length - 1];
            messageParts.setFileName(name);
            multipart.addBodyPart(messageParts);
        }

        // Now set content to multipart
        message.setContent(multipart);

        message.setSentDate(new Date());

        Transport.send(message);
    }
}
