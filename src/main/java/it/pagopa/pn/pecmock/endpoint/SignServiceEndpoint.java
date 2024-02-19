package it.pagopa.pn.pecmock.endpoint;

import it.arubapec.arubasignservice.*;
import it.pagopa.pn.pecmock.configuration.SignServiceConfigurationProperties;
import it.pagopa.pn.pecmock.exception.DataReadException;
import it.pagopa.pn.pecmock.exception.SemaphoreException;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import reactor.core.publisher.Mono;
import javax.xml.namespace.QName;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static it.pagopa.pn.pecmock.utils.LogUtils.*;
import static it.pagopa.pn.pecmock.utils.PecUtils.getRandomNumberBetweenMinMax;

@Slf4j
@Endpoint
public class SignServiceEndpoint {
    private static final String NAMESPACE_URI = "http://arubasignservice.arubapec.it/";
    private Semaphore semaphore = null;
    private final int signDuration;
    private final int hashingDuration;
    private final int timestampingDuration;
    private final int minDelay;
    private final int maxDelay;
    private final QName XML_SIGNATURE_RESPONSE_QNAME = new QName(NAMESPACE_URI, "xmlsignatureResponse");
    private final QName PDF_SIGNATURE_RESPONSE_V2_QNAME = new QName(NAMESPACE_URI, "pdfsignatureV2Response");
    private final QName PKCS7_SIGN_RESPONSE_V2_QNAME = new QName(NAMESPACE_URI, "pkcs7signV2Response");

    @Autowired
    public SignServiceEndpoint(SignServiceConfigurationProperties signServiceConfigurationProperties) {
        int mockPecSemaphore = signServiceConfigurationProperties.semaphores();
        log.debug("SignServiceEndpoint() - {}", mockPecSemaphore);
        this.minDelay = signServiceConfigurationProperties.minDelay();
        this.maxDelay = signServiceConfigurationProperties.maxDelay();
        this.hashingDuration = signServiceConfigurationProperties.hashingDuration();
        this.signDuration = signServiceConfigurationProperties.signDuration();
        this.timestampingDuration = signServiceConfigurationProperties.timestampingDuration();
        this.semaphore = new Semaphore(mockPecSemaphore);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "xmlsignature")
    @ResponsePayload
    public JAXBElement<XmlsignatureResponse> signXmlDocument(@RequestPayload JAXBElement<Xmlsignature> xmlsignature) {

        log.debug(INVOKING_OPERATION, SIGN_XML_DOCUMENT, xmlsignature.getValue());

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        return Mono.just(xmlsignature.getValue().getSignRequestV2())
                .map(signRequestV2 -> {
                    try {
                        byte[] fileBytes = signRequestV2.getStream().getInputStream().readAllBytes();
                        signRequestV2.setBinaryinput(fileBytes);
                        return signRequestV2;
                    } catch (IOException e) {
                        throw new DataReadException(e.getMessage());
                    }
                })
                .flatMap(signRequestV2 -> Mono.defer(() -> signDocument(signRequestV2.getBinaryinput(), signRequestV2.isRequiredmark())))
                .map(signReturnV2 -> {
                    var response = new XmlsignatureResponse();
                    response.setReturn(signReturnV2);
                    return response;
                })
                .map(xmlsignatureResponse -> new JAXBElement<>(XML_SIGNATURE_RESPONSE_QNAME, XmlsignatureResponse.class, xmlsignatureResponse))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, SIGN_XML_DOCUMENT, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, SIGN_XML_DOCUMENT, result))
                .block();
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pdfsignatureV2")
    @ResponsePayload
    public JAXBElement<PdfsignatureV2Response> signPdfDocument(@RequestPayload JAXBElement<PdfsignatureV2> pdfsignatureV2) {

        log.debug(INVOKING_OPERATION, SIGN_PDF_DOCUMENT, pdfsignatureV2.getValue());

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        return Mono.just(pdfsignatureV2.getValue().getSignRequestV2())
                .flatMap(signRequestV2 -> Mono.defer(() -> signDocument(signRequestV2.getBinaryinput(), signRequestV2.isRequiredmark())))
                .map(signReturnV2 -> {
                    var response = new PdfsignatureV2Response();
                    response.setReturn(signReturnV2);
                    return response;
                })
                .map(pdfsignatureV2Response -> new JAXBElement<>(PDF_SIGNATURE_RESPONSE_V2_QNAME, PdfsignatureV2Response.class, pdfsignatureV2Response))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, SIGN_PDF_DOCUMENT, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, SIGN_PDF_DOCUMENT, result))
                .block();

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pkcs7signV2")
    @ResponsePayload
    public JAXBElement<Pkcs7SignV2Response> pkcs7Signature(@RequestPayload JAXBElement<Pkcs7SignV2> pkcs7SignV2) {

        log.debug(INVOKING_OPERATION, PKCS7_SIGNATURE, pkcs7SignV2.getValue());

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        return Mono.just(pkcs7SignV2.getValue().getSignRequestV2())
                .flatMap(signRequestV2 -> signDocument(signRequestV2.getBinaryinput(), signRequestV2.isRequiredmark()))
                .map(signReturnV2 -> {
                    var response = new Pkcs7SignV2Response();
                    response.setReturn(signReturnV2);
                    return response;
                })
                .map(pkcs7SignV2Response -> new JAXBElement<>(PKCS7_SIGN_RESPONSE_V2_QNAME, Pkcs7SignV2Response.class, pkcs7SignV2Response))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, PKCS7_SIGNATURE, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, PKCS7_SIGNATURE, result))
                .block();

    }

    private Mono<SignReturnV2> signDocument(byte[] binaryInput, boolean isRequiredmark) {
        return Mono.fromSupplier(() -> {
                    SignReturnV2 signReturnV2 = new SignReturnV2();
                    signReturnV2.setStatus("OK");
                    signReturnV2.setBinaryoutput(binaryInput);
                    return signReturnV2;
                })
                .onErrorResume(throwable -> {
                    log.debug(GENERIC_ERROR, SIGN_DOCUMENT, throwable, throwable.getMessage());
                    SignReturnV2 signReturnV2 = new SignReturnV2();
                    signReturnV2.setStatus("KO");
                    signReturnV2.setReturnCode("0001");
                    signReturnV2.setDescription("Generic error.");
                    return Mono.just(signReturnV2);
                })
                .transform(delayElement(binaryInput.length, isRequiredmark))
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION, SIGN_DOCUMENT, result))
                .doOnError(throwable -> log.error(ENDING_PROCESS_WITH_ERROR, SIGN_DOCUMENT, throwable, throwable.getMessage()))
                .doFinally(result -> semaphore.release());
    }

    private <T> Function<Mono<T>, Mono<T>> delayElement(int fileSize, boolean isRequiredmark) {
        return tMono -> tMono.flatMap(response -> {
            try {
                Thread.sleep(generateDuration(fileSize, isRequiredmark));
            } catch (InterruptedException e) {
                return Mono.error(e);
            }
            return Mono.just(response);
        });
    }

    private long generateDuration(int fileSize, boolean isRequiredmark) {
        var duration = signDuration + (isRequiredmark ? timestampingDuration : 0) + ((long) hashingDuration * fileSize / (1024 * 1024)) + getRandomNumberBetweenMinMax(minDelay, maxDelay);
        log.debug("Duration : {}", duration);
        return duration;
    }


}
