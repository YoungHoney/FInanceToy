<template>
  <div class='app-shell'>
    <header class='hero'>
      <div class='hero__copy'>
        <p class='hero__eyebrow'>Operator Console</p>
        <h1>장애 주문을 추적하고 대사까지 연결하는 운영 워크스페이스</h1>
        <p class='hero__description'>
          테스트 주문을 직접 주입한 뒤, 문제 주문만 필터링하고, 주문 이벤트와 원장 엔트리,
          감사로그를 타임라인으로 확인하는 운영형 화면입니다.
        </p>
        <div class='hero__chips'>
          <span class='chip chip--blue'>Vue 3</span>
          <span class='chip chip--blue'>Spring Boot</span>
          <span class='chip chip--blue'>Operator Console</span>
        </div>
      </div>
      <div class='hero__stats'>
        <article class='stat-tile'>
          <span class='stat-tile__label'>모니터링 주문</span>
          <strong class='stat-tile__value'>{{ dashboardStats.total }}</strong>
          <p class='stat-tile__hint'>현재 필터 기준</p>
        </article>
        <article class='stat-tile'>
          <span class='stat-tile__label'>문제 주문</span>
          <strong class='stat-tile__value'>{{ dashboardStats.problem }}</strong>
          <p class='stat-tile__hint'>CANCELLED / COMPENSATED / RECONCILE_REQUIRED</p>
        </article>
        <article class='stat-tile'>
          <span class='stat-tile__label'>대사 필요</span>
          <strong class='stat-tile__value'>{{ dashboardStats.reconcile }}</strong>
          <p class='stat-tile__hint'>후속 점검 대상</p>
        </article>
        <article class='stat-tile'>
          <span class='stat-tile__label'>최근 조치</span>
          <strong class='stat-tile__value'>{{ latestActionLabel }}</strong>
          <p class='stat-tile__hint'>가장 최근 상태</p>
        </article>
      </div>
    </header>

    <main class='dashboard'>
      <aside class='control-stack'>
        <section class='panel'>
          <div class='panel__header'>
            <div>
              <p class='panel__eyebrow'>Injection</p>
              <h2>테스트 주문 투입</h2>
            </div>
          </div>
          <p class='panel__description'>운영자가 일부러 장애 시나리오를 만들어 목록과 타임라인에 반영합니다.</p>

          <form class='grid-form grid-form--single' @submit.prevent='submitOrder'>
            <label>
              <span>accountId</span>
              <input v-model.trim='orderForm.accountId' type='text' />
            </label>
            <label>
              <span>instrumentCode</span>
              <input v-model.trim='orderForm.instrumentCode' type='text' />
            </label>
            <label>
              <span>quantity</span>
              <input v-model.number='orderForm.quantity' type='number' min='1' />
            </label>
            <label>
              <span>price</span>
              <input v-model.number='orderForm.price' type='number' min='1' step='0.01' />
            </label>
            <label>
              <span>mode</span>
              <select v-model='orderForm.mode'>
                <option v-for='mode in failureModes' :key='mode' :value='mode'>{{ mode }}</option>
              </select>
            </label>
            <label>
              <span>idempotencyKey</span>
              <input v-model.trim='orderForm.idempotencyKey' type='text' />
            </label>
            <div class='form-actions'>
              <button class='button button--primary' :disabled='orderLoading'>
                {{ orderLoading ? '투입 중...' : '주문 투입' }}
              </button>
              <button class='button button--ghost' type='button' @click='resetOrderIdempotency'>
                새 GUID 느낌 키
              </button>
            </div>
          </form>

          <p v-if='orderError' class='feedback feedback--error'>{{ orderError }}</p>
          <p v-if='orderSuccessMessage' class='feedback feedback--success'>{{ orderSuccessMessage }}</p>
        </section>

        <section class='panel'>
          <div class='panel__header'>
            <div>
              <p class='panel__eyebrow'>Batch</p>
              <h2>운영 액션</h2>
            </div>
          </div>
          <p class='panel__description'>문제 주문을 만든 뒤 바로 대사를 돌려 unresolved 건수를 확인합니다.</p>

          <div class='form-actions form-actions--stack'>
            <button class='button button--primary' :disabled='reconciliationLoading' @click='submitReconciliation'>
              {{ reconciliationLoading ? '대사 실행 중...' : '배치 대사 실행' }}
            </button>
            <button class='button button--ghost' :disabled='feedLoading' @click='loadOrders()'>
              {{ feedLoading ? '목록 새로고침 중...' : '목록 새로고침' }}
            </button>
          </div>

          <p v-if='reconciliationError' class='feedback feedback--error'>{{ reconciliationError }}</p>

          <div v-if='latestReconciliation' class='mini-grid'>
            <article class='mini-card'>
              <span>jobId</span>
              <strong>{{ latestReconciliation.jobId }}</strong>
            </article>
            <article class='mini-card'>
              <span>unresolved</span>
              <strong>{{ latestReconciliation.unresolvedCount }}</strong>
            </article>
            <article class='mini-card'>
              <span>mismatch</span>
              <strong>{{ latestReconciliation.mismatchCount }}</strong>
            </article>
          </div>
        </section>
      </aside>

      <section class='workspace'>
        <section class='panel'>
          <div class='panel__header panel__header--wrap'>
            <div>
              <p class='panel__eyebrow'>Monitor</p>
              <h2>문제 주문 모니터</h2>
            </div>
            <div class='filter-bar'>
              <label>
                <span>status</span>
                <select v-model='filters.status'>
                  <option value=''>ALL</option>
                  <option v-for='status in orderStatuses' :key='status' :value='status'>{{ status }}</option>
                </select>
              </label>
              <label>
                <span>mode</span>
                <select v-model='filters.failureMode'>
                  <option value=''>ALL</option>
                  <option v-for='mode in failureModes' :key='mode' :value='mode'>{{ mode }}</option>
                </select>
              </label>
              <label>
                <span>limit</span>
                <select v-model.number='filters.limit'>
                  <option :value='10'>10</option>
                  <option :value='20'>20</option>
                  <option :value='40'>40</option>
                </select>
              </label>
              <label class='toggle'>
                <input v-model='filters.problemOnly' type='checkbox' />
                <span>문제 주문만 보기</span>
              </label>
              <div class='form-actions'>
                <button class='button button--primary' :disabled='feedLoading' @click='loadOrders()'>
                  {{ feedLoading ? '조회 중...' : '필터 적용' }}
                </button>
                <button class='button button--ghost' :disabled='feedLoading' @click='resetFilters'>초기화</button>
              </div>
            </div>
          </div>

          <p v-if='feedError' class='feedback feedback--error'>{{ feedError }}</p>

          <div class='order-list'>
            <button
              v-for='order in orders'
              :key='order.orderId'
              class='order-row'
              :class='[{ "order-row--active": selectedOrder?.orderId === order.orderId }, statusToneClass(order.status)]'
              @click='selectOrder(order.orderId)'
            >
              <div class='order-row__main'>
                <div class='order-row__top'>
                  <span class='badge' :class='badgeClass(order.status)'>{{ order.status }}</span>
                  <span class='order-row__id'>{{ order.orderId }}</span>
                </div>
                <strong>{{ order.executionResult }}</strong>
                <p>{{ order.instrumentCode }} · {{ order.failureMode }} · {{ formatCurrency(order.reservedAmount) }}</p>
              </div>
              <div class='order-row__meta'>
                <span>{{ formatDate(order.lastUpdatedAt) }}</span>
                <small>{{ order.problem ? '조치 필요' : '정상' }}</small>
              </div>
            </button>

            <div v-if='!orders.length && !feedLoading' class='empty-state'>
              현재 필터 기준으로 표시할 주문이 없습니다.
            </div>
          </div>
        </section>

        <section class='panel detail-panel'>
          <div class='panel__header'>
            <div>
              <p class='panel__eyebrow'>Detail</p>
              <h2>주문 상세 워크스페이스</h2>
            </div>
          </div>

          <p v-if='detailError' class='feedback feedback--error'>{{ detailError }}</p>
          <div v-if='detailLoading' class='empty-state'>상세 정보를 불러오는 중입니다.</div>
          <div v-else-if='selectedOrder' class='detail-stack'>
            <div class='detail-summary'>
              <article class='stat-tile'>
                <span class='stat-tile__label'>orderId</span>
                <strong class='stat-tile__value'>{{ selectedOrder.orderId }}</strong>
                <p class='stat-tile__hint'>{{ selectedOrder.accountId }} / {{ selectedOrder.instrumentCode }}</p>
              </article>
              <article class='stat-tile'>
                <span class='stat-tile__label'>status</span>
                <strong class='stat-tile__value'>{{ selectedOrder.status }}</strong>
                <p class='stat-tile__hint'>{{ selectedOrder.failureMode }}</p>
              </article>
              <article class='stat-tile'>
                <span class='stat-tile__label'>reservedAmount</span>
                <strong class='stat-tile__value'>{{ formatCurrency(selectedOrder.reservedAmount) }}</strong>
                <p class='stat-tile__hint'>price {{ formatCurrency(selectedOrder.price) }} · qty {{ selectedOrder.quantity }}</p>
              </article>
              <article class='stat-tile'>
                <span class='stat-tile__label'>lastUpdatedAt</span>
                <strong class='stat-tile__value'>{{ formatDate(selectedOrder.lastUpdatedAt) }}</strong>
                <p class='stat-tile__hint'>created {{ formatDate(selectedOrder.createdAt) }}</p>
              </article>
            </div>

            <div class='visual-card'>
              <div class='visual-card__header'>
                <div>
                  <h3>주문 상태 흐름</h3>
                  <p>{{ orderStatusCaption }}</p>
                </div>
              </div>
              <div class='state-flow'>
                <div v-for='status in orderStatuses' :key='status' :class='orderStepClass(status)'>
                  <span class='state-flow__dot'></span>
                  <strong>{{ status }}</strong>
                </div>
              </div>
            </div>

            <div class='detail-grid'>
              <article class='timeline-card'>
                <div class='visual-card__header'>
                  <div>
                    <h3>운영 타임라인</h3>
                    <p>이벤트, 원장, 감사로그를 시간순으로 합쳐서 봅니다.</p>
                  </div>
                </div>

                <div class='timeline'>
                  <div v-for='item in timelineItems' :key='item.key' class='timeline__item'>
                    <div class='timeline__line'></div>
                    <div class='timeline__content'>
                      <div class='timeline__meta'>
                        <span class='badge' :class='timelineBadgeClass(item.kind)'>{{ item.label }}</span>
                        <span>{{ formatDate(item.occurredAt) }}</span>
                      </div>
                      <strong>{{ item.title }}</strong>
                      <p>{{ item.description }}</p>
                    </div>
                  </div>
                </div>
              </article>

              <div class='detail-side'>
                <article class='info-card'>
                  <div class='visual-card__header'>
                    <div>
                      <h3>원장 반영</h3>
                      <p>주문별 ledger entry</p>
                    </div>
                  </div>
                  <ul class='info-list'>
                    <li v-for='entry in selectedDetail.ledgerEntries' :key='entry.transactionId'>
                      <strong>{{ entry.entryType }}</strong>
                      <span>{{ formatCurrency(entry.amount) }}</span>
                      <small>가용 {{ formatCurrency(entry.availableBalanceAfter) }} / reserve {{ formatCurrency(entry.reservedBalanceAfter) }}</small>
                    </li>
                  </ul>
                </article>

                <article class='info-card'>
                  <div class='visual-card__header'>
                    <div>
                      <h3>감사 로그</h3>
                      <p>운영 추적용 audit</p>
                    </div>
                  </div>
                  <ul class='info-list'>
                    <li v-for='log in selectedDetail.auditLogs' :key='`${log.actionType}-${log.occurredAt}`'>
                      <strong>{{ log.actionType }}</strong>
                      <span>{{ log.detail }}</span>
                      <small>{{ formatDate(log.occurredAt) }}</small>
                    </li>
                  </ul>
                </article>
              </div>
            </div>
          </div>
          <div v-else class='empty-state'>좌측 목록에서 주문을 선택하면 상세 타임라인이 표시됩니다.</div>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createOrder, fetchOrderDetail, fetchOrders, runReconciliation } from './api'
