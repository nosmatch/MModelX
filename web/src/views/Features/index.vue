<template>
  <div class="features-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <stat-card :icon="Document" tone="primary" :value="featuresStore.views.length" label="特征视图" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="CircleCheck" tone="success" :value="activeViewCount" label="已激活" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="Cpu" tone="warning" :value="totalFeatureCount" label="特征总数" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="TrendCharts" icon-bg="#f0f9ff" icon-color="#409eff" :value="featuresStore.entityTypes.length" label="实体类型" />
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="quick-actions-card">
      <template #header>
        <span>快捷操作</span>
      </template>
      <div class="action-grid">
        <div class="action-item" @click="goTo('FeatureViewList')">
          <div class="action-icon" style="background: #ecf5ff; color: #409eff;">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="action-info">
            <div class="action-title">特征视图</div>
            <div class="action-desc">查看和管理特征视图</div>
          </div>
        </div>
        <div class="action-item" @click="goTo('FeatureCompute')">
          <div class="action-icon" style="background: #f0f9ff; color: #67c23a;">
            <el-icon :size="28"><Cpu /></el-icon>
          </div>
          <div class="action-info">
            <div class="action-title">特征计算</div>
            <div class="action-desc">执行离线特征计算</div>
          </div>
        </div>
        <div class="action-item" @click="goTo('FeatureMaterialize')">
          <div class="action-icon" style="background: #fdf6ec; color: #e6a23c;">
            <el-icon :size="28"><Upload /></el-icon>
          </div>
          <div class="action-info">
            <div class="action-title">特征物化</div>
            <div class="action-desc">物化特征到 Redis</div>
          </div>
        </div>
        <div class="action-item" @click="goTo('OnlineFeatureQuery')">
          <div class="action-icon" style="background: #f0f9ff; color: #409eff;">
            <el-icon :size="28"><Search /></el-icon>
          </div>
          <div class="action-info">
            <div class="action-title">在线查询</div>
            <div class="action-desc">实时查询在线特征</div>
          </div>
        </div>
        <div class="action-item" @click="goTo('FeatureVisualization')">
          <div class="action-icon" style="background: #f0f9ff; color: #67c23a;">
            <el-icon :size="28"><TrendCharts /></el-icon>
          </div>
          <div class="action-info">
            <div class="action-title">特征可视化</div>
            <div class="action-desc">查看特征分布和统计</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 最近更新的特征视图 -->
    <el-card v-loading="featuresStore.loading.views" class="recent-views-card">
      <template #header>
        <div class="card-header">
          <span>最近更新的特征视图</span>
          <el-button size="small" text @click="goTo('FeatureViewList')">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>

      <el-empty v-if="!featuresStore.loading.views && recentViews.length === 0" description="暂无特征视图">
        <el-button type="primary" @click="goTo('FeatureViewList')">去创建</el-button>
      </el-empty>

      <el-table
        v-else
        :data="recentViews"
        stripe
        style="width: 100%"
        @row-click="(row) => goToDetail(row.name)"
      >
        <el-table-column prop="name" label="视图名称" min-width="180">
          <template #default="{ row }">
            <div class="view-name-cell">
              <el-icon color="#409eff"><Document /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="entity" label="实体类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.entity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="features" label="特征数" width="100" align="center">
          <template #default="{ row }">
            {{ row.features?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Document,
  CircleCheck,
  Cpu,
  TrendCharts,
  Upload,
  Search,
  ArrowRight
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'
import { formatDate } from '@/utils/date'
import { FeatureViewStatusLabels, FeatureViewStatusColors } from '@/constants/features'
import StatCard from '@/components/StatCard.vue'

const router = useRouter()
const featuresStore = useFeaturesStore()

const activeViewCount = computed(() => featuresStore.activeViews.length)

const totalFeatureCount = computed(() => {
  return featuresStore.views.reduce((sum, view) => sum + (view.features?.length || 0), 0)
})

const recentViews = computed(() => {
  return [...featuresStore.views]
    .sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0))
    .slice(0, 5)
})

const goTo = (name) => {
  router.push({ name })
}

const goToDetail = (name) => {
  router.push({ name: 'FeatureViewDetail', params: { name } })
}

// 状态映射（统一从 constants/features.js 取）
const getStatusType = (status) => FeatureViewStatusColors[status] || 'info'
const getStatusLabel = (status) => FeatureViewStatusLabels[status] || status

onMounted(() => {
  if (featuresStore.views.length === 0) {
    featuresStore.fetchViews()
  }
})
</script>

<style scoped lang="scss">
.features-page {
  padding: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.quick-actions-card {
  margin-bottom: 24px;

  .action-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
  }

  .action-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border: 1px solid $border-light;
    border-radius: $radius-md;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
      box-shadow: $shadow-hover;
      transform: translateY(-2px);
    }

    .action-icon {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 10px;
      margin-right: 12px;
      flex-shrink: 0;
    }

    .action-info {
      .action-title {
        font-size: 15px;
        font-weight: 600;
        color: $text-primary;
      }

      .action-desc {
        font-size: 12px;
        color: $text-muted;
        margin-top: 4px;
      }
    }
  }
}

.recent-views-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .view-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
}

@media (max-width: 768px) {
  .features-page {
    padding: 16px;
  }

  .action-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
