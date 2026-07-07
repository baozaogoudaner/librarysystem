/**
 * DeepSeek API 调用工具
 * 兼容 OpenAI 格式，支持聊天补全和视觉识别
 */
const DEEPSEEK_API_URL = 'https://api.deepseek.com/chat/completions'
const DEEPSEEK_MODEL = 'deepseek-chat'

/** DeepSeek API Key（优先用 localStorage，没有则用内置默认） */
const DEFAULT_API_KEY = 'sk-08ec4efed43548fdad922c7d7675dacf'

function getApiKey() {
  return localStorage.getItem('deepseek_api_key') || DEFAULT_API_KEY
}

/** 是否有可用 Key */
export function hasApiKey() {
  return getApiKey().length > 20
}

/**
 * 核心：调用 DeepSeek Chat API
 * @param {Array} messages - 消息列表 [{role, content}]
 * @param {Object} options - { temperature, max_tokens }
 * @returns {string} 返回文本
 */
export async function callDeepSeek(messages, options = {}) {
  const key = getApiKey()
  if (!key) throw new Error('请先配置 DeepSeek API Key')

  const res = await fetch(DEEPSEEK_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${key}`
    },
    body: JSON.stringify({
      model: DEEPSEEK_MODEL,
      messages,
      temperature: options.temperature ?? 0.3,
      max_tokens: options.max_tokens ?? 2000,
      stream: false
    })
  })
  if (!res.ok) {
    const err = await res.text()
    throw new Error(`DeepSeek API 错误 (${res.status}): ${err}`)
  }
  const data = await res.json()
  return data.choices?.[0]?.message?.content || ''
}

// ==================== OCR 识别 ====================

/**
 * OCR 识别图片（支持 ISBN / 条码 / 索书号）
 * @param {string} imageBase64 - 图片 base64（不含 data:image 前缀）
 * @param {'isbn'|'barcode'|'callnumber'} type - 识别类型
 * @returns {Object} 结构化识别结果
 */
export async function ocrImage(imageBase64, type) {
  if (!hasApiKey()) return mockOcr(imageBase64, type)

  const prompts = {
    isbn: '这张图片是图书封面或ISBN条码。请识别其中的ISBN号、书名、作者、出版社。返回JSON格式：{"isbn":"","title":"","author":"","publisher":""}。如果某个字段无法识别则留空。',
    barcode: '这张图片包含图书条码。请识别条码中的数字（通常是ISBN）。返回JSON格式：{"barcode":""}。',
    callnumber: '这张图片是索书号标签。请识别索书号（如 TP312/EC12）。返回JSON格式：{"callNumber":"","location":""}。'
  }

  const content = [
    { type: 'text', text: prompts[type] || prompts.isbn },
    { type: 'image_url', image_url: { url: `data:image/jpeg;base64,${imageBase64}` } }
  ]

  const text = await callDeepSeek([{ role: 'user', content }], { temperature: 0.1 })
  // 尝试从返回中提取 JSON
  try {
    const jsonMatch = text.match(/\{[\s\S]*?\}/)
    return jsonMatch ? JSON.parse(jsonMatch[0]) : { raw: text }
  } catch {
    return { raw: text }
  }
}

/** 模拟 OCR（无 Key 时使用） */
function mockOcr(imageBase64, type) {
  if (type === 'barcode') return { barcode: '978-7-111-68412-3' }
  if (type === 'callnumber') return { callNumber: 'TP312/EC12', location: '3F-A区-12架' }
  return { isbn: '978-7-111-68412-3', title: 'Java编程思想（第4版）', author: 'Bruce Eckel', publisher: '机械工业出版社' }
}

// ==================== 智能推荐 ====================

/**
 * AI 智能推荐图书
 * @param {Array} borrowHistory - 用户借阅历史 [{title, author, category}]
 * @param {Array} allBooks - 全部在库图书 [{id, title, author, category, description}]
 * @param {number} limit - 推荐数量
 * @returns {Array} [{bookId, reason}] 推荐列表 + 理由
 */
export async function aiRecommend(borrowHistory, allBooks, limit = 5) {
  if (!hasApiKey()) return mockRecommend(borrowHistory, allBooks, limit)

  // 构建用户阅读画像
  const historyDesc = borrowHistory.map(b =>
    `- 《${b.title}》（${b.author}，${b.category}）`
  ).join('\n')

  const bookList = allBooks.map(b =>
    `{id:${b.id}, title:"${b.title}", author:"${b.author}", category:"${b.category}"}`
  ).join('\n')

  const prompt = `你是一个图书馆智能推荐助手。以下是某位读者的借阅历史：

${historyDesc || '（新读者，暂无借阅记录）'}

请从以下馆藏图书中推荐 ${limit} 本最合适的书，每本给出具体推荐理由。
要求：推荐的图书不能是读者已经借过的书。
返回格式（严格 JSON 数组）：
[{"bookId": 数字, "reason": "推荐理由"}]

馆藏图书列表：
${bookList}`

  const text = await callDeepSeek([
    { role: 'system', content: '你是一个专业的图书推荐助手，返回严格 JSON 格式。' },
    { role: 'user', content: prompt }
  ], { temperature: 0.7, max_tokens: 3000 })

  try {
    const jsonMatch = text.match(/\[[\s\S]*?\]/)
    return jsonMatch ? JSON.parse(jsonMatch[0]) : []
  } catch {
    return []
  }
}

/** 模拟推荐（无 Key 时使用现有 Jaccard 算法） */
function mockRecommend(borrowHistory, allBooks, limit) {
  if (!borrowHistory.length) return allBooks.slice(0, limit).map(b => ({
    bookId: b.id, reason: '热门推荐图书'
  }))
  // 简单按分类匹配
  const favCat = borrowHistory[0]?.category
  const matched = allBooks.filter(b =>
    b.category === favCat && !borrowHistory.some(h => h.title === b.title)
  )
  return matched.slice(0, limit).map(b => ({
    bookId: b.id, reason: `与您读过的《${borrowHistory[0].title}》同属 ${favCat} 分类`
  }))
}
