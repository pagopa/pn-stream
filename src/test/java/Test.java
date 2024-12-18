import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.timeline.details.NormalizedAddressDetailsInt;
import it.pagopa.pn.stream.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.stream.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.stream.service.mapper.SmartMapper;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.TimelineElementDetailsV25;

import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args){

        NormalizedAddressDetailsInt normalizedAddressDetailsInt = new NormalizedAddressDetailsInt();
        normalizedAddressDetailsInt.setRecIndex(1);
        var address = new PhysicalAddressInt();
        address.setFullname("ivan");
        normalizedAddressDetailsInt.setNewAddress(address);
        var x =SmartMapper.mapToClass(normalizedAddressDetailsInt, TimelineElementDetailsV25.class);


        NotificationCancelledDetailsInt source = new NotificationCancelledDetailsInt();
        List<Integer> list = new ArrayList<>();
        list.add(1);
        source.setNotRefinedRecipientIndexes(list);
        source.setNotificationCost(100);

        TimelineElementDetailsV25 ret = SmartMapper.mapToClass(source, TimelineElementDetailsV25.class);

        System.out.println("ret = "+ret);

        source.getNotRefinedRecipientIndexes().clear();
        ret = SmartMapper.mapToClass(source, TimelineElementDetailsV25.class);


        System.out.println("ret = "+ret);
        NotHandledDetailsInt altro = new NotHandledDetailsInt();
        altro.setReason("test");
        ret = SmartMapper.mapToClass(altro, TimelineElementDetailsV25.class);

        System.out.println("ret = "+ret);

    }



}