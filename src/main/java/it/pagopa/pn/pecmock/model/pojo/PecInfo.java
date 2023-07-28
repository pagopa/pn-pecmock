package it.pagopa.pn.pecmock.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PecInfo {

    String receiverAddress;

    public PecInfo receiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
        return this;
    }

}
