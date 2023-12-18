
package it.pagopa.pn.pecmock.endpoint;

import https.bridgews_pec_it.pecimapbridge.*;
import https.bridgews_pec_it.pecimapbridge.SendMailResponse;
import it.pagopa.pn.pecmock.configuration.PecMockConfiguration;
import it.pagopa.pn.pecmock.exception.RequestedErrorException;
import it.pagopa.pn.pecmock.exception.SemaphoreException;
import it.pagopa.pn.pecmock.model.pojo.*;
import it.pagopa.pn.pecmock.utils.PecUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static it.pagopa.pn.pecmock.model.pojo.PecOperation.*;
import static it.pagopa.pn.pecmock.utils.LogUtils.*;
import static it.pagopa.pn.pecmock.utils.PecErrorUtils.errorLookUp;
import static it.pagopa.pn.pecmock.utils.PecErrorUtils.parsedErrorCodes;
import static it.pagopa.pn.pecmock.utils.PecUtils.insertMessageIdInBrackets;

@Endpoint
@Slf4j
public class PecImapBridgeEndpoint {
    private static final String NAMESPACE_URI = "https://bridgews.pec.it/PecImapBridge/";
    @Getter
    private final Map<String, PecInfo> pecMapProcessedElements = new ConcurrentHashMap<>();
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
            log.error(THREAD_WAITING, e, e.getMessage());
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
                }).flatMap(mimeMessage -> {

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
                        sendMailResponse.setErrstr("Eccezione: " + e + " - " + e.getMessage());
                        sendMailResponse.setErrcode(999);
                        sendMailResponse.setErrblock(0);
                        return Mono.just(sendMailResponse);
                    }

                    Map<String, String> errorMap = parsedErrorCodes(subject);
                    if (!errorMap.isEmpty() && errorMap.containsKey(SM.getValue())) {
                        return Mono.error(new RequestedErrorException(Integer.parseInt(errorMap.get(SM.getValue())), "Errore generato dalla richiesta."));
                    }

                    PecInfo pecInfoAccettazione = new PecInfo().messageId(messageID).receiverAddress(receiverAddress).from(from).replyTo(replyTo).subject(subject).pecType(PecType.ACCETTAZIONE).errorCodes(errorMap);
                    PecInfo pecInfoConsegna = new PecInfo().messageId(messageID).receiverAddress(receiverAddress).from(from).replyTo(replyTo).subject(subject).pecType(PecType.CONSEGNA).errorCodes(errorMap);

                    String idInfoAccettazione = PecUtils.generateRandomString(64);
                    String idInfoConsegna = PecUtils.generateRandomString(64);

                    pecMapProcessedElements.put(idInfoAccettazione, pecInfoAccettazione);
                    pecMapProcessedElements.put(idInfoConsegna, pecInfoConsegna);

                    log.debug("Added pec with idInfoAccettazione: {} , idInfoConsegna: {}", idInfoAccettazione, idInfoConsegna);
                    sendMailResponse.setErrstr(messageID);

                    log.debug("---Email presenti nella mappa sendMail()---: {}", pecMapProcessedElements.size());
                    return Mono.just(sendMailResponse);
                })
                .onErrorResume(RequestedErrorException.class, throwable -> {
                    sendMailResponse.setErrcode(throwable.getErrorCode());
                    sendMailResponse.setErrstr(throwable.getErrorMsg());
                    return Mono.just(sendMailResponse);
                })
                .transform(delayElement())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, SEND_MAIL, result))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, SEND_MAIL, throwable, throwable.getMessage()))
                .doFinally(sendMailResponseMsg -> semaphore.release())
                .block();
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMessages")
    @ResponsePayload
    public GetMessagesResponse getMessages(@RequestPayload GetMessages parameters) {
        log.debug(INVOKING_OPERATION, GET_MESSAGES, parameters.toString());

        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();

        //TODO: se Limit diverso da 0, errore
        log.debug("getMessages() - limit {}", parameters.getLimit());
        if (Objects.isNull(parameters.getLimit()) || parameters.getLimit() <= 0) {
            getMessagesResponse.setErrcode(400);
            getMessagesResponse.setErrstr("Il valore deve essere maggiore di 0 per il parametro Limit");
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
            log.error(THREAD_WAITING, e, e.getMessage());
            throw new SemaphoreException();
        }

        return Flux.fromIterable(pecMapProcessedElements.entrySet())
                .filter(pecInfoEntry -> !pecInfoEntry.getValue().isRead())
                .take(parameters.getLimit())
                .map(pecInfoEntry -> {
                    String messageID = insertMessageIdInBrackets(pecInfoEntry.getKey());
                    PecInfo pecInfo = pecInfoEntry.getValue();
                    log.info("MessageID : {}", messageID);
                    if (pecInfo.getPecType().equals(PecType.ACCETTAZIONE))
                        return generateMimeMessageAccettazione(pecInfo, messageID);
                    else return generateMimeMessageConsegna(pecInfo, messageID);
                })
                .collect(MesArrayOfMessages::new, (messages, bytes) -> messages.getItem().add(bytes))
                .filter(mesArrayOfMessages -> mesArrayOfMessages.getItem().size() > 0)
                .doOnNext(getMessagesResponse::setArrayOfMessages)
                .thenReturn(getMessagesResponse)
                .transform(delayElement())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, GET_MESSAGES, result))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, GET_MESSAGES, throwable, throwable.getMessage()))
                .doFinally(result -> semaphore.release())
                .block();
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMessageID")
    @ResponsePayload
    public GetMessageIDResponse getMessageID(@RequestPayload GetMessageID parameters) {
        GetMessageIDResponse getMessageIDResponse = new GetMessageIDResponse();

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(THREAD_WAITING, e, e.getMessage());
            throw new SemaphoreException();
        }

        String messageID = parameters.getMailid();
        log.debug(INVOKING_OPERATION, GET_MESSAGE_ID, messageID);

        //obbligatorio not null
        if (Objects.isNull(messageID) || messageID.equals("")) {
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

        return Mono.justOrEmpty(pecMapProcessedElements.get(messageID))
                .flatMap(pecInfo -> errorLookUp(ID, pecInfo))
                .map(pecInfo ->
                {
                    getMessageIDResponse.setErrcode(0);
                    getMessageIDResponse.setErrstr("ok");
                    getMessageIDResponse.setErrblock(0);
                    pecInfo.setRead(true);
                    pecMapProcessedElements.put(messageID, pecInfo);
                    if (pecInfo.getPecType().equals(PecType.ACCETTAZIONE))
                        getMessageIDResponse.setMessage(generateMimeMessageAccettazione(pecInfo, parameters.getMailid()));
                    else getMessageIDResponse.setMessage(generateMimeMessageConsegna(pecInfo, parameters.getMailid()));
                    return getMessageIDResponse;
                })
                //se pecInfo è null diamo una response di errore
                .switchIfEmpty(Mono.defer(() -> {
                    getMessageIDResponse.setErrcode(404);
                    getMessageIDResponse.setErrstr("Id del messaggio non trovato: " + messageID);
                    getMessageIDResponse.setErrblock(0);
                    return Mono.just(getMessageIDResponse);
                }))
                .onErrorResume(RequestedErrorException.class, requestedErrorException -> {
                    getMessageIDResponse.setErrcode(requestedErrorException.getErrorCode());
                    getMessageIDResponse.setErrstr(requestedErrorException.getErrorMsg());
                    getMessageIDResponse.setErrblock(0);
                    return Mono.just(getMessageIDResponse);
                })
                .transform(delayElement())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, GET_MESSAGE_ID, result))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, GET_MESSAGE_ID, throwable, throwable.getMessage()))
                .doFinally(result -> semaphore.release())
                .block();
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "deleteMail")
    @ResponsePayload
    public DeleteMailResponse deleteMail(@RequestPayload DeleteMail parameters) {

        DeleteMailResponse deleteMailResponse = new DeleteMailResponse();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(THREAD_WAITING, e, e.getMessage());
            throw new SemaphoreException();
        }

        String messageID = parameters.getMailid();
        log.debug(INVOKING_OPERATION, DELETE_MAIL, messageID);

        if (Objects.isNull(messageID) || messageID.equals("")) {
            deleteMailResponse.setErrcode(400);
            deleteMailResponse.setErrstr("Il campo mailId deve essere valorizzato");
            return deleteMailResponse;
        }

        return Mono.justOrEmpty(pecMapProcessedElements.get(messageID))
                .flatMap(pecInfo -> errorLookUp(DM, pecInfo))
                .map(unused -> {
                    pecMapProcessedElements.remove(messageID);
                    deleteMailResponse.setErrcode(0);
                    deleteMailResponse.setErrstr("ok");
                    deleteMailResponse.setErrblock(0);
                    return deleteMailResponse;
                })
                .switchIfEmpty(Mono.defer(() ->
                {
                    deleteMailResponse.setErrcode(99);
                    deleteMailResponse.setErrstr("Messaggio inesistente: " + parameters.getMailid());
                    deleteMailResponse.setErrblock(0);
                    return Mono.just(deleteMailResponse);
                }))
                .onErrorResume(RequestedErrorException.class, throwable -> {
                    deleteMailResponse.setErrcode(throwable.getErrorCode());
                    deleteMailResponse.setErrstr(throwable.getErrorMsg());
                    deleteMailResponse.setErrblock(0);
                    return Mono.just(deleteMailResponse);
                })
                .transform(delayElement())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, DELETE_MAIL, result))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, DELETE_MAIL, throwable, throwable.getMessage()))
                .doFinally(result -> semaphore.release())
                .block();

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMessageCount")
    @ResponsePayload
    public GetMessageCountResponse getMessageCount(@RequestPayload GetMessageCount parameters) {
        GetMessageCountResponse getMessageCountResponse = new GetMessageCountResponse();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(THREAD_WAITING, e, e.getMessage());
            throw new SemaphoreException();
        }

        return Mono.just(parameters)
                .map(getMessageCount -> {
                    getMessageCountResponse.setErrcode(0);
                    getMessageCountResponse.setErrstr("ok");
                    getMessageCountResponse.setErrblock(0);
                    getMessageCountResponse.setCount(pecMapProcessedElements.size());
                    return getMessageCountResponse;
                })
                .transform(delayElement())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, MESSAGE_COUNT, result))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, MESSAGE_COUNT, throwable, throwable.getMessage()))
                .doFinally(result -> semaphore.release())
                .block();
    }

    private <T> Function<Mono<T>, Mono<T>> delayElement() {
        return tMono -> tMono.flatMap(response -> {
            try {
                Thread.sleep(PecUtils.getRandomNumberBetweenMinMax(minDelay, maxDelay));
            } catch (InterruptedException e) {
                return Mono.error(e);
            }
            return Mono.just(response);
        });
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
