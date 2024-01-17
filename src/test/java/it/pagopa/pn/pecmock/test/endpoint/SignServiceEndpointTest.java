package it.pagopa.pn.pecmock.test.endpoint;

import it.arubapec.arubasignservice.*;
import it.pagopa.pn.pecmock.endpoint.SignServiceEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SignServiceEndpointTest {

    @Autowired
    private SignServiceEndpoint signServiceEndpoint;
    private static SignRequestV2 signRequestV2;

    @BeforeAll
    static void initialize() {
        signRequestV2 = new SignRequestV2();
        signRequestV2.setBinaryinput(new byte[1024]);
        signRequestV2.setRequiredmark(true);
    }

    @Test
    void signPdfDocumentOk() {

        PdfsignatureV2 pdfsignatureV2 = new PdfsignatureV2();
        pdfsignatureV2.setSignRequestV2(signRequestV2);

        SignReturnV2 signReturnV2 = signServiceEndpoint.signPdfDocument(pdfsignatureV2).getValue().getReturn();
        Assertions.assertEquals(signRequestV2.getBinaryinput(), signReturnV2.getBinaryoutput());
    }

    @Test
    void signXmlDocumentOk() {

        Xmlsignature xmlsignature = new Xmlsignature();
        xmlsignature.setSignRequestV2(signRequestV2);

        SignReturnV2 signReturnV2 = signServiceEndpoint.signXmlDocument(xmlsignature).getValue().getReturn();
        Assertions.assertEquals(signRequestV2.getBinaryinput(), signReturnV2.getBinaryoutput());
    }

    @Test
    void pkcs7SignatureOk() {

        Pkcs7SignV2 pkcs7SignV2 = new Pkcs7SignV2();
        pkcs7SignV2.setSignRequestV2(signRequestV2);

        SignReturnV2 signReturnV2 = signServiceEndpoint.pkcs7Signature(pkcs7SignV2).getValue().getReturn();
        Assertions.assertEquals(signRequestV2.getBinaryinput(), signReturnV2.getBinaryoutput());
    }

}
