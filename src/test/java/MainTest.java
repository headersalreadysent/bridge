import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonName;
import com.afollestad.bridge.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainTest {

    private final Pipe testPipe = new Pipe() {

        byte[] data = "Hello, world!".getBytes();

        @Override public String hash() {
            return BridgeHashUtil.hash(data);
        }

        @Override public void writeTo(
                @NotNull OutputStream os,
                @Nullable ProgressCallback progressListener) throws IOException {
            os.write(data);
        }

        @NotNull
        @Override
        public String contentType() {
            return "text/plain";
        }

        @Override public int contentLength() throws IOException {
            return data.length;
        }

        @Override public void close() {
            data = null;
        }
    };

    private class ConversionTester {

        String name;
        int age;
        @AsonName(name = "data.$id") int id;
        @AsonName(name = "data.sort") String sort;

        public ConversionTester(String name, int age, int id, String sort) {
            this.name = name;
            this.age = age;
            this.id = id;
            this.sort = sort;
        }
    }

    @Before public void setup() {
        Bridge.config()
                .host("https://httpbin.org")
                .autoFollowRedirects(true)
                .bufferSize(1024);
    }

    @Test public void test_get() throws Exception {
        Response response = Bridge.get("/get?name=%s", "Aidan")
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        Ason args = responseJson.get("args");
        assertNotNull(args);
        assertEquals("Aidan", args.getString("name"));
    }

    @Test public void test_auto_redirect() throws Exception {
        Response response = Bridge.get("/redirect-to?url=%s", "https://www.google.com")
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.didRedirect());
        assertEquals(response.redirectCount(), 1);
        assertEquals(response.url(), "https://www.google.com");
    }

    @Test public void test_auto_redirect_multi() throws Exception {
        Response response = Bridge.get("/redirect/%d", 6)
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.didRedirect());
        assertEquals(response.redirectCount(), 6);
    }

    @Test public void test_post_body() throws Exception {
        Response response = Bridge.get("/post")
                .body(new Ason().put("name", "Aidan"))
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        Ason form = responseJson.get("json");
        assertNotNull(form);
        assertEquals("Aidan", form.getString("name"));
    }

    @Test public void test_post_form() throws Exception {
        Response response = Bridge.get("/post")
                .body(new Form().add("name", "Aidan"))
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        Ason form = responseJson.get("form");
        assertNotNull(form);
        assertEquals("Aidan", form.getString("name"));
    }

    @Test public void test_post_multipart_form() throws Exception {
        MultipartForm sendForm = new MultipartForm()
                .add("name", "Aidan")
                .add("file", "test.txt", testPipe);
        Response response = Bridge.get("/post")
                .body(sendForm)
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        Ason form = responseJson.get("form");
        assertNotNull(form);
        assertEquals("Aidan", form.getString("name"));

        Ason files = responseJson.get("files");
        assertNotNull(files);
        assertEquals("Hello, world!", files.get("file"));
    }

    @Test public void test_converter_object_request() {

    }

    @Test public void test_converter_array_request() {

    }

    @Test public void test_converter_list_request() {

    }

    @Test public void test_converter_object_response() {

    }

    @Test public void test_converter_array_response() {

    }

    @Test public void test_converter_list_response() {

    }
}
