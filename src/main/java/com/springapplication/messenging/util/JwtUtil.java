package com.springapplication.messenging.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * 
 * @author Atul Sinha
 */
@Service
public class JwtUtil {

    @Value("${auth.secret_key}")
    private String SECRET_KEY;

    @Value("${auth.token_expiry}")
    private long token_expiry;

    @Value("${auth.jwt_issuer}")
    private String jwt_issuer;

    String encodedKey = Base64.getUrlEncoder().encodeToString(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    private final Key key = Keys.hmacShaKeyFor(encodedKey.getBytes(StandardCharsets.UTF_8));

    private final Key randomKey = Keys.secretKeyFor(SignatureAlgorithm.HS256); // generates a random key

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        Map<String, Object> header = new HashMap<>();
        return createToken(claims, header, userDetails);
    }

    private String createToken(Map<String, Object> claims, Map<String, Object> header, UserDetails userDetails) {

        return Jwts.builder().addClaims(claims).setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setHeader(header)
                .setExpiration(new Date(Instant.now().toEpochMilli() + token_expiry))
                .setIssuer(jwt_issuer)
                .signWith(key) //this will automatically select an algorithm to encode
                .compact();
    }

    public boolean validateToken(String token){

    }

    private Jws<Claims> extractAllClaims(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

}
