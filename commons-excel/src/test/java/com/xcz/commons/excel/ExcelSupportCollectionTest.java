package com.xcz.commons.excel;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.xcz.commons.excel.database.annotation.ExcelField;
import com.xcz.commons.excel.support.ExcelSupport;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExcelSupportCollectionTest {

    @TempDir
    Path tempDir;

    @Data
    static class CollectionHolder {
        @ExcelField(index = 1)
        private List<String> items;

        @ExcelField(index = 2)
        private Set<Integer> tags;

        @ExcelField(index = 3)
        private Map<Long, String> mapping;
    }

    @Test
    void parseListSetMapCellFormats() throws Exception {
        Method method = ExcelSupport.class.getDeclaredMethod("convertFieldValue", Object.class, Field.class);
        method.setAccessible(true);

        Field listField = CollectionHolder.class.getDeclaredField("items");
        Field setField = CollectionHolder.class.getDeclaredField("tags");
        Field mapField = CollectionHolder.class.getDeclaredField("mapping");

        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) method.invoke(null, "{gold,silver}", listField);
        assertEquals(List.of("gold", "silver"), list);

        @SuppressWarnings("unchecked")
        Set<Integer> set = (Set<Integer>) method.invoke(null, "{1,2,3}", setField);
        assertEquals(Set.of(1, 2, 3), set);

        @SuppressWarnings("unchecked")
        Map<Long, String> map = (Map<Long, String>) method.invoke(null, "{1:方片,2:梅花}", mapField);
        assertEquals("方片", map.get(1L));
        assertEquals("梅花", map.get(2L));
    }

    @Test
    void readListAndMapFromExcelRow() {
        String dir = tempDir.toString();
        ExcelSupport.createExcel(dir, GameConfig.class);
        String excelPath = ExcelSupport.resolveExcelPath(dir, GameConfig.class);

        try (ExcelWriter writer = ExcelUtil.getWriter(excelPath)) {
            writer.setSheet("场次配置");
            int row = 2;
            writer.writeCellValue(0, row, "测试房间");
            writer.writeCellValue(1, row, 10);
            writer.writeCellValue(2, row, 4);
            writer.writeCellValue(3, row, "{gold,silver,copper}");
            writer.writeCellValue(4, row, "{1:方片,2:梅花}");
        }

        GameConfig config = ExcelSupport.readExcel(dir, GameConfig.class);
        RoomConfig room = config.getRoomConfigs().getFirst();
        assertEquals(List.of("gold", "silver", "copper"), room.getJiangli());
        assertEquals("方片", room.getPoker().get(1L));
        assertEquals("梅花", room.getPoker().get(2L));
    }
}
