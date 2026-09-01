# commons-core

核心基础模块，提供通用工具类、统一 API 响应模型、异常体系、常量定义及安全链扩展点。其他 commons 子模块大多直接或间接依赖本模块。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-core</artifactId>
</dependency>
```

## 目录

- [包结构](#包结构)
- [自动配置](#自动配置)
- [text — 文本与类型转换](#text--文本与类型转换)
  - [CharsetKit](#charsetkit)
  - [Convert](#convert)
- [utils — 通用工具](#utils--通用工具)
  - [StringUtils](#stringutils)
  - [DateUtils](#dateutils)
  - [Convert 相关](#convert-相关)
  - [ServletUtils](#servletutils)
  - [SpringUtils](#springutils)
  - [PageUtils](#pageutils)
  - [ExceptionUtil](#exceptionutil)
  - [LogSanitizer](#logsanitizer)
  - [BigDecimalUtils](#bigdecimalutils)
  - [ValidationUtils](#validationutils)
  - [CryptoUtils](#cryptoutils)
  - [SecurityHelper](#securityhelper)
  - [RequestSignatureUtils](#requestsignatureutils)
- [utils.file — 文件工具](#utilsfile--文件工具)
  - [FileUtils](#fileutils)
  - [FileTypeUtils](#filetypeutils)
  - [MimeTypeUtils](#mimetypeutils)
  - [ImageUtils](#imageutils)
- [utils.html — HTML 工具](#utilshtml--html-工具)
  - [EscapeUtil](#escapeutil)
  - [HTMLFilter](#htmlfilter)
- [utils.ip — IP 工具](#utilsip--ip-工具)
  - [IpUtils](#iputils)
  - [Ipv6Utils](#ipv6utils)
  - [IpLocationInfo](#iplocationinfo)
- [utils.poi — Excel 工具](#utilspoi--excel-工具)
  - [ExcelUtil](#excelutil)
  - [ExcelHandlerAdapter](#excelhandleradapter)
- [utils.reflect — 反射工具](#utilsreflect--反射工具)
  - [ReflectUtils](#reflectutils)
- [utils.response — 响应构建](#utilsresponse--响应构建)
  - [ResponseEntityUtils](#responseentityutils)
- [utils.sign — 编码工具](#utilssign--编码工具)
  - [Base64](#base64)
- [utils.uuid — ID 生成](#utilsuuid--id-生成)
  - [IdUtils](#idutils)
  - [Seq](#seq)
  - [UUID](#uuid)
- [log — 请求日志](#log--请求日志)
  - [RequestLogAttributes](#requestlogattributes)
- [constant — 常量](#constant--常量)
- [domain / web — 统一响应](#domain--web--统一响应)
- [annotation — 注解](#annotation--注解)
- [xss — XSS 防护](#xss--xss-防护)
- [security — 安全链扩展](#security--安全链扩展)
- [exception — 异常体系](#exception--异常体系)
- [event — 领域事件](#event--领域事件)
- [配置与依赖说明](#配置与依赖说明)
- [相关模块](#相关模块)

---

## 包结构

```
com.xcz.commons.core
├── annotation/          # @Excel、@Excels 等注解
├── config/              # 自动配置（日期格式等）
├── constant/            # 全局常量
├── domain/              # ResponseEntity 统一响应体
├── event/               # 领域事件
├── exception/           # 异常体系
├── log/                 # 请求日志 attribute 键
├── security/            # Security 链扩展接口
├── text/                # 类型转换、字符集
├── utils/               # 通用工具
│   ├── file/            # 文件、图片、MIME
│   ├── html/            # HTML 转义、XSS 过滤
│   ├── ip/              # IP 解析与归属地
│   ├── poi/             # Excel 导入导出
│   ├── reflect/         # 反射
│   ├── response/        # 响应构建
│   ├── sign/            # Base64
│   └── uuid/            # ID / UUID 生成
├── web.vo.params/       # AjaxResult
└── xss/                 # @Xss 校验注解
```

---

## 自动配置

| 类 | 作用 |
|----|------|
| `SpringUtils` | 注册为 `BeanFactoryPostProcessor`，提供静态 `getBean()` |
| `DateFormatConfig` | 统一 `LocalDateTime` 序列化格式为 `yyyy-MM-dd HH:mm:ss`；反序列化兼容 ISO-8601 与自定义格式 |

---

## text — 文本与类型转换

### CharsetKit

**简介：** 字符集常量与字符串编码互转工具，解决 HTTP 乱码、旧系统 GBK 数据迁移等场景。

**使用示例：**

```java
// 获取系统默认字符集
String charset = CharsetKit.systemCharset();

