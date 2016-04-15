package com.oss.action;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.struts2.ServletActionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.opensymphony.xwork2.ActionSupport;

/**
 * @阿里云OSS——Web直传
 * @author 凡瑶
 * @time 2015-12-14
 */
@Controller
@Scope("prototype")
public class OssAction extends ActionSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getInfo() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		String parameter = request.getQueryString();
		InputStream is = getClass().getResourceAsStream("/oss.properties");
		Properties properties = new Properties();
		try {
			properties.load(is);
		} catch (Exception e) {
			System.err.println("不能读取属性文件. " + "请确保db.properties在CLASSPATH指定的路径中");
		}

		String id = properties.getProperty("oss.id");
		String key = properties.getProperty("oss.key");
		String bucket = properties.getProperty("oss.bucket");
		String endpoint = properties.getProperty("oss.endpoint");
		String host = "http://" + bucket + "." + endpoint;
		String callbackUrl = properties.getProperty("oss.callbackUrl");
		OSSClient client = new OSSClient(endpoint, id, key);
		
		String callback_body = "{\"callbackUrl\":\"" + callbackUrl + "\",\"callbackBody\":\"filename=${object}&size=${size}&mimeType=${mimeType}&height=${imageInfo.height}&width=${imageInfo.width})&" + parameter + "\",\"callbackBodyType\":\"application/x-www-form-urlencoded\"}";
		byte[] binary_callback_body = callback_body.getBytes();
		String base64_callback_body = BinaryUtil.toBase64String(binary_callback_body);

		String dir = "user-dir";
		long expireTime = 30;
		long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
		Date expiration = new Date(expireEndTime);
		PolicyConditions policyConds = new PolicyConditions();
		policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
		policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);
		
		String postPolicy = client.generatePostPolicy(expiration, policyConds);
		byte[] binaryData = postPolicy.getBytes("utf-8");
		String encodedPolicy = BinaryUtil.toBase64String(binaryData);
		String postSignature = client.calculatePostSignature(postPolicy);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("accessid", id);
		map.put("host", host);
		map.put("policy", encodedPolicy);
		map.put("signature", postSignature);
		map.put("expire", (new Date().getTime() / 1000) + 10);
		map.put("dir", dir);
		map.put("callback", base64_callback_body);
		JSONObject json = JSONObject.fromObject(map);
		
		response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST");
        
		String callbackFunName = request.getParameter("callback");
		if (callbackFunName == null || callbackFunName.equalsIgnoreCase(""))
			response.getWriter().println(json.toString());
		else
			response.getWriter().println(callbackFunName + "( " + json.toString() + " )");
		response.setStatus(HttpServletResponse.SC_OK);
		response.flushBuffer();

		return null;
	}

	@SuppressWarnings("unused")
	public String delete() throws Exception {
		HttpServletResponse res = ServletActionContext.getResponse();
		HttpServletRequest request = ServletActionContext.getRequest();
		String objectKey = request.getParameter("key");
		InputStream is = getClass().getResourceAsStream("/oss.properties");
		Properties properties = new Properties();
		try {
			properties.load(is);
		} catch (Exception e) {
			System.err.println("不能读取属性文件. " + "请确保db.properties在CLASSPATH指定的路径中");
		}

		String id = properties.getProperty("oss.id");
		String key = properties.getProperty("oss.key");
		String host = properties.getProperty("oss.host");
		String endpoint = properties.getProperty("oss.endpoint");
		String bucketName = properties.getProperty("oss.bucketName");
		OSSClient client = new OSSClient(endpoint, id, key);

		// 删除Object
		if (objectKey != null && objectKey != "") {
			client.deleteObject(bucketName, objectKey);
			res.getWriter().println("ok");
		} else
			res.getWriter().println("error");

		return null;
	}
}