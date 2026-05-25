package com.mogu.data.common.serving;

import java.util.List;
import java.util.Map;

/**
 * 模型推理接口
 * 与训练模块解耦，Serving 服务只依赖此接口，不依赖具体的训练实现
 */
public interface ModelPredictor {

    /**
     * 支持的模型框架类型标识
     * @return "lightgbm" 或 "xgboost"
     */
    String getFrameworkType();

    /**
     * 从文件路径加载模型到内存
     * @param modelPath 模型文件路径
     * @return 框架特定的模型对象
     */
    Object loadModel(String modelPath);

    /**
     * 执行推理
     * @param model loadModel 返回的模型对象
     * @param features 特征向量 (key=特征名, value=特征值)
     * @return 预测结果列表
     */
    List<Double> predict(Object model, Map<String, Object> features);

    /**
     * 释放模型资源（可选，用于显式卸载）
     * @param model 模型对象
     */
    default void unloadModel(Object model) {
        // 默认空实现
    }
}
