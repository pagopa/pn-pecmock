package it.pagopa.pn.pecmock.endpoint;

import https.bridgews_pec_it.pecimapbridge.*;
import https.bridgews_pec_it.pecimapbridge.SendMailResponse;
import it.pagopa.pn.pecmock.configuration.PecMockConfiguration;
import it.pagopa.pn.pecmock.exception.SemaphoreException;
import it.pagopa.pn.pecmock.model.pojo.EmailAttachment;
import it.pagopa.pn.pecmock.model.pojo.EmailField;
import it.pagopa.pn.pecmock.model.pojo.PecInfo;
import it.pagopa.pn.pecmock.model.pojo.PecType;
import it.pagopa.pn.pecmock.utils.PecUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import reactor.core.publisher.Mono;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;

import static it.pagopa.pn.pecmock.utils.LogUtils.*;

@Endpoint
@Slf4j
@AutoConfiguration
public class PecImapBridgeEndpoint {
    private static final String NAMESPACE_URI = "https://bridgews.pec.it/PecImapBridge/";
    private final Map<String, PecInfo> pecMapProcessedElements = new HashMap<>();
    private int mockPecSemaphore;
    private int minDelay;
    private int maxDelay;
    private Semaphore semaphore = null;
    private static final String MOCK_PEC = "MOCK_PEC";