// ISO-8859-1 误解析后转 UTF-8
String fixed = CharsetKit.convert(garbled, CharsetKit.ISO_8859_1, CharsetKit.UTF_8);
```

**常量：**

| 名称 | 说明 |
|------|------|
| `ISO_8859_1` / `CHARSET_ISO_8859_1` | 西欧单字节编码 |
| `UTF_8` / `CHARSET_UTF_8` | Unicode 编码 |
| `GBK` / `CHARSET_GBK` | 中文 Windows 常用编码 |

**方法列表：**

| 方法 | 说明 |
|------|------|
| `charset(String charset)` | 字符串转 `Charset`，空则返回系统默认 |
| `convert(String, String, String)` | 按字符集名转码 |
| `convert(String, Charset, Charset)` | 按 `Charset` 转码，默认 ISO-8859-1 → UTF-8 |
| `systemCharset()` | 返回 JVM 默认字符集名称 |

---

### Convert

**简介：** 通用类型转换器，将任意 `Object` 安全转换为目标 Java 类型。`null` 或解析失败时不抛异常，返回默认值。广泛用于请求参数解析、Excel 导入、JWT claims 读取等。

**使用示例：**

```java
int page = Convert.toInt(request.getParameter("page"), 1);
Long version = Convert.toLong(claims.get("ver"), 0L);
Integer[] ids = Convert.toIntArray("1,2,3");
Boolean enabled = Convert.toBool("yes");  // true
String text = Convert.utf8Str(byteArray);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `toStr(Object)` / `toStr(Object, String)` | 转字符串 |
| `toChar(Object)` / `toChar(Object, Character)` | 转字符（取首字符） |
| `toByte/toShort/toInt/toLong/toFloat/toDouble` | 转数值，均支持 `(value)` 与 `(value, defaultValue)` 重载 |
| `toNumber(Object)` / `toNumber(Object, Number)` | 转通用 Number，支持千分位格式 |
| `toBool(Object)` / `toBool(Object, Boolean)` | 转布尔，识别 true/yes/ok/1 与 false/no/0 |
| `toEnum(Class, Object)` / `toEnum(Class, Object, E)` | 转枚举 |
| `toBigInteger/toBigDecimal` | 转大数 |
| `toIntArray(String)` / `toIntArray(String split, String)` | 分隔字符串转 `Integer[]`，失败项默认 0 |
| `toLongArray(String)` / `toLongArray(String split, String)` | 分隔字符串转 `Long[]` |
| `toStrArray(String)` / `toStrArray(String split, String)` | 分隔字符串转 `String[]` |
| `utf8Str(Object)` | 以 UTF-8 解码对象为字符串 |
| `str(Object, String/Charset)` | 对象/字节数组/ByteBuffer 转字符串 |
| `str(byte[], Charset)` / `str(ByteBuffer, Charset)` | 字节数据解码 |
| `toSBC(String)` / `toSBC(String, Set<Character>)` | 半角转全角 |
| `toDBC(String)` / `toDBC(String, Set<Character>)` | 全角转半角 |
| `digitUppercase(double)` | 数字金额转中文大写 |

---

## utils — 通用工具

### StringUtils

**简介：** 字符串工具类，继承 Apache Commons Lang3 的 `StringUtils`，在其基础上扩展集合/Map 判空、命名风格转换、Ant 路径匹配等能力。

**使用示例：**

```java
if (StringUtils.isEmpty(str)) { ... }
String camel = StringUtils.toCamelCase("user_name");       // userName
String under = StringUtils.toUnderScoreCase("userName");   // user_name
boolean match = StringUtils.isMatch("/api/**", "/api/user");
```

**方法列表（扩展方法，另含父类全部方法）：**

