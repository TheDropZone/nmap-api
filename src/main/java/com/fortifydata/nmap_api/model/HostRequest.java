package com.fortifydata.nmap_api.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class HostRequest {
    private Long companyId;
    private Long scanId;
    private String host;
    private NmapScanType scanType;
}
