<template>
  <div class="books-page">
    <div class="page-header">
      <h2>图书与资源管理</h2>
      <div>
        <el-button @click="ocrUploadClick"><el-icon><Camera /></el-icon> 拍照识别</el-button>
        <input type="file" ref="ocrInput" accept="image/*" style="display:none" @change="onOcrFile" />
        <el-button type="primary" @click="showAddDialog"><el-icon><Plus /></el-icon> 入库新书</el-button>
      </div>
    </div>

    <!-- Stats -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card shadow="never"><el-statistic :title="s.label" :value="s.value" /></el-card>
      </el-col>
    </el-row>

    <!-- Filters -->
    <el-card style="margin-bottom: 16px">
      <el-form :inline="true">
        <el-form-item label="分类">
          <el-select v-model="filters.category" clearable placeholder="全部分类" @change="loadBooks">
            <el-option v-for="c in categories" :key="c.category" :label="`${c.category} (${c.count})`" :value="c.category" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="书名/作者/ISBN" clearable @keyup.enter="loadBooks" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBooks">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card>
      <el-table :data="books" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="书名" min-width="180" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="130" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.availableStock <= 3 ? 'red' : 'inherit' }">
              {{ row.availableStock }}/{{ row.totalStock }}
            </span>
            <el-tag v-if="row.availableStock <= 3 && row.availableStock > 0" type="danger" size="small" style="margin-left:4px">低</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="borrowCount" label="借阅次数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" size="small" type="danger" @click="handleOffline(row)">下架</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="handleReOnline(row)">上架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="loadBooks" />
      </div>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingBook?.id ? '编辑图书' : '新增图书'" width="650px">
      <el-form :model="bookForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="ISBN"><el-input v-model="bookForm.isbn" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="书名"><el-input v-model="bookForm.title" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="作者"><el-input v-model="bookForm.author" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社"><el-input v-model="bookForm.publisher" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类"><el-input v-model="bookForm.category" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格"><el-input-number v-model="bookForm.price" :precision="2" :min="0" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总库存"><el-input-number v-model="bookForm.totalStock" :min="1" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="馆藏位置"><el-input v-model="bookForm.location" placeholder="如：3F-A区-12架" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="索书号"><el-input v-model="bookForm.callNumber" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面URL"><el-input v-model="bookForm.coverUrl" /></el-form-item>
        <el-form-item label="电子版URL"><el-input v-model="bookForm.ebookUrl" placeholder="有电子资源时填写" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="bookForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBook" :loading="saving">
          {{ editingBook?.id ? '保存修改' : '确认入库' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Camera } from '@element-plus/icons-vue'
import { listBooks, addBook, updateBook, offlineBook, reOnlineBook, getCategoryStats, getLowStockBooks } from '../../api/book'
import { ocrImage } from '../../utils/deepseek'

const books = ref([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const categories = ref([])
const stats = ref([])
const filters = reactive({ category: '', keyword: '' })
const dialogVisible = ref(false)
const editingBook = ref(null)
const bookForm = reactive({
  isbn: '', title: '', author: '', publisher: '', category: '',
  price: 0, totalStock: 1, location: '', callNumber: '',
  coverUrl: '', ebookUrl: '', description: ''
})

// OCR 识别
const ocrInput = ref(null)
const ocring = ref(false)

function ocrUploadClick() { ocrInput.value?.click() }

async function onOcrFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  ocring.value = true
  try {
    // 图片转 base64
    const base64 = await fileToBase64(file)
    // 先尝试识别 ISBN
    const result = await ocrImage(base64.split(',')[1], 'isbn')
    if (result.isbn || result.title) {
      // 打开入库对话框并自动填充
      showAddDialog()
      if (result.isbn) bookForm.isbn = result.isbn
      if (result.title) bookForm.title = result.title
      if (result.author) bookForm.author = result.author
      if (result.publisher) bookForm.publisher = result.publisher
      ElMessage.success('识别成功，已自动填充表单')
    } else {
      ElMessage.warning('未能识别出图书信息，请手动录入')
    }
  } catch (err) {
    ElMessage.error('识别失败: ' + err.message)
  } finally {
    ocring.value = false
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

onMounted(() => { loadBooks(); loadStats() })

async function loadBooks() {
  loading.value = true
  try {
    const res = await listBooks({ pageNum: pageNum.value, pageSize: pageSize.value, ...filters })
    const d = res.data
    books.value = d.records
    total.value = d.total
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const [catRes, lowRes] = await Promise.all([getCategoryStats(), getLowStockBooks()])
    categories.value = catRes.data || []
    stats.value = [
      { label: '总藏书量', value: total.value },
      { label: '分类数', value: categories.value.length },
      { label: '低库存预警', value: (lowRes.data || []).length },
      { label: '今日借阅', value: '-' }
    ]
  } catch (e) { /* silent */ }
}

function statusType(s) { return { 0: 'success', 1: 'warning', 2: 'info', 3: 'danger' }[s] || 'info' }
function statusText(s) { return { 0: '在库', 1: '借出', 2: '下架', 3: '遗失' }[s] || '未知' }

function showAddDialog() {
  editingBook.value = null
  Object.assign(bookForm, { isbn: '', title: '', author: '', publisher: '', category: '', price: 0, totalStock: 1, location: '', callNumber: '', coverUrl: '', ebookUrl: '', description: '' })
  dialogVisible.value = true
}

function showEditDialog(book) {
  editingBook.value = book
  Object.assign(bookForm, book)
  dialogVisible.value = true
}

async function saveBook() {
  saving.value = true
  try {
    if (editingBook.value?.id) {
      await updateBook(editingBook.value.id, bookForm)
      ElMessage.success('更新成功')
    } else {
      await addBook(bookForm)
      ElMessage.success('入库成功')
    }
    dialogVisible.value = false
    loadBooks()
  } catch (e) { ElMessage.error(e.response?.data?.message || '操作失败') }
  finally { saving.value = false }
}

async function handleOffline(book) {
  await ElMessageBox.confirm(`确认下架《${book.title}》？`, '下架确认', { type: 'warning' })
  try {
    await offlineBook(book.id)
    ElMessage.success('已下架')
    loadBooks()
  } catch (e) { ElMessage.error(e.response?.data?.message || '下架失败') }
}

async function handleReOnline(book) {
  try {
    await reOnlineBook(book.id)
    ElMessage.success('已上架')
    loadBooks()
  } catch (e) { ElMessage.error(e.response?.data?.message || '上架失败') }
}
</script>

<style scoped>
.books-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