| 方法 | 说明 |
|------|------|
| `nvl(T, T)` | 空则返回默认值 |
| `isEmpty/isNotEmpty` | 判空：Collection、Object[]、Map、String |
| `isNull/isNotNull` | 对象判空 |
| `isArray(Object)` | 是否为数组 |
| `hasText(String)` | 是否有非空白文本 |
| `ishttp(String)` | 是否为 http/https 链接 |
| `containsAny(Collection, String...)` | 集合是否包含任一字符串 |
| `toUnderScoreCase(String)` | 驼峰转下划线 |
| `convertToCamelCase(String)` / `toCamelCase(String)` | 下划线转驼峰 |
| `inStringIgnoreCase(String, String...)` | 忽略大小写匹配 |
| `matches(String, List<String>)` | 字符串是否匹配列表中任一模式 |
| `isMatch(String pattern, String url)` | Ant 风格路径匹配 |
| `cast(Object)` | 泛型强转 |
| `padl(Number, int)` / `padl(String, int, char)` | 左补零/补字符 |

---

### DateUtils

**简介：** 日期时间工具类，继承 Apache Commons Lang3 的 `DateUtils`，提供格式化、解析、路径生成及 Java 8 时间互转。

**使用示例：**

```java
String today = DateUtils.getDate();                    // yyyy-MM-dd
Date date = DateUtils.parseDate("2026-09-01 14:00:00");
String diff = DateUtils.getDatePoor(endDate, startDate); // 相差描述
```

**格式常量：** `YYYY`、`YYYY_MM`、`YYYY_MM_DD`、`YYYYMMDDHHMMSS`、`YYYY_MM_DD_HH_MM_SS`

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getNowDate()` | 当前 `Date` |
| `getDate()` | 当前日期字符串 `yyyy-MM-dd` |
| `getTime()` | 当前时间字符串 `yyyy-MM-dd HH:mm:ss` |
| `dateTimeNow()` / `dateTimeNow(String format)` | 按格式返回当前时间字符串 |
| `dateTime(Date)` / `parseDateToStr(String, Date)` | 日期格式化 |
| `dateTime(String format, String ts)` | 字符串解析为 `Date` |
| `datePath()` | 日期路径 `yyyy/MM/dd` |
| `dateTime()` | 时间路径 `yyyyMMdd` |
| `parseDate(Object)` | 多格式自动解析 |
| `getServerStartDate()` | JVM 启动时间 |
| `getDatePoor(Date end, Date now)` | 两日期间隔描述 |
| `toDate(LocalDateTime)` / `toDate(LocalDate)` | Java 8 时间转 `Date` |

---

### Convert 相关

`ServletUtils`、`ExcelUtil`、`ReflectUtils` 等内部均依赖 `Convert` 做类型转换，详见 [Convert](#convert)。

---

### ServletUtils

**简介：** Web 请求上下文工具，封装 Servlet 与 WebFlux 场景下的参数读取、Header 解析、User-Agent 识别及响应写出。

**使用示例：**

```java
String ip = ServletUtils.getParameter("keyword", "");
Integer page = ServletUtils.getParameterToInt("pageNum", 1);
HttpServletRequest request = ServletUtils.getRequest();
ServletUtils.renderString(response, JSON.toJSONString(result));
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getUserAgent()` / `getUserAgentOs()` / `getUserAgentBrowser()` | 解析 User-Agent |
| `getParameter(String)` / `getParameter(String, String)` | 读取字符串参数 |
| `getParameterToInt/Bool` | 读取数值/布尔参数，支持默认值重载 |
| `getParams(ServletRequest)` | 原始参数 Map |
| `getParamMap(ServletRequest)` | 单值参数 Map |
| `getRequest()` / `getResponse()` / `getSession()` | 当前请求上下文 |
| `getRequestAttributes()` | `ServletRequestAttributes` |
| `getHeader(HttpServletRequest, String)` | 读取请求头 |
| `getHeaders(HttpServletRequest)` | 全部请求头 Map |
| `renderString(HttpServletResponse, String)` | 写出 JSON 字符串响应 |
| `isAjaxRequest(HttpServletRequest)` | 是否 Ajax 请求 |
| `urlEncode(String)` / `urlDecode(String)` | URL 编解码 |
| `webFluxResponseWriter(...)` | WebFlux 场景写出 JSON 响应（多个重载） |

---

### SpringUtils

**简介：** Spring 容器静态访问工具，在无法注入 Bean 的场景（如工具类、静态方法）中获取 Bean 或 AOP 代理。

**使用示例：**

```java
UserService service = SpringUtils.getBean(UserService.class);
SomeService proxy = SpringUtils.getAopProxy(someService);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getBean(String name)` | 按名称获取 Bean |
| `getBean(Class<T> clz)` | 按类型获取 Bean |
| `containsBean(String)` | Bean 是否存在 |
| `isSingleton(String)` | 是否单例 |
| `getType(String)` | Bean 类型 |
| `getAliases(String)` | Bean 别名 |
| `getAopProxy(T invoker)` | 获取 AOP 代理对象 |

---

### PageUtils

**简介：** 内存分页工具，对已有 List 做切片，适用于非数据库分页或聚合后分页。

**使用示例：**

```java
List<User> page = PageUtils.slice(allUsers, 1, 10);
long total = PageUtils.sliceTotal(allUsers);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `slice(List<T>, int pageNum, int pageSize)` | 内存分页切片 |
| `sliceTotal(List<?>)` | 返回总条数 |

