//package com.chinasaventures.ledger.security;
//
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import java.util.Base64;
//
//public class KeyGenerator {
//    public static void main(String[] args) {
//        var key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//        System.out.println("this is your key" + Base64.getEncoder().encodeToString(key.getEncoded()));
//    }
//}