import { failureModes, orderStatuses } from './constants'

const latestReconciliation = ref(null)
const orders = ref([])
const selectedDetail = ref(null)

const feedLoading = ref(false)
const detailLoading = ref(false)
const orderLoading = ref(false)
const reconciliationLoading = ref(false)

const feedError = ref('')
const detailError = ref('')
const orderError = ref('')
const reconciliationError = ref('')
const orderSuccessMessage = ref('')

const filters = reactive({
  status: '',
  failureMode: '',
  problemOnly: true,
  limit: 20,
})

const orderForm = reactive({
  accountId: 'demo-investor',
  instrumentCode: 'EQ-CORE-001',
  quantity: 1,
  price: 1000,
  idempotencyKey: createIdempotencyKey(),
  mode: 'DELAYED_CALLBACK',
})

const statusPaths = {
  CREATED: ['CREATED'],
  RESERVED: ['CREATED', 'RESERVED'],
  EXECUTING: ['CREATED', 'RESERVED', 'EXECUTING'],
  EXECUTED: ['CREATED', 'RESERVED', 'EXECUTING', 'EXECUTED'],
  CANCELLED: ['CREATED', 'RESERVED', 'EXECUTING', 'CANCELLED'],
  COMPENSATED: ['CREATED', 'RESERVED', 'EXECUTING', 'COMPENSATED'],
  RECONCILE_REQUIRED: ['CREATED', 'RESERVED', 'EXECUTING', 'COMPENSATED', 'RECONCILE_REQUIRED'],
}