---

### ExceptionUtil

**简介：** 异常信息提取工具，用于日志打印或前端错误提示。

**使用示例：**

```java
String msg = ExceptionUtil.getExceptionMessage(e);
String root = ExceptionUtil.getRootErrorMessage(e);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getExceptionMessage(Throwable)` | 获取完整异常链信息 |
| `getRootErrorMessage(Exception)` | 获取根因异常信息 |

---

### LogSanitizer

**简介：** 日志脱敏工具，在打印请求/响应 JSON 前移除 password、token 等敏感字段。

**使用示例：**

```java
String safe = LogSanitizer.sanitizeJson(jsonBody, objectMapper);
Object safeObj = LogSanitizer.sanitizeObject(body, objectMapper);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `sanitizeJson(String, ObjectMapper)` | 脱敏 JSON 字符串 |
| `sanitizeObject(Object, ObjectMapper)` | 脱敏对象 |

---

### BigDecimalUtils

**简介：** 大数辅助工具。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `nullToZero(BigDecimal)` | `null` 转 `BigDecimal.ZERO` |

---

### ValidationUtils

**简介：** 业务参数校验工具。

**使用示例：**

```java
Long shopId = ValidationUtils.requireShopId(paramShopId); // null 时抛 ServiceException
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `requireShopId(Long shopId)` | 校验 shopId 非空，否则抛异常 |

---

### CryptoUtils

**简介：** 加密解密工具，提供 AES、RSA、SHA-256 等常用算法封装。

**使用示例：**

```java
String encrypted = CryptoUtils.aesEncrypt(data, key, iv);
String decrypted = CryptoUtils.aesDecrypt(encrypted, key, iv);
String hash = CryptoUtils.sha256Hash(data);
Map<String, String> keyPair = CryptoUtils.generateRsaKeyPair();
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `aesEncrypt(String data, String key, String iv)` | AES 加密 |
| `aesDecrypt(String encryptedData, String key, String iv)` | AES 解密 |
| `sha256Hash(String data)` | SHA-256 哈希 |
| `generateAesKey()` | 生成 AES 密钥 |
| `generateIv()` | 生成初始化向量 |
| `rsaDecrypt(String encryptedData, String privateKey)` | RSA 私钥解密 |
| `generateRsaKeyPair()` | 生成 RSA 密钥对 |

---

### SecurityHelper

**简介：** 请求安全辅助工具，封装签名 Header 生成及 AES 加解密，配合网关加密通信使用。

**使用示例：**

```java
Map<String, String> headers = SecurityHelper.generateSecureHeaders(params, secret);
SecurityHelper.EncryptedData enc = SecurityHelper.encryptData(json, secret);
String plain = SecurityHelper.decryptData(enc.getEncryptedData(), enc.getIv(), secret);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `generateSecureHeaders(Map, String secret)` | 生成 timestamp/nonce/signature 等安全头 |
| `encryptData(String data, String secret)` | AES 加密，返回 `EncryptedData` |
| `decryptData(String encryptedData, String iv, String secret)` | AES 解密 |
| `decryptResponse(String encryptedResponse, Map headers, String secret)` | 解密加密响应体 |
| `EncryptedData.getEncryptedData()` / `getIv()` | 加密结果数据与 IV |

---

### RequestSignatureUtils

**简介：** 请求签名校验工具，按参数字典序拼接后 HMAC 签名。

