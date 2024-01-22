package it.pagopa.pn.pecmock.test.endpoint;

import it.arubapec.arubasignservice.*;
import it.pagopa.pn.pecmock.endpoint.SignServiceEndpoint;
import it.pagopa.pn.pecmock.test.endpoint.utils.ByteArrayDataSource;
import jakarta.activation.DataHandler;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.xml.namespace.QName;

@SpringBootTest
public class SignServiceEndpointTest {

    @Autowired
    private SignServiceEndpoint signServiceEndpoint;
    private static SignRequestV2 signRequestV2;
    private static final String NAMESPACE_URI = "http://arubasignservice.arubapec.it/";
    private final QName XML_SIGNATURE_QNAME = new QName(NAMESPACE_URI, "xmlsignature");
    private final QName PDF_SIGNATURE_V2_QNAME = new QName(NAMESPACE_URI, "pdfsignatureV2");
    private final QName PKCS7_SIGN_V2_QNAME = new QName(NAMESPACE_URI, "pkcs7signV2");

    @BeforeAll
    static void initialize() {
        byte[] fileBytes = new byte[1024];
        signRequestV2 = new SignRequestV2();
        ByteArrayDataSource dataSource = new ByteArrayDataSource(fileBytes);
        signRequestV2.setStream(new DataHandler(dataSource));
        signRequestV2.setBinaryinput(fileBytes);
        signRequestV2.setRequiredmark(true);
    }

    @Test
    void signPdfDocumentOk() {

        PdfsignatureV2 pdfsignatureV2 = new PdfsignatureV2();
        pdfsignatureV2.setSignRequestV2(signRequestV2);

        SignReturnV2 signReturnV2 = signServiceEndpoint.signPdfDocument(new JAXBElement<>(PDF_SIGNATURE_V2_QNAME, PdfsignatureV2.class, pdfsignatureV2)).getValue().getReturn();
        Assertions.assertEquals(signRequestV2.getBinaryinput(), signReturnV2.getBinaryoutput());
    }

    @Test
    void signXmlDocumentOk() {

        Xmlsignature xmlsignature = new Xmlsignature();
        xmlsignature.setSignRequestV2(signRequestV2);

        SignReturnV2 signReturnV2 = signServiceEndpoint.signXmlDocument(new JAXBElement<>(XML_SIGNATURE_QNAME, Xmlsignature.class, xmlsignature)).getValue().getReturn();
        Assertions.assertEquals(signRequestV2.getBinaryinput(), signReturnV2.getBinaryoutput());
    }

    @Test
    void pkcs7SignatureOk() {

        Pkcs7SignV2 pkcs7SignV2 = new Pkcs7SignV2();
        pkcs7SignV2.setSignRequestV2(signRequestV2);

        SignReturnV2 signReturnV2 = signServiceEndpoint.pkcs7Signature(new JAXBElement<>(PKCS7_SIGN_V2_QNAME, Pkcs7SignV2.class, pkcs7SignV2)).getValue().getReturn();
        Assertions.assertEquals(signRequestV2.getBinaryinput(), signReturnV2.getBinaryoutput());
    }

}
