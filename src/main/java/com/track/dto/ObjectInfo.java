package com.track.dto;

import java.util.List;

/**
 * 表信息 DTO
 */
public class ObjectInfo {
    private String objectName;
    private String objectComment;
    private List<ColumnInfo> columns;

    public ObjectInfo() {}

    public ObjectInfo(String objectName, String objectComment, List<ColumnInfo> columns) {
        this.objectName = objectName;
        this.objectComment = objectComment;
        this.columns = columns;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getObjectComment() {
        return objectComment;
    }

    public void setObjectComment(String objectComment) {
        this.objectComment = objectComment;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }
}