const orderStatusCaptionMap = {
  CREATED: '주문이 생성된 직후 상태입니다.',
  RESERVED: '주문 금액을 먼저 reserve 한 상태입니다.',
  EXECUTING: '외부 체결 연계를 시도 중인 상태입니다.',
  EXECUTED: '체결과 원장 반영이 모두 끝난 정상 흐름입니다.',
  CANCELLED: '외부 거절로 자동취소까지 끝난 흐름입니다.',
  COMPENSATED: '타임아웃 이후 보상처리로 정합성을 맞춘 상태입니다.',
  RECONCILE_REQUIRED: '늦은 콜백이 도착해 배치 대사가 필요한 상태입니다.',
}

const selectedOrder = computed(() => selectedDetail.value?.order ?? null)

const dashboardStats = computed(() => ({
  total: orders.value.length,
  problem: orders.value.filter((order) => order.problem).length,
  reconcile: orders.value.filter((order) => order.status === 'RECONCILE_REQUIRED').length,
  latest: orders.value[0]?.status ?? '-',
}))

const latestActionLabel = computed(() => dashboardStats.value.latest)

const orderStatusCaption = computed(() => {
  if (!selectedOrder.value) return ''
  return orderStatusCaptionMap[selectedOrder.value.status] ?? ''
})

