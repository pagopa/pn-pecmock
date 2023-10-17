package it.pagopa.pn.pecmock.configuration;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;


@EnableWs
@Configuration
public class PecImapBridgeConfiguration extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/PecImapBridge/*");
    }

    @Bean(name = "pec")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema pecSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("PecImapBridge");
        wsdl11Definition.setLocationUri("/PecImapBridge");
        wsdl11Definition.setTargetNamespace("https://bridgews.pec.it/PecImapBridge/");
        wsdl11Definition.setSchema(pecSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema pecSchema() {
        return new SimpleXsdSchema(new ClassPathResource("pec/PecImapBridgeBWS1.5.xsd"));
    }

}
