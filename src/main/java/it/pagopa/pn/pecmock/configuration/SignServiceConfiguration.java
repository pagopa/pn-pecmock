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

import javax.xml.crypto.dsig.XMLSignatureFactory;

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

    @Bean(name = "sign")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema pecSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("SignService");
        wsdl11Definition.setLocationUri("/SignService");
        wsdl11Definition.setTargetNamespace("http://arubasignservice.arubapec.it/");
        wsdl11Definition.setSchema(pecSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema signServiceSchema() {
        return new SimpleXsdSchema(new ClassPathResource("sign/ArubaSignService.xsd"));
    }

}