const timelineItems = computed(() => {
  if (!selectedDetail.value) return []

  const orderEvents = selectedDetail.value.orderEvents.map((event, index) => ({
    key: `event-${event.eventType}-${index}`,
    kind: 'event',
    label: 'ORDER EVENT',
    title: event.eventType,
    description: event.detail,
    occurredAt: event.occurredAt,
  }))

  const ledgerEntries = selectedDetail.value.ledgerEntries.map((entry) => ({
    key: `ledger-${entry.transactionId}`,
    kind: 'ledger',
    label: 'LEDGER',
    title: entry.entryType,
    description: `${formatCurrency(entry.amount)} | 가용 ${formatCurrency(entry.availableBalanceAfter)} / reserve ${formatCurrency(entry.reservedBalanceAfter)}`,
    occurredAt: entry.occurredAt,
  }))

  const auditLogs = selectedDetail.value.auditLogs.map((log, index) => ({
    key: `audit-${log.actionType}-${index}`,
    kind: 'audit',
    label: 'AUDIT',
    title: log.actionType,
    description: log.detail,
    occurredAt: log.occurredAt,
  }))

  return [...orderEvents, ...ledgerEntries, ...auditLogs]
    .sort((left, right) => new Date(left.occurredAt) - new Date(right.occurredAt))
})

