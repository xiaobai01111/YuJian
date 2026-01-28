package plugins.sms.services

import groovy.transform.CompileStatic
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import java.text.SimpleDateFormat

/**
 * 阿里云短信服务
 * 文档: https://help.aliyun.com/document_detail/101414.html
 */
@CompileStatic
class AliyunSmsProvider implements SmsProvider {
    
    private static final String ENDPOINT = "https://dysmsapi.aliyuncs.com"
    private static final String VERSION = "2017-05-25"
    private static final String ACTION = "SendSms"
    
    def ctx
    
    AliyunSmsProvider(def ctx) {
        this.ctx = ctx
    }
    
    @Override
    Map send(String phone, String code) {
        def accessKeyId = ctx.config.get("aliyun.accessKeyId", "") as String
        def accessKeySecret = ctx.config.get("aliyun.accessKeySecret", "") as String
        def signName = ctx.config.get("aliyun.signName", "") as String
        def templateCode = ctx.config.get("aliyun.templateCode", "") as String
        
        if (!accessKeyId || !accessKeySecret) {
            return [success: false, message: "阿里云AccessKey未配置"]
        }
        if (!signName || !templateCode) {
            return [success: false, message: "短信签名或模板未配置"]
        }
        
        try {
            def params = buildParams(accessKeyId, signName, templateCode, phone, code)
            def signature = sign(params, accessKeySecret)
            params.put("Signature", signature)
            
            def queryString = params.collect { k, v -> 
                "${encode(k)}=${encode(v)}" 
            }.join("&")
            
            def response = ctx.http.get("${ENDPOINT}?${queryString}")
            def result = ctx.json.parse(response.body)
            
            if (result.Code == "OK") {
                return [success: true, bizId: result.BizId]
            } else {
                ctx.log.warn("阿里云短信发送失败: ${result.Code} - ${result.Message}")
                return [success: false, message: mapErrorMessage(result.Code as String)]
            }
        } catch (Exception e) {
            ctx.log.error("阿里云短信发送异常: ${e.message}", e)
            return [success: false, message: "发送失败: ${e.message}"]
        }
    }
    
    private Map<String, String> buildParams(String accessKeyId, String signName, 
                                             String templateCode, String phone, String code) {
        def timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").with {
            setTimeZone(TimeZone.getTimeZone("UTC"))
            format(new Date())
        }
        
        def params = new TreeMap<String, String>()
        params.put("AccessKeyId", accessKeyId)
        params.put("Action", ACTION)
        params.put("Format", "JSON")
        params.put("PhoneNumbers", phone)
        params.put("RegionId", "cn-hangzhou")
        params.put("SignName", signName)
        params.put("SignatureMethod", "HMAC-SHA1")
        params.put("SignatureNonce", UUID.randomUUID().toString())
        params.put("SignatureVersion", "1.0")
        params.put("TemplateCode", templateCode)
        params.put("TemplateParam", "{\"code\":\"${code}\"}")
        params.put("Timestamp", timestamp)
        params.put("Version", VERSION)
        
        return params
    }
    
    private String sign(Map<String, String> params, String secret) {
        def sortedQuery = params.collect { k, v ->
            "${encode(k)}=${encode(v)}"
        }.join("&")
        
        def stringToSign = "GET&${encode("/")}&${encode(sortedQuery)}"
        
        def mac = Mac.getInstance("HmacSHA1")
        mac.init(new SecretKeySpec("${secret}&".getBytes("UTF-8"), "HmacSHA1"))
        def signData = mac.doFinal(stringToSign.getBytes("UTF-8"))
        
        return Base64.getEncoder().encodeToString(signData)
    }
    
    private String encode(String value) {
        URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
    
    private String mapErrorMessage(String code) {
        switch (code) {
            case "isv.BUSINESS_LIMIT_CONTROL": return "发送频率超限"
            case "isv.MOBILE_NUMBER_ILLEGAL": return "手机号格式错误"
            case "isv.TEMPLATE_MISSING_PARAMETERS": return "模板参数缺失"
            case "isv.INVALID_PARAMETERS": return "参数无效"
            case "SignatureDoesNotMatch": return "签名验证失败，请检查AccessKey"
            default: return "发送失败(${code})"
        }
    }
}
