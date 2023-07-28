package it.pagopa.pn.pecmock.service;

import it.pagopa.pn.pecmock.model.pojo.PecInfo;
import it.pagopa.pn.pecmock.utils.PecUtils;
import it.pec.bridgews.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.jaxws.ServerAsyncResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.mail.internet.MimeMessage;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import static it.pagopa.pn.pecmock.utils.LogUtils.*;

@Service
@Slf4j
public class PecImapBridgeService implements PecImapBridge {

    //Key : messageID della PEC
    //Value : info sulla PEC
    private final Map<String, PecInfo> pecMap = new HashMap<>();
    private final Semaphore semaphore = new Semaphore(10);

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
            throw new RuntimeException(e);
        }

        log.debug(INVOKING_OPERATION, SEND_MAIL, parameters);
        byte[] data = parameters.getData().trim().getBytes(StandardCharsets.UTF_8);
        MimeMessage mimeMessage = PecUtils.getMimeMessage(data);
        try {
            String messageID = mimeMessage.getMessageID();
            String receiverAddress = mimeMessage.getAllRecipients()[0].toString();
            pecMap.put(messageID, new PecInfo().receiverAddress(receiverAddress));
            log.debug("Pec with messageID '{}' saved in temporary memory", messageID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SendMailResponse sendMailResponse = new SendMailResponse();
        log.info(SUCCESSFUL_OPERATION, SEND_MAIL, sendMailResponse);

        semaphore.release();

        return sendMailResponse;
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
        int mapSize = pecMap.size();

        //Prendiamo il minimo tra la lunghezza della mappa e il parametro "limit" fornito dalla request.
        //In questo modo, se il limit supera la size della mappa, evitiamo una IndexOutOfBoundsException.
        int limit = Math.min(parameters.getLimit() == null ? mapSize : parameters.getLimit(), mapSize);
        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();

        //Il parametro "MesArrayOfMessages" deve essere inizializzato SOLO se sono presenti messaggi in memoria.
        if (limit > 0) {
            MesArrayOfMessages mesArrayOfMessages = new MesArrayOfMessages();
            for (int i = 0; i < limit; i++) {
                //TODO Controlli sulle PEC salvate in memoria. Restituiamo preset di ACCETTAZIONE e CONSEGNA.
            }
            getMessagesResponse.setArrayOfMessages(mesArrayOfMessages);
        }

        log.info(SUCCESSFUL_OPERATION, GET_MESSAGES, getMessagesResponse);
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
        log.debug(INVOKING_OPERATION, GET_MESSAGE_ID, parameters);
        String messageID = parameters.getMailid();
        PecInfo pecInfo = pecMap.remove(messageID);
        log.debug("Removed pec with messageID '{}' associated to receiverAddress '{}'", messageID, pecInfo.getReceiverAddress());
        GetMessageIDResponse getMessageIDResponse = new GetMessageIDResponse();
        log.info(SUCCESSFUL_OPERATION, SEND_MAIL, getMessageIDResponse);
        return getMessageIDResponse;
    }
}
