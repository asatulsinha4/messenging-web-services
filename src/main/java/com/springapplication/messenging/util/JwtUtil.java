package com.springapplication.messenging.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import com.springapplication.messenging.models.AdminUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * 
 * @author Atul Sinha
 */
@Component
public class JwtUtil {

    @Value("${auth.secret_key}")
    private String SECRET_KEY;

    @Value("${auth.token_expiry}")
    private long token_expiry;

    @Value("${auth.jwt_issuer}")
    private String jwt_issuer;

    final String encodedKey = PasswordUtil.base64UrlEncoder(SECRET_KEY);

    private final Key key = Keys.hmacShaKeyFor(encodedKey.getBytes(StandardCharsets.UTF_8));

    private final Key randomKey = Keys.secretKeyFor(SignatureAlgorithm.HS256); // generates a random key

    /**
     * Use this function post authentication only. Generates {@code Jwt} token
     * @param userDetails
     * @param claims
     * @param header
     * @return {@code Jwt} token
     */
    public String generateToken(AdminUser userDetails, Map<String, Object> claims, Map<String, Object> header) {
        return createToken(claims, header, userDetails, this.key);
    }

    /**
     * 
     * @param claims
     * @param header
     * @param AdminUser
     * @return Jwt token
     */
    private String createToken(Map<String, Object> claims, Map<String, Object> header, AdminUser userDetails, Key key) {

        return Jwts.builder().addClaims(claims).setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setHeader(header)
                .setExpiration(new Date(Instant.now().toEpochMilli() + token_expiry))
                .setIssuer(jwt_issuer)
                .signWith(key) //this will automatically select an algorithm to encode
                .compact();
    }

    /**
     * This function checks if the token is valid or not
     * 
     * @param token
     * @return boolean if it is valid or not
     * @throws JwtException if token is invalid
     */
    public boolean validateToken(String token){
        return !extractAllClaims(token).getExpiration().before(Date.from(Instant.now()));
    }

    /**Jwt parser
     * 
     * @param token
     * @return {@code Claims} 
     * @throws JwtException if the token is invalid
     */
    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * Extracts username from {@code Jwt} token
     * @param token
     * @return username string
     * @throws JwtException when the token seems invalid
     */
    public String extractUsername(String token){
        return extractAllClaims(token).getSubject();
    }

    /**
     * Generate {@code Jwt} token with random key to make life harder for this particular user, as 
     * token is validated using another key.So the user will have to login after every network call
     * @param AdminUser
     * @return {@code Jwt} token
     */
    public String tokenToMakeLifeHarder(AdminUser userDetails, Map<String, Object> claims, Map<String, Object> header){
        return createToken(claims, header, userDetails, this.randomKey);
    }

}
