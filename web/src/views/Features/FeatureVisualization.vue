<template>
  <div class="feature-visualization">
    <!-- 控制面板 -->
    <el-card class="control-card">
      <template #header>
        <div class="card-header">
          <span>数据源配置</span>
          <el-button size="small" :icon="Refresh" @click="refreshData">
            刷新数据
          </el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="特征视图">
            <el-select
              v-model="selectedViewName"
              placeholder="选择特征视图"
              style="width: 100%"
              @change="handleViewChange"
            >
              <el-option
                v-for="view in activeViews"
                :key="view.name"
                :label="view.name"
                :value="view.name"
              >
                <span class="view-option">{{ view.name }}</span>
                <span class="view-meta">({{ view.features?.length || 0 }} 特征)</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="实体样本数">
            <el-select
              v-model="sampleSize"
              placeholder="选择样本数量"
              style="width: 100%"
              @change="loadFeatureData"
            >
              <el-option label="100 样本" :value="100" />
              <el-option label="500 样本" :value="500" />
              <el-option label="1000 样本" :value="1000" />
              <el-option label="5000 样本" :value="5000" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="刷新间隔">
            <el-select
              v-model="refreshInterval"
              placeholder="自动刷新"
              style="width: 100%"
              @change="handleRefreshIntervalChange"
            >
              <el-option label="不自动刷新" :value="0" />
              <el-option label="每 10 秒" :value="10" />
              <el-option label="每 30 秒" :value="30" />
              <el-option label="每 60 秒" :value="60" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 特征选择 -->
      <el-form-item label="选择特征">
        <div class="feature-selector">
          <el-select
            v-model="selectedFeatures"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择要可视化的特征"
            style="width: 100%"
            @change="handleFeatureChange"
          >
            <el-option
              v-for="feature in availableFeatures"
              :key="feature.name"
              :label="feature.name"
              :value="feature.name"
            >
              <div class="feature-option">
                <span class="feature-name">{{ feature.name }}</span>
                <el-tag size="small" :type="getDataTypeTagType(feature.dtype)">
                  {{ feature.dtype }}
                </el-tag>
              </div>
            </el-option>
          </el-select>

          <el-button
            size="small"
            style="margin-left: 12px"
            @click="selectAllFeatures"
          >
            全选
          </el-button>
          <el-button
            size="small"
            @click="clearAllFeatures"
          >
            清空
          </el-button>
        </div>
      </el-form-item>
    </el-card>

    <!-- 统计摘要 -->
    <el-card v-if="featureStats" class="stats-card">
      <template #header>
        <span>统计摘要</span>
      </template>

      <el-row :gutter="16">
        <el-col
          v-for="(stat, index) in featureStats"
          :key="index"
          :span="6"
        >
          <div class="stat-box">
            <div class="stat-header">
              <span class="stat-name">{{ stat.name }}</span>
              <el-tag size="small" :type="getDataTypeTagType(stat.dtype)">
                {{ stat.dtype }}
              </el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-item">
                <span class="label">总数:</span>
                <span class="value">{{ stat.count }}</span>
              </div>
              <div class="stat-item">
                <span class="label">均值:</span>
                <span class="value">{{ stat.mean?.toFixed(2) || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="label">标准差:</span>
                <span class="value">{{ stat.std?.toFixed(2) || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="label">最小值:</span>
                <span class="value">{{ stat.min?.toFixed(2) || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="label">最大值:</span>
                <span class="value">{{ stat.max?.toFixed(2) || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="label">缺失率:</span>
                <span class="value warning">{{ (stat.nullRatio * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 可视化图表 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 分布图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>特征分布</span>
              <el-select
                v-model="distributionFeature"
                size="small"
                style="width: 150px"
              >
                <el-option
                  v-for="feature in numericFeatures"
                  :key="feature.name"
                  :label="feature.name"
                  :value="feature.name"
                />
              </el-select>
            </div>
          </template>

          <div ref="distributionChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 箱线图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>特征箱线图</span>
          </template>

          <div ref="boxplotChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <!-- 相关性热力图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>特征相关性</span>
          </template>

          <div ref="correlationChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 散点矩阵 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>散点矩阵</span>
          </template>

          <div ref="scatterChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 时间序列图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>时间序列趋势</span>
          <el-select
            v-model="timeSeriesFeature"
            size="small"
            style="width: 150px"
          >
            <el-option
              v-for="feature in numericFeatures"
              :key="feature.name"
              :label="feature.name"
              :value="feature.name"
            />
          </el-select>
        </div>
      </template>

      <div ref="timeSeriesChart" class="chart-container-large"></div>
    </el-card>

    <!-- 特征对比 -->
    <el-card class="chart-card">
      <template #header>
        <span>特征对比</span>
      </template>

      <div ref="comparisonChart" class="chart-container-large"></div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 特征可视化面板
 *
 * 功能：
 * - 特征分布图（直方图）
 * - 特征相关性热力图
 * - 特征时间序列图
 * - 特征统计摘要
 * - 多视图对比
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useFeaturesStore } from '@/stores/features'

// ==================== Store ====================
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
const selectedViewName = ref('')
const sampleSize = ref(1000)
const refreshInterval = ref(0)
const selectedFeatures = ref([])
const distributionFeature = ref('')
const timeSeriesFeature = ref('')

// 特征数据
const featureData = ref([])
const featureStats = ref(null)

// ECharts 实例
const distributionChart = ref(null)
const boxplotChart = ref(null)
const correlationChart = ref(null)
const scatterChart = ref(null)
const timeSeriesChart = ref(null)
const comparisonChart = ref(null)

let charts = []
let refreshTimer = null

// ==================== 计算属性 ====================
/**
 * 激活的特征视图
 */
const activeViews = computed(() => {
  return featuresStore.activeViews
})

/**
 * 选中的视图
 */
const selectedView = computed(() => {
  if (!selectedViewName.value) return null
  return activeViews.value.find(v => v.name === selectedViewName.value)
})

/**
 * 可用的特征列表
 */
const availableFeatures = computed(() => {
  return selectedView.value?.features || []
})

/**
 * 数值型特征
 */
const numericFeatures = computed(() => {
  return availableFeatures.value.filter(f =>
    f.dtype === 'INT64' || f.dtype === 'FLOAT64'
  )
})

// ==================== 方法 ====================
/**
 * 处理视图变化
 */
const handleViewChange = () => {
  selectedFeatures.value = []
  featureData.value = []
  featureStats.value = null

  if (selectedView.value && selectedView.value.features) {
    // 默认选择前5个数值型特征
    const numeric = numericFeatures.value.slice(0, 5)
    selectedFeatures.value = numeric.map(f => f.name)

    if (numeric.length > 0) {
      distributionFeature.value = numeric[0].name
      timeSeriesFeature.value = numeric[0].name
    }

    loadFeatureData()
  }
}

/**
 * 处理特征选择变化
 */
const handleFeatureChange = () => {
  if (selectedFeatures.value.length > 0) {
    loadFeatureData()
  }
}

/**
 * 加载特征数据
 */
const loadFeatureData = async () => {
  if (!selectedViewName.value || selectedFeatures.value.length === 0) {
    return
  }

  try {
    // 这里应该调用API获取实际数据
    // 暂时生成模拟数据
    await generateMockData()

    // 计算统计摘要
    calculateStats()

    // 渲染图表
    await nextTick()
    renderAllCharts()

    ElMessage.success('数据已更新')
  } catch (error) {
    ElMessage.error('加载数据失败: ' + error.message)
  }
}

/**
 * 生成模拟数据
 */
const generateMockData = async () => {
  const data = []
  const features = selectedFeatures.value

  for (let i = 0; i < sampleSize.value; i++) {
    const row = {
      _id: i,
      _timestamp: Date.now() - (sampleSize.value - i) * 3600000 // 每小时一个点
    }

    // 为每个特征生成随机数据
    features.forEach(featureName => {
      const feature = availableFeatures.value.find(f => f.name === featureName)
      if (!feature) return

      if (feature.dtype === 'INT64' || feature.dtype === 'FLOAT64') {
        // 生成正态分布数据
        row[featureName] = generateNormalRandom(100, 50)
      } else if (feature.dtype === 'BOOLEAN') {
        row[featureName] = Math.random() > 0.5
      } else {
        row[featureName] = `value_${i}`
      }
    })

    data.push(row)
  }

  featureData.value = data
}

/**
 * 生成正态分布随机数
 */
const generateNormalRandom = (mean, std) => {
  let u = 0, v = 0
  while (u === 0) u = Math.random()
  while (v === 0) v = Math.random()
  const z = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v)
  return z * std + mean
}

/**
 * 计算统计摘要
 */
const calculateStats = () => {
  const stats = []
  const numeric = selectedFeatures.value.filter(fname => {
    const feature = availableFeatures.value.find(f => f.name === fname)
    return feature && (feature.dtype === 'INT64' || feature.dtype === 'FLOAT64')
  })

  numeric.forEach(featureName => {
    const values = featureData.value.map(row => row[featureName]).filter(v => v != null)

    const sum = values.reduce((a, b) => a + b, 0)
    const mean = sum / values.length
    const variance = values.reduce((a, b) => a + Math.pow(b - mean, 2), 0) / values.length
    const std = Math.sqrt(variance)
    const min = Math.min(...values)
    const max = Math.max(...values)
    const nullCount = featureData.value.filter(row => row[featureName] == null).length
    const nullRatio = nullCount / featureData.value.length

    stats.push({
      name: featureName,
      dtype: 'FLOAT64',
      count: values.length,
      mean,
      std,
      min,
      max,
      nullRatio
    })
  })

  featureStats.value = stats
}

/**
 * 渲染所有图表
 */
const renderAllCharts = () => {
  renderDistributionChart()
  renderBoxplotChart()
  renderCorrelationChart()
  renderScatterChart()
  renderTimeSeriesChart()
  renderComparisonChart()
}

/**
 * 渲染分布图
 */
const renderDistributionChart = () => {
  if (!distributionChart.value) return

  const chart = echarts.init(distributionChart.value)
  charts.push(chart)

  const featureName = distributionFeature.value
  const values = featureData.value.map(row => row[featureName])

  // 计算直方图数据
  const bins = 20
  const min = Math.min(...values)
  const max = Math.max(...values)
  const binWidth = (max - min) / bins

  const histogram = new Array(bins).fill(0)
  values.forEach(v => {
    const binIndex = Math.min(Math.floor((v - min) / binWidth), bins - 1)
    histogram[binIndex]++
  })

  const binLabels = []
  for (let i = 0; i < bins; i++) {
    const start = (min + i * binWidth).toFixed(1)
    const end = (min + (i + 1) * binWidth).toFixed(1)
    binLabels.push(`${start}-${end}`)
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    xAxis: {
      type: 'category',
      data: binLabels,
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value',
      name: '频数'
    },
    series: [{
      data: histogram,
      type: 'bar',
      itemStyle: { color: '#409eff' }
    }],
    grid: {
      left: '60px',
      right: '20px',
      bottom: '80px',
      top: '20px'
    }
  }

  chart.setOption(option)
}

/**
 * 渲染箱线图
 */
const renderBoxplotChart = () => {
  if (!boxplotChart.value) return

  const chart = echarts.init(boxplotChart.value)
  charts.push(chart)

  const numeric = selectedFeatures.value.filter(fname => {
    const feature = availableFeatures.value.find(f => f.name === fname)
    return feature && (feature.dtype === 'INT64' || feature.dtype === 'FLOAT64')
  })

  const data = numeric.map(featureName => {
    const values = featureData.value.map(row => row[featureName]).sort((a, b) => a - b)
    const q1 = values[Math.floor(values.length * 0.25)]
    const median = values[Math.floor(values.length * 0.5)]
    const q3 = values[Math.floor(values.length * 0.75)]
    const min = values[0]
    const max = values[values.length - 1]

    return [min, q1, median, q3, max]
  })

  const option = {
    tooltip: {
      trigger: 'item',
      axisPointer: { type: 'shadow' }
    },
    xAxis: {
      type: 'category',
      data: numeric,
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value',
      name: '值'
    },
    series: [{
      type: 'boxplot',
      data
    }],
    grid: {
      left: '60px',
      right: '20px',
      bottom: '80px',
      top: '20px'
    }
  }

  chart.setOption(option)
}

/**
 * 渲染相关性热力图
 */
const renderCorrelationChart = () => {
  if (!correlationChart.value) return

  const chart = echarts.init(correlationChart.value)
  charts.push(chart)

  const numeric = selectedFeatures.value.filter(fname => {
    const feature = availableFeatures.value.find(f => f.name === fname)
    return feature && (feature.dtype === 'INT64' || feature.dtype === 'FLOAT64')
  }).slice(0, 5) // 最多5个特征

  // 计算相关系数矩阵
  const correlation = []
  for (let i = 0; i < numeric.length; i++) {
    correlation[i] = []
    for (let j = 0; j < numeric.length; j++) {
      correlation[i][j] = calculateCorrelation(
        featureData.value.map(row => row[numeric[i]]),
        featureData.value.map(row => row[numeric[j]])
      )
    }
  }

  const data = []
  for (let i = 0; i < numeric.length; i++) {
    for (let j = 0; j < numeric.length; j++) {
      data.push([i, j, correlation[i][j]])
    }
  }

  const option = {
    tooltip: {
      position: 'top',
      formatter: params => {
        return `${numeric[params.value[0]]} vs ${numeric[params.value[1]]}: ${params.value[2].toFixed(3)}`
      }
    },
    grid: {
      height: '70%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: numeric,
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: numeric,
      splitArea: { show: true }
    },
    visualMap: {
      min: -1,
      max: 1,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: {
        color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffcc',
                '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
      }
    },
    series: [{
      type: 'heatmap',
      data,
      label: { show: false },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }

  chart.setOption(option)
}

/**
 * 计算相关系数
 */
const calculateCorrelation = (x, y) => {
  const n = x.length
  const sumX = x.reduce((a, b) => a + b, 0)
  const sumY = y.reduce((a, b) => a + b, 0)
  const sumXY = x.reduce((sum, xi, i) => sum + xi * y[i], 0)
  const sumX2 = x.reduce((sum, xi) => sum + xi * xi, 0)
  const sumY2 = y.reduce((sum, yi) => sum + yi * yi, 0)

  const numerator = n * sumXY - sumX * sumY
  const denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))

  return denominator === 0 ? 0 : numerator / denominator
}

/**
 * 渲染散点图
 */
const renderScatterChart = () => {
  if (!scatterChart.value) return

  const chart = echarts.init(scatterChart.value)
  charts.push(chart)

  const numeric = selectedFeatures.value.filter(fname => {
    const feature = availableFeatures.value.find(f => f.name === fname)
    return feature && (feature.dtype === 'INT64' || feature.dtype === 'FLOAT64')
  }).slice(0, 4) // 最多4个特征

  const series = []
  for (let i = 0; i < numeric.length - 1; i++) {
    for (let j = i + 1; j < numeric.length; j++) {
      series.push({
        name: `${numeric[i]} vs ${numeric[j]}`,
        type: 'scatter',
        symbolSize: 6,
        data: featureData.value.map(row => [row[numeric[i]], row[numeric[j]]])
      })
    }
  }

  const option = {
    legend: {
      bottom: 0
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '15%'
    },
    xAxis: {
      name: 'X',
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    yAxis: {
      name: 'Y',
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series
  }

  chart.setOption(option)
}

/**
 * 渲染时间序列图
 */
const renderTimeSeriesChart = () => {
  if (!timeSeriesChart.value) return

  const chart = echarts.init(timeSeriesChart.value)
  charts.push(chart)

  const featureName = timeSeriesFeature.value
  const data = featureData.value.map(row => [row._timestamp, row[featureName]])

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'time',
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      name: featureName
    },
    series: [{
      name: featureName,
      type: 'line',
      data,
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
        ])
      },
      itemStyle: { color: '#409eff' }
    }],
    grid: {
      left: '60px',
      right: '20px',
      bottom: '60px',
      top: '40px'
    }
  }

  chart.setOption(option)
}

/**
 * 渲染对比图
 */
const renderComparisonChart = () => {
  if (!comparisonChart.value) return

  const chart = echarts.init(comparisonChart.value)
  charts.push(chart)

  const numeric = selectedFeatures.value.filter(fname => {
    const feature = availableFeatures.value.find(f => f.name === fname)
    return feature && (feature.dtype === 'INT64' || feature.dtype === 'FLOAT64')
  }).slice(0, 6)

  const series = numeric.map(featureName => {
    return {
      name: featureName,
      type: 'bar',
      data: featureData.value.slice(0, 20).map(row => row[featureName])
    }
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: Array.from({ length: 20 }, (_, i) => `样本${i + 1}`),
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value'
    },
    series,
    grid: {
      left: '60px',
      right: '20px',
      bottom: '80px',
      top: '40px'
    }
  }

  chart.setOption(option)
}

/**
 * 刷新数据
 */
const refreshData = () => {
  loadFeatureData()
}

/**
 * 处理刷新间隔变化
 */
const handleRefreshIntervalChange = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }

  if (refreshInterval.value > 0) {
    refreshTimer = setInterval(() => {
      loadFeatureData()
    }, refreshInterval.value * 1000)
  }
}

/**
 * 全选特征
 */
const selectAllFeatures = () => {
  selectedFeatures.value = numericFeatures.value.map(f => f.name)
}

/**
 * 清空特征选择
 */
const clearAllFeatures = () => {
  selectedFeatures.value = []
}

/**
 * 获取数据类型标签类型
 */
const getDataTypeTagType = (dtype) => {
  const types = {
    INT64: '',
    FLOAT64: 'success',
    STRING: 'warning',
    BOOLEAN: 'info'
  }
  return types[dtype] || 'info'
}

// ==================== 生命周期 ====================
onMounted(() => {
  featuresStore.fetchViews()

  // 默认选择第一个视图
  if (activeViews.value.length > 0) {
    selectedViewName.value = activeViews.value[0].name
    handleViewChange()
  }

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  // 销毁所有图表实例
  charts.forEach(chart => chart.dispose())
  charts = []

  // 清除定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }

  window.removeEventListener('resize', handleResize)
})

/**
 * 处理窗口大小变化
 */
const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

// 监听特征分布选择变化
watch(distributionFeature, () => {
  renderDistributionChart()
})

// 监听时间序列选择变化
watch(timeSeriesFeature, () => {
  renderTimeSeriesChart()
})
</script>

<style scoped lang="scss">
.feature-visualization {
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.view-option {
  font-weight: 500;
}

.view-meta {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.feature-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .feature-name {
    font-weight: 500;
  }
}

.feature-selector {
  display: flex;
  align-items: center;
  width: 100%;
}

.control-card {
  margin-bottom: 20px;
}

.stats-card {
  margin-bottom: 20px;
}

.stat-box {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;

  .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .stat-name {
      font-weight: 600;
      font-size: 14px;
      color: #303133;
    }
  }

  .stat-body {
    .stat-item {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      margin-bottom: 6px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        color: #606266;
      }

      .value {
        font-weight: 500;
        color: #303133;

        &.warning {
          color: #e6a23c;
        }
      }
    }
  }
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 450px;

  .chart-container {
    height: 350px;
  }

  .chart-container-large {
    height: 400px;
  }
}

// 响应式
@media (max-width: 1200px) {
  .charts-row {
    :deep(.el-col) {
      margin-bottom: 20px;
    }
  }
}

@media (max-width: 768px) {
  .feature-visualization {
    padding: 16px;
  }

  .stat-box {
    margin-bottom: 12px;
  }

  .chart-card {
    height: auto;
  }

  .chart-container,
  .chart-container-large {
    height: 300px;
  }
}
</style>
