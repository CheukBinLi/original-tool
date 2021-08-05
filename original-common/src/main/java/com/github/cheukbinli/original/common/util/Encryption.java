package com.github.cheukbinli.original.common.util;

import com.github.cheukbinli.original.common.util.conver.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 *
 * @Title: com.github.cheukbinli.original.common.util
 * @Description:
 * <p>
 * 1生成私钥：
 * openssl genrsa -out rsa_private_key.pem -passout pass:123456 -des3 1024
 *
 * 2根据私钥生成公钥：
 * openssl rsa -in rsa_private_key.pem -out rsa_public_key.pem  -pubout
 * 3 java证书(带密码)
 * openssl pkcs8 -topk8 -in rsa_private_key.pem -out pkcs8_rsa_private_key.pem –nocrypt
 * (免密码)
 * openssl pkcs8 -in rsa_private_key.pem -topk8  -nocrypt -out pkcs8_rsa_private_key.pem
 *
 * </p>
 *
 * @Company:
 * @Email: checkbinli@icloud.com
 * @author cheuk.bin.li
 * @date 2021/8/5
 *
 */
public class Encryption {

    private static final Encryption newInstance = new Encryption();

    private final Map<String, Cipher> CIPHER_MAP = new ConcurrentHashMap<>();

    final int MAX_ENCRYPT_BLOCK = 117;
    final int MAX_DECRYPT_BLOCK = 128;

    private BASE64Decoder base64Decoder = new BASE64Decoder();

    private final KeyFactory keyFactory;

    public static enum CipherType {
        NULL, ENCRYPT_MODE, DECRYPT_MODE;
    }

    public static final Encryption newInstance() {
        return newInstance;
    }

    private MessageDigest MD5;

    private static final Logger LOG = LoggerFactory.getLogger(Encryption.class);

