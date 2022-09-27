package com.roundaboutlabs.jwtexamples;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;

import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ContainerFactory;

import org.json.simple.parser.ParseException;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

public class App implements RequestHandler<SQSEvent, Void>{

    private String parameterSecretKey = "jwt-example-secret";
    private String secret = null;
    private LambdaLogger logger;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        // get the logger and set the private logger 
        logger = context.getLogger();

        // get the secret from the parameter store
        if(secret == null) {
            String paraName = "jwt-example-secret";

            Region region = Region.EU_WEST_1;
            SsmClient ssmClient = SsmClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

            try {
                GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(paraName)
                    .withDecryption(true) // make sure it's decrypted 
                    .build();
    
                GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);

                secret = parameterResponse.parameter().value();
                logger.log(String.format("The parameter value is %s", secret));
    
            } catch (SsmException e) {
                logger.log(e.getMessage());
                throw new Error(e);
            }

            ssmClient.close();
        }

        int i = 0;
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();

        // loop over all the SQS messages in the batch
        for(SQSMessage msg : event.getRecords()) {

            logger.log(String.format("Message %d", i));

            // the message body in this context is simply a JWT
            String msgBody = new String(msg.getBody());

            // JWT are split into 3 parts, header, payload and signature, separated by a period '.'
            String[] parts = msgBody.split("\\.");

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String encodedSignature = parts[2];

            String headerJson = new String(decoder.decode(encodedHeader));
            String payloadJson = new String(decoder.decode(encodedPayload));
            String signatureJson = encodedSignature; // to check if the JWT is valid, we need to test if this is correct

            // logger.log(headerJson);
            // logger.log(payloadJson);
            // logger.log(signatureJson);

            String alg = null;
            String typ = null;
            try {
                // use org.json.simple because we don't need to do any complex parsing 
                // of a large JSON object
                // https://openid.net/specs/draft-jones-json-web-key-03.html#ExampleJWK

                // the header is only small, and needs an "alg" and "typ"
                // if it fails, the lambda needs to fail
                JSONObject header = (JSONObject) new JSONParser().parse(headerJson);
                alg = (String)header.get("alg");
                typ = (String)header.get("typ");
                // logger.log("Header:");
                // logger.log(String.format("algorithm: %s", alg));
                // logger.log(String.format("Type: %s", typ));

                ContainerFactory containerFactory = new ContainerFactory() {
                    @Override
                    public Map createObjectContainer() {
                       return new LinkedHashMap<>();
                    }
                    @Override
                    public List creatArrayContainer() {
                       return new LinkedList<>();
                    }
                };

                JSONParser payloadParser = new JSONParser();
                Map map = (Map)payloadParser.parse(payloadJson, containerFactory);
                map.forEach((k,v)->System.out.println("Key : " + k + " Value : " + v));
                
            } catch (ParseException pe) {
                logger.log(pe.getMessage());
                throw new Error("Parse error on JWT"); // throw an error to end the lambda processing
            }

            // let's check the signature


            // JWT data for the signature, from the spec
            String data = String.format("%s.%s", encodedHeader, encodedPayload);
            String sig = getSignature(alg, headerJson, payloadJson, secret);

            logger.log(String.format("sig: %s", sig));

            // java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder();
            // try {
            //     String encodeSig = encoder.encodeToString(hmac.getBytes("UTF-8"));
            //     logger.log(encodeSig);
            // } catch (Exception e) {
            //     throw new Error("Unable to encode signature");
            // }
            
            i++;
        }
        return null;
    }

    /*
     * JWS signature signing
     */
    private String getSignature(String algorithm, String header, String payload, String secret) {
        // for HMAC based signatures
        try {
            java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder();
            String encodedHeader = encoder.encodeToString(header.getBytes("UTF-8"));
            String encodedPayload = encoder.encodeToString(payload.getBytes("UTF-8"));
            String data = String.format("%s.%s", encodedHeader, encodedPayload);
            if(logger != null) {
                logger.log(String.format("header: %s", header));
                logger.log(String.format("payload: %s", payload));
                logger.log(String.format("payload(e): %s", encodedHeader));
                logger.log(String.format("header(e): %s", encodedPayload));
                logger.log(String.format("data: %s", data));
                logger.log(String.format("secret: %s", secret));
            }
            switch (algorithm) {
                case "HS256":
                    HmacUtils hs256 = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret);
                    byte[] hashedData = hs256.hmac(data.getBytes("UTF-8"));
                    // hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex();
                    String sig = encoder.encodeToString(hashedData);
                    if(logger != null) {
                        logger.log(String.format("sig: %s", sig));
                    }
                    return sig;
                default:
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            throw new Error("Cannot encode to UTF-8");
        }

        return null;
    }
    
}
