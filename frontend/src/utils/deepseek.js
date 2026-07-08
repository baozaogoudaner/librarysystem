/**
 * 小米 MiMo AI API 调用工具（仅用于 OCR 识别）
 * 推荐功能已改为本地算法
 */
const API_URL = 'https://api.xiaomimimo.com/v1/chat/completions'
const MODEL_TEXT = 'mimo-v2.5-pro'
const MODEL_VISION = 'mimo-v2.5'

const DEFAULT_API_KEY = 'sk-ct533uevsewp31jokrx04nvdtcnjigi5ju7a02hrgbviersg'

function getApiKey() {
  return localStorage.getItem('mimo_api_key') || DEFAULT_API_KEY
}

export function hasApiKey() {
  return getApiKey().length > 20
}

/** 核心：调用 MiMo API */
export async function callMiMo(messages, options = {}) {
  const key = getApiKey()
  if (!key) throw new Error('请先配置 API Key')
  const model = options.vision ? MODEL_VISION : MODEL_TEXT
  const res = await fetch(API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${key}`,
      'api-key': key
    },
    body: JSON.stringify({
      model, messages,
      temperature: options.temperature ?? 0.3,
      max_tokens: options.max_tokens ?? 2000,
      stream: false
    })
  })
  if (!res.ok) {
    const err = await res.text()
    throw new Error(`MiMo API 错误 (${res.status}): ${err}`)
  }
  const data = await res.json()
  return data.choices?.[0]?.message?.content || ''
}

// ==================== OCR 识别 ====================

export async function ocrImage(imageBase64, type) {
  if (!hasApiKey()) return mockOcr(imageBase64, type)
  const prompts = {
    isbn: '这张图片是图书封面或ISBN条码。请识别其中的ISBN号、书名、作者、出版社。返回JSON格式：{"isbn":"","title":"","author":"","publisher":""}。',
    barcode: '这张图片包含图书条码。请识别条码中的数字。返回JSON格式：{"barcode":""}。',
    callnumber: '这张图片是索书号标签。请识别索书号。返回JSON格式：{"callNumber":"","location":""}。'
  }
  const content = [
    { type: 'text', text: prompts[type] || prompts.isbn },
    { type: 'image_url', image_url: { url: `data:image/jpeg;base64,${imageBase64}` } }
  ]
  const text = await callMiMo([{ role: 'user', content }], { vision: true, temperature: 0.1 })
  try {
    const jsonMatch = text.match(/\{[\s\S]*?\}/)
    return jsonMatch ? JSON.parse(jsonMatch[0]) : { raw: text }
  } catch {
    return { raw: text }
  }
}

function mockOcr(imageBase64, type) {
  if (type === 'barcode') return { barcode: '978-7-111-68412-3' }
  if (type === 'callnumber') return { callNumber: 'TP312/EC12', location: '3F-A区-12架' }
  return { isbn: '978-7-111-68412-3', title: 'Java编程思想（第4版）', author: 'Bruce Eckel', publisher: '机械工业出版社' }
}

// ==================== 智能推荐（本地算法） ====================

/**
 * 基于内容的图书推荐（本地算法，无需 API）
 * 分析用户借阅历史，按分类+作者相似度评分
 */
export async function aiRecommend(borrowHistory, allBooks, limit = 5) {
  const borrowedTitles = new Set(borrowHistory.map(h => h.title))

  // 无借阅记录 → 返回热门图书
  if (!borrowHistory.length) {
    return allBooks.slice(0, limit).map(b => ({
      bookId: b.id, reason: '热门推荐图书'
    }))
  }

  // 1. 统计用户偏好
  const catScores = {}
  const authorScores = {}
  borrowHistory.forEach(h => {
    if (h.category) catScores[h.category] = (catScores[h.category] || 0) + 1
    if (h.author) authorScores[h.author] = (authorScores[h.author] || 0) + 1
  })

  // 2. 对候选图书评分
  const scored = allBooks
    .filter(b => !borrowedTitles.has(b.title))
    .map(b => {
      let score = 0
      if (b.category && catScores[b.category]) score += (catScores[b.category] / borrowHistory.length) * 0.5
      if (b.author && authorScores[b.author]) score += 0.3

      let reason = ''
      if (b.category && catScores[b.category]) {
        const topCat = Object.entries(catScores).sort((a,b) => b[1]-a[1])[0][0]
        const borrowed = borrowHistory.filter(h => h.category === topCat).map(h => '《' + h.title + '》').slice(0,2).join('、')
        reason = '因为您读过' + borrowed + '，推荐同属「' + topCat + '」分类的《' + b.title + '》'
      } else {
        reason = '推荐阅读《' + b.title + '》'
      }
      return { bookId: b.id, score, reason }
    })
    .sort((a, b) => b.score - a.score)

  // 3. 取评分最高的
  const result = scored.slice(0, limit)

  // 4. 不足时补充热门
  if (result.length < limit) {
    const ids = new Set(result.map(r => r.bookId))
    allBooks.filter(b => !borrowedTitles.has(b.title) && !ids.has(b.id))
      .slice(0, limit - result.length)
      .forEach(b => result.push({ bookId: b.id, score: 0, reason: '热门推荐图书' }))
  }

  return result.map(r => ({ bookId: r.bookId, reason: r.reason }))
}
