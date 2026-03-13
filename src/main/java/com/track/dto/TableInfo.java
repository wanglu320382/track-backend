package com.track.dto;

import java.util.List;

/**
 * 表信息 DTO
 */
public class TableInfo {
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;

    public TableInfo() {}

    public TableInfo(String tableName, String tableComment, List<ColumnInfo> columns) {
        this.tableName = tableName;
        this.tableComment = tableComment;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }
}