**使用示例：**

```java
String sign = RequestSignatureUtils.generateSignature(params, timestamp, nonce, secret);
boolean ok = RequestSignatureUtils.verifySignature(params, timestamp, nonce, sign, secret);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `generateSignature(Map, String timestamp, String nonce, String secret)` | 生成签名 |
| `verifySignature(Map, String timestamp, String nonce, String signature, String secret)` | 校验签名 |

---

## utils.file — 文件工具

### FileUtils

**简介：** 文件读写、下载响应头设置、文件名校验等。

**使用示例：**

```java
FileUtils.writeBytes("/path/file.pdf", outputStream);
FileUtils.setAttachmentResponseHeader(response, "report.xlsx");
boolean allowed = FileUtils.checkAllowDownload(resourcePath);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `writeBytes(String filePath, OutputStream os)` | 读取文件写入输出流 |
| `deleteFile(String filePath)` | 删除文件 |
| `isValidFilename(String filename)` | 文件名是否合法 |
| `checkAllowDownload(String resource)` | 是否允许下载（防路径穿越） |
| `setFileDownloadHeader(HttpServletRequest, String fileName)` | 设置下载文件名 Header |
| `getName(String filePath)` | 提取文件名 |
| `isFileSeparator(char c)` | 是否路径分隔符 |
| `setAttachmentResponseHeader(HttpServletResponse, String realFileName)` | 设置附件下载响应头 |
| `percentEncode(String s)` | 百分号编码 |

---

### FileTypeUtils

**简介：** 文件类型与扩展名识别。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getFileType(File)` / `getFileType(String fileName)` | 获取文件类型 |
| `getExtension(MultipartFile)` | 上传文件扩展名 |
| `getFileExtendName(byte[] photoByte)` | 根据文件头魔数识别扩展名 |

---

### MimeTypeUtils

**简介：** MIME 类型常量与扩展名映射。

**常量：** `IMAGE_PNG/JPG/JPEG/BMP/GIF`、`IMAGE_EXTENSION`、`FLASH_EXTENSION`、`MEDIA_EXTENSION`、`VIDEO_EXTENSION`、`DEFAULT_ALLOWED_EXTENSION`

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getExtension(String prefix)` | MIME 前缀转扩展名 |

---

### ImageUtils

**简介：** 图片/文件字节读取。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getImage(String imagePath)` | 读取图片为 byte[] |
| `getFile(String imagePath)` | 读取为 InputStream |
| `readFile(String url)` | 从 URL 读取文件字节 |

---

## utils.html — HTML 工具

### EscapeUtil

**简介：** HTML 转义与 XSS 清理，过滤危险标签与脚本。

**使用示例：**

```java
String safe = EscapeUtil.escape(userInput);
String cleaned = EscapeUtil.clean(htmlContent);
String restored = EscapeUtil.unescape(escaped);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `escape(String text)` | HTML 转义 |
| `unescape(String content)` | HTML 反转义 |
| `clean(String content)` | 清除 HTML 标签 |
| `decode(String content)` | 解码 HTML 实体 |

---

### HTMLFilter

**简介：** 白名单 HTML 过滤器，允许安全标签、过滤危险属性。

**使用示例：**

```java
HTMLFilter filter = new HTMLFilter();
String safe = filter.filter(richText);
String escaped = HTMLFilter.htmlSpecialChars(input);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `HTMLFilter()` / `HTMLFilter(Map conf)` | 构造过滤器 |
| `filter(String input)` | 过滤 HTML |
| `htmlSpecialChars(String s)` | 特殊字符转义（静态） |
| `chr(int decimal)` | 数字转字符（静态） |
| `isAlwaysMakeTags()` / `isStripComments()` | 配置查询 |

---

## utils.ip — IP 工具

### IpUtils

**简介：** IP 地址获取、内外网判断、归属地查询（依赖 ip2region.xdb）。

**使用示例：**

```java
String ip = IpUtils.getIpAddr(request);
String location = IpUtils.getFriendlyIpLocation(ip);   // 如：中国-广东-深圳-电信
IpLocationInfo info = IpUtils.getIpLocationInfo(ip);
boolean internal = IpUtils.internalIp(ip);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getIpAddr(HttpServletRequest)` | 从请求获取客户端 IP |
| `getServletIp()` | 从当前 Servlet 上下文获取 IP |
| `getIpLocation(String ip)` | 原始归属地字符串 |
| `getFriendlyIpLocation(String ip)` | 友好格式归属地 |
| `getFriendlyIpLocationByLocation(String location)` | 格式化已有归属地字符串 |
| `getIpLocationInfo(String ip)` | 结构化归属地信息 |
| `internalIp(String ip)` | 是否内网 IP |
| `textToNumericFormatV4(String text)` | IPv4 文本转字节数组 |
| `getHostIp()` / `getHostName()` | 本机 IP / 主机名 |
| `getMultistageReverseProxyIp(String ip)` | 处理多级反向代理 IP |
| `isUnknown(String checkString)` | 是否为 unknown 占位值 |

---

### Ipv6Utils

**简介：** IPv6 客户端 IP 获取与校验。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getClientIp(HttpServletRequest)` | 获取客户端 IP（兼容 IPv6） |
| `isValidIp(String ip)` | IP 格式是否合法 |
| `normalizeIp(String ip)` | 规范化 IP 字符串 |

