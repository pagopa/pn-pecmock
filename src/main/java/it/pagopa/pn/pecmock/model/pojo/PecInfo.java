package it.pagopa.pn.pecmock.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PecInfo {

    String messageId;
    String receiverAddress;
    String subject;
    String from;
    String replyTo;
    PecType pecType;
    Map<String, String> errorMap;
    boolean read;

    public PecInfo messageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public PecInfo receiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
        return this;
    }

    public PecInfo subject(String subject) {
        this.subject = subject;
        return this;
    }

    public PecInfo from(String from) {
        this.from = from;
        return this;
    }

    public PecInfo replyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public PecInfo pecType(PecType pecType) {
        this.pecType = pecType;
        return this;
    }

    public PecInfo read(boolean read) {
        this.read = read;
        return this;
    }

    public PecInfo errorCodes(Map<String, String> errorMap) {
        this.errorMap = errorMap;
        return this;
    }

}
