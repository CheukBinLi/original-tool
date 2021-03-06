package com.github.cheukbinli.original.common.util;

import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.common.util.conver.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

public class SignUtil {

    static Logger log = LoggerFactory.getLogger(SignUtil.class);

    public static enum SignType {
        MD5,
        HMACSHA256
    }

    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final Random RANDOM = new SecureRandom();

    public interface ConverHandler {
        default String conver(Object o) {
            return o.toString().trim();
        }
    }

    private static ConverHandler DEFAULT_CONVER_HANDLER = new ConverHandler() {
    };

    /***
     *
     * @param data
     *            数据集
     * @param head
     *            头部数据
     * @param tail
     *            尾部数据
     * @param signType
     *            签名
     * @param ignores
     *            忽略字段
     * @param key
     *            公匙
     * @return
     * @throws Exception
     */
    public static String generateSignature(final Map<String, Object> data, String head, String tail, SignType signType, String key, boolean underscoreCamel, String... ignores) throws Exception {
        return generateSignature(data, null, head, tail, signType, key, null, null, underscoreCamel, ignores);

    }

    public static String generateSignature(final Map<String, Object> data, ConverHandler converHandler, String head, String tail, SignType signType, String key, boolean underscoreCamel, String... ignores) throws Exception {
        return generateSignature(data, converHandler, head, tail, signType, key, null, null, underscoreCamel, ignores);

    }

	public static String paramSort(final Map<String, Object> data, String head, String tail, String assignmentCharacter, String linkCharacter, boolean underscoreCamel, String... ignores) throws Exception {
		Set<String> keySet = data.keySet();
		Set<String> ignore = (null == ignores || ignores.length < 1) ? null : new HashSet<>(Arrays.asList(ignores));
		String[] keyArray = keySet.toArray(new String[keySet.size()]);
		assignmentCharacter = null == assignmentCharacter ? "=" : assignmentCharacter;
		linkCharacter = null == linkCharacter ? "&" : linkCharacter;
		Arrays.sort(keyArray);
		StringBuilder sb = new StringBuilder();
		Object value;
		for (String k : keyArray) {
			// 参数值为空，则不参与签名
			if (null == (value = data.get(k)) || (null != ignore && ignore.contains(k))) {
				continue;
			}
			sb.append(linkCharacter).append(underscoreCamel ? StringUtil.toLowerCaseUnderscoreCamel(k) : k).append(assignmentCharacter).append(value.toString().trim());
		}
		return StringUtil.isEmpty(head, "") + (sb.length() > 0 && CollectionUtil.isNotEmpty(data) ? sb.substring(linkCharacter.length()) : "") + StringUtil.isEmpty(tail, "");
	}

    public static String generateSignature(final Map<String, Object> data, ConverHandler converHandler, String head, String tail, SignType signType, String key, String assignmentCharacter, String linkCharacter, boolean underscoreCamel, String... ignores) throws Exception {
        converHandler = null == converHandler ? DEFAULT_CONVER_HANDLER : converHandler;
        Set<String> keySet = data.keySet();
        Set<String> ignore = (null == ignores || ignores.length < 1) ? null : new HashSet<>(Arrays.asList(ignores));
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        assignmentCharacter = null == assignmentCharacter ? "=" : assignmentCharacter;
        linkCharacter = null == linkCharacter ? "&" : linkCharacter;
        Arrays.sort(keyArray);
        StringBuilder sb = new StringBuilder();
        Object value;
        for (String k : keyArray) {
            // 参数值为空，则不参与签名
            if (null == (value = data.get(k)) || (null != ignore && ignore.contains(k))) {
                continue;
            }
            sb.append(underscoreCamel ? StringUtil.toLowerCaseUnderscoreCamel(k) : k).append(assignmentCharacter).append(converHandler.conver(value)).append(linkCharacter);
        }
        if (!StringUtil.isEmpty(key)) {
            sb.append("key").append(assignmentCharacter).append(key).append(linkCharacter);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - (null == linkCharacter ? 0 : linkCharacter.length()));
        }
        if (!StringUtil.isEmpty(head)) {
            sb.insert(0, head);
        }
        if (!StringUtil.isEmpty(tail)) {
            sb.append(tail);
        }
        if (log.isInfoEnabled()) {
            log.info("generateSignature:" + sb.toString());
        }
        if (SignType.MD5.equals(signType)) {
            return MD5(sb.toString()).toUpperCase();
        } else if (SignType.HMACSHA256.equals(signType)) {
            return HMACSHA256(sb.toString(), key);
        } else {
            throw new Exception(String.format("Invalid sign_type: %s", signType));
        }
    }

	public static String HMACSHA256(String data, String key) throws Exception {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
		sha256_HMAC.init(secret_key);
		byte[] array = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (byte item : array) {
			sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString().toUpperCase();
	}

	public static String MD5(String data) throws Exception {
		java.security.MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] array = md.digest(data.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (byte item : array) {
			sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString().toUpperCase();
	}

	public static String generateNonceStr(int len) {
		if (len < 0)
			return null;
		char[] nonceChars = new char[len];
		for (int index = 0; index < len; ++index) {
			nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
		}
		return new String(nonceChars);
	}

}
