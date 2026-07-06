package com.xcz.commons.core.utils.ip;

import lombok.Data;

/**
 * IpLocationInfo
 */
@Data
public class IpLocationInfo {
    /***国家**/
    private String country;
    /***区域**/
    private String region;
    /***省份**/
    private String province;
    /***城市**/
    private String city;
    /***运营商**/
    private String isp;

    public String getLocationInfo() {
        return String.format("%s-%s-%s-%s", country, province, city, isp);
    }
}
