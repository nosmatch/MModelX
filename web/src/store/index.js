import { createPinia, defineStore } from 'pinia'

const pinia = createPinia()

export default pinia

// Overview store
export const useOverviewStore = defineStore('overview', {
  state: () => ({
    systemStatus: null,
    metrics: [],
    tasks: [],
    resources: null
  }),
  actions: {
    async fetchSummary() {
      // TODO: 实现API调用
    }
  }
})

// Features store
export const useFeaturesStore = defineStore('features', {
  state: () => ({
    features: [],
    currentFeature: null
  }),
  actions: {
    async fetchFeatures() {
      // TODO: 实现API调用
    }
  }
})

// Samples store
export const useSamplesStore = defineStore('samples', {
  state: () => ({
    datasets: [],
    currentDataset: null
  }),
  actions: {
    async fetchDatasets() {
      // TODO: 实现API调用
    }
  }
})

// Training store
export const useTrainingStore = defineStore('training', {
  state: () => ({
    experiments: [],
    currentExperiment: null
  }),
  actions: {
    async fetchExperiments() {
      // TODO: 实现API调用
    }
  }
})

// Serving store
export const useServingStore = defineStore('serving', {
  state: () => ({
    models: [],
    currentModel: null
  }),
  actions: {
    async fetchModels() {
      // TODO: 实现API调用
    }
  }
})