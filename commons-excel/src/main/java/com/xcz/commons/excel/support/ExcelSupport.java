package com.xcz.commons.excel.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.cell.CellUtil;
import com.xcz.commons.excel.database.Config;
import com.xcz.commons.excel.database.ExcelContent;
import com.xcz.commons.excel.database.annotation.ExcelBase;
import com.xcz.commons.excel.database.annotation.ExcelField;
import com.xcz.commons.excel.database.annotation.ExcelSheet;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public final class ExcelSupport {

    //从第二行开始才是数据内容，第零行是标题，第一行是表头
    private static final int DATA_START_ROW = 2;

    private ExcelSupport() {
    }

    /**
     * 获取目录路径。
     *
     * @param path 文件或目录路径
     * @return 目录路径
     */
    public static String getDir(String path) {
        String parent = new File(path).getParent();
        return parent != null ? parent : path;
    }

    /**
     * 获取目录下最新版本的 Excel 文件。
     *
     * @param dir         保存目录
     * @param configClass 配置类
     * @return 最新版本文件，不存在时返回 null
     */
    public static File getLastVersionFile(String dir, Class<?> configClass) {
        ExcelBase annotation = configClass.getAnnotation(ExcelBase.class);
        if (annotation == null) {
            throw new IllegalArgumentException("该类不是Excel注解类");
        }

        String name = StringUtils.hasText(annotation.name()) ? annotation.name() : configClass.getSimpleName();
        Pattern pattern = Pattern.compile("^" + Pattern.quote(name) + "-(.+)" + Pattern.quote(ExcelContent.Suffix) + "$");

        File directory = new File(dir);
        if (!directory.exists()) {
            return null;
        }
        File[] files = directory.listFiles((d, fileName) -> pattern.matcher(fileName).matches());
        if (files == null || files.length == 0) {
            return null;
        }
        return Arrays.stream(files)
                .max(Comparator.comparing(f -> extractVersionFromFileName(f.getName(), name), versionComparator()))
                .orElse(null);
    }

    /**
     * 从文件名截取版本号。
     *
     * @param fileName 文件名
     * @param baseName 文件前缀
     * @return 版本号
     */
    private static String extractVersionFromFileName(String fileName, String baseName) {
        return fileName.substring(baseName.length() + 1, fileName.length() - ExcelContent.Suffix.length());
    }

    /**
     * 版本号比较器。
     *
     * @return 升序比较器
     */
    public static Comparator<String> versionComparator() {
        return (v1, v2) -> {
            String[] p1 = v1.split("\\.");
            String[] p2 = v2.split("\\.");
            int len = Math.max(p1.length, p2.length);
            for (int i = 0; i < len; i++) {
                int n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
                int n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;
                if (n1 != n2) return Integer.compare(n1, n2);
            }
            return 0;
        };
    }

    /**
     * 读取版本历史数据行。
     *
     * @param file Excel 文件
     * @return 数据行（不含标题行和表头行）
     */
    public static List<List<Object>> lastVersionConfig(File file) {
        try (ExcelReader reader = ExcelUtil.getReader(file, ExcelContent.DEFAULT_SHEET)) {
            List<List<Object>> read = reader.read();
            if (!read.isEmpty()) {
                read.removeFirst();
            }
            if (!read.isEmpty()) {
                read.removeFirst();
            }
            return read;
        }
    }

    /**
     * 创建 Excel 文件。
     *
     * @param path        目录路径
     * @param configClass 配置类
     */
    public static void createExcel(String path, Class<?> configClass) {
        String dir = getDir(path);
        //获取文件注解
        ExcelBase annotation = configClass.getAnnotation(ExcelBase.class);
        if (annotation == null) {
            throw new IllegalArgumentException("该类不是Excel注释类");
        }

        String version = annotation.version();
        String name = StringUtils.hasText(annotation.name()) ? annotation.name() : configClass.getSimpleName();
        String excelPath = Paths.get(dir, name + "-" + version + ExcelContent.Suffix).toString();

        File destFile = new File(excelPath);
        List<List<Object>> init = new ArrayList<>();
        //如果文件不存在，则创建，否则返回
        if (!destFile.exists()) {
            //获取最新版本文件，获取历史版本信息
            File lastVersionFile = getLastVersionFile(dir, configClass);
            if (lastVersionFile != null) {
                init.addAll(lastVersionConfig(lastVersionFile));
            }
        } else {
            log.info("Excel文件已经存在：{}", excelPath);
            return;
        }
        //创建excel固定初始化信息
        ExcelWriter writer = ExcelUtil.getWriter(destFile);
        writer.setSheet(ExcelContent.DEFAULT_SHEET);
        List<Object> titles = new ArrayList<>();
        titles.add(ExcelContent.VERSION);
        titles.add(ExcelContent.COMMENT);
        titles.add(ExcelContent.TIMESTAMP);
        writer.merge(titles.size() - 1, "版本配置");
        init.addFirst(titles);

        List<Object> values = new ArrayList<>();
        values.add(version);
        values.add(annotation.comment());
        values.add(DateUtil.formatLocalDateTime(LocalDateTime.now()));
        init.addLast(values);

        writer.write(init, true);
        writer.setColumnWidth(-1, 15);
        createExcel(writer, configClass);

        removeDefaultSheet(writer);
        writer.close();
    }

    /**
     * 写入配置类对应的 Sheet 页。
     *
     * @param writer      Excel 写入器
     * @param configClass 配置类
     */
    public static void createExcel(ExcelWriter writer, Class<?> configClass) {
        //遍历配置类的变量进行配置
        for (Field field : configClass.getDeclaredFields()) {
            //获取变量注解
            ExcelSheet annotation = field.getAnnotation(ExcelSheet.class);
            if (annotation == null) {
                continue;
            }
            //设置写入的sheet
            writer.setSheet(annotation.sheet());
            String groupTitle = StringUtils.hasText(annotation.comment()) ? annotation.comment() : field.getName();
            int startColumn = annotation.startList();
            int headerRow = 1;

            if (Map.class.isAssignableFrom(field.getType())) {
                writeMapSheet(writer, field, annotation, groupTitle, startColumn, headerRow);
            } else if (List.class.isAssignableFrom(field.getType())) {
                writeListSheet(writer, field, groupTitle, startColumn, headerRow);
            }
        }
    }

    /**
     * 写入 Map 类型 Sheet。
     *
     * @param writer      Excel 写入器
     * @param field       Map 字段
     * @param annotation  Sheet 注解
     * @param groupTitle  分组标题
     * @param startColumn 起始列
     * @param headerRow   表头行
     */
    private static void writeMapSheet(ExcelWriter writer, Field field, ExcelSheet annotation,
                                      String groupTitle, int startColumn, int headerRow) {
        //获取具体泛型类型
        List<Class<?>> types = ClassAnalyseSupport.analyse(field);
        Class<?> keyType = types.get(0);
        Class<?> valueType = types.get(1);
        //判读那是否都为基本类型
        if (!ClassAnalyseSupport.isBasicType(keyType) || !ClassAnalyseSupport.isBasicType(valueType)) {
            throw new IllegalArgumentException(
                    "Map 的 key 和 value 必须为基本类型，字段: " + field.getName()
                            + "，当前: " + keyType.getSimpleName() + " -> " + valueType.getSimpleName());
        }
        //合并标题栏
        int endColumn = startColumn + 1;
        writer.merge(0, 0, startColumn, endColumn, groupTitle, true);
        //获取键和值的表头并写入对应的单元格
        String keyLabel = StringUtils.hasText(annotation.mapKey()) ? annotation.mapKey() : field.getName();
        String valueLabel = StringUtils.hasText(annotation.mapValue()) ? annotation.mapValue() : field.getName();
        writer.writeCellValue(startColumn, headerRow, keyLabel, true);
        writer.setColumnWidth(startColumn, 15);
        writer.writeCellValue(startColumn + 1, headerRow, valueLabel, true);
        writer.setColumnWidth(startColumn + 1, 15);
    }

    /**
     * 写入 List 类型 Sheet。
     *
     * @param writer      Excel 写入器
     * @param field       List 字段
     * @param groupTitle  分组标题
     * @param startColumn 起始列
     * @param headerRow   表头行
     */
    private static void writeListSheet(ExcelWriter writer, Field field, String groupTitle,
                                       int startColumn, int headerRow) {
        //获取具体泛型类型
        Class<?> elementType = ClassAnalyseSupport.analyse(field).getFirst();
        //如果是基本类型，直接该列写入数据
        if (ClassAnalyseSupport.isBasicType(elementType)) {
            writer.writeCellValue(startColumn, 0, groupTitle, true);
            writer.writeCellValue(startColumn, headerRow, field.getName(), true);
            writer.setColumnWidth(startColumn, 15);
            return;
        }
        //获取有注解变量
        List<Field> excelFields = Arrays.stream(elementType.getDeclaredFields())
                .filter(it -> it.getAnnotation(ExcelField.class) != null)
                .sorted(Comparator.comparingInt(it -> it.getAnnotation(ExcelField.class).index()))
                .toList();
        if (excelFields.isEmpty()) {
            return;
        }
        //合并标题行
        int endColumn = startColumn + excelFields.size() - 1;
        writer.merge(0, 0, startColumn, endColumn, groupTitle, true);
        //逐个设置表头
        for (Field excelField : excelFields) {
            ExcelField filed = excelField.getAnnotation(ExcelField.class);
            int col = startColumn + filed.index() - 1;
            String label = StringUtils.hasText(filed.label()) ? filed.label() : excelField.getName();
            writer.writeCellValue(col, headerRow, label, true);

            if (StringUtils.hasText(filed.comment())) {
                Cell cell = writer.getSheet().getRow(headerRow).getCell(col);
                CellUtil.setComment(cell, filed.comment(), "system", null);
            }
            writer.setColumnWidth(col, 15);
        }
    }

    /**
     * 删除默认 sheet1。
     *
     * @param writer Excel 写入器
     */
    private static void removeDefaultSheet(ExcelWriter writer) {
        Workbook workbook = writer.getWorkbook();
        int index = workbook.getSheetIndex("sheet1");
        if (index >= 0 && workbook.getNumberOfSheets() > 1) {
            workbook.removeSheetAt(index);
        }
    }

    /**
     * 读取 Excel 并映射为配置对象。
     *
     * @param path        目录或文件路径
     * @param configClass 配置类
     * @param <T>         配置类型
     * @return 配置实例
     */
    public static <T> T readExcel(String path, Class<T> configClass) {
        String excelPath = resolveExcelPath(path, configClass);
        File file = new File(excelPath);
        if (!file.exists()) {
            createExcel(path, configClass);
            log.warn("Excel文件不存在，进行创建：{}",excelPath);
        }
        try (ExcelReader reader = ExcelUtil.getReader(file)) {
            return readExcel(reader, configClass);
        }
    }

    /**
     * 解析 Excel 文件路径。
     *
     * @param path        目录或文件路径
     * @param configClass 配置类
     * @return Excel 文件路径
     */
    public static String resolveExcelPath(String path, Class<?> configClass) {
        String dir = getDir(path);
        //获取配置注解
        ExcelBase annotation = configClass.getAnnotation(ExcelBase.class);
        if (annotation == null) {
            throw new IllegalArgumentException("该类不是Excel类: " + configClass.getName());
        }
        String version = annotation.version();
        String name = StringUtils.hasText(annotation.name()) ? annotation.name() : configClass.getSimpleName();
        return Paths.get(dir, name + "-" + version + ExcelContent.Suffix).toString();
    }

    /**
     * 根据配置项解析 Excel 文件。
     *
     * @param config 配置项
     * @return Excel 文件
     */
    public static File resolveExcelFile(Config config) {
        try {
            Class<?> clazz = Class.forName(config.getClazz());
            File file = new File(resolveExcelPath(config.getPath(), clazz));
            if (!file.exists()) {
                throw new IllegalArgumentException("Excel文件不存在: " + file.getAbsolutePath());
            }
            return file;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("类不存在: " + config.getClazz(), e);
        }
    }

    /**
     * 从 Reader 读取配置对象。
     *
     * @param reader      Excel 读取器
     * @param configClass 配置类
     * @param <T>         配置类型
     * @return 配置实例
     */
    private static <T> T readExcel(ExcelReader reader, Class<T> configClass) {
        T instance = createInstance(configClass);
        readExcelSheet(reader, instance, configClass);
        return instance;
    }

    /**
     * 读取配置类中所有 Sheet 字段。
     *
     * @param reader      Excel 读取器
     * @param instance    配置实例
     * @param configClass 配置类
     */
    private static void readExcelSheet(ExcelReader reader, Object instance, Class<?> configClass) {
        //逐个处理字段的数据读取
        for (Field field : configClass.getDeclaredFields()) {
            ExcelSheet annotation = field.getAnnotation(ExcelSheet.class);
            if (annotation == null) {
                continue;
            }
            reader.setSheet(annotation.sheet());
            field.setAccessible(true);
            try {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.set(instance, readExcelMap(reader, field, annotation));
                } else if (List.class.isAssignableFrom(field.getType())) {
                    field.set(instance, readExcelList(reader, field, annotation.startList()));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("设置字段失败: " + field.getName(), e);
            }
        }
    }

    /**
     * 读取 Map 类型字段。
     *
     * @param reader     Excel 读取器
     * @param field      Map 字段
     * @param annotation Sheet 注解
     * @return Map 数据
     */
    private static Map<Object, Object> readExcelMap(ExcelReader reader, Field field, ExcelSheet annotation) {
        //获取起始列以及对应的表头名
        int startColumn = annotation.startList();
        List<Class<?>> types = ClassAnalyseSupport.analyse(field);
        Class<?> keyType = types.get(0);
        Class<?> valueType = types.get(1);

        //从第二行开始读取数据
        List<List<Object>> rows = reader.read(DATA_START_ROW);
        Map<Object, Object> map = new LinkedHashMap<>();
        //逐行转换数据
        for (List<Object> row : rows) {
            if (isEmptyRow(row, startColumn, startColumn + 1)) {
                break;
            }
            Object key = CellValueConverter.convertValue(getCell(row, startColumn), keyType);
            Object value = CellValueConverter.convertValue(getCell(row, startColumn + 1), valueType);
            if (key != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * 读取 List 类型字段。
     *
     * @param reader      Excel 读取器
     * @param field       List 字段
     * @param startColumn 起始列
     * @return List 数据
     */
    private static List<Object> readExcelList(ExcelReader reader, Field field, int startColumn) {
        Class<?> elementType = ClassAnalyseSupport.analyse(field).getFirst();
        if (ClassAnalyseSupport.isBasicType(elementType)) {
            return readBasicList(reader, startColumn, elementType);
        }
        return readObjectList(reader, startColumn, elementType);
    }

    /**
     * 读取基本类型 List。
     *
     * @param reader      Excel 读取器
     * @param startColumn 起始列
     * @param elementType 元素类型
     * @return List 数据
     */
    private static List<Object> readBasicList(ExcelReader reader, int startColumn, Class<?> elementType) {
        List<Object> columnValues = reader.readColumn(startColumn, DATA_START_ROW);
        List<Object> result = new ArrayList<>();
        for (Object value : columnValues) {
            if (CellValueConverter.isBlank(value)) {
                break;
            }
            result.add(CellValueConverter.convertValue(value, elementType));
        }
        return result;
    }

    /**
     * 读取对象类型 List。
     *
     * @param reader      Excel 读取器
     * @param startColumn 起始列
     * @param elementType 元素类型
     * @return List 数据
     */
    private static List<Object> readObjectList(ExcelReader reader, int startColumn, Class<?> elementType) {
        List<Field> excelFields = getExcelFields(elementType);
        if (excelFields.isEmpty()) {
            return List.of();
        }

        List<List<Object>> rows = reader.read(DATA_START_ROW);
        List<Object> result = new ArrayList<>();
        for (List<Object> row : rows) {
            if (isEmptyDataRow(row, startColumn, excelFields)) {
                break;
            }
            Object element = createInstance(elementType);
            for (Field excelField : excelFields) {
                ExcelField filed = excelField.getAnnotation(ExcelField.class);
                int col = startColumn + filed.index() - 1;
                Object cellValue = getCell(row, col);
                Object converted = CellValueConverter.convertFieldValue(cellValue, excelField);
                if (converted == null && StringUtils.hasText(filed.value())) {
                    converted = CellValueConverter.convertFieldValue(filed.value(), excelField);
                }
                excelField.setAccessible(true);
                try {
                    excelField.set(element, converted);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("设置字段失败: " + excelField.getName(), e);
                }
            }
            result.add(element);
        }
        return result;
    }

    /**
     * 获取带 ExcelField 注解的字段列表。
     *
     * @param elementType 元素类型
     * @return 字段列表
     */
    private static List<Field> getExcelFields(Class<?> elementType) {
        return Arrays.stream(elementType.getDeclaredFields())
                .filter(it -> it.getAnnotation(ExcelField.class) != null)
                .sorted(Comparator.comparingInt(it -> it.getAnnotation(ExcelField.class).index()))
                .toList();
    }

    /**
     * 获取单元格值。
     *
     * @param row         行数据
     * @param columnIndex 列索引
     * @return 单元格值，越界时返回 null
     */
    private static Object getCell(List<Object> row, int columnIndex) {
        if (row == null || columnIndex < 0 || columnIndex >= row.size()) {
            return null;
        }
        return row.get(columnIndex);
    }

    /**
     * 判断什么时候
     * @param row 行
     * @param columns   列
     * @return  是否为空行
     */
    private static boolean isEmptyRow(List<Object> row, int... columns) {
        for (int column : columns) {
            if (!CellValueConverter.isBlank(getCell(row, column))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断对象行映射列是否全为空。
     *
     * @param row         行数据
     * @param startColumn 起始列
     * @param excelFields 映射字段
     * @return 是否全为空
     */
    private static boolean isEmptyDataRow(List<Object> row, int startColumn, List<Field> excelFields) {
        for (Field excelField : excelFields) {
            ExcelField filed = excelField.getAnnotation(ExcelField.class);
            int col = startColumn + filed.index() - 1;
            if (!CellValueConverter.isBlank(getCell(row, col))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 创建配置实例。
     *
     * @param clazz 类型
     * @param <T>   实例类型
     * @return 实例
     */
    private static <T> T createInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建实例: " + clazz.getName(), e);
        }
    }

}
