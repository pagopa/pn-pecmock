package it.pagopa.pn.pecmock.model.pojo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.OutputStream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class EmailAttachment {

    String nameWithExtension;
    OutputStream content;
    String url;

}
