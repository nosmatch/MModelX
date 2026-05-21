package com.mogu.data.training.service;

import com.mogu.data.training.entity.TrainingConfig;

/**
 * 训练器接口
 * 定义模型训练的通用接口
 */
public interface Trainer {

    /**
     * 训练模型
     * @param config 训练配置
     * @return 模型路径
     */
    String train(TrainingConfig config);

    /**
     * 验证模型
     * @param modelPath 模型路径
     * @param config 训练配置
     * @return 验证结果
     */
    java.util.Map<String, Object> validate(String modelPath, TrainingConfig config);

    /**
     * 保存模型
     * @param modelPath 模型路径
     * @param modelName 模型名称
     * @return 保存后的路径
     */
    String saveModel(String modelPath, String modelName);

    /**
     * 加载模型
     * @param modelPath 模型路径
     * @return 模型对象
     */
    Object loadModel(String modelPath);

    /**
     * 预测
     * @param model 模型对象
     * @param features 特征数据
     * @return 预测结果
     */
    java.util.List<Double> predict(Object model, java.util.Map<String, Object> features);
}