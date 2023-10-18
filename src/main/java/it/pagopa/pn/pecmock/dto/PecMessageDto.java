package it.pagopa.pn.pecmock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PecMessageDto {

    String messageID;
    String receiverAddress;

}
