package it.pagopa.pn.pecmock.utils;

import it.pagopa.pn.pecmock.exception.ComposeMimeMessageException;
import it.pagopa.pn.pecmock.model.pojo.EmailField;
import it.pagopa.pn.pecmock.model.pojo.PagopaMimeMessage;
import lombok.extern.slf4j.Slf4j;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@Slf4j
public class PecUtils {

    public static MimeMessage getMimeMessage(byte[] bytes) {
        try {
            log.info("---> Start getting MimeMessage from byte array with length '{}' <---", bytes.length);
            return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(bytes));
        } catch (MessagingException e) {
            throw new RuntimeException();
        }
    }

    public static MimeMessage getMimeMessage(EmailField emailField) {
        try {
            var session = Session.getInstance(new Properties());
            MimeMessage mimeMessage;
            if (emailField.getMsgId() == null) {
                mimeMessage = new MimeMessage(session);
            } else {
                mimeMessage = new PagopaMimeMessage(session, emailField.getMsgId());
            }

            mimeMessage.setFrom(new InternetAddress(emailField.getFrom(), "", "UTF-8"));
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(emailField.getTo(), "", "UTF-8"));
            mimeMessage.setSubject(emailField.getSubject(), "UTF-8");

            var htmlOrPlainTextPart = new MimeBodyPart();
            htmlOrPlainTextPart.setContent(emailField.getText(), emailField.getContentType());

            var mimeMultipart = new MimeMultipart();

            mimeMultipart.addBodyPart(htmlOrPlainTextPart);

            var emailAttachments = emailField.getEmailAttachments();
            if (emailAttachments != null) {
                emailAttachments.forEach(attachment -> {
                    var attachmentPart = new MimeBodyPart();
                    var byteArrayOutputStream = (ByteArrayOutputStream) attachment.getContent();
                    DataSource aAttachment = new ByteArrayDataSource(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM_VALUE);
                    try {
                        attachmentPart.setDataHandler(new DataHandler(aAttachment));
                        attachmentPart.setFileName(attachment.getNameWithExtension());
                        mimeMultipart.addBodyPart(attachmentPart);
                    } catch (MessagingException exception) {
                        log.error(exception.getMessage());
                        throw new ComposeMimeMessageException();
                    }
                });
            }

            mimeMessage.setContent(mimeMultipart);

            return mimeMessage;
        } catch (MessagingException exception) {
            throw new ComposeMimeMessageException();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
