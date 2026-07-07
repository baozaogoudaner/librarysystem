package com.library.book.ocr;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 核心服务
 * - 条码识别：ZXing 纯本地解码
 * - 文字识别：百度OCR API（可配），无API时使用模拟数据
 */
@Slf4j
@Service
public class OcrService {

    /** 百度OCR - 获取 token 的 URL */
    private static final String BAIDU_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    /** 百度OCR - 通用文字识别 URL */
    private static final String BAIDU_OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    @Value("${ocr.baidu.api-key:}")
    private String baiduApiKey;

    @Value("${ocr.baidu.secret-key:}")
    private String baiduSecretKey;

    /** 是否启用模拟模式（无API时返回示例数据） */
    private boolean isMockMode() {
        return baiduApiKey == null || baiduApiKey.isEmpty()
                || baiduSecretKey == null || baiduSecretKey.isEmpty();
    }

    // ==================== 条码识别 ====================

    /**
     * 从图片中解码条码/二维码
     *
     * @param file 上传的图片
     * @return 解码出的文本内容（通常是ISBN或图书编码）
     */
    public String decodeBarcode(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new RuntimeException("无法读取图片，请确认上传的是有效的图片文件");
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // 尝试多种条码格式
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(
                    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39,
                    BarcodeFormat.QR_CODE, BarcodeFormat.ITF,
                    BarcodeFormat.UPC_A, BarcodeFormat.UPC_E));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            String decoded = result.getText();
            log.info("条码识别成功: {} (格式: {})", decoded, result.getBarcodeFormat());
            return decoded;

        } catch (NotFoundException e) {
            log.warn("图片中未检测到条码/二维码");
            throw new RuntimeException("未检测到条码或二维码，请确保图片中包含清晰的条码");
        } catch (Exception e) {
            log.error("条码识别失败: {}", e.getMessage());
            throw new RuntimeException("条码识别失败: " + e.getMessage());
        }
    }

    // ==================== 文字识别 ====================

    /**
     * 从图片中识别文字（百度OCR或模拟模式）
     *
     * @param file 上传的图片
     * @return 识别出的文字列表（按行）
     */
    public List<String> recognizeText(MultipartFile file) {
        if (isMockMode()) {
            log.info("百度OCR未配置，使用模拟模式");
            return mockRecognizeText(file);
        }
        return baiduOcr(file);
    }

    /**
     * 百度OCR API 调用
     */
    private List<String> baiduOcr(MultipartFile file) {
        try {
            // 1. 获取 access_token
            String tokenResp = HttpUtil.post(BAIDU_TOKEN_URL,
                    "grant_type=client_credentials&client_id=" + baiduApiKey
                            + "&client_secret=" + baiduSecretKey, 5000);
            JSONObject tokenJson = JSONUtil.parseObj(tokenResp);
            String accessToken = tokenJson.getStr("access_token");
            if (accessToken == null) {
                log.warn("百度OCR获取token失败: {}", tokenResp);
                return mockRecognizeText(file);
            }

            // 2. 图片转 base64
            byte[] bytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);

            // 3. 调用OCR接口
            String ocrResp = HttpUtil.post(BAIDU_OCR_URL + "?access_token=" + accessToken,
                    "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8"), 10000);
            JSONObject ocrJson = JSONUtil.parseObj(ocrResp);

            // 4. 解析结果
            List<String> texts = new ArrayList<>();
            JSONArray words = ocrJson.getJSONArray("words_result");
            if (words != null) {
                for (int i = 0; i < words.size(); i++) {
                    texts.add(words.getJSONObject(i).getStr("words", ""));
                }
            }
            log.info("百度OCR识别到 {} 行文字", texts.size());
            return texts;

        } catch (Exception e) {
            log.error("百度OCR调用失败: {}", e.getMessage());
            return mockRecognizeText(file);
        }
    }

    // ==================== ISBN 提取 ====================

    /**
     * 从文字中提取 ISBN
     * 匹配 ISBN-10 和 ISBN-13 格式
     */
    public String extractIsbn(List<String> texts) {
        // ISBN-13: 978 或 979 开头，后跟 10 位数字
        Pattern isbn13 = Pattern.compile("(?:ISBN[\\s:]*)?(97[89]\\d{10})(?:\\s|$)");
        // ISBN-10: 10位数字或数字+X结尾
        Pattern isbn10 = Pattern.compile("(?:ISBN[\\s:]*)?(\\d{9}[\\dXx])(?:\\s|$)");

        for (String line : texts) {
            Matcher m = isbn13.matcher(line);
            if (m.find()) return m.group(1);

            m = isbn10.matcher(line);
            if (m.find()) return m.group(1).toUpperCase();
        }
        return null;
    }

    // ==================== 索书号提取 ====================

    /**
     * 从文字中提取索书号
     * 常见格式：TP312/EC12 或 I247.5/1234 等
     */
    public String extractCallNumber(List<String> texts) {
        Pattern callNo = Pattern.compile(
                "[A-Z]{1,3}\\d+(?:\\.\\d+)?(?:/[A-Za-z0-9.-]+)?");

        for (String line : texts) {
            Matcher m = callNo.matcher(line);
            if (m.find()) return m.group();
        }
        return null;
    }

    // ==================== 模拟模式 ====================

    /**
     * 模拟OCR识别（无百度API时用于演示）
     * 根据文件名/内容返回示例数据
     */
    private List<String> mockRecognizeText(MultipartFile file) {
        String fileName = file.getOriginalFilename() != null ?
                file.getOriginalFilename().toLowerCase() : "";

        // 根据文件名返回模拟数据，方便前端测试
        if (fileName.contains("isbn") || fileName.contains("978")) {
            return Arrays.asList("ISBN 978-7-111-68412-3", "Java编程思想（第4版）", "Bruce Eckel");
        }
        if (fileName.contains("call") || fileName.contains("索书")) {
            return Arrays.asList("TP312/EC12", "3F-A区-12架", "Java编程思想");
        }
        // 默认模拟数据
        return Arrays.asList("ISBN 978-7-302-58123-5", "算法导论（第4版）", "Thomas H. Cormen");
    }
}
