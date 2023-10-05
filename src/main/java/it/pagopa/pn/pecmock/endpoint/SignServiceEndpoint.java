package it.pagopa.pn.pecmock.endpoint;

import it.arubapec.arubasignservice.*;
import it.pagopa.pn.pecmock.configuration.PecMockConfiguration;
import it.pagopa.pn.pecmock.configuration.SignServiceProperties;
import it.pagopa.pn.pecmock.exception.SemaphoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static it.pagopa.pn.pecmock.utils.PecUtils.getRandomNumberBetweenMinMax;

@Endpoint
@Slf4j
@AutoConfiguration
public class SignServiceEndpoint {
    private static final String NAMESPACE_URI = "http://arubasignservice.arubapec.it/";
    private Semaphore semaphore = null;
    private int mockPecSemaphore;
    private int minDelay;
    private int maxDelay;

    private long signDuration;
    private long timestampingDuration;
    private long hashingBaseDuration;

    @Autowired
    public SignServiceEndpoint(SignServiceProperties signServiceProperties) {
        log.debug("SignServiceEndpoint() - {}", mockPecSemaphore);
        mockPecSemaphore = signServiceProperties.semaphores();
        minDelay = signServiceProperties.minDelay();
        maxDelay = signServiceProperties.maxDelay();
        semaphore = new Semaphore(mockPecSemaphore);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "xmlsignature")
    @ResponsePayload
    public SignReturnV2 signXmlDocument(Xmlsignature xmlsignature) {

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        var signRequestV2 = xmlsignature.getSignRequestV2();

        return Mono.just(signRequestV2)
                .transform(signDocument(signRequestV2.getBinaryinput().length, signRequestV2.isRequiredmark()))
                .block();

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pdfsignatureV2")
    @ResponsePayload
    public SignReturnV2 signPdfDocument(PdfsignatureV2 pdfsignatureV2) {

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        var signRequestV2 = pdfsignatureV2.getSignRequestV2();

        return Mono.just(signRequestV2)
                .transform(signDocument(signRequestV2.getBinaryinput().length, signRequestV2.isRequiredmark()))
                .block();

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pkcs7signV2")
    @ResponsePayload
    public SignReturnV2 pcks7Signature(Pkcs7SignV2 pkcs7SignV2) {

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted: {} - {}", e, e.getMessage());
            throw new SemaphoreException();
        }

        var signRequestV2 = pkcs7SignV2.getSignRequestV2();

        return Mono.just(signRequestV2)
                .transform(signDocument(signRequestV2.getBinaryinput().length, signRequestV2.isRequiredmark()))
                .block();

    }

    Function<Mono<SignRequestV2>, Mono<SignReturnV2>> signDocument(int fileSize, boolean isRequiredmark) {

        return signRequest -> signRequest.map(signRequestV2 -> {
                    SignReturnV2 signReturnV2 = new SignReturnV2();
                    signReturnV2.setStatus("OK");
                    signReturnV2.setBinaryoutput(signRequestV2.getBinaryinput());
                    return signReturnV2;
                })
                .onErrorResume(throwable -> {
                    SignReturnV2 signReturnV2 = new SignReturnV2();
                    signReturnV2.setStatus("KO");
                    signReturnV2.setReturnCode("0001");
                    signReturnV2.setDescription("Generic error.");
                    return Mono.just(signReturnV2);
                })
                .delayElement(Duration.ofMillis(generateDuration(fileSize, isRequiredmark)))
                .doFinally(result -> semaphore.release());

    }

    private long generateDuration(int fileSize, boolean isRequiredmark) {
        return signDuration + (isRequiredmark ? timestampingDuration : 0) + (hashingBaseDuration * fileSize) + getRandomNumberBetweenMinMax(minDelay, maxDelay);
    }


}
