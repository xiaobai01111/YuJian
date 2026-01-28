package plugins.sms.services

import groovy.transform.CompileStatic
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.text.SimpleDateFormat

/**
 * 腾讯云短信服务
 * 文档: https://cloud.tencent.com/document/product/382/55981
 */
@CompileStatic
class TencentSmsProvider implements SmsProvider {
    
    private static final String ENDPOINT = "sms.tencentcloudapi.com"
    private static final String SERVICE = "sms"
    private static final String VERSION = "2021-01-11"
    private static final String ACTION = "SendSms"
    private static final String REGION = "ap-guangzhou"
    
    def ctx
    
    TencentSmsProvider(def ctx) {
        this.ctx = ctx
    }
    
    @Override
    Map send(String phone, String code) {
        def secretId = ctx.config.get("tencent.secretId", "") as String
        def secretKey = ctx.config.get("tencent.secretKey", "") as String
        def appId = ctx.config.get("tencent.appId", "") as String
        def signName = ctx.config.get("tencent.signName", "") as String
        def templateId = ctx.config.get("tencent.templateId", "") as String
        
        if (!secretId || !secretKey) {
            return [success: false, message: "腾讯云SecretKey未配置"]
        }
        if (!appId || !signName || !templateId) {
            return [success: false, message: "短信AppId、签名或模板未配置"]
        }
        
        try {
            def timestamp = (System.currentTimeMillis() / 1000) as Long
            def date = new SimpleDateFormat("yyyy-MM-dd").with {
                setTimeZone(TimeZone.getTimeZone("UTC"))
                format(new Date(timestamp * 1000))
            }
            
            // 构建请求体
            def payload = ctx.json.stringify([
                PhoneNumberSet: ["+86${phone}"],
                SmsSdkAppId: appId,
                SignName: signName,
                TemplateId: templateId,
                TemplateParamSet: [code]
            ])
            
            // 构建签名
            def headers = buildHeaders(secretId, secretKey, timestamp, date, payload)
            
            def response = ctx.http.post("https://${ENDPOINT}", payload, [
                headers: headers
            ])
            
            def result = ctx.json.parse(response.body)
            def resp = result.Response
            
            if (resp.Error) {
                ctx.log.warn("腾讯云短信发送失败: ${resp.Error.Code} - ${resp.Error.Message}")
                return [success: false, message: mapErrorMessage(resp.Error.Code as String)]
            }
            
            def sendStatus = resp.SendStatusSet?.getAt(0)
            if (sendStatus?.Code == "Ok") {
                return [success: true, bizId: sendStatus.SerialNo]
            } else {
                return [success: false, message: mapErrorMessage(sendStatus?.Code as String)]
            }
        } catch (Exception e) {
            ctx.log.error("腾讯云短信发送异常: ${e.message}", e)
            return [success: false, message: "发送失败: ${e.message}"]
        }
    }
    
    private Map<String, String> buildHeaders(String secretId, String secretKey, 
                                              long timestamp, String date, String payload) {
        def algorithm = "TC3-HMAC-SHA256"
        def httpRequestMethod = "POST"
        def canonicalUri = "/"
        def canonicalQueryString = ""
        def canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:${ENDPOINT}\n"
        def signedHeaders = "content-type;host"
        def hashedRequestPayload = sha256Hex(payload)
        
        def canonicalRequest = [
            httpRequestMethod,
            canonicalUri,
            canonicalQueryString,
            canonicalHeaders,
            signedHeaders,
            hashedRequestPayload
        ].join("\n")
        
        def credentialScope = "${date}/${SERVICE}/tc3_request"
        def hashedCanonicalRequest = sha256Hex(canonicalRequest)
        def stringToSign = [
            algorithm,
            timestamp,
            credentialScope,
            hashedCanonicalRequest
        ].join("\n")
        
        def secretDate = hmacSha256("TC3${secretKey}".getBytes("UTF-8"), date)
        def secretService = hmacSha256(secretDate, SERVICE)
        def secretSigning = hmacSha256(secretService, "tc3_request")
        def signature = hmacSha256Hex(secretSigning, stringToSign)
        
        def authorization = "${algorithm} Credential=${secretId}/${credentialScope}, " +
                           "SignedHeaders=${signedHeaders}, Signature=${signature}"
        
        return [
            "Authorization": authorization,
            "Content-Type": "application/json; charset=utf-8",
            "Host": ENDPOINT,
            "X-TC-Action": ACTION,
            "X-TC-Timestamp": timestamp.toString(),
            "X-TC-Version": VERSION,
            "X-TC-Region": REGION
        ]
    }
    
    private byte[] hmacSha256(byte[] key, String msg) {
        def mac = Mac.getInstance("HmacSHA256")
        mac.init(new SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(msg.getBytes("UTF-8"))
    }
    
    private String hmacSha256Hex(byte[] key, String msg) {
        return bytesToHex(hmacSha256(key, msg))
    }
    
    private String sha256Hex(String s) {
        def md = MessageDigest.getInstance("SHA-256")
        return bytesToHex(md.digest(s.getBytes("UTF-8")))
    }
    
    private String bytesToHex(byte[] bytes) {
        def sb = new StringBuilder()
        for (byte b : bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
    
    private String mapErrorMessage(String code) {
        switch (code) {
            case "LimitExceeded.PhoneNumberDailyLimit": return "单个手机号日发送超限"
            case "LimitExceeded.PhoneNumberOneHourLimit": return "单个手机号1小时发送超限"
            case "LimitExceeded.PhoneNumberThirtySecondLimit": return "单个手机号30秒发送超限"
            case "InvalidParameterValue.IncorrectPhoneNumber": return "手机号格式错误"
            case "FailedOperation.SignatureIncorrectOrUnapproved": return "签名未审批或格式错误"
            case "FailedOperation.TemplateIncorrectOrUnapproved": return "模板未审批或格式错误"
            case "AuthFailure.SecretIdNotFound": return "SecretId无效"
            case "AuthFailure.SignatureFailure": return "签名验证失败"
            default: return "发送失败(${code})"
        }
    }
}