---

### IpLocationInfo

**简介：** IP 归属地结构化数据模型（country/region/province/city/isp）。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getLocationInfo()` | 返回 `国家-省份-城市-运营商` 格式字符串 |
| Getter/Setter | Lombok `@Data` 生成 |

---

## utils.poi — Excel 工具

### ExcelUtil

**简介：** 基于 `@Excel` 注解的 Excel 导入导出工具，支持字典转换、下拉校验、图片单元格、统计行等。

**使用示例：**

```java
// 导出
ExcelUtil<User> util = new ExcelUtil<>(User.class);
util.exportExcel(response, userList, "用户数据");

// 导入
List<User> list = util.importExcel(inputStream);

// 实体字段
@Excel(name = "用户名", sort = 1, readConverterExp = "0=男,1=女")
private String username;
```

**方法列表（主要公开 API）：**

| 分类 | 方法 | 说明 |
|------|------|------|
| 构造 | `ExcelUtil(Class<T> clazz)` | 绑定实体类 |
| 静态 | `convertByExp(String, String, String)` | 字典值转显示文本 |
| 静态 | `reverseByExp(String, String, String)` | 显示文本转字典值 |
| 导入 | `importExcel(InputStream)` | 导入 Excel |
| 导入 | `importExcel(InputStream, int titleNum)` | 指定标题行数 |
| 导入 | `importExcel(String sheetName, InputStream, int titleNum)` | 指定 Sheet |
| 导出 | `exportExcel(response, list, sheetName)` | 导出数据 |
| 导出 | `exportExcel(response, list, sheetName, title)` | 带大标题导出 |
| 导出 | `importTemplateExcel(response, sheetName)` | 导出导入模板 |
| 配置 | `hideColumn(String... fields)` | 隐藏列 |
| 配置 | `init(list, sheetName, title, type)` | 初始化工作簿 |

---

### ExcelHandlerAdapter

**简介：** Excel 自定义格式化适配器接口，配合 `@Excel(handler = Xxx.class)` 使用。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `format(Object value, String[] args)` | 自定义单元格格式化 |

---

## utils.reflect — 反射工具

### ReflectUtils

**简介：** 反射操作封装，支持 getter/setter 调用、字段读写、泛型类型获取，自动做基本类型转换。

**使用示例：**

```java
Object value = ReflectUtils.invokeGetter(user, "username");
ReflectUtils.invokeSetter(user, "status", 1);
ReflectUtils.setFieldValue(user, "remark", "note");
Method m = ReflectUtils.getAccessibleMethod(obj, "save", String.class);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `invokeGetter(Object, String propertyName)` | 调用 getter |
| `invokeSetter(Object, String propertyName, E value)` | 调用 setter |
| `getFieldValue(Object, String fieldName)` | 读字段 |
| `setFieldValue(Object, String fieldName, E value)` | 写字段 |
| `invokeMethod(Object, String, Class[], Object[])` | 精确签名调用方法 |
| `invokeMethodByName(Object, String, Object[])` | 按方法名调用（自动匹配） |
| `getAccessibleField(Object, String)` | 获取可访问字段 |
| `getAccessibleMethod(Object, String, Class...)` | 获取可访问方法 |
| `getAccessibleMethodByName(Object, String, int argsNum)` | 按方法名和参数个数获取 |
| `makeAccessible(Method/Field)` | 设置可访问 |
| `getClassGenricType(Class)` / `getClassGenricType(Class, int index)` | 获取泛型类型 |
| `getUserClass(Object)` | 获取用户类（非 CGLIB 代理类） |
| `convertReflectionExceptionToUnchecked(String, Exception)` | 反射异常转运行时异常 |

