<template>
  <div class="app-shell">
    <header class="hero">
      <div class="hero__copy">
        <p class="hero__eyebrow">Finance Core Sandbox</p>
        <h1>주문, 원장, 실험, 대사를 한 화면에서 확인하는 작업 콘솔</h1>
        <p class="hero__description">
          Vue 3 기반 프론트에서 현재 Spring Boot API를 직접 호출합니다.
          주문 실패 mode, 반복 실험, 대사 결과를 빠르게 만지기 위한 운영형 UI입니다.
        </p>
        <div class="hero__chips">
          <span class="chip chip--blue">Vue 3</span>
          <span class="chip chip--blue">Java 21</span>
          <span class="chip chip--blue">PostgreSQL</span>
        </div>
      </div>
      <div class="hero__stats">
        <article class="stat-tile">
          <span class="stat-tile__label">기본 계좌</span>
          <strong class="stat-tile__value">demo-investor</strong>
          <p class="stat-tile__hint">백엔드 초기 데이터</p>
        </article>
        <article class="stat-tile">
          <span class="stat-tile__label">지원 mode</span>
          <strong class="stat-tile__value">{{ failureModes.length }}</strong>
          <p class="stat-tile__hint">NORMAL~DELAYED_CALLBACK</p>
        </article>
        <article class="stat-tile">
          <span class="stat-tile__label">주문 상태</span>
          <strong class="stat-tile__value">{{ orderStatuses.length }}</strong>
          <p class="stat-tile__hint">CREATED~RECONCILE_REQUIRED</p>
        </article>
      </div>
    </header>

    <main class="dashboard">
      <section class="panel">
        <div class="panel__header">
          <div>
            <p class="panel__eyebrow">Order</p>
            <h2>매수 주문 시뮬레이션</h2>
          </div>
        </div>
        <p class="panel__description">reserve -> 외부 체결 -> 원장 반영 또는 보상 흐름을 바로 확인합니다.</p>

        <form class="grid-form" @submit.prevent="submitOrder">
          <label>
            <span>accountId</span>
            <input v-model.trim="orderForm.accountId" type="text" />
          </label>
          <label>
            <span>instrumentCode</span>
            <input v-model.trim="orderForm.instrumentCode" type="text" />
          </label>
          <label>
            <span>quantity</span>
            <input v-model.number="orderForm.quantity" type="number" min="1" />
          </label>
          <label>
            <span>price</span>
            <input v-model.number="orderForm.price" type="number" min="1" step="0.01" />
          </label>
          <label>
            <span>idempotencyKey</span>
            <input v-model.trim="orderForm.idempotencyKey" type="text" />
          </label>
          <label>
            <span>mode</span>
            <select v-model="orderForm.mode">
              <option v-for="mode in failureModes" :key="mode" :value="mode">{{ mode }}</option>
            </select>
          </label>
          <div class="form-actions">
            <button class="button button--primary" :disabled="orderLoading">
              {{ orderLoading ? '주문 처리 중...' : '주문 생성' }}
            </button>
            <button class="button button--ghost" type="button" @click="resetOrderIdempotency">
              새 GUID 느낌 키
            </button>
          </div>
        </form>

        <p v-if="orderError" class="feedback feedback--error">{{ orderError }}</p>

        <div v-if="latestOrder" class="result-box">
          <div class="result-grid">
            <article class="stat-tile"><span class="stat-tile__label">orderId</span><strong class="stat-tile__value">{{ latestOrder.orderId }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">status</span><strong class="stat-tile__value">{{ latestOrder.status }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">reservedAmount</span><strong class="stat-tile__value">{{ latestOrder.reservedAmount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">lastUpdatedAt</span><strong class="stat-tile__value">{{ formatDate(latestOrder.lastUpdatedAt) }}</strong></article>
          </div>
          <pre class="result-json">{{ pretty(latestOrder) }}</pre>
        </div>
      </section>

      <section class="panel">
        <div class="panel__header">
          <div>
            <p class="panel__eyebrow">Experiment</p>
            <h2>반복 실험 실행</h2>
          </div>
        </div>
        <p class="panel__description">mode / tryCount / repeatCount 조합으로 주문을 반복 생성하고 집계 결과를 확인합니다.</p>

        <form class="compact-form" @submit.prevent="submitExperiment">
          <label>
            <span>mode</span>
            <select v-model="experimentForm.mode">
              <option v-for="mode in failureModes" :key="mode" :value="mode">{{ mode }}</option>
            </select>
          </label>
          <label>
            <span>tryCount</span>
            <input v-model.number="experimentForm.tryCount" type="number" min="1" />
          </label>
          <label>
            <span>repeatCount</span>
            <input v-model.number="experimentForm.repeatCount" type="number" min="1" />
          </label>
          <button class="button button--primary" :disabled="experimentLoading">
            {{ experimentLoading ? '실행 중...' : '실험 실행' }}
          </button>
        </form>

        <p v-if="experimentError" class="feedback feedback--error">{{ experimentError }}</p>

        <div v-if="latestExperiment" class="result-box">
          <div class="result-grid result-grid--quad">
            <article class="stat-tile"><span class="stat-tile__label">runId</span><strong class="stat-tile__value">{{ latestExperiment.runId }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">totalOrders</span><strong class="stat-tile__value">{{ latestExperiment.totalOrders }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">failureCount</span><strong class="stat-tile__value">{{ latestExperiment.failureCount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">compensationCount</span><strong class="stat-tile__value">{{ latestExperiment.compensationCount }}</strong></article>
          </div>
          <div class="result-grid result-grid--quad result-grid--secondary">
            <article class="stat-tile"><span class="stat-tile__label">executed</span><strong class="stat-tile__value">{{ latestExperiment.summaryMetrics.executedCount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">cancelled</span><strong class="stat-tile__value">{{ latestExperiment.summaryMetrics.cancelledCount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">compensated</span><strong class="stat-tile__value">{{ latestExperiment.summaryMetrics.compensatedCount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">reconcileRequired</span><strong class="stat-tile__value">{{ latestExperiment.summaryMetrics.reconcileRequiredCount }}</strong></article>
          </div>
          <pre class="result-json">{{ pretty(latestExperiment) }}</pre>
        </div>
      </section>

      <section class="panel">
        <div class="panel__header">
          <div>
            <p class="panel__eyebrow">Reconciliation</p>
            <h2>배치 대사 실행</h2>
          </div>
        </div>
        <p class="panel__description">주문 상태와 원장 엔트리의 일치 여부를 점검하고 unresolved 항목을 확인합니다.</p>

        <div class="form-actions form-actions--single">
          <button class="button button--primary" :disabled="reconciliationLoading" @click="submitReconciliation">
            {{ reconciliationLoading ? '대사 실행 중...' : '대사 실행' }}
          </button>
        </div>

        <p v-if="reconciliationError" class="feedback feedback--error">{{ reconciliationError }}</p>

        <div v-if="latestReconciliation" class="result-box">
          <div class="result-grid result-grid--quad">
            <article class="stat-tile"><span class="stat-tile__label">jobId</span><strong class="stat-tile__value">{{ latestReconciliation.jobId }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">matchedCount</span><strong class="stat-tile__value">{{ latestReconciliation.matchedCount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">mismatchCount</span><strong class="stat-tile__value">{{ latestReconciliation.mismatchCount }}</strong></article>
            <article class="stat-tile"><span class="stat-tile__label">unresolvedCount</span><strong class="stat-tile__value">{{ latestReconciliation.unresolvedCount }}</strong></article>
          </div>
          <ul v-if="latestReconciliation.unresolvedItems.length" class="unresolved-list">
            <li v-for="item in latestReconciliation.unresolvedItems" :key="item">{{ item }}</li>
          </ul>
          <pre class="result-json">{{ pretty(latestReconciliation) }}</pre>
        </div>
      </section>

      <section class="panel">
        <div class="panel__header">
          <div>
            <p class="panel__eyebrow">Lookup</p>
            <h2>기존 결과 다시 조회</h2>
          </div>
        </div>
        <p class="panel__description">orderId, runId, jobId를 직접 넣어 이미 생성된 결과를 다시 불러옵니다.</p>

        <div class="lookup-grid">
          <form class="lookup-box" @submit.prevent="lookupOrder">
            <label>
              <span>orderId</span>
              <input v-model.trim="lookup.orderId" type="text" placeholder="주문 ID" />
            </label>
            <button class="button button--secondary" :disabled="lookupLoading.order">주문 조회</button>
          </form>

          <form class="lookup-box" @submit.prevent="lookupExperimentRun">
            <label>
              <span>runId</span>
              <input v-model.trim="lookup.runId" type="text" placeholder="실험 실행 ID" />
            </label>
            <button class="button button--secondary" :disabled="lookupLoading.experiment">실험 조회</button>
          </form>

          <form class="lookup-box" @submit.prevent="lookupReconciliationJob">
            <label>
              <span>jobId</span>
              <input v-model.trim="lookup.jobId" type="text" placeholder="대사 작업 ID" />
            </label>
            <button class="button button--secondary" :disabled="lookupLoading.reconciliation">대사 조회</button>
          </form>
        </div>

        <p v-if="lookupError" class="feedback feedback--error">{{ lookupError }}</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { createOrder, fetchExperiment, fetchOrder, fetchReconciliation, runExperiment, runReconciliation } from './api'
import { failureModes, orderStatuses } from './constants'

const latestOrder = ref(null)
const latestExperiment = ref(null)
const latestReconciliation = ref(null)

const orderLoading = ref(false)
const experimentLoading = ref(false)
const reconciliationLoading = ref(false)

const orderError = ref('')
const experimentError = ref('')
const reconciliationError = ref('')
const lookupError = ref('')

const lookupLoading = reactive({
  order: false,
  experiment: false,
  reconciliation: false,
})

const orderForm = reactive({
  accountId: 'demo-investor',
  instrumentCode: 'EQ-CORE-001',
  quantity: 1,
  price: 1000,
  idempotencyKey: createIdempotencyKey(),
  mode: 'NORMAL',
})

const experimentForm = reactive({
  mode: 'NORMAL',
  tryCount: 2,
  repeatCount: 2,
})

const lookup = reactive({
  orderId: '',
  runId: '',
  jobId: '',
})

function createIdempotencyKey() {
  return `ui-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

function resetOrderIdempotency() {
  orderForm.idempotencyKey = createIdempotencyKey()
}

async function submitOrder() {
  orderLoading.value = true
  orderError.value = ''

  try {
    latestOrder.value = await createOrder({
      accountId: orderForm.accountId,
      instrumentCode: orderForm.instrumentCode,
      side: 'BUY',
      quantity: Number(orderForm.quantity),
      price: Number(orderForm.price),
      idempotencyKey: orderForm.idempotencyKey,
      mode: orderForm.mode,
    })
    lookup.orderId = latestOrder.value.orderId
  } catch (error) {
    orderError.value = error.message
  } finally {
    orderLoading.value = false
  }
}

async function submitExperiment() {
  experimentLoading.value = true
  experimentError.value = ''

  try {
    latestExperiment.value = await runExperiment({
      mode: experimentForm.mode,
      tryCount: Number(experimentForm.tryCount),
      repeatCount: Number(experimentForm.repeatCount),
    })
    lookup.runId = latestExperiment.value.runId
  } catch (error) {
    experimentError.value = error.message
  } finally {
    experimentLoading.value = false
  }
}

async function submitReconciliation() {
  reconciliationLoading.value = true
  reconciliationError.value = ''

  try {
    latestReconciliation.value = await runReconciliation()
    lookup.jobId = latestReconciliation.value.jobId
  } catch (error) {
    reconciliationError.value = error.message
  } finally {
    reconciliationLoading.value = false
  }
}

async function lookupOrder() {
  lookupLoading.order = true
  lookupError.value = ''

  try {
    latestOrder.value = await fetchOrder(lookup.orderId)
  } catch (error) {
    lookupError.value = error.message
  } finally {
    lookupLoading.order = false
  }
}

async function lookupExperimentRun() {
  lookupLoading.experiment = true
  lookupError.value = ''

  try {
    latestExperiment.value = await fetchExperiment(lookup.runId)
  } catch (error) {
    lookupError.value = error.message
  } finally {
    lookupLoading.experiment = false
  }
}

async function lookupReconciliationJob() {
  lookupLoading.reconciliation = true
  lookupError.value = ''

  try {
    latestReconciliation.value = await fetchReconciliation(lookup.jobId)
  } catch (error) {
    lookupError.value = error.message
  } finally {
    lookupLoading.reconciliation = false
  }
}

function pretty(value) {
  return JSON.stringify(value, null, 2)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}
</script>
