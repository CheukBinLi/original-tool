package com.github.cheukbinli.original.common.cache.redis;

import com.github.cheukbinli.original.common.util.conver.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/***
 * *
 *
 * @Title: original-tool
 * @Package com.github.cheukbinli.original.cache.redis
 * @Description:
 * @Company:
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2020-09-19 09:17
 *
 *
 */
public interface RedisUtil extends RedisFactory {

    Script generateSerialNumberScript = new Script();

    Map<String, String> getSha();

    Map<String, String> getScriptPath();

    Map<String, Script> getScript();

    Map<String, String> getScriptLoaded();

    default RedisFactory appendScript(Script... script) {
        for (Script item : script) {
            getScript().put(item.getName(), item);
        }
        return this;
    }

    default String scriptLoad(Script script, String... keys) throws RedisExcecption {
        String key = script.format(keys);
        String sha1 = new String(scriptLoad(key, script.getScript()));
        getScriptLoaded().put(key, sha1);
        return sha1;
    }

    default Object evalShaByScript(String scriptName, int keys, String... keysAndArgs) throws RedisExcecption {
//		String [] keysParam=new String[keys];
//		if (keys > 0 && (null == keysAndArgs || keysAndArgs.length >= keys)) {
//			keysParam = Arrays.copyOfRange(keysAndArgs, 0, keys);
//		}
        Script script = getScript().get(scriptName);
        String key = Script.format(script.getSlotName(), keysAndArgs);
        String sha1 = getScriptLoaded().get(key);
        if (StringUtil.isBlank(sha1)) {
            sha1 = scriptLoad(getScript().get(scriptName), keysAndArgs);
        }
        return evalSha(sha1, keys, keysAndArgs);
    }


    default Script getGenerateSerialNumberScript() throws RedisExcecption {
        try {
            if (StringUtil.isBlank(generateSerialNumberScript.getScript())) {
                synchronized (RedisFactory.class) {
                    if (StringUtil.isBlank(generateSerialNumberScript.getScript())) {
                        InputStream in = RedisFactory.class.getResource("GenerateSerialNumber.properties").openStream();
                        Properties properties = System.getProperties();
                        properties.load(in);
                        generateSerialNumberScript.setName(properties.get("redis.script.GenerateSerialNumber.name").toString());
                        generateSerialNumberScript.setSlotName(properties.get("redis.script.GenerateSerialNumber.slotName").toString());
                        in.close();
                        in = RedisFactory.class.getResource("GenerateSerialNumber.lua").openStream();
                        InputStreamReader reader = new InputStreamReader(in);
                        char[] buff = new char[1024];
                        int offer = 0;
                        StringBuilder script = new StringBuilder();
                        while ((offer = reader.read(buff)) > 0) {
                            script.append(buff, 0, offer);
                        }
                        reader.close();
                        in.close();
                        generateSerialNumberScript.setScript(script.toString());
                        properties.setProperty("redis.script.GenerateSerialNumber.script", generateSerialNumberScript.getScript());
                    }
                }
            }
            return generateSerialNumberScript;
        } catch (IOException e) {
            throw new RedisExcecption(e);
        }
    }

    default List<String> generateSerialNumber(String tag, String tenant, String application, String module, int quantity) throws RedisExcecption {
        Script script = getGenerateSerialNumberScript();
        if (null == script) {
            throw new RedisExcecption("can't read GenerateSerialNumber.properties");
        }
        String[] keysAndArgs = new String[]{tag, tenant, application, module, Integer.toString(quantity)};
        String key = Script.format(String.format(script.getSlotName(), tag, tenant, application, module), keysAndArgs);


        String sha1 = getScriptLoaded().get(key);
        if (StringUtil.isBlank(sha1)) {
            String scriptStr = script.getScript();
            script = getScript().get(key);
            if (null == script) {
                script = new Script();
                script
                        .setName(key)
                        .setSlotName(key)
                        .setScript(scriptStr);
            }
            sha1 = scriptLoad(script, keysAndArgs);
            getScript().put(key, script);
        }
        Object result = null;
        try {
            result = evalSha(sha1, 1, keysAndArgs);
        } catch (RedisExcecption e) {

            if (null != e && null != e.getMessage() && e.getMessage().contains("NOSCRIPT")) {
                try {
                    scriptLoad(script, keysAndArgs);
                    result = evalSha(sha1, 1, keysAndArgs);
                } catch (RedisExcecption ex) {
                    throw ex;
                }
            }
            throw new RedisExcecption(e);
        }
        if (result instanceof Collection) {
            return (List) result;
        }
        return Arrays.asList(result.toString());
    }

}
