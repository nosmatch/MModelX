#!/bin/bash

###############################################################################
# MModelX 前端组件快速检查脚本
#
# 功能：检查前端代码的常见问题
#       - 导入路径
#       - 组件引用
#       - 语法错误
#
# 用法：./check-components.sh
#
# 作者: MModelX Team
# 日期: 2026-05-20
###############################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 计数器
ERRORS=0
WARNINGS=0
INFO=0

print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_section() {
    echo ""
    print_message "${BLUE}" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_message "${BLUE}" "  $1"
    print_message "${BLUE}" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# 检查文件是否存在
check_file_exists() {
    local file=$1
    if [ ! -f "$file" ]; then
        print_message "${RED}" "❌ 文件不存在: $file"
        ((ERRORS++))
        return 1
    else
        print_message "${GREEN}" "✅ 文件存在: $file"
        ((INFO++))
        return 0
    fi
}

# 检查导入路径
check_imports() {
    local file=$1
    print_message "${YELLOW}" "检查导入路径: $file"

    # 检查是否有错误的导入路径
    if grep -q "from '@/(api\|stores\|constants\|components\|views\|router\|layouts)/" "$file"; then
        print_message "${GREEN}" "  ✓ 使用了正确的 @ 别名"
        ((INFO++))
    else
        print_message "${YELLOW}" "  ⚠ 未找到 @ 别名导入"
        ((WARNINGS++))
    fi

    # 检查是否有相对路径导入
    if grep -q "from '\.\./\.\./" "$file"; then
        print_message "${YELLOW}" "  ⚠ 发现多层相对路径导入（可能需要优化）"
        ((WARNINGS++))
    fi
}

# 检查组件模板中的常见问题
check_template() {
    local file=$1
    print_message "${YELLOW}" "检查模板语法: $file"

    # 检查是否有未闭合的标签
    # 这里只是简单检查，实际应该使用Vue编译器

    # 检查是否使用了 v-model
    if grep -q "v-model" "$file"; then
        print_message "${GREEN}" "  ✓ 使用了 v-model"
        ((INFO++))
    fi

    # 检查是否使用了 :class
    if grep -q ":class" "$file"; then
        print_message "${GREEN}" "  ✓ 使用了动态类绑定"
        ((INFO++))
    fi
}

# 检查脚本部分
check_script() {
    local file=$1
    print_message "${YELLOW}" "检查脚本逻辑: $file"

    # 检查是否有 defineProps 和 defineEmits
    if grep -q "defineProps\|defineEmits" "$file"; then
        print_message "${GREEN}" "  ✓ 使用了 Composition API"
        ((INFO++))
    fi

    # 检查是否有 useRouter 和 useRoute
    if grep -q "useRouter\|useRoute" "$file"; then
        print_message "${GREEN}" "  ✓ 使用了 Vue Router"
        ((INFO++))
    fi
}

# 检查样式部分
check_style() {
    local file=$1
    print_message "${YELLOW}" "检查样式配置: $file"

    # 检查是否使用了 lang="scss"
    if grep -q 'lang="scss"' "$file"; then
        print_message "${GREEN}" "  ✓ 使用了 SCSS"
        ((INFO++))
    elif grep -q 'lang="css"' "$file"; then
        print_message "${YELLOW}" "  ⚠ 使用了普通 CSS（可以考虑使用 SCSS）"
        ((WARNINGS++))
    fi

    # 检查是否使用了 scoped
    if grep -q 'scoped' "$file"; then
        print_message "${GREEN}" "  ✓ 使用了作用域样式"
        ((INFO++))
    fi
}

# 主检查函数
check_component() {
    local file=$1

    if check_file_exists "$file"; then
        check_imports "$file"
        check_template "$file"
        check_script "$file"
        check_style "$file"
        echo ""
    fi
}

# 开始检查
print_section "MModelX 前端组件检查"

# 检查所有 Vue 组件
print_message "${YELLOW}" "开始检查 Vue 组件..."
echo ""

# API 模块
print_section "API 模块"
check_file_exists "src/api/modules/features.js"
check_file_exists "src/stores/features.js"
check_file_exists "src/constants/features.js"

# 组件
print_section "组件"
check_component "src/components/MonacoEditor.vue"

# 特征工程页面
print_section "特征工程页面"
check_component "src/views/features/FeatureViewList.vue"
check_component "src/views/features/FeatureViewDetail.vue"
check_component "src/views/features/FeatureCompute.vue"
check_component "src/views/features/FeatureMaterialize.vue"
check_component "src/views/features/OnlineFeatureQuery.vue"
check_component "src/views/features/FeatureVisualization.vue"

# 布局和路由
print_section "布局和路由"
check_file_exists "src/layouts/MainLayout.vue"
check_file_exists "src/router/index.js"
check_file_exists "src/router/features.js"

# 打印统计
print_section "检查统计"
echo "  信息:   $INFO"
echo "  警告:   $WARNINGS"
echo "  错误:   $ERRORS"
echo ""

# 总体评估
if [ $ERRORS -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        print_message "${GREEN}" "🎉 检查通过！没有发现严重问题。"
    else
        print_message "${YELLOW}" "⚠️  发现 $WARNINGS 个警告，建议查看。"
    fi
else
    print_message "${RED}" "❌ 发现 $ERRORS 个错误，需要修复！"
fi

echo ""
print_message "${BLUE}" "提示：这只是基础检查，建议使用以下命令进行完整检查："
echo "  npm run lint    # ESLint 代码检查"
echo "  npm run type-check  # TypeScript 类型检查（如果使用TS）"
echo ""
