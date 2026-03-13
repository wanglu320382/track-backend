package com.track.dto;

/**
 * 列信息 DTO
 */
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private String columnType;
    private String columnComment;
    private String columnKey;
    private String nullable;
    private Boolean primaryKey;

    public ColumnInfo() {}

    public ColumnInfo(String columnName, String dataType, String columnComment, Boolean primaryKey) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.columnComment = columnComment;
        this.primaryKey = primaryKey;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }

    public String getNullable() {
        return nullable;
    }

    public void setNullable(String nullable) {
        this.nullable = nullable;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }
}
