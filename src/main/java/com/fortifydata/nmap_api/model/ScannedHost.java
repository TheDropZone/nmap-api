package com.fortifydata.nmap_api.model;

import lombok.*;
import org.nmap4j.data.nmaprun.Host;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ScannedHost {
    private Long companyId;
    private Long scanId;
    private Host host;
}