    public Encryption() {
        super();
        try {
            MD5 = MessageDigest.getInstance("MD5");
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            LOG.error("init", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized String MD5(String strSrc) {
        String strDes = null;
        try {
            byte[] bt = strSrc.getBytes();
            MD5.update(bt);
            strDes = bytes2Hex(MD5.digest()); // to HexString
            return strDes;
        } finally {
            if (LOG.isDebugEnabled())
                LOG.debug("strSrc:{} MD5:{}", strSrc, strDes);
            MD5.reset();
        }
    }

    public synchronized String MD5(byte[] bt) {
        String strDes = null;
        try {
            MD5.update(bt);
            strDes = bytes2Hex(MD5.digest()); // to HexString
            return strDes;
        } finally {
            MD5.reset();
        }
    }

    private String bytes2Hex(byte[] bts) {
        StringBuffer des = new StringBuffer();
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des.append("0");
            }
            des.append(tmp);
        }
        return des.toString();
    }

    /***
     * 加解密方法
     * @param key base64密钥
     * @param isPrivateKey 当前KEY是否私匙
     * @param cipherType 执行 加密/解密
     * @param content 处理内容
     * @return
     * @throws IOException
     */
    public byte[] doFinal(String key, boolean isPrivateKey, CipherType cipherType, String content) throws IOException {
        return doFinal(key, isPrivateKey, cipherType, StringUtil.isBlank(content, StringUtil.EMPTY).getBytes());
    }

    public byte[] doFinal(String key, boolean isPrivateKey, CipherType cipherType, byte[] contents) throws IOException {
        if (null == contents || contents.length < 1) {
            return null == contents ? null : contents;
        } else if (StringUtil.isBlank(key)) {
            throw new RuntimeException("key can't be null.");
        } else if (null == cipherType || CipherType.NULL == cipherType) {
            throw new RuntimeException("cipherType can't be null.");
        }
        try {
            Cipher cipher = getCipher(key, isPrivateKey, cipherType);
            int maxBlock = CipherType.DECRYPT_MODE == cipherType ? MAX_DECRYPT_BLOCK : MAX_ENCRYPT_BLOCK;
            int contentsLen = contents.length;
            if (contentsLen > maxBlock) {
                int offset = 0;
                int len;
                byte[] data;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                do {
                    data = cipher.doFinal(contents, offset, (len = contentsLen - offset) > maxBlock ? maxBlock : len);
                    offset += maxBlock;
                    out.write(data);
                } while (offset < contentsLen);
                return out.toByteArray();
            } else {
                return cipher.doFinal(contents);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void addCipher(String fullKey, String key, boolean isPrivateKey, CipherType cipherType, Cipher value) {
        CIPHER_MAP.put(fullKey, value);
    }

    public Cipher getCipher(String fullKey, String key, boolean isPrivateKey, CipherType cipherType) {
        return CIPHER_MAP.get(fullKey);
    }

    private Cipher getCipher(String key, boolean isPrivateKey, CipherType cipherType) throws IOException, InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        String cacheKey = cipherType.name() + "_" + key;
        Cipher cipher = getCipher(cacheKey, key, isPrivateKey, cipherType);
        if (null == cipher) {
            byte[] buffer = base64Decoder.decodeBuffer(key);
            Key publicKey = isPrivateKey ? keyFactory.generatePrivate(new PKCS8EncodedKeySpec(buffer)) : keyFactory.generatePublic(new X509EncodedKeySpec(buffer));
            cipher = Cipher.getInstance("RSA");
            cipher.init(cipherType.ordinal(), publicKey);
//            CIPHER_MAP.put(cacheKey, cipher);
            addCipher(cacheKey, key, isPrivateKey, cipherType, cipher);
        }
        return cipher;
    }
//
//    public static void main(String[] args) throws IOException {
//
//        String privateKey = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAK7yDKZhsL16Gaix\n" +
//                "5UQRFrigYCbjm8A8q0aCCtUvcc+cUVxUxMEKmfLk8oE2L6q6ux3MW6OZh0a4NG0Y\n" +
//                "HUB53tewhA7S5tIQPbwkyj/cslmmrPbPUG4lMjjWbJxGsUHpF2y7aBOVbr6FF4Ph\n" +
//                "VmsOMJlrhD3HXq7FzgqdalIY9Do5AgMBAAECgYEAoSZkEqtwU8lyc1uTVhTruhw5\n" +
//                "FfmZ0gXQu700X1Y530UeCRAowa2TTBQrcmzIkds6W+OQEm5tFu69lDSfVXItmzfU\n" +
//                "eBfOIAKZdomoTV7fGKpktfakXKXKlnmMtHGOWunxwBM3Qrv9NjjGHe0QnIc3eObZ\n" +
//                "xn6HYXFAsXPadKz2E7UCQQDpAB7Y06fK3/mukhJSi6NW9xXvLGgLs54swX60bvU9\n" +
//                "0Zf62HPxxS6DpNaq/+FqfISNhERqagGL2Cd90S//CXvnAkEAwDbj+HiTRw1PlFy6\n" +
//                "Nj/NKoKr/EaP6wMxhnqxOSTWkCwwKqrmNcjkHiWVgbDkwOIHG/6RqIPcNrLrzTVM\n" +
//                "j3rU3wJBAITuSqsN5jb6naqZL9bkT+Y3xc3Ume/DJEUIh89NVqFUSM8WWt/ezXDR\n" +
//                "xJ9+qQ5lyAItKhNEM2mjgrRnemiY8pUCQFcdya8Ivv95+fJtIHyL6Cn3NYnOVVYX\n" +
//                "iW/A1efnWVPYozADavk/hpxfPmacTOFa0AwREeVFdh5Yc2T7Xiq9ahMCQQDK2sb3\n" +
//                "znPMJADZdK1LLX0Cz/dDn0jj6Rv949ioBLaJXkZJxIS1n7sBVGI8AUtWxx+3iQew\n" +
//                "xC+5L8p3hf1oeT2F";
//        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCu8gymYbC9ehmoseVEERa4oGAm\n" +
//                "45vAPKtGggrVL3HPnFFcVMTBCpny5PKBNi+qursdzFujmYdGuDRtGB1Aed7XsIQO\n" +
//                "0ubSED28JMo/3LJZpqz2z1BuJTI41mycRrFB6Rdsu2gTlW6+hReD4VZrDjCZa4Q9\n" +
//                "x16uxc4KnWpSGPQ6OQIDAQAB";
//
//        String content = "1、QPS\n" +
//                "QPS Queries Per Second 是每秒查询率 ,是一台服务器每秒能够相应的查询次数，是对一个特定的查询服务器在规定时间内所处理流量多少的衡量标准, 即每秒的响应请求数，也即是最大吞吐能力。\n" +
//                "\n" +
//                "2、TPS\n" +
//                "TPS Transactions Per Second 也就是事务数/秒。一个事务是指一个客户机向服务器发送请求然后服务器做出反应的过程。客户机在发送请求时开始计时，收到服务器响应后结束计时，以此来计算使用的时间和完成的事务个数，\n" +
//                "\n" +
//                "3、QPS和TPS区别\n" +
//                "\n" +
//                "个人理解如下：\n" +
//                "\n" +
//                "1、Tps即每秒处理事务数，包括了\n" +
//                "\n" +
//                "用户请求服务器\n" +
//                "服务器自己的内部处理\n" +
//                "服务器返回给用户\n" +
//                "这三个过程，每秒能够完成N个这三个过程，Tps也就是N；\n" +
//                "\n" +
//                "2、Qps基本类似于Tps，但是不同的是，对于一个页面的一次访问，形成一个Tps；但一次页面请求，可能产生多次对服务器的请求，服务器对这些请求，就可计入“Qps”之中。\n" +
//                "\n" +
//                "例子：\n" +
//                "\n" +
//                "例如：访问一个页面会请求服务器3次，一次放，产生一个“T”，产生3个“Q”\n" +
//                "\n" +
//                "例如：一个大胃王一秒能吃10个包子，一个女孩子0.1秒能吃1个包子，那么他们是不是一样的呢？答案是否定的，因为这个女孩子不可能在一秒钟吃下10个包子，她可能要吃很久。这个时候这个大胃王就相当于TPS，而这个女孩子则是QPS。虽然很相似，但其实是不同的。\n" +
//                "\n" +
//                "\n" +
//                "4、并发数\n" +
//                "\n" +
//                "并发数（并发度）：指系统同时能处理的请求数量，同样反应了系统的负载能力。这个数值可以分析机器1s内的访问日志数量来得到\n" +
//                "\n" +
//                "5、吐吞量\n" +
//                "吞吐量是指系统在单位时间内处理请求的数量，TPS、QPS都是吞吐量的常用量化指标。\n" +
//                "\n" +
//                "系统吞吐量要素\n" +
//                "\n" +
//                "一个系统的吞吐量（承压能力）与request（请求）对cpu的消耗，外部接口，IO等等紧密关联。\n" +
//                "\n" +
//                "单个request 对cpu消耗越高，外部系统接口，IO影响速度越慢，系统吞吐能力越低，反之越高。\n" +
//                "\n" +
//                "重要参数\n" +
//                "\n" +
//                "QPS(TPS),并发数，响应时间\n" +
//                "\n" +
//                "QPS(TPS)：每秒钟request/事务 数量\n" +
//                "并发数：系统同时处理的request/事务数\n" +
//                "响应时间：一般取平均响应时间\n" +
//                "关系\n" +
//                "\n" +
//                "QPS(TPS)=并发数/平均响应时间\n" +
//                "\n" +
//                "一个系统吞吐量通常有QPS(TPS),并发数两个因素决定，每套系统这个两个值都有一个相对极限值，在应用场景访问压力下，只要某一项达到系统最高值，系统吞吐量就上不去了，如果压力继续增大，系统的吞吐量反而会下降，原因是系统超负荷工作，上下文切换，内存等等其他消耗导致系统性能下降。\n" +
//                "\n" +
//                "6、PV\n" +
//                "PV（Page View）：页面访问量，即页面浏览量或点击量，用户每次刷新即被计算一次。可以统计服务一天的访问日志得到。\n" +
//                "\n" +
//                "7、UV\n" +
//                "UV（Unique Visitor）：独立访客，统计1天内访问某站点的用户数。可以统计服务一天的访问日志并根据用户的唯一标识去重得到。响应时间（RT）：响应时间是指系统对请求作出响应的时间，一般取平均响应时间。可以通过Nginx、Apache之类的Web Server得到。\n" +
//                "\n" +
//                "8、DAU\n" +
//                "DAU(Daily Active User)，日活跃用户数量。常用于反映网站、互联网应用或网络游戏的运营情况。DAU通常统计一日（统计日）之内，登录或使用了某个产品的用户数（去除重复登录的用户），与UV概念相似\n" +
//                "\n" +
//                "9、MAU\n" +
//                "MAU(Month Active User)：月活跃用户数量，指网站、app等去重后的月活跃用户数量\n" +
//                "\n" +
//                "10、系统吞吐量评估\n" +
//                "我们在做系统设计的时候就需要考虑CPU运算，IO，外部系统响应因素造成的影响以及对系统性能的初步预估。\n" +
//                "而通常情况下，我们面对需求，我们评估出来的出来QPS，并发数之外，还有另外一个维度：日pv。\n" +
//                "\n" +
//                "通过观察系统的访问日志发现，在用户量很大的情况下，各个时间周期内的同一时间段的访问流量几乎一样。比如工作日的每天早上。只要能拿到日流量图和QPS我们就可以推算日流量。\n" +
//                "\n" +
//                "通常的技术方法：\n" +
//                "\n" +
//                "1、找出系统的最高TPS和日PV，这两个要素有相对比较稳定的关系（除了放假、季节性因素影响之外）\n" +
//                "\n" +
//                "2、通过压力测试或者经验预估，得出最高TPS，然后跟进1的关系，计算出系统最高的日吞吐量。B2B中文和淘宝面对的客户群不一样，这两个客户群的网络行为不应用，他们之间的TPS和PV关系比例也不一样。\n" +
//                "\n" +
//                "11、软件性能测试的基本概念和计算公式\n" +
//                "软件做性能测试时需要关注哪些性能呢？\n" +
//                "\n" +
//                "首先，开发软件的目的是为了让用户使用，我们先站在用户的角度分析一下，用户需要关注哪些性能。\n" +
//                "\n" +
//                "对于用户来说，当点击一个按钮、链接或发出一条指令开始，到系统把结果已用户感知的形式展现出来为止，这个过程所消耗的时间是用户对这个软件性能的直观印 象。也就是我们所说的响应时间，当相应时间较小时，用户体验是很好的，当然用户体验的响应时间包括个人主观因素和客观响应时间，在设计软件时，我们就需要 考虑到如何更好地结合这两部分达到用户最佳的体验。如：用户在大数据量查询时，我们可以将先提取出来的数据展示给用户，在用户看的过程中继续进行数据检 索，这时用户并不知道我们后台在做什么。\n" +
//                "\n" +
//                "用户关注的是用户操作的相应时间。\n" +
//                "\n" +
//                "其次，我们站在管理员的角度考虑需要关注的性能点。\n" +
//                "\n" +
//                "1、 响应时间\n" +
//                "2、 服务器资源使用情况是否合理\n" +
//                "3、 应用服务器和数据库资源使用是否合理\n" +
//                "4、 系统能否实现扩展\n" +
//                "5、 系统最多支持多少用户访问、系统最大业务处理量是多少\n" +
//                "6、 系统性能可能存在的瓶颈在哪里\n" +
//                "7、 更换那些设备可以提高性能\n" +
//                "8、 系统能否支持7×24小时的业务访问\n" +
//                "\n" +
//                "再次，站在开发（设计）人员角度去考虑。\n" +
//                "\n" +
//                "1、 架构设计是否合理\n" +
//                "2、 数据库设计是否合理\n" +
//                "3、 代码是否存在性能方面的问题\n" +
//                "4、 系统中是否有不合理的内存使用方式\n" +
//                "5、 系统中是否存在不合理的线程同步方式\n" +
//                "6、 系统中是否存在不合理的资源竞争";
//
//        Encryption encryption = Encryption.newInstance();
//        byte[] encoding = encryption.doFinal(privateKey, true, CipherType.ENCRYPT_MODE, content);
//        byte[] decoding = encryption.doFinal(publicKey, false, CipherType.DECRYPT_MODE, encoding);
//        System.out.println(content.getBytes().length + ":::" + decoding.length);
//
//    }


}
