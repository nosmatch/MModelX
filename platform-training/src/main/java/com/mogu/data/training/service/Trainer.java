package com.mogu.data.training.service;

import com.mogu.data.common.serving.ModelPredictor;
import com.mogu.data.training.entity.TrainingConfig;

/**
 * 训练器接口
 * 定义模型训练的通用接口，同时继承 ModelPredictor 提供推理能力
 */
public interface Trainer extends ModelPredictor {

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
}