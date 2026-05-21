package com.mogu.data.feature.datasource;

/**
 * Data Source Type Enum
 *
 * 支持的数据源类型
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
public enum DataSourceType {

    /**
     * PostgreSQL数据库
     */
    POSTGRESQL("postgresql"),

    /**
     * MySQL数据库
     */
    MYSQL("mysql"),

    /**
     * MinIO对象存储 (Parquet/CSV/JSON)
     */
    MINIO("minio"),

    /**
     * Kafka消息队列
     */
    KAFKA("kafka"),

    /**
     * Redis数据
     */
    REDIS("redis"),

    /**
     * API接口
     */
    API("api"),

    /**
     * 本地文件 (CSV/JSON/Excel)
     */
    LOCAL_FILE("local_file");

    private final String code;

    DataSourceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 从字符串获取数据源类型
     */
    public static DataSourceType fromString(String code) {
        for (DataSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown data source type: " + code);
    }
}
