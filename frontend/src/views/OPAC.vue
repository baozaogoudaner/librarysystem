<template>
  <div class="opac-page">
    <!-- Search Hero -->
    <div class="search-hero">
      <h1>统一资源检索 <span class="subtitle">OPAC</span></h1>
      <p>一站式检索纸质图书、电子资源，快速定位馆藏位置</p>
      <div class="search-box">
        <el-input v-model="keyword" size="large" placeholder="输入书名、作者、ISBN或关键词搜索..."
                  @keyup.enter="doSearch" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" @click="doSearch" :loading="loading">
          <el-icon><Search /></el-icon> 检索
        </el-button>
      </div>
      <div style="margin-top:12px">
        <el-button size="small" @click="ocrClick" :loading="ocrLoading">
          <el-icon><Camera /></el-icon> 扫码借书/还书
        </el-button>
        <input type="file" ref="ocrFileInput" accept="image/*" style="display:none" @change="onOcrFile" />
      </div>
    </div>

    <!-- Hot Books -->
    <div class="section" v-if="hotBooks.length > 0 && !searched">
      <h2><el-icon><TrendCharts /></el-icon> 热门图书</h2>
      <el-row :gutter="20">
        <el-col :span="6" v-for="book in hotBooks" :key="book.id">
          <el-card shadow="hover" class="book-card" @click="viewBook(book)">
            <div class="book-cover">
              <img :src="book.coverUrl || 'https://via.placeholder.com/200x280?text=No+Cover'" :alt="book.title" />
            </div>
            <div class="book-info">
              <h4>{{ book.title }}</h4>
              <p class="author">{{ book.author }}</p>
              <p class="meta">
                <el-tag :type="book.availableStock > 0 ? 'success' : 'danger'" size="small">
                  {{ book.availableStock > 0 ? `可借(${book.availableStock})` : '已借完' }}
                </el-tag>
                <el-tag v-if="book.hasEbook" type="warning" size="small" style="margin-left:4px">电子版</el-tag>
              </p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- AI Recommend -->
    <div class="section" v-if="!searched">
      <h2><el-icon><Reading /></el-icon> AI 智能推荐</h2>
      <div v-if="!aiLoaded && !aiLoading" style="margin-bottom:16px">
        <p style="color:#909399;margin-bottom:12px">基于你的借阅记录，由 DeepSeek AI 智能推荐</p>
        <el-button type="primary" @click="loadAiRecommend" :loading="aiLoading" icon="MagicStick">
          {{ aiLoading ? 'AI 思考中...' : '生成我的推荐' }}
        </el-button>
        <el-tag v-if="!hasAiKey" type="warning" size="small" style="margin-left:8px">需配置 API Key</el-tag>
      </div>
      <div v-if="aiLoading" style="text-align:center;padding:40px">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p style="color:#909399;margin-top:12px">正在分析你的阅读偏好...</p>
      </div>
      <div v-if="aiError" style="color:#f56c6c;margin-bottom:12px">{{ aiError }}</div>
      <el-row :gutter="20" v-if="aiLoaded">
        <el-col :span="8" v-for="item in aiResults" :key="item.bookId" style="margin-bottom:20px">
          <el-card shadow="hover" class="book-card" @click="viewBook(item.book)">
            <div class="book-cover">
              <img :src="item.book?.coverUrl || 'https://via.placeholder.com/200x280?text=No+Cover'" :alt="item.book?.title" />
            </div>
            <div class="book-info">
              <h4>{{ item.book?.title }}</h4>
              <p class="author">{{ item.book?.author }}</p>
              <p style="font-size:12px;color:#666;margin-top:8px;line-height:1.6">{{ item.reason }}</p>
              <p class="meta" style="margin-top:8px">
                <el-tag :type="item.book?.availableStock > 0 ? 'success' : 'danger'" size="small">
                  {{ item.book?.availableStock > 0 ? `可借(${item.book?.availableStock})` : '已借完' }}
                </el-tag>
              </p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Search Results -->
    <div class="section" v-if="searched">
      <h2>搜索结果 <span class="count">({{ books.length }} 条)</span></h2>
      <el-table :data="books" stripe style="width: 100%" @row-click="viewBook" v-loading="loading">
        <el-table-column prop="title" label="书名" min-width="200" />
        <el-table-column prop="author" label="作者" width="150" />
        <el-table-column prop="publisher" label="出版社" width="150" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column label="馆藏位置" width="120">
          <template #default="{ row }">
            <span>{{ row.location || '主馆' }}</span>
            <br/>
            <small class="call-number">{{ row.callNumber || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="row.availableStock > 0 ? 'success' : 'danger'">
              {{ row.availableStock > 0 ? `在馆(${row.availableStock}/${row.totalStock})` : '已借完' }}
            </el-tag>
            <el-tag v-if="row.hasEbook" type="warning" size="small" style="margin-top:4px;display:block;text-align:center">
              <el-icon><Reading /></el-icon> 电子版
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="viewBook(row)">详情</el-button>
            <el-button v-if="row.availableStock > 0" size="small" type="success" @click.stop="borrowBook(row)">
              借书
            </el-button>
            <el-button v-else size="small" @click.stop="reserveBookAction(row)">预约</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Book Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="currentBook?.title" width="700px">
      <el-descriptions v-if="currentBook" :column="2" border>
        <el-descriptions-item label="书名">{{ currentBook.title }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ currentBook.author }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ currentBook.isbn }}</el-descriptions-item>
        <el-descriptions-item label="出版社">{{ currentBook.publisher }}</el-descriptions-item>
        <el-descriptions-item label="索书号">{{ currentBook.callNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="馆藏位置">{{ currentBook.location || '主馆' }}</el-descriptions-item>
        <el-descriptions-item label="可借数量">{{ currentBook.availableStock }}/{{ currentBook.totalStock }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ (currentBook.price || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="电子资源" :span="2">
          <a v-if="currentBook.hasEbook && currentBook.ebookUrl" :href="currentBook.ebookUrl" target="_blank">
            在线阅读/下载
          </a>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ currentBook.description || '暂无简介' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentBook?.availableStock > 0" type="primary" @click="borrowBook(currentBook)">借阅此书</el-button>
        <el-button v-if="currentBook?.availableStock === 0" type="warning" @click="reserveBookAction(currentBook)">预约此书</el-button>
      </template>
    </el-dialog>

    <!-- Borrow Confirm -->
    <el-dialog v-model="borrowVisible" title="确认借阅" width="400px">
      <p>确认借阅《{{ currentBook?.title }}》？</p>
      <p>借阅期限：30天，可续借2次</p>
      <template #footer>
        <el-button @click="borrowVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBorrow" :loading="borrowing">确认借阅</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, TrendCharts, Reading, MagicStick, Loading, Camera } from '@element-plus/icons-vue'
import { searchBooks, getHotBooks, getBook, listBooks } from '../api/book'
import { borrowBook as apiBorrow, reserveBook as apiReserve } from '../api/borrowing'
import { aiRecommend, ocrImage, hasApiKey } from '../utils/deepseek'

const hasAiKey = hasApiKey()

const router = useRouter()
const keyword = ref('')
const books = ref([])
const hotBooks = ref([])
const loading = ref(false)
const searched = ref(false)
const currentBook = ref(null)
const detailVisible = ref(false)
const borrowVisible = ref(false)
const borrowing = ref(false)

// AI 推荐
const aiResults = ref([])
const aiLoading = ref(false)
const aiLoaded = ref(false)
const aiError = ref('')

// OCR 扫码借还书
const ocrFileInput = ref(null)
const ocrLoading = ref(false)
const ocrMode = ref('') // '' | 'borrow' | 'return'

function ocrClick() {
  const token = localStorage.getItem('token')
  if (!token) { ElMessage.warning('请先登录'); return }
  ocrFileInput.value?.click()
}

async function onOcrFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  ocrLoading.value = true
  try {
    // 图片转 base64
    const b64 = await fileToBase64(file)
    // 识别条码/ISBN
    const result = await ocrImage(b64.split(',')[1], 'barcode')
    const isbn = result.barcode || result.isbn
    if (!isbn) { ElMessage.warning('未识别到条码'); return }

    // 按 ISBN 搜索图书
    const cleanIsbn = isbn.replace(/[\s-]/g, '')
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const searchRes = await searchBooks(cleanIsbn)
    const books = searchRes.data || []
    if (!books.length) { ElMessage.warning('未找到对应图书'); return }

    const book = books[0]
    currentBook.value = book

    if (book.availableStock > 0) {
      // 可借 → 直接借书
      await apiBorrow({ bookId: book.id, isbn: book.isbn, bookTitle: book.title })
      ElMessage.success(`借书成功：《${book.title}》`)
      doSearch()
    } else {
      ElMessage.info('该书已被借完，已为您打开预约页面')
      // 打开详情对话框让用户预约
      detailVisible.value = true
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '操作失败')
  } finally {
    ocrLoading.value = false
    e.target.value = ''
  }
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

onMounted(() => {
  loadHotBooks()
})

async function loadHotBooks() {
  try {
    const res = await getHotBooks(12)
    hotBooks.value = res.data || []
  } catch (e) { /* silent */ }
}

async function loadAiRecommend() {
  aiLoading.value = true
  aiError.value = ''
  try {
    // 获取借阅记录
    const token = localStorage.getItem('token')
    if (!token) { aiError.value = '请先登录'; aiLoading.value = false; return }
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const borrowRes = await fetch(`/api/borrow/my?pageSize=50`, {
      headers: { 'Authorization': 'Bearer ' + token }
    }).then(r => r.json())
    const borrows = borrowRes.data?.records || []

    // 获取全部在库图书
    const bookRes = await listBooks({ pageSize: 200 })
    const allBooks = bookRes.data?.records || []

    // 构建借阅历史
    const history = borrows.map(b => ({
      title: b.bookTitle,
      author: b.bookAuthor || '',
      category: b.category || ''
    }))

    // 调用 DeepSeek 推荐
    const suggestions = await aiRecommend(history, allBooks, 6)

    // 匹配图书详情
    const bookMap = {}
    allBooks.forEach(b => { bookMap[b.id] = b })
    aiResults.value = suggestions
      .filter(s => bookMap[s.bookId])
      .map(s => ({ ...s, book: bookMap[s.bookId] }))
    aiLoaded.value = true
    if (!aiResults.value.length) aiError.value = '暂无推荐结果'
  } catch (e) {
    aiError.value = e.message || '推荐失败'
  } finally {
    aiLoading.value = false
  }
}

async function doSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await searchBooks(keyword.value.trim())
    books.value = res.data || []
    if (books.value.length === 0) {
      ElMessage.info('未找到匹配的资源')
    }
  } catch (e) {
    ElMessage.error('检索失败')
  } finally {
    loading.value = false
  }
}

function viewBook(book) {
  currentBook.value = book
  detailVisible.value = true
}

async function borrowBook(book) {
  currentBook.value = book
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再借阅')
    router.push('/login')
    return
  }
  borrowVisible.value = true
}

