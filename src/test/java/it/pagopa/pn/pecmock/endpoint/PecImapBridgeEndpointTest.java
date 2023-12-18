package it.pagopa.pn.pecmock.endpoint;

import https.bridgews_pec_it.pecimapbridge.*;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext( classMode= DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
@Slf4j
public class PecImapBridgeEndpointTest {

    @Autowired
    private PecImapBridgeEndpoint pecImapBridgeEndpoint;

    @Test
    void sendMailOk()
    {
        //GIVEN
        SendMail sendMail=new SendMail();
        sendMail.setData(getData("subject"));

        //WHEN
        SendMailResponse sendMailResponse = pecImapBridgeEndpoint.sendMail(sendMail);

        //THEN
        Assertions.assertNotNull(sendMailResponse);
        Assertions.assertNotNull(sendMailResponse.getErrstr());
    }

    @Test
    void getOneMessageOk() {
        SendMail sendMail = new SendMail();
        sendMail.setData(getData("subject"));
        pecImapBridgeEndpoint.sendMail(sendMail);

        //GIVEN
        GetMessages getMessages = new GetMessages();
        getMessages.setLimit(1);
        getMessages.setOuttype(2);
        getMessages.setUnseen(1);
        getMessages.setMsgtype("ALL");
        getMessages.setMarkseen(0);


        //WHEN
        GetMessagesResponse getMessagesResponse = pecImapBridgeEndpoint.getMessages(getMessages);

        //THEN
        Assertions.assertNotNull(getMessagesResponse);
        Assertions.assertNotNull(getMessagesResponse.getArrayOfMessages());
        Assertions.assertEquals(1, getMessagesResponse.getArrayOfMessages().getItem().size());
    }

    @Test
    void getMoreMessagesOk() {
        SendMail sendMail = new SendMail();
        sendMail.setData(getData("subject"));
        pecImapBridgeEndpoint.sendMail(sendMail);

        //GIVEN
        GetMessages getMessages = new GetMessages();
        getMessages.setLimit(10);
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

    @Test
    void getMessagesEmptyFolderOk() {

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
        Assertions.assertNull(getMessagesResponse.getArrayOfMessages());
    }

    @Test
    void testMessageCount() {
        for (int i = 0; i<=1; i++){
            SendMail sendMail = new SendMail();
            sendMail.setData(getData("subject"));
            pecImapBridgeEndpoint.sendMail(sendMail);
        }

        //WHEN
        GetMessageCountResponse getMessageCountResponse = pecImapBridgeEndpoint.getMessageCount(new GetMessageCount());

        //THEN
        Assertions.assertNotNull(getMessageCountResponse);
        Assertions.assertNotNull(getMessageCountResponse.getCount());
        Assertions.assertEquals(4, getMessageCountResponse.getCount());


    }

    @Test
    void testDeleteMessageOk(){
        SendMail sendMail = new SendMail();
        sendMail.setData(getData("subject"));
        pecImapBridgeEndpoint.sendMail(sendMail);

        String messageId = pecImapBridgeEndpoint.getPecMapProcessedElements().keySet().iterator().next();
        Assertions.assertEquals(2, pecImapBridgeEndpoint.getPecMapProcessedElements().size());

        pecImapBridgeEndpoint.deleteMail(new DeleteMail(){{
            setMailid(messageId);
            setIsuid(2);
        }});

        Assertions.assertEquals(1, pecImapBridgeEndpoint.getPecMapProcessedElements().size());
    }

    @Test
    void testDeleteMessageKo(){
      DeleteMailResponse response =  pecImapBridgeEndpoint.deleteMail(new DeleteMail(){{
            setMailid("messageId");
            setIsuid(2);
        }});

       Assertions.assertEquals(response.getErrcode(), 99);
    }

    private String getData(String subject) {
        String s = """
                <![CDATA[
                Message-ID: messageID
                Date: Sun, 3 Mar 2019 10:56:05 +0100
                From: standard1@postacert.pre.demoaruba.com
                MIME-Version: 1.0
                To:standard3@postacert.pre.demoaruba.com
                Cc:standard2@postacert.pre.demoaruba.com
                Subject: %s
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

        return String.format(s, subject);
    }



}
