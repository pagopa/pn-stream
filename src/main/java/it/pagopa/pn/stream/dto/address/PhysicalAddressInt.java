package it.pagopa.pn.stream.dto.address;

import lombok.*;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Setter
@Getter
@ToString
public class PhysicalAddressInt {
    private String fullname;
    private String at;
    private String address;
    private String addressDetails;
    private String zip;
    private String municipality;
    private String municipalityDetails;
    private String province;
    private String foreignState;

}