async function confirmBorrow() {
  if (!currentBook.value) return
  borrowing.value = true
  try {
    await apiBorrow({
      bookId: currentBook.value.id,
      isbn: currentBook.value.isbn,
      bookTitle: currentBook.value.title
    })
    ElMessage.success('借阅成功')
    borrowVisible.value = false
    detailVisible.value = false
    // Refresh
    if (searched.value) doSearch()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '借阅失败')
  } finally {
    borrowing.value = false
  }
}

async function reserveBookAction(book) {
  currentBook.value = book
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再预约')
    router.push('/login')
    return
  }
  try {
    await apiReserve({
      bookId: book.id,
      isbn: book.isbn,
      bookTitle: book.title
    })
    ElMessage.success('预约成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '预约失败')
  }
}
</script>

<style scoped>
.opac-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.search-hero {
  text-align: center; padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px; color: white; margin-bottom: 30px;
}
.search-hero h1 { font-size: 36px; margin-bottom: 8px; }
.search-hero .subtitle { font-size: 18px; opacity: .8; margin-left: 8px; }
.search-hero p { opacity: .9; margin-bottom: 24px; }
.search-box { display: flex; gap: 12px; max-width: 700px; margin: 0 auto; }
.search-box .el-input { flex: 1; }
.section { margin-top: 30px; }
.section h2 { font-size: 20px; margin-bottom: 16px; display: flex; align-items: center; gap: 6px; }
.count { font-size: 14px; color: #999; font-weight: normal; }
.book-card { cursor: pointer; margin-bottom: 20px; transition: transform .2s; }
.book-card:hover { transform: translateY(-4px); }
.book-cover { text-align: center; padding: 10px; }
.book-cover img { width: 120px; height: 160px; object-fit: cover; border-radius: 4px; }
.book-info h4 { margin: 8px 0 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.book-info .author { color: #666; font-size: 13px; margin-bottom: 8px; }
.book-info .meta { display: flex; gap: 4px; }
.call-number { color: #999; font-size: 12px; }
</style>
