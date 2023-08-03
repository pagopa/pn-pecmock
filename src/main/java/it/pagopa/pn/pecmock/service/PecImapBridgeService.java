package it.pagopa.pn.pecmock.service;

import it.pagopa.pn.pecmock.exception.SemaphoreException;
import it.pagopa.pn.pecmock.model.pojo.EmailAttachment;
import it.pagopa.pn.pecmock.model.pojo.EmailField;
import it.pagopa.pn.pecmock.model.pojo.PecInfo;
import it.pagopa.pn.pecmock.utils.PecUtils;
import it.pec.bridgews.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.jaxws.ServerAsyncResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.pecmock.utils.LogUtils.*;

@Service
@Slf4j
public class PecImapBridgeService implements PecImapBridge {

    //Key : messageID della PEC
    //Value : info sulla PEC
    private final Map<String, PecInfo> pecMap = new HashMap<>();
    private final Semaphore semaphore = new Semaphore(10);
    private static final int MIN = 1000;
    private static final int MAX = 3000;
    private static final String MOCK_PEC = "MOCK_PEC";

    @Override
    public Response<SendMailResponse> sendMailAsync(SendMail parameters) {
        return null;
    }

    @Override
    public Future<?> sendMailAsync(SendMail parameters, AsyncHandler<SendMailResponse> asyncHandler) {
        return null;
    }

    @Override
    public SendMailResponse sendMail(SendMail parameters) {

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        return Mono.just(parameters).map(sendMail -> {

            log.debug(INVOKING_OPERATION, SEND_MAIL, sendMail);
            byte[] data = sendMail.getData().trim().getBytes(StandardCharsets.UTF_8);

            return PecUtils.getMimeMessage(data);
        }).map(mimeMessage -> {

            String messageID = null;
            try {
                messageID = mimeMessage.getMessageID();
                PecInfo pecInfo = new PecInfo();
                pecInfo.setSubject(mimeMessage.getSubject()); //oggetto dell'email
                pecInfo.setFrom(mimeMessage.getFrom()[0].toString()); //mittente
                pecInfo.setReplyTo(mimeMessage.getReplyTo()[0].toString()); //replyTo
                pecInfo.setReceiverAddress(mimeMessage.getAllRecipients()[0].toString());

                pecMap.put(messageID, pecInfo);
                log.debug("Pec with messageID '{}' saved in temporary memory", messageID);
            } catch (MessagingException  e) {
                log.error("The retrivial of the field MessageID caused an exception: {} - {}", e, e.getMessage());
                SendMailResponse sendMailResponseError = new SendMailResponse();
                sendMailResponseError.setErrstr("Eccezione: " + e + " - " + e.getMessage());
                sendMailResponseError.setErrcode(999);
                sendMailResponseError.setErrblock(0);

                return sendMailResponseError;
            }

            SendMailResponse sendMailResponse = new SendMailResponse();
            sendMailResponse.setErrcode(0);
            sendMailResponse.setErrblock(0);
            sendMailResponse.setErrstr(messageID);
            log.info(SUCCESSFUL_OPERATION, SEND_MAIL, sendMailResponse);
            return sendMailResponse;
        })
        .delayElement(Duration.ofMillis(PecUtils.getRandomNumberBetweenMinMax(MIN, MAX)))
        .map(sendMailResponse -> {
             semaphore.release();
             return sendMailResponse;
        }).block();
    }

    @Override
    public Response<GetMessagesResponse> getMessagesAsync(GetMessages parameters) {
        return null;
    }

    @Override
    public Future<?> getMessagesAsync(GetMessages parameters, AsyncHandler<GetMessagesResponse> asyncHandler) {
        return null;
    }

