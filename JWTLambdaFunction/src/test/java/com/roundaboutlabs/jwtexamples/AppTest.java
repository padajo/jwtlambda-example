package com.roundaboutlabs.jwtexamples;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.amazonaws.services.lambda.runtime.Context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
// import static org.junit.Assert.assertNotNull;
// import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AppTest {

  // https://github.com/aws/aws-lambda-java-libs/tree/master/aws-lambda-java-tests 
  // @Test
  // public void injectSingleBasicSQSEvent() {
  //   App app = new App();
  //   SQSEvent event = EventLoader.loadEvent("sqs_event.json", SQSEvent.class);
  //   Context context = new TestContext();
  //   Void result = app.handleRequest(event, context);
  //   assertNull(result);
  // }

  @Test
  public void injectSimpleJWTDataSQSEvent() {
    App app = new App();
    SQSEvent event = EventLoader.loadEvent("sqs_john_doe_jwt_event.json", SQSEvent.class);
    /* See jwt.io for decoding (that's where I took the test jwt from)
     * Token in body is: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.Z-quzzUBR0Yyj6B37GElTRVPiHoIAWY4-q9i05aYCA8
     * Header: 
      {
        "alg": "HS256",
        "typ": "JWT"
      }
     * Payload Data:
      {
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022
      }
     * signature secret is: "abc123"
     * Using this secret for ease of testing rather than anything else
     */
    
    Context context = new TestContext();
    Void result = app.handleRequest(event, context);

    assertNull(result);
  }

}
