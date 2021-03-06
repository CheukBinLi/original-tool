package com.github.cheukbinli.original.oauth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.cheukbinli.original.common.exception.OverdueException;
import com.github.cheukbinli.original.oauth.model.AuthInfo;
import com.github.cheukbinli.original.oauth.model.encryption.KeyModel;

import java.util.Date;
import java.util.HashSet;

public class JWTUtil {

    interface AuthInfoField {
        String ID = "A", TENANT = "B", TYPE = "C", USER_NAME = "D", NONCE = "E", ROLE = "F", SOURCE = "G";
    }

    public static Algorithm getAlgorithm(KeyModel key) {
        switch (key.getEncryptionType()) {
            case HMAC256:
                return Algorithm.HMAC256(key.getSecret());
            case HMAC384:
                return Algorithm.HMAC384(key.getSecret());
            case HMAC512:
                return Algorithm.HMAC512(key.getSecret());

            default:
                throw new RuntimeException("not support the encryption type [" + key.getEncryptionType().toString() + "].");
        }
    }

    public static String generateToken(AuthInfo auth, KeyModel key, long expireSeconds) throws Throwable {
        Algorithm algorithm = getAlgorithm(key);
        String type = auth.getSource().trim().toUpperCase();
        JWTCreator.Builder builder = JWT.create().withIssuer(key.getIssuer()).withExpiresAt(new Date(System.currentTimeMillis() + (expireSeconds * 1000)));
        builder.withClaim(AuthInfoField.ID, auth.getId());
        builder.withClaim(AuthInfoField.TENANT, auth.getTenant());
        builder.withClaim(AuthInfoField.SOURCE, auth.getSource());
        builder.withClaim(AuthInfoField.NONCE, auth.getNonceStr());
        builder.withArrayClaim(AuthInfoField.ROLE, auth.getRole().toArray(new String[0]));
        return type + ":" + builder.sign(algorithm);
    }

    public static AuthInfo parser(String token, KeyModel key) throws OverdueException, Exception {
        Algorithm algorithm = getAlgorithm(key);
        algorithm = Algorithm.HMAC256(key.getSecret());
        JWTVerifier verifier = JWT.require(algorithm).withIssuer(key.getIssuer()).build();
        DecodedJWT jwt = verifier.verify(token);
        Date expire = jwt.getExpiresAt();
        if (null == expire || expire.getTime() <= System.currentTimeMillis()) {
            throw new OverdueException();
        }
        return new AuthInfo(jwt.getClaim(AuthInfoField.NONCE).asString())
                .setId(jwt.getClaim(AuthInfoField.ID).asString())
                .setTenant(jwt.getClaim(AuthInfoField.TENANT).asString())
                .setSource(jwt.getClaim(AuthInfoField.SOURCE).asString())
                .setRole(new HashSet<String>(jwt.getClaim(AuthInfoField.ROLE).asList(String.class)));
    }
}
