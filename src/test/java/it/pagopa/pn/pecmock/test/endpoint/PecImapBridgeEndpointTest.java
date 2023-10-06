package it.pagopa.pn.pecmock.test.endpoint;

import https.bridgews_pec_it.pecimapbridge.*;
import it.pagopa.pn.pecmock.endpoint.PecImapBridgeEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PecImapBridgeEndpointTest {

    @Autowired
    private PecImapBridgeEndpoint pecImapBridgeEndpoint;

    @Test
    void sendMailOk()
    {
        //GIVEN
        SendMail sendMail=new SendMail();
        sendMail.setData(getData());

        //WHEN
        SendMailResponse sendMailResponse = pecImapBridgeEndpoint.sendMail(sendMail);

        //THEN
        Assertions.assertNotNull(sendMailResponse);
        Assertions.assertNotNull(sendMailResponse.getErrstr());
    }

    @Test
    void getMessagesOk() {
        SendMail sendMail = new SendMail();
        sendMail.setData(getData());
        pecImapBridgeEndpoint.sendMail(sendMail);

        //GIVEN
        GetMessages getMessages = new GetMessages();
        getMessages.setLimit(2);
        getMessages.setOuttype(2);
        getMessages.setUnseen(1);
        getMessages.setMsgtype("ALL");
        getMessages.setMarkseen(0);


        //WHEN
        GetMessagesResponse getMessagesResponse = pecImapBridgeEndpoint.getMessages(getMessages);

        //THEN
        Assertions.assertNotNull(getMessagesResponse);
        Assertions.assertNotNull(getMessagesResponse.getArrayOfMessages());
        Assertions.assertEquals(2, getMessagesResponse.getArrayOfMessages().getItem().size());
    }

    private String getData() {
        return """
                <![CDATA[
                Message-ID: messageID
                Date: Sun, 3 Mar 2019 10:56:05 +0100
                From: standard1@postacert.pre.demoaruba.com
                MIME-Version: 1.0
                To:standard3@postacert.pre.demoaruba.com
                Cc:standard2@postacert.pre.demoaruba.com
                Subject: Messaggio di test
                Content-Type: multipart/alternative;
                boundary="------------060001050801050801010200"
                This is a multi-part message in MIME format.
                --------------060001050801050801010200
                Content-Type: text/plain; charset=iso-8859-15; format=flowed
                Content-Transfer-Encoding: 7bit
                PAG. 27 DI 29
                Test
                --------------060001050801050801010200
                Content-Type: text/html; charset=iso-8859-15
                Content-Transfer-Encoding: 7bit
                <html><head><meta http-equiv="content-type" content="text/html;
                 charset=ISO-8859-15">
                 </head><body bgcolor="#FFFFFF" text="#000000"><p>Test</p></body></html>
                --------------060001050801050801010200--
                 ]]>""";
    }

}
