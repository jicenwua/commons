package com.xcz.commons.core.utils.ip;

import jakarta.servlet.http.HttpServletRequest;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 纯获取ipv地址，不转换为具体的省份
 */
public class Ipv6Utils {

    private static final List<String> IP_HEADERS = Arrays.asList(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR",
        "X-Real-IP"
    );

    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
        "(^127\\.)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)|(^::1$)|(^[fF][cCdD])",
        Pattern.CANON_EQ
    );

    /**
     * 获取客户端真实IP地址（支持IPv6）
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = getIpFromHeaders(request);

        // 验证是否为有效IP（包括IPv6）
        if (isValidIp(ip)) {
            return normalizeIp(ip);
        }

        // 如果头部没有获取到有效IP，则使用remoteAddr
        ip = request.getRemoteAddr();

        // 处理IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 从请求头中获取IP地址
     */
    private static String getIpFromHeaders(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                // 可能有多个IP（如X-Forwarded-For: client, proxy1, proxy2）
                String[] ips = ipList.split(",");
                for (String ip : ips) {
                    String trimmedIp = ip.trim();
                    if (isValidIp(trimmedIp) && !isPrivateIp(trimmedIp)) {
                        return trimmedIp;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 验证IP地址是否有效（支持IPv4和IPv6）
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return !inetAddress.isAnyLocalAddress()
                   && !inetAddress.isLoopbackAddress()
                   && !inetAddress.isMulticastAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 判断是否为内网IP
     */
    private static boolean isPrivateIp(String ip) {
        return PRIVATE_IP_PATTERN.matcher(ip).find();
    }

    /**
     * 规范化IP地址表示（特别是IPv6）
     */
    public static String normalizeIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            if (inetAddress instanceof Inet6Address) {
                // IPv6地址规范化处理
                return inetAddress.getHostAddress().toLowerCase();
            }
            return ip;
        } catch (UnknownHostException e) {
            return ip;
        }
    }
}
