package it.pagopa.pn.pecmock.utils;

import it.pagopa.pn.pecmock.exception.RequestedErrorException;
import it.pagopa.pn.pecmock.model.pojo.PecInfo;
import it.pagopa.pn.pecmock.model.pojo.PecOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.pagopa.pn.pecmock.model.pojo.PecOperation.DM;
import static it.pagopa.pn.pecmock.model.pojo.PecOperation.SM;

public class PecErrorUtils {

    public static Map<String, String> parsedErrorCodes(String subject) {
        Matcher m = Pattern.compile("^ERR\\[(.*?)\\]$").matcher(subject);
        if (m.matches())
            return Arrays.stream(m.group(1).split(","))
                    .map(String::trim)
                    .map(s -> s.split("_"))
                    .collect(Collectors.toMap(array -> array[0], array -> array[1]));
        else return new HashMap<>();
    }

    public static Mono<PecInfo> errorLookUp(PecOperation op, PecInfo pecInfo) {
        var errorMap = pecInfo.getErrorMap();
        if (!errorMap.isEmpty() && errorMap.containsKey(op.getValue()))
            return Mono.error(new RequestedErrorException(Integer.parseInt(errorMap.get(op.getValue())), "Errore generato dalla richiesta."));
        else return Mono.just(pecInfo);
    }

}
