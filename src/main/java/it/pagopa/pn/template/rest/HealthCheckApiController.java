package it.pagopa.pn.template.rest;

import it.pagopa.pn.template.rest.v1.api.HealthCheckApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class HealthCheckApiController implements HealthCheckApi {

    @Override
    public Mono<ResponseEntity<String>> status(ServerWebExchange exchange) {
        log.debug("Start status");
        return Mono.just(ResponseEntity.ok("OK"));
    }
}
