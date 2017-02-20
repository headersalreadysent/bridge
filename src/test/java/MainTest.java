import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import com.afollestad.bridge.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

    @Before public void setup() {
        Bridge.config()
                .host("https://httpbin.org")
                .autoFollowRedirects(true)
                .bufferSize(1024)
                .maxRedirects(6);
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

    @Test public void test_converter_object_request() throws Exception {
        RequestConvertTestObj object = new RequestConvertTestObj(
                "Aidan", 1995, 1, 2);

        Response response = Bridge.get("/post")
                .body(object)
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        Ason jsonObj = responseJson.get("json");
        assertNotNull(jsonObj);

        String expectedJson = "{\"data\":{\"sort\":2,\"$id\":1},\"born\":1995,\"name\":\"Aidan\"}";
        assertEquals(expectedJson, jsonObj.toString());
    }

    @Test public void test_converter_array_request() throws Exception {
        RequestConvertTestObj[] objects = new RequestConvertTestObj[]{
                new RequestConvertTestObj("Aidan", 1995, 1, 2),
                new RequestConvertTestObj("Waverly", 1997, 2, 1)
        };

        Response response = Bridge.get("/post")
                .body(objects)
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        AsonArray jsonObj = responseJson.get("json");
        assertNotNull(jsonObj);

        String expectedJson = "[{\"data\":{\"sort\":2,\"$id\":1},\"born\":1995,\"name\":\"Aidan\"}," +
                "{\"data\":{\"sort\":1,\"$id\":2},\"born\":1997,\"name\":\"Waverly\"}]";
        assertEquals(expectedJson, jsonObj.toString());
    }

    @Test public void test_converter_list_request() throws Exception {
        List<RequestConvertTestObj> objects = new ArrayList<>(2);
        objects.add(new RequestConvertTestObj("Aidan", 1995, 1, 2));
        objects.add(new RequestConvertTestObj("Waverly", 1997, 2, 1));

        Response response = Bridge.get("/post")
                .body(objects)
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        Ason responseJson = response.asAsonObject();
        assertNotNull(responseJson);

        AsonArray jsonObj = responseJson.get("json");
        assertNotNull(jsonObj);

        String expectedJson = "[{\"data\":{\"sort\":2,\"$id\":1},\"born\":1995,\"name\":\"Aidan\"}," +
                "{\"data\":{\"sort\":1,\"$id\":2},\"born\":1997,\"name\":\"Waverly\"}]";
        assertEquals(expectedJson, jsonObj.toString());
    }

    @Test public void test_converter_object_response() throws Exception {
        RequestConvertTestObj object = new RequestConvertTestObj(
                "Aidan", 1995, 1, 2);

        Response response = Bridge.get("/post")
                .body(object)
                .throwIfNotSuccess()
                .response();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("application/json", response.contentType());

        ResponseConvertTestObj result = response.asClass(ResponseConvertTestObj.class);
        assertNotNull(result);
        assertEquals(object.name, result.name);
        assertEquals(object.born, result.born);
        assertEquals(object.id, result.id);
        assertEquals(object.sort, result.sort);
    }
}
