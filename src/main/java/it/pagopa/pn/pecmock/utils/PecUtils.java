package it.pagopa.pn.pecmock.utils;

import lombok.extern.slf4j.Slf4j;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.Properties;
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

}
