package it.pagopa.pn.pecmock.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum PecOperation {

    DM("DM"), //DELETE MAIL
    SM("SM"), //SEND MAIL
    GM("GM"), //GET MESSAGE
    ID("ID"), //GET MESSAGE BY ID
    GC("GC"); //GET MESSAGE COUNT

    private String value;

}
