package com.xcz.commons.core.utils.ip;

import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * IP 工具类
 * 用于获取 IP 地址、IP 归属地解析（基于 ip2region v2.x/v3.x）
 *
 * @author xcz
 */
@Slf4j
public class IpUtils {

    /**
     * IP库缓存数据
     * 为了避免每次查询都进行 IO 操作，将 xdb 文件内容加载到内存中。
     * ip2region.xdb 文件通常只有几 MB，完全可以常驻内存。
     */
    private static byte[] cBuff;

    static {
        // 静态代码块：类加载时初始化 IP 库
        initIpDb();
    }

    /**
     * 初始化 IP 数据库
     * 将 xdb 文件一次性读取到 byte[] 中
     */
    private static void initIpDb() {
        try (InputStream stream = IpUtils.class.getResourceAsStream("/ip2region/ip2region.xdb")) {
            if (stream == null) {
                log.error("IP地址库文件不存在: /ip2region/ip2region.xdb");
                return;
            }
            cBuff = IOUtils.toByteArray(stream);
            log.info("IP地址库加载成功，大小: {} KB", cBuff.length / 1024);
        } catch (Exception e) {
            log.error("IP地址库加载失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取 IP 归属地（简单格式）
     * 例如：中国-上海、内网IP
     *
     * @param ip IP地址
     * @return 归属地字符串
     */
    @SneakyThrows
    public static String getIpLocation(String ip) {
        if (internalIp(ip)) {
            return "内网IP";
        }
        IpLocationInfo ipLocationInfo = getIpLocationInfo(ip);
        return ipLocationInfo.getLocationInfo();
    }

    /**
     * 获取友好的 IP 归属地显示
     * 如果是国内IP，只显示省份（如：上海）；国外则显示 国家-省份（如：美国-加利福尼亚）
     *
     * @param ip IP地址
     * @return 友好显示的地址
     */
    public static String getFriendlyIpLocation(String ip) {
        IpLocationInfo ipLocationInfo = getIpLocationInfo(ip);
        if (Objects.equals(ipLocationInfo.getCountry(), "中国")) {
            return ipLocationInfo.getProvince();
        }
        return String.format("%s-%s", ipLocationInfo.getCountry(), ipLocationInfo.getProvince());
    }

    /**
     * 根据已有的位置字符串解析友好显示
     * 例如输入：中国-上海-上海市--电信 -> 输出：上海
     *
     * @param location 原始位置字符串 (格式通常为：国家-区域-省份-城市-ISP)
     * @return 友好显示的地址
     */
    public static String getFriendlyIpLocationByLocation(String location) {
        if (StringUtils.isEmpty(location)) {
            return "未知地址";
        }
        String[] split = location.split("-");
        // 简单的健壮性判断，只要能取到国家和省份即可
        if (split.length < 3) {
            return location;
        }
        String country = split[0];
        // 如果是中国，去掉"省"、"市"后缀，显示更简洁
        if ("中国".equals(country)) {
            return String.format("%s", split[2].replace("省", "").replace("市", ""));
        }
        return String.format("%s-%s", country, split[2]);
    }

    /**
     * 获取 IP 详细归属地信息对象
     * 核心查询方法，已优化为内存查询模式
     *
     * @param ip IP地址
     * @return IpLocationInfo 对象
     */
    public static IpLocationInfo getIpLocationInfo(String ip) {
        IpLocationInfo ipLocationInfo = new IpLocationInfo();

        // 校验缓存是否加载成功以及IP有效性
        if (cBuff == null || StringUtils.isEmpty(ip)) {
            return ipLocationInfo;
        }

        try {
            // 使用内存中的 cBuff 创建 Searcher，避免 IO 操作
            // 注意：Searcher 对象不是线程安全的，但在 newWithBuffer 模式下，每次创建非常快且轻量，
            // 这种方式是官方推荐的并发安全用法（用空间换并发安全）。
            LongByteArray longByteArray = new LongByteArray(cBuff.length);
            longByteArray.append(cBuff);
            Searcher searcher = Searcher.newWithBuffer(Version.IPv4,longByteArray);

            String region = searcher.search(ip);

            // 关闭 searcher (虽然基于内存的关闭没做什么实质操作，但保持好习惯)
            searcher.close();

            // 解析结果：国家|区域|省份|城市|ISP
            if (StringUtils.isNotEmpty(region)) {
                String[] ipRegionArr = region.split("\\|");
                if (ipRegionArr.length == 5) {
                    ipLocationInfo.setCountry(filterZero(ipRegionArr[0]));
                    ipLocationInfo.setRegion(filterZero(ipRegionArr[1]));
                    ipLocationInfo.setProvince(filterZero(ipRegionArr[2]));
                    ipLocationInfo.setCity(filterZero(ipRegionArr[3]));
                    ipLocationInfo.setIsp(filterZero(ipRegionArr[4]));
                }
            }
        } catch (Exception e) {
            log.error("IP解析异常, ip: {}, error: {}", ip, e.getMessage());
        }

        return ipLocationInfo;
    }

    /**
     * 过滤 ip2region 返回的 "0"
     * ip2region 对于未知区域会返回 "0"，展示时需要过滤掉
     *
     * @param str 原始字符串
     * @return 过滤后的字符串
     */
    private static String filterZero(String str) {
        return "0".equals(str) ? "未知" : str;
    }

    /**
     * 获取当前 HTTP 请求的客户端 IP
     *
     * @return IP地址
     */
    public static String getServletIp() {
        return getIpAddr(ServletUtils.getRequest());
    }

    /**
     * 获取客户端 IP 地址
     * 考虑了多级反向代理的情况
     *
     * @param request 请求对象
     * @return IP 地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理 IPv6 地址转换
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }

        return getMultistageReverseProxyIp(ip);
    }

    /**
     * 检查是否为内部 IP 地址
     *
     * @param ip IP地址
     * @return true: 是内网IP, false: 是外网IP
     */
    public static boolean internalIp(String ip) {
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            return true;
        }
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr);
    }

    /**
     * 判断是否为内部 IP (私有IP网段)
     * A类: 10.0.0.0    - 10.255.255.255
     * B类: 172.16.0.0  - 172.31.255.255
     * C类: 192.168.0.0 - 192.168.255.255
     *
     * @param addr byte数组格式的IP
     * @return true: 是内网IP
     */
    private static boolean internalIp(byte[] addr) {
        if (StringUtils.isNull(addr) || addr.length < 2) {
            return true;
        }
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        // 172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                switch (b1) {
                    case SECTION_6:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * 将 IPv4 地址转换成字节数组
     * 用于 IP 校验逻辑
     *
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (text.isEmpty()) {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try {
            long l;
            int i;
            switch (elements.length) {
                case 1:
                    l = Long.parseLong(elements[0]);
                    if ((l < 0L) || (l > 4294967295L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                    bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 2:
                    l = Integer.parseInt(elements[0]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l & 0xFF);
                    l = Integer.parseInt(elements[1]);
                    if ((l < 0L) || (l > 16777215L)) {
                        return null;
                    }
                    bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 3:
                    for (i = 0; i < 2; ++i) {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    l = Integer.parseInt(elements[2]);
                    if ((l < 0L) || (l > 65535L)) {
                        return null;
                    }
                    bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 4:
                    for (i = 0; i < 4; ++i) {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return bytes;
    }

    /**
     * 获取本机 IP 地址
     *
     * @return 本地IP地址
     */
    public static String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }
        return "127.0.0.1";
    }

    /**
     * 获取本机主机名
     *
     * @return 本地主机名
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }
        return "未知";
    }

    /**
     * 从多级反向代理中获得第一个非 unknown IP 地址
     * 这里的逻辑是取 X-Forwarded-For 中第一个有效的 IP
     *
     * @param ip 获得的IP地址字符串 (可能包含多个IP)
     * @return 第一个非 unknown IP 地址
     */
    public static String getMultistageReverseProxyIp(String ip) {
        // 多级反向代理检测
        if (ip != null && ip.indexOf(",") > 0) {
            final String[] ips = ip.trim().split(",");
            for (String subIp : ips) {
                if (!isUnknown(subIp)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
    }

    /**
     * 检测给定字符串是否为未知，多用于检测 HTTP 请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    public static boolean isUnknown(String checkString) {
        return StringUtils.isBlank(checkString) || "unknown".equalsIgnoreCase(checkString);
    }

    public static void main(String[] args) {
        // 测试外网IP
        System.out.println(getIpLocation("220.196.160.51"));
        // 测试内网IP
        System.out.println(getIpLocation("127.0.0.1"));
        System.out.println(getIpLocation("192.168.1.1"));
    }
}