---

## utils.response — 响应构建

### ResponseEntityUtils

**简介：** 统一响应体 `ResponseEntity<T>` 的构建工具，替代手写 code/msg/data。

**使用示例：**

```java
return ResponseEntityUtils.ok(user);
return ResponseEntityUtils.okPage(list, total, "查询成功");
return ResponseEntityUtils.fail("操作失败");
return ResponseEntityUtils.fail(401, "未授权");
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `ok()` / `ok(T data)` / `ok(T data, String msg)` | 成功响应 |
| `okPage(List<T>, long total, String msg)` | 分页成功响应 |
| `fail()` / `fail(String msg)` / `fail(T data)` / `fail(T data, String msg)` | 失败响应 |
| `fail(int code, String msg)` | 指定错误码 |
| `isError(ResponseEntity)` / `isSuccess(ResponseEntity)` | 判断响应状态 |

---

## utils.sign — 编码工具

### Base64

**简介：** Base64 编解码（自定义实现，非 JDK 标准库封装）。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `encode(byte[] binaryData)` | Base64 编码 |
| `decode(String encoded)` | Base64 解码 |

---

## utils.uuid — ID 生成

### IdUtils

**简介：** UUID 快捷生成。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `randomUUID()` | 标准 UUID（含 `-`） |
| `simpleUUID()` | 无 `-` 的 UUID |
| `fastUUID()` | 快速 UUID（含 `-`） |
| `fastSimpleUUID()` | 快速 UUID（无 `-`） |

---

### Seq

**简介：** 基于原子计数器的有序 ID 生成，适用于订单号、流水号等。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `getId()` | 默认类型 ID |
| `getId(String type)` | 指定类型前缀 ID |
| `getId(AtomicInteger, int length)` | 自定义计数器与长度 |

---

### UUID

**简介：** 增强版 UUID 工具（内部实现，供 `IdUtils` 调用）。

---

## log — 请求日志

### RequestLogAttributes

**简介：** 请求日志模块与安全模块之间的 attribute 键常量，用于在 Filter / 异常处理器 / 日志拦截器之间传递数据。

**常量列表：**

| 常量 | 值 | 说明 |
|------|-----|------|
| `EXCEPTION` | `xcz.request.exception` | 全局异常处理器捕获的异常 |
| `REQUEST_BODY` | `xcz.request.body` | LogFilter 缓存的原始请求体 |

**使用场景：**

- `LogFilter` 写入 `REQUEST_BODY`，供 `RequestLogInterceptor` 打印完整请求日志
- `GlobalExceptionHandler` 写入 `EXCEPTION`，日志拦截器据此避免重复打印堆栈

---

## constant — 常量

| 类 | 主要内容 |
|----|----------|
| `Constants` | HTTP 状态码、分页参数名、登录状态、验证码过期、资源前缀、定时任务白/黑名单 |
| `SecurityConstants` | 认证头、内部调用头、Feign 标记、白名单路径、角色权限键 |
| `SecurityHeaderConstants` | 加密通信头（X-Encrypted-Body、X-IV、X-Signature 等） |
| `TokenConstants` | JWT 字段名（user_id、username、authorities、exp 等） |
| `UserConstants` | 用户/角色/菜单状态、用户名密码长度限制 |
| `NotifyConstants` | 工单消息 Topic、WebSocket 路径 |
| `ScheduleConstants` | 定时任务类名、Misfire 策略 |
| `GenConstants` | 代码生成器模板、字段类型、HTML 控件类型 |

---

## domain / web — 统一响应

### ResponseEntity\<T\>

**简介：** 统一 API 响应模型，字段包含 `code`、`msg`、`data`。

**方法列表：**

| 方法 | 说明 |
|------|------|
| `isOk()` | 是否成功（code == 200） |

---

### AjaxResult

**简介：** 基于 `HashMap` 的 Ajax 响应封装，兼容旧版前端 `{code, msg, data}` 结构。

**使用示例：**

```java
return AjaxResult.success(data);
return AjaxResult.error("操作失败");
return AjaxResult.warn("请注意").put("extra", value);
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `success()` / `success(Object)` / `success(String)` / `success(String, Object)` | 成功 |
| `warn(String)` / `warn(String, Object)` | 警告 |
| `error()` / `error(String)` / `error(String, Object)` / `error(int, String)` | 失败 |
| `isSuccess()` / `isError()` | 状态判断 |
| `put(String key, Object value)` | 追加字段 |

