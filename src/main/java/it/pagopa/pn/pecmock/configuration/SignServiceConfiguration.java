package it.pagopa.pn.pecmock.configuration;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.endpoint.adapter.DefaultMethodEndpointAdapter;
import org.springframework.ws.server.endpoint.adapter.method.MarshallingPayloadMethodProcessor;
import org.springframework.ws.soap.server.SoapMessageDispatcher;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;

@EnableWs
@Configuration
public class SignServiceConfiguration extends WsConfigurerAdapter {

    @Bean(name = "signServiceServlet")
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/SignService/*");
    }

    @Bean
    public SoapMessageDispatcher messageReceiver(DefaultMethodEndpointAdapter customMethodEndpointAdapter) {
        SoapMessageDispatcher messageDispatcher = new SoapMessageDispatcher();
        messageDispatcher.setEndpointAdapters(List.of(customMethodEndpointAdapter));
        return messageDispatcher;
    }

    @Bean(name = "sign")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema signServiceSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("SignService");
        wsdl11Definition.setLocationUri("/SignService");
        wsdl11Definition.setTargetNamespace("http://arubasignservice.arubapec.it/");
        wsdl11Definition.setSchema(signServiceSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema signServiceSchema() {
        return new SimpleXsdSchema(new ClassPathResource("sign/ArubaSignService.xsd"));
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setMtomEnabled(true);
        marshaller.setContextPath("it.arubapec.arubasignservice");
        return marshaller;
    }

    @Bean
    public MarshallingPayloadMethodProcessor marshallingPayloadMethodProcessor(Jaxb2Marshaller marshaller) {
        return new MarshallingPayloadMethodProcessor(marshaller);
    }

    @Bean
    public DefaultMethodEndpointAdapter customMethodEndpointAdapter(MarshallingPayloadMethodProcessor marshallingPayloadMethodProcessor) {
        DefaultMethodEndpointAdapter customMethodEndpointAdapter = new DefaultMethodEndpointAdapter();
        customMethodEndpointAdapter.setMethodArgumentResolvers(List.of(marshallingPayloadMethodProcessor));
        customMethodEndpointAdapter.setMethodReturnValueHandlers(List.of(marshallingPayloadMethodProcessor));
        return customMethodEndpointAdapter;
    }



}