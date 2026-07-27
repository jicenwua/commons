package com.xcz.commons.excel;

import com.xcz.commons.excel.database.ExcelType;
import com.xcz.commons.excel.database.annotation.ExcelBase;
import com.xcz.commons.excel.database.annotation.ExcelSheet;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 游戏 Excel 配置总类（一个文件对应一个 GameConfig）。
 */
@Data
@ExcelBase(version = "1.0.0")
public class GameConfig {

    @ExcelSheet(sheet = "场次配置", startList = 0, comment = "场次配置")
    private List<RoomConfig> roomConfigs;

    @ExcelSheet(sheet = "表情配置", type = ExcelType.Map, startList = 0, comment = "表情配置", mapKey = "编号", mapValue = "表情名称")
    private Map<Long, String> emojiConfigs;

    @ExcelSheet(sheet = "场次配置", startList = 5, comment = "测试列")
    private List<String> test;
}
