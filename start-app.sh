#!/bin/bash

# MModelX 启动脚本
# 用于快速启动应用并验证功能

set -e

echo "=========================================="
echo "MModelX Platform 启动脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 获取脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${YELLOW}步骤 1/6: 检查Docker服务...${NC}"
docker ps | grep mmodelx-postgres > /dev/null 2>&1 && echo -e "${GREEN}✓ PostgreSQL 运行正常${NC}" || echo -e "${RED}✗ PostgreSQL 未运行${NC}"
docker ps | grep mmodelx-redis > /dev/null 2>&1 && echo -e "${GREEN}✓ Redis 运行正常${NC}" || echo -e "${RED}✗ Redis 未运行${NC}"
echo ""

echo -e "${YELLOW}步骤 2/6: 检查数据库连接...${NC}"
docker exec mmodelx-postgres pg_isready -U mmodelx > /dev/null 2>&1 && echo -e "${GREEN}✓ 数据库连接正常${NC}" || echo -e "${RED}✗ 数据库连接失败${NC}"
echo ""

echo -e "${YELLOW}步骤 3/6: 检查数据库表...${NC}"
TABLE_COUNT=$(docker exec mmodelx-postgres psql -U mmodelx -d mmodelx -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';" 2>/dev/null || echo "0")
echo "找到 $TABLE_COUNT 个表"
docker exec mmodelx-postgres psql -U mmodelx -d mmodelx -c "\dt" 2>/dev/null || echo -e "${RED}无法查询表列表${NC}"
echo ""

echo -e "${YELLOW}步骤 4/6: 编译项目...${NC}"
cd platform-api
mvn clean compile -DskipTests -q && echo -e "${GREEN}✓ 编译成功${NC}" || echo -e "${RED}✗ 编译失败${NC}"
echo ""

echo -e "${YELLOW}步骤 5/6: 打包应用...${NC}"
mvn package -DskipTests -q && echo -e "${GREEN}✓ 打包成功${NC}" || echo -e "${RED}✗ 打包失败${NC}"
echo ""

echo -e "${YELLOW}步骤 6/6: 启动应用...${NC}"
echo "正在启动Spring Boot应用..."
echo "应用将在后台启动，日志输出到: ../logs/app-startup.log"
echo ""
echo "监控日志命令: tail -f ../logs/app-startup.log"
echo "停止应用: pkill -f 'platform-api'"
echo ""

# 停止可能存在的旧进程
pkill -f "platform-api.*jar" 2>/dev/null || true

# 启动应用
nohup java -jar target/platform-api-1.0.0-SNAPSHOT.jar > ../logs/app-startup.log 2>&1 &
APP_PID=$!

echo -e "${GREEN}应用已启动，PID: $APP_PID${NC}"
echo ""

# 等待应用启动
echo "等待应用启动 (30秒)..."
for i in {1..30}; do
    sleep 1
    echo -n "."

    # 检查应用是否启动成功
    if curl -s http://localhost:8089/actuator/health > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}✓ 应用启动成功！${NC}"
        echo ""
        echo "=========================================="
        echo "应用信息"
        echo "=========================================="
        echo "PID: $APP_PID"
        echo "健康检查: http://localhost:8089/actuator/health"
        echo "API文档: http://localhost:8089/swagger-ui/index.html"
        echo "日志文件: ../logs/app-startup.log"
        echo ""

        # 测试登录API
        echo "=========================================="
        echo "测试认证API"
        echo "=========================================="
        echo "测试用户登录..."
        LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8089/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"username": "admin", "password": "admin123"}' || echo '{"error": "API调用失败"}')

        if echo "$LOGIN_RESPONSE" | grep -q "token"; then
            echo -e "${GREEN}✓ 登录API测试成功${NC}"
            echo "$LOGIN_RESPONSE" | head -n 20
        else
            echo -e "${RED}✗ 登录API测试失败${NC}"
            echo "$LOGIN_RESPONSE"
        fi

        echo ""
        echo "=========================================="
        echo "下一步操作"
        echo "=========================================="
        echo "1. 访问API文档测试其他接口"
        echo "2. 查看应用日志: tail -f ../logs/app-startup.log"
        echo "3. 停止应用: kill $APP_PID"
        echo ""

        exit 0
    fi
done

echo ""
echo -e "${RED}应用启动超时，请检查日志: ../logs/app-startup.log${NC}"
tail -n 50 ../logs/app-startup.log
exit 1
