package com.xcz.commons.excel;

import com.xcz.commons.excel.database.annotation.ExcelField;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 场次配置行数据（对应 Excel 中一行）。
 */
@Data
public class RoomConfig {

    @ExcelField(label = "场次名", index = 1, comment = "游戏场次的名称", value = "默认房间")
    private String roomName;

    @ExcelField(label = "桌费", index = 2, comment = "开局固定桌费", value = "0")
    private BigDecimal deskCoin;

    @ExcelField(label = "对局人数", index = 3, comment = "每局玩家数量", value = "4")
    private int playerNum;

    @ExcelField(label = "奖励列表", index = 4, comment = "奖励列表")
    private List<String> jiangli;

    @ExcelField(label = "牌堆", index = 5, comment = "牌堆配置")
    private Map<Long, String> poker;
}
