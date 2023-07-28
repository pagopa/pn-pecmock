package it.pagopa.pn.pecmock.configuration;


import it.pagopa.pn.pecmock.service.PecImapBridgeService;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;


@Configuration
public class PecImapBridgeConfiguration {

    @Autowired
    private Bus bus;

    @Bean
    public Endpoint endpoint(PecImapBridgeService pecImapBridgeService) {
        EndpointImpl endpoint = new EndpointImpl(bus, pecImapBridgeService);
        endpoint.publish("/pec");
        return endpoint;
    }

}
