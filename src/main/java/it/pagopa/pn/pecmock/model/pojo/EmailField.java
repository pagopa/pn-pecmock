package it.pagopa.pn.pecmock.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EmailField {

    String msgId;
    String from;
    String to;
    String subject;
    String text;
    String contentType;
    List<EmailAttachment> emailAttachments;
}