    @Override
    public GetMessagesResponse getMessages(GetMessages parameters) {
        log.debug(INVOKING_OPERATION, GET_MESSAGES, parameters);

        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();
        AtomicReference<MesArrayOfMessages> mesArrayOfMessages = new AtomicReference<>();

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        Mono.just(parameters).map(getMessages -> {
            int mapSize = pecMap.size();

            //Prendiamo il minimo tra la lunghezza della mappa e il parametro "limit" fornito dalla request.
            //In questo modo, se il limit supera la size della mappa, evitiamo una IndexOutOfBoundsException.
            int limit = Math.min(parameters.getLimit() == null ? mapSize * 2 : parameters.getLimit(), mapSize * 2);

            //mapsize = 10, limit = 12
            if (limit % 2 != 0) {
                getMessagesResponse.setErrcode(400);
                getMessagesResponse.setErrstr("Sono accettati valori pari per il parametro limit");
                throw new RuntimeException("Sono accettati valori pari per il parametro limit");
            }
            //flux from iterable, creare lista degli id necessari
            //Il parametro "MesArrayOfMessages" deve essere inizializzato SOLO se sono presenti messaggi in memoria.
            List<PecInfo> listPecInfo = null;
            if (limit > 0) {
                limit = limit / 2;
                PecInfo pecInfo = new PecInfo();
                listPecInfo = new ArrayList<>();
                Iterator<Map.Entry<String, PecInfo>> iterator = pecMap.entrySet().iterator();

                for (int i = 0; i < limit; i++) {
                    String messageIdIterator = null;
                    PecInfo pecInfoIterator = null;
                    if (iterator.hasNext()) {
                        Map.Entry<String, PecInfo> entry = iterator.next();
                        messageIdIterator = entry.getKey();
                        pecInfoIterator = entry.getValue();

                        pecInfo.setMessageId(messageIdIterator);
                        pecInfo.setReceiverAddress(pecInfoIterator.getReceiverAddress());
                        pecInfo.setSubject(pecInfoIterator.getSubject());
                        pecInfo.setFrom(pecInfoIterator.getFrom());
                        pecInfo.setReplyTo(pecInfoIterator.getReplyTo());

                        listPecInfo.add(pecInfo);
                    } else {
                        break;
                    }
                }

            }
            return listPecInfo;
        }).flatMapIterable(pecInfos -> pecInfos).map(pecInfo -> {
            String timeStampData = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
            String timeStampOrario = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

            StringBuffer daticertAccettazione = PecUtils.generateDaticertAccettazione(pecInfo.getFrom(), pecInfo.getReceiverAddress(), pecInfo.getReplyTo(), pecInfo.getSubject(),
                    MOCK_PEC, timeStampData, timeStampOrario, pecInfo.getMessageId());

            StringBuffer daticertConsegna = PecUtils.generateDaticertConsegna(pecInfo.getFrom(), pecInfo.getReceiverAddress(), pecInfo.getReplyTo(), pecInfo.getSubject(),
                    MOCK_PEC, timeStampData, timeStampOrario, pecInfo.getMessageId());

            StringBuffer ricevutaAccettazione = PecUtils.generateRicevutaAccettazione(timeStampData, timeStampOrario, pecInfo.getSubject(), pecInfo.getFrom(), pecInfo.getReceiverAddress());
            StringBuffer ricevutaConsegna = PecUtils.generateRicevutaConsegna(timeStampData, timeStampOrario, pecInfo.getSubject(), pecInfo.getFrom(), pecInfo.getReceiverAddress());

            ByteArrayOutputStream byteArrayOutputStreamAccettazione = new ByteArrayOutputStream(daticertAccettazione.length());
            ByteArrayOutputStream byteArrayOutputStreamConsegna = new ByteArrayOutputStream(daticertConsegna.length());
            try {
                byteArrayOutputStreamAccettazione.write(daticertAccettazione.toString().getBytes());
                byteArrayOutputStreamConsegna.write(daticertConsegna.toString().getBytes());
            }catch (IOException ioException){
                log.error("IOException: {} - {}", ioException, ioException.getMessage());
            }

            EmailAttachment emailAttachmentAccettazione = EmailAttachment.builder()
                    .nameWithExtension("daticert.xml")
                    .content(byteArrayOutputStreamAccettazione)//generare una stringa, da cui generare l'outputstram
                    .build();

            List<EmailAttachment> listAttachmentsAccettazione = new ArrayList<>();
            listAttachmentsAccettazione.add(emailAttachmentAccettazione);

            EmailField emailFieldAccettazione = EmailField.builder()
                    .subject("ACCETTAZIONE")
                    .to(pecInfo.getFrom())//mittende della mappa, se non c'è è da salvare
                    .from("posta-certificata@pec.aruba.it")
                    .contentType("multipart/mixed")
                    .msgId(PecUtils.generateRandomString(64))//stringa random 64 caratteri
                    .text(String.valueOf(ricevutaAccettazione))//ricomporre stringa "Ricevuta di accettazione del messaggio indirizzato"
                    .emailAttachments(listAttachmentsAccettazione).build();

            EmailAttachment emailAttachmentConsegna = EmailAttachment.builder()
                    .nameWithExtension("daticert.xml")
                    .content(byteArrayOutputStreamAccettazione)//generare una stringa, da cui generare l'outputstram
                    .build();

            List<EmailAttachment> listAttachmentsConsegna = new ArrayList<>();
            listAttachmentsConsegna.add(emailAttachmentConsegna);

            EmailField emailFieldConsegna = EmailField.builder()
                    .subject("ACCETTAZIONE")
                    .to(pecInfo.getFrom())//mittende della mappa, se non c'è è da salvare
                    .from("posta-certificata@pec.aruba.it")
                    .contentType("multipart/mixed")
                    .msgId(PecUtils.generateRandomString(64))//stringa random 64 caratteri
                    .text(String.valueOf(ricevutaConsegna))//ricomporre stringa "Ricevuta di accettazione del messaggio indirizzato"
                    .emailAttachments(listAttachmentsConsegna).build();

            mesArrayOfMessages.get().getItem().add(Base64.getEncoder().encodeToString(emailFieldAccettazione.toString().getBytes()).getBytes());//aggiungere emailfield con encode base64
            mesArrayOfMessages.get().getItem().add(Base64.getEncoder().encodeToString(emailFieldConsegna.toString().getBytes()).getBytes());//aggiungere emailfield con encode base64

            return null;
        }).subscribe();

            getMessagesResponse.setArrayOfMessages(mesArrayOfMessages.get());
            log.info(SUCCESSFUL_OPERATION, GET_MESSAGES, getMessagesResponse);

            try{
                Thread.sleep(PecUtils.getRandomNumberBetweenMinMax(MIN, MAX));
            }catch (Exception e){
                log.error("Excepion Thread");
            }
            semaphore.release();

            return getMessagesResponse;
    }

    @Override
    public Response<GetMessageIDResponse> getMessageIDAsync(GetMessageID parameters) {
        return null;
    }

    @Override
    public Future<?> getMessageIDAsync(GetMessageID parameters, AsyncHandler<GetMessageIDResponse> asyncHandler) {
        return null;
    }

    @Override
    public GetMessageIDResponse getMessageID(GetMessageID parameters) {

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        return Mono.just(parameters).map(sendMail -> {

            log.debug(INVOKING_OPERATION, GET_MESSAGE_ID, sendMail);
            String messageID = sendMail.getMailid();
            PecInfo pecInfo = pecMap.remove(messageID);
            log.debug("Removed pec with messageID '{}' associated to receiverAddress '{}'", messageID, pecInfo.getReceiverAddress());
            GetMessageIDResponse getMessageIDResponse = new GetMessageIDResponse();
            log.info(SUCCESSFUL_OPERATION, SEND_MAIL, getMessageIDResponse);

            return  getMessageIDResponse;
        }).delayElement(Duration.ofMillis(PecUtils.getRandomNumberBetweenMinMax(MIN, MAX)))
        .map(getMessageIDResponse -> {
            semaphore.release();
            return getMessageIDResponse;
        }).block();

    }
}
