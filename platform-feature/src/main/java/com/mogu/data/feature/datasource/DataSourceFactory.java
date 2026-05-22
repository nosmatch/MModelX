package com.mogu.data.feature.datasource;

import com.mogu.data.common.logger.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Map;

/**
 * Data Source Factory
 *
 * 数据源适配器工厂，根据类型返回对应的适配器
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
public class DataSourceFactory {

    private static final Logger log = Logger.getLogger(DataSourceFactory.class);

    private final PostgreSqlDataSourceAdapter postgreSqlAdapter;
    private final MinioDataSourceAdapter minioAdapter;
    private final KafkaDataSourceAdapter kafkaAdapter;
    private final RedisDataSourceAdapter redisAdapter;
    private final ApiDataSourceAdapter apiAdapter;
    private final LocalFileDataSourceAdapter localFileAdapter;

    private final Map<DataSourceType, DataSourceAdapter> adapterMap = new EnumMap<>(DataSourceType.class);

    /**
     * 构造函数注入所有适配器
     */
    public DataSourceFactory(
            PostgreSqlDataSourceAdapter postgreSqlAdapter,
            MinioDataSourceAdapter minioAdapter,
            KafkaDataSourceAdapter kafkaAdapter,
            RedisDataSourceAdapter redisAdapter,
            ApiDataSourceAdapter apiAdapter,
            LocalFileDataSourceAdapter localFileAdapter) {

        this.postgreSqlAdapter = postgreSqlAdapter;
        this.minioAdapter = minioAdapter;
        this.kafkaAdapter = kafkaAdapter;
        this.redisAdapter = redisAdapter;
        this.apiAdapter = apiAdapter;
        this.localFileAdapter = localFileAdapter;
    }

    /**
     * 初始化适配器映射
     */
    @PostConstruct
    public void init() {
        adapterMap.put(DataSourceType.POSTGRESQL, postgreSqlAdapter);
        adapterMap.put(DataSourceType.MYSQL, postgreSqlAdapter); // MySQL复用PostgreSQL适配器（JDBC SQL兼容）
        adapterMap.put(DataSourceType.MINIO, minioAdapter);
        adapterMap.put(DataSourceType.KAFKA, kafkaAdapter);
        adapterMap.put(DataSourceType.REDIS, redisAdapter);
        adapterMap.put(DataSourceType.API, apiAdapter);
        adapterMap.put(DataSourceType.LOCAL_FILE, localFileAdapter);
    }

    /**
     * 获取数据源适配器
     *
     * @param type 数据源类型
     * @return 对应的适配器
     */
    public DataSourceAdapter getAdapter(DataSourceType type) {
        DataSourceAdapter adapter = adapterMap.get(type);
        if (adapter == null) {
            throw new UnsupportedOperationException(
                "Data source type not supported: " + type
            );
        }
        return adapter;
    }

    /**
     * 测试数据源连接
     *
     * @param type 数据源类型
     * @param config 配置
     * @return 是否连接成功
     */
    public boolean testConnection(DataSourceType type, String config) {
        try {
            return getAdapter(type).testConnection(config);
        } catch (Exception e) {
            log.error("Failed to test connection for type: {}", type, e);
            return false;
        }
    }
}