    @Autowired
    public PecImapBridgeEndpoint(PecMockConfiguration mockConf) {
        log.debug("MockEndpoint() - {}", mockPecSemaphore);
        mockPecSemaphore = mockConf.semaphores();
        minDelay = mockConf.minDelay();
        maxDelay = mockConf.maxDelay();
        semaphore = new Semaphore(mockPecSemaphore);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "sendMail")
    @ResponsePayload
    public SendMailResponse sendMail(@RequestPayload SendMail parameters) {
        SendMailResponse sendMailResponse = new SendMailResponse();

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        if (Objects.isNull(parameters.getData()) || parameters.getData().equals("")) {
            sendMailResponse.setErrcode(400);
            sendMailResponse.setErrstr("Il campo Data deve essere valorizzato");
            throw new RuntimeException("Il campo Data deve essere valorizzato");
        }

        return Mono.just(parameters).map(sendMail -> {

                    log.debug(INVOKING_OPERATION, SEND_MAIL, sendMail);
                    byte[] data = sendMail.getData().trim().getBytes(StandardCharsets.UTF_8);

                    return PecUtils.getMimeMessage(data);
                }).map(mimeMessage -> {

                    String subject = null;
                    String messageID = null;
                    String from = null;
                    String replyTo = null;
                    String receiverAddress = null;

                    try {
                        messageID = mimeMessage.getMessageID();
                        log.debug("---MESSAGE ID---: {}", messageID);
                        subject = mimeMessage.getSubject(); //oggetto dell'email
                        from = mimeMessage.getFrom()[0].toString(); //mittente
                        replyTo = mimeMessage.getReplyTo()[0].toString(); //replyTo
                        receiverAddress = mimeMessage.getAllRecipients()[0].toString();
                    } catch (MessagingException e) {
                        log.error("The retrivial of the field MessageID caused an exception: {} - {}", e, e.getMessage());
                        SendMailResponse sendMailResponseError = new SendMailResponse();
                        sendMailResponseError.setErrstr("Eccezione: " + e + " - " + e.getMessage());
                        sendMailResponseError.setErrcode(999);
                        sendMailResponseError.setErrblock(0);

                        return sendMailResponseError;
                    }

                    PecInfo pecInfoAccettazione = new PecInfo().messageId(messageID).receiverAddress(receiverAddress).from(from).replyTo(replyTo).subject(subject).pecType(PecType.ACCETTAZIONE);
                    PecInfo pecInfoConsegna = new PecInfo().messageId(messageID).receiverAddress(receiverAddress).from(from).replyTo(replyTo).subject(subject).pecType(PecType.CONSEGNA);

                    pecMapProcessedElements.put(PecUtils.generateRandomString(64), pecInfoAccettazione);
                    pecMapProcessedElements.put(PecUtils.generateRandomString(64), pecInfoConsegna);

                    sendMailResponse.setErrcode(0);
                    sendMailResponse.setErrblock(0);
                    sendMailResponse.setErrstr(messageID);

                    log.debug("---Email presenti nella mappa sendMail()---: {}", pecMapProcessedElements.size());
                    log.info(SUCCESSFUL_OPERATION, SEND_MAIL, sendMailResponse);
                    return sendMailResponse;
                })
                .delayElement(Duration.ofMillis(PecUtils.getRandomNumberBetweenMinMax(minDelay, maxDelay)))
                .map(sendMailResponseMsg -> {
                    semaphore.release();
                    return sendMailResponseMsg;
                }).block();
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMessages")
    @ResponsePayload
    public GetMessagesResponse getMessages(@RequestPayload GetMessages parameters) {
        log.debug(INVOKING_OPERATION, GET_MESSAGES, parameters.toString());

        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();
        MesArrayOfMessages mesArrayOfMessages = new MesArrayOfMessages();

        //TODO: se Limit diverso da 0, errore
        log.debug("getMessages() - limit {}", parameters.getLimit());
        if (Objects.isNull(parameters.getLimit()) || parameters.getLimit() == 0) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("Il valore deve essere diverso da 0 per il parametro Limit");
            return getMessagesResponse;
        }
        //TODO: se outtype diverso da 2, errore
        if (Objects.isNull(parameters.getOuttype()) || parameters.getOuttype() != 2) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("E' accettato solo il valore 2 per il parametro Outtype");
            return getMessagesResponse;
        }
        //TODO: se unseen diverso da 1, errore
        if (Objects.isNull(parameters.getUnseen()) || parameters.getUnseen() != 1) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("E' accettato solo il valore 1 per il parametro Unseen");
            return getMessagesResponse;
        }
        //TODO: se offset diverso da 0/valorizzato, errore
        if (Objects.nonNull(parameters.getOffset()) && parameters.getOffset() != 0) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("E' accettato solo il valore 0 per il parametro Offset");
            return getMessagesResponse;
        }
        //TODO: se msgType diverso da "ALL", errore
        if (Objects.nonNull(parameters.getMsgtype()) && !parameters.getMsgtype().equals("ALL")) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("E' accettato solo il valore 'ALL' per il parametro Msgtype");
            return getMessagesResponse;
        }
        //TODO: se markseen uguale a "1", errore
        if (Objects.nonNull(parameters.getMarkseen()) && parameters.getMarkseen() == 1) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("E' accettato solo il valore diverso da 1 per il parametro Markseen");
            return getMessagesResponse;
        }
        //TODO: se markseen uguale a "1", errore
        if (Objects.nonNull(parameters.getFolder()) && !parameters.getFolder().equals("INBOX")) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("E' accettato solo il valore INBOX per il parametro Folder");
            return getMessagesResponse;
        }
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        Mono.just(parameters).map(getMessages -> {
            int mapSize = pecMapProcessedElements.size();

            int limit = parameters.getLimit() == null ? 0 : parameters.getLimit();

            if (limit % 2 != 0 || limit == 0) {
                getMessagesResponse.setErrcode(400);
                getMessagesResponse.setErrstr("Sono accettati valori pari per il parametro limit");
                throw new RuntimeException("Sono accettati valori pari per il parametro limit");
            }
            //Prendiamo il minimo tra la lunghezza della mappa e il parametro "limit" fornito dalla request.
            //In questo modo, se il limit supera la size della mappa, evitiamo una IndexOutOfBoundsException.
            limit = Math.min(parameters.getLimit() == null ? mapSize * 2 : parameters.getLimit(), mapSize * 2);

            log.debug("getMessages() - calculated limit {}", limit);
            //flux from iterable, creare lista degli id necessari
            //Il parametro "MesArrayOfMessages" deve essere inizializzato SOLO se sono presenti messaggi in memoria.
            if (limit > 0) {
                Iterator<Map.Entry<String, PecInfo>> iterator = pecMapProcessedElements.entrySet().iterator();
                for (int i = 0; i < limit; i++) {
                    log.debug("getMessages() - message n. {}", i);
                    String messageIdIterator = null;
                    PecInfo pecInfo = null;
                    if (iterator.hasNext()) {
                        Map.Entry<String, PecInfo> entry = iterator.next();
                        messageIdIterator = entry.getKey();
                        log.debug("---messageIdIterator---: {}", messageIdIterator);
                        pecInfo = entry.getValue();

                        if (pecInfo.getPecType().equals(PecType.ACCETTAZIONE))
                            mesArrayOfMessages.getItem().add(generateMimeMessageAccettazione(pecInfo, messageIdIterator));
                        else mesArrayOfMessages.getItem().add(generateMimeMessageConsegna(pecInfo, messageIdIterator));

                    } else {
                        log.debug("break");
                        break;
                    }
                }
            }

            log.debug("---pecMapProcessedElements.size() getMessages()---: {}", pecMapProcessedElements.size());
            return Mono.empty();
        }).subscribe();

        getMessagesResponse.setArrayOfMessages(mesArrayOfMessages);
        log.info(SUCCESSFUL_OPERATION, GET_MESSAGES, getMessagesResponse);

        try {
            int iDelay = PecUtils.getRandomNumberBetweenMinMax(minDelay, maxDelay);
            log.debug("getMessages() - delay {}", iDelay);
            Thread.sleep(iDelay);
        } catch (Exception e) {
            log.error("Exception Thread");
        }
        semaphore.release();

        return getMessagesResponse;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMessageID")
    @ResponsePayload
    public GetMessageIDResponse getMessageID(@RequestPayload GetMessageID parameters) {
        GetMessageIDResponse getMessageIDResponse = new GetMessageIDResponse();

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        //obbligatorio not null
        if (Objects.isNull(parameters.getMailid()) || parameters.getMailid().equals("")) {
            getMessageIDResponse.setErrcode(400);
            getMessageIDResponse.setErrstr("Il campo mailId deve essere valorizzato");
            return getMessageIDResponse;
        }
        //isuid deve essere 2 e not null
        if (Objects.isNull(parameters.getIsuid()) || !parameters.getIsuid().equals(2)) {
            getMessageIDResponse.setErrcode(400);
            getMessageIDResponse.setErrstr("E' accettato solo il valore 2 per il parametro isuId");
            return getMessageIDResponse;
        }
        //markseen not null e valorizzato a 1
        if (Objects.isNull(parameters.getMarkseen()) || !parameters.getMarkseen().equals(1)) {
            getMessageIDResponse.setErrcode(400);
            getMessageIDResponse.setErrstr("E' accettato solo il valore 1 per il parametro markseen");
            return getMessageIDResponse;
        }

        return Mono.just(parameters).map(sendMail -> {

                    log.debug(INVOKING_OPERATION, GET_MESSAGE_ID, sendMail);
                    String messageID = sendMail.getMailid();
                    var pecInfo = pecMapProcessedElements.remove(messageID);
                    log.debug("Removed pec with messageID '{}'", messageID);

                    //se pecInfo è null diamo una response di errore
                    if (Objects.isNull(pecInfo)) {
                        getMessageIDResponse.setErrcode(404);
                        getMessageIDResponse.setErrstr("Id del messaggio non trovato: " + messageID);
                        getMessageIDResponse.setErrblock(0);
                    } else {
                        getMessageIDResponse.setErrcode(0);
                        getMessageIDResponse.setErrstr("ok");
                        getMessageIDResponse.setErrblock(0);
                        if (pecInfo.getPecType().equals(PecType.ACCETTAZIONE))
                            getMessageIDResponse.setMessage(generateMimeMessageAccettazione(pecInfo, parameters.getMailid()));
                        else getMessageIDResponse.setMessage(generateMimeMessageConsegna(pecInfo, parameters.getMailid()));
                    }
                    log.info(SUCCESSFUL_OPERATION, SEND_MAIL, getMessageIDResponse);

                    return getMessageIDResponse;
                }).delayElement(Duration.ofMillis(PecUtils.getRandomNumberBetweenMinMax(minDelay, maxDelay)))
                .map(getMessageIDResponseMsg -> {
                    semaphore.release();
                    return getMessageIDResponseMsg;
                }).block();
    }

    private EmailAttachment generateDaticertAccettazioneField(PecInfo pecInfo) {

        String subject = pecInfo.getSubject();
        String messageID = pecInfo.getMessageId();
        String from = pecInfo.getFrom();
        String replyTo = pecInfo.getReplyTo();
        String receiverAddress = pecInfo.getReceiverAddress();

        String timeStampData = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
        String timeStampOrario = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

        StringBuffer daticert = PecUtils.generateDaticertAccettazione(from, receiverAddress, replyTo, subject,
                MOCK_PEC, timeStampData, timeStampOrario, messageID);

        ByteArrayOutputStream byteArrayOutputStreamDaticert = new ByteArrayOutputStream(daticert.length());
        try {
            byteArrayOutputStreamDaticert.write(daticert.toString().getBytes());
        } catch (IOException ioException) {
            log.error("IOException: {} - {}", ioException, ioException.getMessage());
        }

        return EmailAttachment.builder()
                .nameWithExtension("daticert.xml")
                .content(byteArrayOutputStreamDaticert)//generare una stringa, da cui generare l'outputstram
                .build();
    }

    private EmailAttachment generateDaticertConsegnaField(PecInfo pecInfo) {

        String subject = pecInfo.getSubject();
        String messageID = pecInfo.getMessageId();
        String from = pecInfo.getFrom();
        String replyTo = pecInfo.getReplyTo();
        String receiverAddress = pecInfo.getReceiverAddress();

        String timeStampData = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
        String timeStampOrario = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

        StringBuffer daticert = PecUtils.generateDaticertConsegna(from, receiverAddress, replyTo, subject,
                MOCK_PEC, timeStampData, timeStampOrario, messageID);

        ByteArrayOutputStream byteArrayOutputStreamDaticert = new ByteArrayOutputStream(daticert.length());
        try {
            byteArrayOutputStreamDaticert.write(daticert.toString().getBytes());
        } catch (IOException ioException) {
            log.error("IOException: {} - {}", ioException, ioException.getMessage());
        }

        return EmailAttachment.builder()
                .nameWithExtension("daticert.xml")
                .content(byteArrayOutputStreamDaticert)//generare una stringa, da cui generare l'outputstram
                .build();
    }

    private byte[] generateMimeMessageAccettazione(PecInfo pecInfo, String msgId) {
        String subject = pecInfo.getSubject();
        String from = pecInfo.getFrom();
        String receiverAddress = pecInfo.getReceiverAddress();

        String timeStampData = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
        String timeStampOrario = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

        StringBuffer ricevutaAccettazione = PecUtils.generateRicevutaAccettazione(timeStampData, timeStampOrario, subject, from, receiverAddress);
        List<EmailAttachment> listAttachmentsAccettazione = new ArrayList<>();
        listAttachmentsAccettazione.add(generateDaticertAccettazioneField(pecInfo));

        EmailField emailFieldAccettazione = EmailField.builder()
                .subject("ACCETTAZIONE")
                .to(from)//mittende della mappa, se non c'è è da salvare
                .from("posta-certificata@pec.aruba.it")
                .contentType("text/plain")
                .msgId(msgId)//stringa random 64 caratteri
                .text(String.valueOf(ricevutaAccettazione))//ricomporre stringa "Ricevuta di accettazione del messaggio indirizzato"
                .emailAttachments(listAttachmentsAccettazione).build();

        return PecUtils.getMimeMessageInBase64(emailFieldAccettazione);
    }

    private byte[] generateMimeMessageConsegna(PecInfo pecInfo, String msgId) {
        String subject = pecInfo.getSubject();
        String messageID = pecInfo.getMessageId();
        String from = pecInfo.getFrom();
        String replyTo = pecInfo.getReplyTo();
        String receiverAddress = pecInfo.getReceiverAddress();

        String timeStampData = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
        String timeStampOrario = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

        StringBuffer ricevutaConsegna = PecUtils.generateRicevutaAccettazione(timeStampData, timeStampOrario, subject, from, receiverAddress);
        List<EmailAttachment> listAttachmentsConsegna = new ArrayList<>();
        listAttachmentsConsegna.add(generateDaticertConsegnaField(pecInfo));


        EmailField emailFieldConsegna = EmailField.builder()
                .subject("CONSEGNA")
                .to(from)//mittende della mappa, se non c'è è da salvare
                .from("posta-certificata@pec.aruba.it")
                .contentType("text/plain")
                .msgId(msgId)//stringa random 64 caratteri
                .text(String.valueOf(ricevutaConsegna))//ricomporre stringa "Ricevuta di accettazione del messaggio indirizzato"
                .emailAttachments(listAttachmentsConsegna).build();

        return PecUtils.getMimeMessageInBase64(emailFieldConsegna);
    }

}
