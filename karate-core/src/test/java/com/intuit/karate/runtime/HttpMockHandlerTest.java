package com.intuit.karate.runtime;

import com.intuit.karate.server.ArmeriaHttpClient;
import com.intuit.karate.match.Match;
import com.intuit.karate.match.MatchResult;
import com.intuit.karate.server.HttpConstants;
import com.intuit.karate.server.HttpRequestBuilder;
import com.intuit.karate.server.HttpServer;
import com.intuit.karate.server.Response;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
class HttpMockHandlerTest {

    static final Logger logger = LoggerFactory.getLogger(HttpMockHandlerTest.class);

    MockHandler handler;
    HttpServer server;
    FeatureBuilder mock;
    HttpRequestBuilder http;
    Response response;

    HttpRequestBuilder handle() {
        handler = new MockHandler(mock.build());
        server = new HttpServer(0, handler);
        ArmeriaHttpClient client = new ArmeriaHttpClient(new Config(), new com.intuit.karate.Logger());
        http = new HttpRequestBuilder(client);
        http.url("http://localhost:" + server.getPort());
        return http;
    }

    FeatureBuilder background(String... lines) {
        mock = FeatureBuilder.background(lines);
        return mock;
    }

    private void match(Object actual, Object expected) {
        MatchResult mr = Match.that(actual).isEqualTo(expected);
        assertTrue(mr.pass, mr.message);
    }

    @AfterEach
    void afterEach() {
        server.stop();
    }

    @Test
    void testSimpleGet() {
        background().scenario(
                "pathMatches('/hello')",
                "def response = 'hello world'");
        response = handle().path("/hello").invoke("get");
        match(response.getBodyAsString(), "hello world");
    }

    @Test
    void testUrlWithSpecialCharacters() {
        background().scenario(
                "pathMatches('/hello/{raw}')",
                "def response = { success: true }"
        );
        response = handle().path("/hello/�Ill~Formed@RequiredString!").invoke("get");
        match(response.getBodyConverted(), "{ success: true }");
    }

    @Test
    void testGraalJavaClassLoading() {
        background().scenario(
                "pathMatches('/hello')",
                "def Utils = Java.type('com.intuit.karate.runtime.MockUtils')",
                "def response = Utils.testBytes"
        );
        response = handle().path("/hello").invoke("get");
        match(response.getBody(), MockUtils.testBytes);
    }
    
    @Test
    void testEmptyResponse() {
        background().scenario(
                "pathMatches('/hello')",
                "def response = null"
        );
        response = handle().path("/hello").invoke("get");
        match(response.getBody(), HttpConstants.ZERO_BYTES);
    }    

}
