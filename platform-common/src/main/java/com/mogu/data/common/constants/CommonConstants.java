package com.mogu.data.common.constants;

/**
 * 通用常量
 */
public class CommonConstants {

    /**
     * 成功码
     */
    public static final String SUCCESS_CODE = "200";

    /**
     * 成功消息
     */
    public static final String SUCCESS_MESSAGE = "success";

    /**
     * 失败码
     */
    public static final String ERROR_CODE = "500";

    /**
     * UTF-8编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 线程池核心大小
     */
    public static final int CORE_POOL_SIZE = 4;

    /**
     * 线程池最大大小
     */
    public static final int MAX_POOL_SIZE = 8;

    /**
     * 线程池队列大小
     */
    public static final int QUEUE_CAPACITY = 100;

    /**
     * 特征存储桶名
     */
    public static final String FEATURE_BUCKET = "features";

    /**
     * 模型存储桶名
     */
    public static final String MODEL_BUCKET = "models";

    /**
     * 数据集存储桶名
     */
    public static final String DATASET_BUCKET = "datasets";

    /**
     * 在线特征缓存前缀
     */
    public static final String ONLINE_FEATURE_PREFIX = "feature:";

    /**
     * 模型缓存前缀
     */
    public static final String MODEL_CACHE_PREFIX = "model:";
}