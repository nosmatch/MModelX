package com.mogu.data.feature.datasource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Data Source Adapter Interface
 *
 * 数据源适配器接口，统一不同数据源的访问方式
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
public interface DataSourceAdapter {

    /**
     * 读取数据
     *
     * @param config 数据源配置 (JSON格式)
     * @param partitionDate 分区日期
     * @return 按entity_id分组的数据
     *         Map<entity_id, List<记录>>
     */
    Map<String, List<Map<String, Object>>> readData(String config, LocalDate partitionDate);

    /**
     * 测试连接
     *
     * @param config 数据源配置
     * @return 是否连接成功
     */
    boolean testConnection(String config);

    /**
     * 获取数据源描述
     *
     * @return 数据源类型描述
     */
    String getDescription();
}