function createIdempotencyKey() {
  return `ui-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

function resetOrderIdempotency() {
  orderForm.idempotencyKey = createIdempotencyKey()
}

function resetFilters() {
  filters.status = ''
  filters.failureMode = ''
  filters.problemOnly = true
  filters.limit = 20
  loadOrders()
}

function orderStepClass(status) {
  if (!selectedOrder.value) {
    return 'state-flow__step'
  }

  const path = statusPaths[selectedOrder.value.status] ?? []
  return [
    'state-flow__step',
    path.includes(status) ? 'state-flow__step--done' : '',
    selectedOrder.value.status === status ? 'state-flow__step--current' : '',
  ].filter(Boolean).join(' ')
}

function badgeClass(status) {
  if (status === 'RECONCILE_REQUIRED') return 'badge--danger'
  if (status === 'COMPENSATED' || status === 'CANCELLED') return 'badge--warning'
  if (status === 'EXECUTED') return 'badge--success'
  return 'badge--neutral'
}

function statusToneClass(status) {
  if (status === 'RECONCILE_REQUIRED') return 'order-row--danger'
  if (status === 'COMPENSATED' || status === 'CANCELLED') return 'order-row--warning'
  return 'order-row--neutral'
}

function timelineBadgeClass(kind) {
  if (kind === 'ledger') return 'badge--success'
  if (kind === 'audit') return 'badge--warning'
  return 'badge--neutral'
}

async function loadOrders(preferredOrderId = selectedOrder.value?.orderId ?? '') {
  feedLoading.value = true
  feedError.value = ''

  try {
    orders.value = await fetchOrders({
      status: filters.status,
      failureMode: filters.failureMode,
      problemOnly: filters.problemOnly,
      limit: filters.limit,
    })

    const nextOrderId = preferredOrderId || orders.value[0]?.orderId || ''
    if (nextOrderId) {
      await loadOrderDetail(nextOrderId)
    } else {
      selectedDetail.value = null
    }
  } catch (error) {
    feedError.value = error.message
  } finally {
    feedLoading.value = false
  }
}

async function loadOrderDetail(orderId) {
  detailLoading.value = true
  detailError.value = ''

  try {
    selectedDetail.value = await fetchOrderDetail(orderId)
  } catch (error) {
    detailError.value = error.message
  } finally {
    detailLoading.value = false
  }
}

async function selectOrder(orderId) {
  await loadOrderDetail(orderId)
}

async function submitOrder() {
  orderLoading.value = true
  orderError.value = ''
  orderSuccessMessage.value = ''

  try {
    const order = await createOrder({
      accountId: orderForm.accountId,
      instrumentCode: orderForm.instrumentCode,
      side: 'BUY',
      quantity: Number(orderForm.quantity),
      price: Number(orderForm.price),
      idempotencyKey: orderForm.idempotencyKey,
      mode: orderForm.mode,
    })

    orderSuccessMessage.value = `${order.status} 상태 주문이 반영되었습니다. ${order.orderId}`
    await loadOrderDetail(order.orderId)
    await loadOrders(order.orderId)
  } catch (error) {
    orderError.value = error.message
  } finally {
    orderLoading.value = false
  }
}

async function submitReconciliation() {
  reconciliationLoading.value = true
  reconciliationError.value = ''

  try {
    latestReconciliation.value = await runReconciliation()
    await loadOrders(selectedOrder.value?.orderId ?? '')
  } catch (error) {
    reconciliationError.value = error.message
  } finally {
    reconciliationLoading.value = false
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

function formatCurrency(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return new Intl.NumberFormat('ko-KR', {
    maximumFractionDigits: 2,
  }).format(Number(value))
}

onMounted(() => {
  loadOrders()
})
</script>
