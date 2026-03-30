package com.track.common;

import java.sql.SQLException;

/**
 * 将异常格式化为可返回前端的详细文本（含 JDBC 链式 SQLException）。
 */
public final class ExceptionDetailUtil {

    private ExceptionDetailUtil() {
    }

    /**
     * 返回数据库驱动/JDBC 的完整错误信息（含 {@link SQLException#getNextException()} 链）。
     */
    public static String formatSqlException(SQLException e) {
        if (e == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        SQLException cur = e;
        while (cur != null) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            String m = cur.getMessage();
            if (m != null && !m.isEmpty()) {
                sb.append(m);
            } else {
                sb.append(cur.getClass().getSimpleName());
            }
            if (cur.getSQLState() != null) {
                sb.append(" [SQLState=").append(cur.getSQLState()).append("]");
            }
            if (cur.getErrorCode() != 0) {
                sb.append(" [ErrorCode=").append(cur.getErrorCode()).append("]");
            }
            cur = cur.getNextException();
        }
        return sb.toString();
    }

    /**
     * 将异常及其 {@link Throwable#getCause()} 链展开为一段文本；遇到 {@link SQLException} 时用 {@link #formatSqlException(SQLException)}。
     */
    public static String formatThrowableChain(Throwable e) {
        if (e == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 20) {
            if (sb.length() > 0) {
                sb.append(" | Cause: ");
            }
            if (cur instanceof SQLException) {
                sb.append(formatSqlException((SQLException) cur));
            } else {
                String m = cur.getMessage();
                if (m != null && !m.isEmpty()) {
                    sb.append(m);
                } else {
                    sb.append(cur.getClass().getName());
                }
            }
            cur = cur.getCause();
            depth++;
        }
        return sb.length() > 0 ? sb.toString() : e.getClass().getName();
    }
}
