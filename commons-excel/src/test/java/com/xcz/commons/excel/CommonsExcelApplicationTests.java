package com.xcz.commons.excel;

import com.xcz.commons.excel.support.ExcelSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Excel 读写核心功能测试（不依赖 Spring 容器）。
 */
class CommonsExcelApplicationTests {

    @TempDir
    Path tempDir;

    @Test
    void createAndReadExcel() {
        String dir = tempDir.toString();
        ExcelSupport.createExcel(dir, GameConfig.class);

        GameConfig config = ExcelSupport.readExcel(dir, GameConfig.class);
        assertNotNull(config);
        assertNotNull(config.getRoomConfigs());
    }

    @Test
    void versionComparatorOrdersCorrectly() {
        var comparator = ExcelSupport.versionComparator();
        assertTrue(comparator.compare("1.0.1", "1.0.0") > 0);
        assertTrue(comparator.compare("2.0.0", "1.9.9") > 0);
    }
}
