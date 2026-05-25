#!/bin/bash
# ================================================================
# MModelX KinD 集群启动脚本
# 用于本地开发环境快速创建 K8s 集群并加载推理服务镜像
# ================================================================

set -e

CLUSTER_NAME="mmodelx"
INFERENCE_IMAGE="mmodelx-inference:latest"
KIND_CONFIG="deploy/k8s/kind-config.yaml"

echo "=========================================="
echo "MModelX KinD 集群初始化"
echo "=========================================="

# 检查 kind 是否安装
if ! command -v kind &> /dev/null; then
    echo "错误: kind 未安装，请先安装 KinD"
    echo "安装方式:"
    echo "  Mac: brew install kind"
    echo "  Linux: curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.22.0/kind-linux-amd64 && chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind"
    exit 1
fi

# 检查 kubectl 是否安装
if ! command -v kubectl &> /dev/null; then
    echo "错误: kubectl 未安装，请先安装 kubectl"
    exit 1
fi

# 检查 docker 是否运行
if ! docker info &> /dev/null; then
    echo "错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查集群是否已存在
if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
    echo "集群 ${CLUSTER_NAME} 已存在，跳过创建"
else
    echo "创建 KinD 集群: ${CLUSTER_NAME}"
    kind create cluster --name "${CLUSTER_NAME}" --config "${KIND_CONFIG}"
    echo "集群创建成功"
fi

# 设置 kubectl 上下文
echo "设置 kubectl 上下文..."
kubectl config use-context "kind-${CLUSTER_NAME}"

# 检查推理服务镜像是否存在
if ! docker image inspect "${INFERENCE_IMAGE}" &> /dev/null; then
    echo "警告: 推理服务镜像 ${INFERENCE_IMAGE} 不存在"
    echo "请先构建镜像: docker build -t ${INFERENCE_IMAGE} inference-service/"
    read -p "是否现在构建镜像? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "构建推理服务镜像..."
        docker build -t "${INFERENCE_IMAGE}" inference-service/
    else
        echo "跳过镜像加载，后续可能需要手动加载"
        exit 0
    fi
fi

# 加载镜像到 KinD 集群
echo "加载推理服务镜像到 KinD 集群..."
kind load docker-image "${INFERENCE_IMAGE}" --name "${CLUSTER_NAME}"

echo "=========================================="
echo "KinD 集群初始化完成"
echo "=========================================="
echo ""
echo "集群信息:"
kubectl cluster-info
echo ""
echo "节点列表:"
kubectl get nodes
echo ""
echo "可用命令:"
echo "  kubectl get pods -A              # 查看所有 Pod"
echo "  kubectl get ns                   # 查看所有 Namespace"
echo "  kubectl get deployments -A       # 查看所有 Deployment"
echo ""
echo "kubeconfig 路径:"
kubectl config view --minify --raw | grep server
echo ""
echo "提示: 在 application.yml 中配置 k8s.kubeconfig-path:"
echo "  k8s.kubeconfig-path: $(kubectl config view --minify --raw | grep -o '/[^\"]*config' | head -1 || echo '~/.kube/config')"
