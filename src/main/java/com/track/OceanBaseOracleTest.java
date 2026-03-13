package com.track;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OceanBaseOracleTest {
    public static void main(String[] args) {
        // 绕过代理
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("http.nonProxyHosts", "*.oceanbase.cloud|*.aliyuncs.com");
        System.setProperty("https.nonProxyHosts", "*.oceanbase.cloud|*.aliyuncs.com");

        // ===================== 【Oracle 模式配置】=====================
        // 驱动（OceanBase Oracle 模式）
        String driverClass = "com.oceanbase.jdbc.Driver";

        // Oracle 模式 URL
        String url = "jdbc:oceanbase://obot7key8ys465s0-mi.aliyun-cn-hangzhou-internet.oceanbase.cloud:1521/TRACK";

        // 用户名（你给的：TRACK）
        String user = "TRACK@obot7key8ys465s0";

        // 密码不变
        String password = "LInkage@@12345";
        // ===========================================================

        Connection conn = null;
        try {
            Class.forName(driverClass);
            System.out.println("✅ 驱动加载成功");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("🎉 Oracle 模式 OceanBase 连接成功！");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ 缺少 oceanbase-client 驱动！");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ 连接失败！");
            System.err.println("错误码：" + e.getErrorCode());
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("🔌 连接已关闭");
                } catch (SQLException e) {}
            }
        }
    }
}