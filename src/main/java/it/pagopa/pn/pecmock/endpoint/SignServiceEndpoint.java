package it.pagopa.pn.pecmock.endpoint;

import it.arubapec.arubasignservice.*;
import it.pagopa.pn.pecmock.configuration.SignServiceConfigurationProperties;
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
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static it.pagopa.pn.pecmock.utils.LogUtils.*;
import static it.pagopa.pn.pecmock.utils.PecUtils.getRandomNumberBetweenMinMax;

@Slf4j
@Endpoint
public class SignServiceEndpoint {
    private static final String NAMESPACE_URI = "http://arubasignservice.arubapec.it/";
    private Semaphore semaphore = null;
    private int mockPecSemaphore;
    private final int signDuration;
    private final int hashingDuration;
    private final int timestampingDuration;
    private final int minDelay;
    private final int maxDelay;
    private final String XML_SIGNATURE_QNAME = "xmlsignature";
    private final String PDF_SIGNATURE_V2_QNAME = "pdfsignatureV2";
    private final String PKCS7_SIGN_V2_QNAME = "pkcs7signV2";

    @Autowired
    public SignServiceEndpoint(SignServiceConfigurationProperties signServiceConfigurationProperties) {
        this.mockPecSemaphore = signServiceConfigurationProperties.semaphores();
        log.debug("SignServiceEndpoint() - {}", mockPecSemaphore);
        this.minDelay = signServiceConfigurationProperties.minDelay();
        this.maxDelay = signServiceConfigurationProperties.maxDelay();
        this.hashingDuration = signServiceConfigurationProperties.hashingDuration();
        this.signDuration = signServiceConfigurationProperties.signDuration();
        this.timestampingDuration = signServiceConfigurationProperties.timestampingDuration();
        this.semaphore = new Semaphore(mockPecSemaphore);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = XML_SIGNATURE_QNAME)
    @ResponsePayload
    public JAXBElement<XmlsignatureResponse> signXmlDocument(@RequestPayload Xmlsignature xmlsignature) {

        log.debug(INVOKING_OPERATION, SIGN_XML_DOCUMENT, xmlsignature);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        var signRequestV2 = xmlsignature.getSignRequestV2();

        return Mono.just(signRequestV2)
                .transform(signDocument(signRequestV2.getBinaryinput().length, signRequestV2.isRequiredmark()))
                .map(signReturnV2 -> {
                    var response = new XmlsignatureResponse();
                    response.setReturn(signReturnV2);
                    return response;
                })
                .map(xmlsignatureResponse -> new JAXBElement<>(QName.valueOf(XML_SIGNATURE_QNAME), XmlsignatureResponse.class, xmlsignatureResponse))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, SIGN_XML_DOCUMENT, result))
                .block();

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = PDF_SIGNATURE_V2_QNAME)
    @ResponsePayload
    public JAXBElement<PdfsignatureV2Response> signPdfDocument(@RequestPayload PdfsignatureV2 pdfsignatureV2) {

        log.debug(INVOKING_OPERATION, SIGN_PDF_DOCUMENT, pdfsignatureV2);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        var signRequestV2 = pdfsignatureV2.getSignRequestV2();

        return Mono.just(signRequestV2)
                .transform(signDocument(signRequestV2.getBinaryinput().length, signRequestV2.isRequiredmark()))
                .map(signReturnV2 -> {
                    var response = new PdfsignatureV2Response();
                    response.setReturn(signReturnV2);
                    return response;
                })
                .map(pdfsignatureV2Response -> new JAXBElement<>(QName.valueOf(PDF_SIGNATURE_V2_QNAME), PdfsignatureV2Response.class, pdfsignatureV2Response))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, SIGN_PDF_DOCUMENT, result))
                .block();

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = PKCS7_SIGN_V2_QNAME)
    @ResponsePayload
    public JAXBElement<Pkcs7SignV2Response> pkcs7Signature(@RequestPayload Pkcs7SignV2 pkcs7SignV2) {

        log.debug(INVOKING_OPERATION, PKCS7_SIGNATURE, pkcs7SignV2);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        var signRequestV2 = pkcs7SignV2.getSignRequestV2();

        return Mono.just(signRequestV2)
                .transform(signDocument(signRequestV2.getBinaryinput().length, signRequestV2.isRequiredmark()))
                .map(signReturnV2 -> {
                    var response = new Pkcs7SignV2Response();
                    response.setReturn(signReturnV2);
                    return response;
                })
                .map(pkcs7SignV2Response -> new JAXBElement<>(QName.valueOf(PKCS7_SIGN_V2_QNAME), Pkcs7SignV2Response.class, pkcs7SignV2Response))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION, PKCS7_SIGNATURE, result))
                .block();

    }

    Function<Mono<SignRequestV2>, Mono<SignReturnV2>> signDocument(int fileSize, boolean isRequiredmark) {
        log.debug(INVOKING_OPERATION + ARG, SIGN_DOCUMENT, fileSize, isRequiredmark);
        return signRequest -> signRequest.map(signRequestV2 -> {
                    SignReturnV2 signReturnV2 = new SignReturnV2();
                    signReturnV2.setStatus("OK");
                    signReturnV2.setBinaryoutput(signRequestV2.getBinaryinput());
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
                .transform(delayElement(fileSize, isRequiredmark))
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