---

## annotation — 注解

### @Excel

**简介：** 标注实体字段，配合 `ExcelUtil` 控制导入导出列名、格式、字典转换、样式等。

**主要属性：**

| 属性 | 说明 |
|------|------|
| `name` | 列名 |
| `sort` | 列顺序 |
| `dateFormat` | 日期格式 |
| `readConverterExp` | 字典表达式（如 `0=男,1=女`） |
| `separator` | 多值分隔符 |
| `scale` / `roundingMode` | BigDecimal 精度 |
| `height` / `width` | 行高列宽 |
| `suffix` / `defaultValue` | 后缀 / 空值默认 |
| `prompt` / `combo` | 提示 / 下拉选项 |
| `isExport` / `type` | 是否导出 / 导入导出类型 |
| `align` / `color` | 对齐 / 颜色 |
| `handler` / `args` | 自定义格式化适配器 |

### @Excels

**简介：** `@Excel` 容器注解，同一字段多 Sheet 导出时使用。

---

## xss — XSS 防护

### @Xss

**简介：** JSR-303 校验注解，标注在 String 字段上，拒绝包含 HTML 脚本的内容。

**使用示例：**

```java
@Xss
@NotBlank
private String content;
```

### XssValidator

**简介：** `@Xss` 的校验实现，内部调用 HTML 过滤逻辑判断输入是否安全。

---

## security — 安全链扩展

### SecurityChainFilter

**简介：** Servlet 环境下 Security 链扩展点，允许在认证 Filter 前插入自定义 Filter。

**使用示例：**

```java
@Bean
public SecurityChainFilter myFilter() {
    return () -> new MyAuthenticationFilter();
}
```

**方法列表：**

| 方法 | 说明 |
|------|------|
| `filter()` | 返回要插入的 `Filter` |

### ReactiveSecurityChainFilter

**简介：** WebFlux 环境下的同类扩展点，返回 `WebFilter`。

---

## exception — 异常体系

| 异常类 | 用途 |
|--------|------|
| `BaseException` | 异常基类 |
| `ServiceException` | 通用业务异常 |
| `CheckedException` | 检查型业务异常 |
| `UtilException` | 工具类异常 |
| `NotLoginException` | 未登录 |
| `NotPermissionException` | 无权限 |
| `NotRoleException` | 无角色 |
| `PreAuthorizeException` | 预授权失败 |
| `InnerAuthException` | 内部调用鉴权失败 |
| `DemoModeException` | 演示模式禁止操作 |
| `CaptchaException` / `CaptchaExpireException` | 验证码错误 / 过期 |
| `UserException` / `UserPasswordNotMatchException` | 用户相关异常 |
| `FileException` / `FileSizeLimitExceededException` / `FileNameLengthLimitExceededException` / `InvalidExtensionException` | 文件相关异常 |
| `TaskException` | 定时任务异常 |

---

## event — 领域事件

### TicketMessageNotifyEvent

**简介：** 工单消息通知领域事件，供消息推送模块订阅。

---

## 配置与依赖说明

本模块无专属 `@ConfigurationProperties`。`DateFormatConfig` 自动生效，无需额外配置。

若使用 IP 归属地，需在 `resources/ip2region/` 下放置 `ip2region.xdb` 文件。

**依赖说明：**

- `spring-boot-starter-web` / `webflux` 为 **optional**，纯工具场景可不引入 Web 依赖
- 主要第三方库：ip2region、UserAgentUtils、Apache POI、Fastjson2、Commons IO

---

## 相关模块

- 被 [commons-log](../commons-log/README.md)、[commons-redis](../commons-redis/README.md) 直接依赖
- [commons-security](../commons-security/README.md) 通过 commons-redis 间接依赖

[← 返回总览](../README.md)
