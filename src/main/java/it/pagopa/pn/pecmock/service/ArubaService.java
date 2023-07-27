package it.pagopa.pn.pecmock.service;

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

import static it.pagopa.pn.pecmock.utils.LogUtils.*;


@Service
@Slf4j
public class ArubaService implements PecImapBridge {

    private final Map<String, String> pecMap = new HashMap<>();

    @Override
    public Response<SendMailResponse> sendMailAsync(SendMail parameters) {
        ServerAsyncResponse<SendMailResponse> serverAsyncResponse = new ServerAsyncResponse<>();
        serverAsyncResponse.set(sendMail(parameters));
        return serverAsyncResponse;
    }

    @Override
    public Future<?> sendMailAsync(SendMail parameters, AsyncHandler<SendMailResponse> asyncHandler) {
        return Mono.fromCallable(() -> sendMailAsync(parameters))
                .doOnNext(asyncHandler::handleResponse)
                .toFuture();
    }

    @Override
    public SendMailResponse sendMail(SendMail parameters) {
        log.debug(INVOKING_OPERATION, SEND_MAIL, parameters);
        byte[] data = parameters.getData().trim().getBytes(StandardCharsets.UTF_8);
        MimeMessage mimeMessage = PecUtils.getMimeMessage(data);
        try {
            String messageID = mimeMessage.getMessageID();
            String receiverAddress = mimeMessage.getAllRecipients()[0].toString();
            pecMap.put(messageID, receiverAddress);
            log.info("Pec with messageID '{}' saved in temporary memory", messageID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SendMailResponse sendMailResponse = new SendMailResponse();
        log.info(SUCCESSFUL_OPERATION_ON, SEND_MAIL, sendMailResponse);
        return sendMailResponse;
    }

    @Override
    public Response<GetMessagesResponse> getMessagesAsync(GetMessages parameters) {
        ServerAsyncResponse<GetMessagesResponse> serverAsyncResponse = new ServerAsyncResponse<>();
        serverAsyncResponse.set(getMessages(parameters));
        return serverAsyncResponse;
    }

    @Override
    public Future<?> getMessagesAsync(GetMessages parameters, AsyncHandler<GetMessagesResponse> asyncHandler) {
        log.debug(INVOKING_OPERATION, GET_MESSAGES_ASYNC, parameters);
        return Mono.fromCallable(() -> getMessagesAsync(parameters))
                .doOnError(throwable -> log.error(throwable.getMessage()))
                .doOnNext(asyncHandler::handleResponse)
                .doOnSuccess(result->log.info(SUCCESSFUL_OPERATION_ON, GET_MESSAGES_ASYNC, result))
                .toFuture();
    }

    @Override
    public GetMessagesResponse getMessages(GetMessages parameters) {
        log.debug(INVOKING_OPERATION, GET_MESSAGES, parameters);
        int mapSize = pecMap.size();
        int limit = Math.min(parameters.getLimit() == null ? mapSize : parameters.getLimit(), mapSize);
        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();
        MesArrayOfMessages mesArrayOfMessages = new MesArrayOfMessages();
        for (int i = 0; i < limit; i++) {
            //Controlli sulle PEC salvate in memoria
            //Restituiamo un preset di ACCETTAZIONE o CONSEGNA a seconda del destinatario
            mesArrayOfMessages.getItem().add(new byte[10]);
        }
        getMessagesResponse.setArrayOfMessages(mesArrayOfMessages);
        log.info(SUCCESSFUL_OPERATION_ON, GET_MESSAGES, getMessagesResponse);
        return getMessagesResponse;
    }

    @Override
    public Response<GetMessageIDResponse> getMessageIDAsync(GetMessageID parameters) {
        ServerAsyncResponse<GetMessageIDResponse> serverAsyncResponse = new ServerAsyncResponse<>();
        serverAsyncResponse.set(getMessageID(parameters));
        return serverAsyncResponse;
    }

    @Override
    public Future<?> getMessageIDAsync(GetMessageID parameters, AsyncHandler<GetMessageIDResponse> asyncHandler) {
        return Mono.fromCallable(() -> getMessageIDAsync(parameters))
                .doOnNext(asyncHandler::handleResponse)
                .toFuture();
    }

    @Override
    public GetMessageIDResponse getMessageID(GetMessageID parameters) {
        log.debug(INVOKING_OPERATION, GET_MESSAGE_ID, parameters);
        String messageID = parameters.getMailid();
        String receiverAddress = pecMap.remove(messageID);
        log.debug("Removed messageID '{}' associated to receiverAddress '{}'", messageID, receiverAddress);
        GetMessageIDResponse getMessageIDResponse = new GetMessageIDResponse();
        log.debug(SUCCESSFUL_OPERATION_ON, SEND_MAIL, getMessageIDResponse);
        return getMessageIDResponse;
    }
}
