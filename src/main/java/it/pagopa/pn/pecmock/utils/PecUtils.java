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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;

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

    public static int getRandomNumberBetweenMinMax(int min, int max){
        Random random = new Random();

        return random.nextInt(max + 1 - min) + min;
    }

    public static String generateRandomString(int length) {
        Random random = new Random();

        // Use the nextBytes() method to generate a random sequence of bytes.
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        // Convert the bytes to a string using the Base64 encoding.
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ByteArrayOutputStream getMimeMessageOutputStream(EmailField emailField) {
        var output = new ByteArrayOutputStream();
        try {
            getMimeMessage(emailField).writeTo(output);
            return output;
        } catch (IOException | MessagingException exception) {
            log.error("getMimeMessageOutputStream() - {} - {}", exception, exception.getMessage());
            throw new ComposeMimeMessageException();
        }
    }

    public static byte[] getMimeMessageInBase64(EmailField emailField) {
//        return Base64.getEncoder().encode(getMimeMessageOutputStream(emailField).toByteArray());
        return getMimeMessageOutputStream(emailField).toByteArray();
    }

    public static StringBuffer generateDaticertAccettazione(String from, String receiver, String replyTo, String subject, String gestoreMittente, String data, String orario, String messageId){

        //Costruzione del daticert
        StringBuffer stringBufferContent = new StringBuffer();
        stringBufferContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBufferContent.append("<postacert tipo=\"accettazione\" errore=\"nessuno\">");
        stringBufferContent.append("<intestazione>");
        stringBufferContent.append("<mittente>").append(from).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBufferContent.append("<destinatari tipo=\"certificato\">").append(receiver).append("</destinatari>"); //destinatario dell'email, sta nella mappa
        stringBufferContent.append("<risposte>").append(replyTo).append("</risposte>"); //nel messaggio che uso per popolare la mappa c'è un reply-to
        stringBufferContent.append("<oggetto>").append(subject).append("</oggetto>"); //oggetto dell'email, sta nella mappa
        stringBufferContent.append("</intestazione>");
        stringBufferContent.append("<dati>");
        stringBufferContent.append("<gestore-emittente>").append(gestoreMittente).append("</gestore-emittente>"); //da inventare = "mock-pec" costante
        stringBufferContent.append("<data zona=\"+0200\">"); //lasciare così
        stringBufferContent.append("<giorno>").append(data).append("</giorno>"); //impostare in base all'ora
        stringBufferContent.append("<ora>").append(orario).append("</ora>"); //impostare in base all'ora
        stringBufferContent.append("</data>");
        stringBufferContent.append("<identificativo>").append(generateRandomString(64)).append("</identificativo>"); //stringa random 64 caratteri
        stringBufferContent.append("<msgid>").append("&lt;").append(messageId).append("&gt;").append("</msgid>"); //msgid della mappa, nella forma url encoded. fare url encode della stringa
        stringBufferContent.append("</dati>");
        stringBufferContent.append("</postacert>");

        return stringBufferContent;
    }
    public static StringBuffer generateDaticertConsegna(String from, String receiver, String replyTo, String subject, String gestoreMittente, String data, String orario, String messageId){

        //Costruzione del daticert
        StringBuffer stringBufferContent = new StringBuffer();
        stringBufferContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBufferContent.append("<postacert tipo=\"avvenuta-consegna\" errore=\"nessuno\">");
        stringBufferContent.append("<intestazione>");
        stringBufferContent.append("<mittente>").append(from).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBufferContent.append("<destinatari tipo=\"certificato\">").append(receiver).append("</destinatari>"); //destinatario dell'email, sta nella mappa
        stringBufferContent.append("<risposte>").append(replyTo).append("</risposte>"); //nel messaggio che uso per popolare la mappa c'è un reply-to
        stringBufferContent.append("<oggetto>").append(subject).append("</oggetto>"); //oggetto dell'email, sta nella mappa
        stringBufferContent.append("</intestazione>");
        stringBufferContent.append("<dati>");
        stringBufferContent.append("<gestore-emittente>").append(gestoreMittente).append("</gestore-emittente>"); //da inventare = "mock-pec" costante
        stringBufferContent.append("<data zona=\"+0200\">"); //lasciare così
        stringBufferContent.append("<giorno>").append(data).append("</giorno>"); //impostare in base all'ora
        stringBufferContent.append("<ora>").append(orario).append("</ora>"); //impostare in base all'ora
        stringBufferContent.append("</data>");
        stringBufferContent.append("<identificativo>").append(generateRandomString(64)).append("</identificativo>"); //stringa random 64 caratteri
        stringBufferContent.append("<msgid>").append("&lt;").append(messageId).append("&gt;").append("</msgid>"); //msgid della mappa, nella forma url encoded. fare url encode della stringa
        stringBufferContent.append("<ricevuta tipo=\"completa\" />");
        stringBufferContent.append("<consegna>").append(receiver).append("</consegna>");
        stringBufferContent.append("</dati>");
        stringBufferContent.append("</postacert>");

        return stringBufferContent;
    }

    public static StringBuffer generateRicevutaAccettazione(String data, String orario, String subject, String from, String receiver){

        //Costruzione della Ricevuta di accettazione del messaggio indirizzato
        StringBuffer stringBufferText = new StringBuffer();
        stringBufferText.append("Il giorno ").append(data).append(" alle ore ").append(orario).append(" (+0200) il messaggio con Oggetto");
        stringBufferText.append("\"").append(subject).append("\" inviato da \"").append(from).append("\""); // oggetto - from
        stringBufferText.append("ed indirizzato a: ").append(receiver).append(" (\"posta certificata\")"); // pecInfoIterator.getReceiverAddress()
        stringBufferText.append("è stato accettato dal sistema ed inoltrato.");
        stringBufferText.append("Identificativo del messaggio: ").append(generateRandomString(64)); // stringa random 64 caratteri
        stringBufferText.append("L'allegato daticert.xml contiene informazioni di servizio sulla trasmissione");

        return stringBufferText;
    }

    public static StringBuffer generateRicevutaConsegna(String data, String orario, String subject, String from, String receiver){

        //Costruzione della Ricevuta di accettazione del messaggio indirizzato
        StringBuffer stringBufferText = new StringBuffer();
        stringBufferText.append("Il giorno ").append(data).append(" alle ore ").append(orario).append(" (+0200) il messaggio con Oggetto");
        stringBufferText.append("\"").append(subject).append("\" inviato da \"").append(from).append("\""); // oggetto - from
        stringBufferText.append("ed indirizzato a: ").append(receiver).append(" (\"posta certificata\")"); // pecInfoIterator.getReceiverAddress()
        stringBufferText.append("è stato correttamente consegnato al destinatario.");
        stringBufferText.append("Identificativo del messaggio: ").append(generateRandomString(64)); // stringa random 64 caratteri
        stringBufferText.append("Il messaggio originale è incluso in allegato, per aprirlo cliccare sul fi=\n" +
                "le \"postacert.eml\" (nella webmail o in alcuni client di posta l'allegato po=\n" +
                "trebbe avere come nome l'oggetto del messaggio originale).\n" +
                "L'allegato daticert.xml contiene informazioni di servizio sulla trasmissione");

        return stringBufferText;
    }

    public static String insertMessageIdInBrackets(String messageID) {
        return String.format("%s%s%s", "<", messageID, ">");
    }

}
