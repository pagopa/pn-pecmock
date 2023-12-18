package it.pagopa.pn.pecmock.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum PecOperation {

    DM("DM"),
    SM("SM"),
    GM("GM"),
    ID("ID"),
    GC("GC");

    private String value;

}
