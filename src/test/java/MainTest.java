import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import com.afollestad.bridge.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** @author Aidan Follestad (afollestad) */
public class MainTest {

  private final Pipe testPipe =
      new Pipe() {

        byte[] data = "Hello, world!".getBytes();

        @Override
        public String hash() {
          return BridgeHashUtil.hash(data);
        }

        @Override
        public void writeTo(@NotNull OutputStream os, @Nullable ProgressCallback progressListener)
            throws IOException {
          os.write(data);
        }

        @NotNull
        @Override
        public String contentType() {
          return "text/plain";
        }

        @Override
        public int contentLength() throws IOException {
          return data.length;
        }

        @Override
        public void close() {
          data = null;
        }
      };

  private final ResponseValidator testValidator =
      new ResponseValidator() {

        @Override
        public boolean validate(@NotNull Response response) throws Exception {
          if (!"application/json".equals(response.contentType())) {
            throw new Exception("Expected application/json");
          }
          Ason ason = response.asAsonObject();
          return ason != null && ason.getBool("data.test");
        }

        @NotNull
        @Override
        public String id() {
          return "test";
        }
      };

  @Before
  public void setup() {
    Bridge.config()
        .host("https://postman-echo.com")
        .autoFollowRedirects(true)
        .bufferSize(1024)
        .maxRedirects(6);
  }

  @Test
  public void test_get() throws Exception {
    Response response = Bridge.get("/get?name=%s", "Aidan").throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);

    Ason args = responseJson.get("args");
    assertNotNull(args);
    assertEquals("Aidan", args.getString("name"));
  }

  @Test
  public void test_headers() throws Exception {
    Response response =
        Bridge.get("/headers").header("aidan", "follestad").throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());

    Ason body = response.asAsonObject();
    assertNotNull(body);
    Ason headers = body.get("headers");
    assertNotNull(headers);
    assertEquals("follestad", headers.get("aidan"));
  }

  @Test
  public void test_auto_redirect() throws Exception {
    Response response =
        Bridge.get("https://httpbin.org/redirect-to?url=%s", "https://www.google.com")
            .throwIfNotSuccess()
            .response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertTrue(response.didRedirect());
    assertEquals(response.redirectCount(), 1);
    assertEquals(response.url(), "https://www.google.com");
  }

  @Test
  public void test_auto_redirect_multi() throws Exception {
    Response response =
        Bridge.get("https://httpbin.org/redirect/%d", 6).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertTrue(response.didRedirect());
    assertEquals(response.redirectCount(), 6);
  }

  @Test
  public void test_post_body() throws Exception {
    Response response =
        Bridge.post("/post").body(new Ason().put("name", "Aidan")).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);

    Ason data = responseJson.get("data");
    assertNotNull(data);
    assertEquals("Aidan", data.getString("name"));
  }

  @Test
  public void test_put_body() throws Exception {
    Response response =
        Bridge.put("/put").body(new Ason().put("name", "Aidan")).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);

    Ason data = responseJson.get("data");
    assertNotNull(data);
    assertEquals("Aidan", data.getString("name"));
  }

  @Test
  public void test_delete() throws Exception {
    Response response = Bridge.delete("/delete").throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
  }

  @Test
  public void test_post_form() throws Exception {
    Response response =
        Bridge.post("/post").body(new Form().add("name", "Aidan")).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);

    Ason form = responseJson.get("form");
    assertNotNull(form);
    assertEquals("Aidan", form.getString("name"));
  }

  @Test
  public void test_post_multipart_form() throws Exception {
    MultipartForm sendForm = new MultipartForm().add("name", "Aidan").add("file", "test", testPipe);
    Response response = Bridge.post("/post").body(sendForm).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);

    Ason form = responseJson.get("form");
    assertNotNull(form);
    assertEquals("Aidan", form.getString("name"));

    Ason files = responseJson.get("files");
    assertNotNull(files);
    assertEquals("data:application/octet-stream;base64,SGVsbG8sIHdvcmxkIQ==", files.get("test"));
  }

  @Test
  public void test_basic_auth_success() throws Exception {
    Authentication auth = BasicAuthentication.create("postman", "password");
    Response response = Bridge.get("/basic-auth").authentication(auth).response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
  }

  @Test
  public void test_basic_auth_fail() throws Exception {
    try {
      Authentication auth = BasicAuthentication.create("postman", "hi");
      Bridge.get("/basic-auth").authentication(auth).request();
      assertFalse("No exception was thrown for failed auth.", false);
    } catch (BridgeException ignored) {
    }
  }

  @Test
  public void test_gzip_decompress() throws Exception {
    Response response = Bridge.get("/gzip").throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());

    Ason json = response.asAsonObject();
    assertNotNull(json);
    assertEquals(true, json.get("gzipped"));
  }

  @Test
  public void test_converter_object_request() throws Exception {
    RequestConvertTestObj object = new RequestConvertTestObj("Aidan", 1995, 1, 2);

    Response response = Bridge.get("/post").body(object).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);
    Ason jsonObj = responseJson.get("json");
    assertNotNull(jsonObj);

    assertEquals(2, jsonObj.getInt("data.sort"));
    assertEquals(1, jsonObj.getInt("data.id"));
    assertEquals(1995, jsonObj.getInt("born"));
    assertEquals("Aidan", jsonObj.get("name"));
  }

  @Test
  public void test_converter_array_request() throws Exception {
    RequestConvertTestObj[] objects =
        new RequestConvertTestObj[] {
          new RequestConvertTestObj("Aidan", 1995, 1, 2),
          new RequestConvertTestObj("Waverly", 1997, 2, 1)
        };

    Response response = Bridge.get("/post").body(objects).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);
    AsonArray jsonAry = responseJson.get("json");
    assertNotNull(jsonAry);

    Ason one = jsonAry.getJsonObject(0);
    assertEquals(2, one.getInt("data.sort"));
    assertEquals(1, one.getInt("data.id"));
    assertEquals(1995, one.getInt("born"));
    assertEquals("Aidan", one.get("name"));

    Ason two = jsonAry.getJsonObject(1);
    assertEquals(1, two.getInt("data.sort"));
    assertEquals(2, two.getInt("data.id"));
    assertEquals(1997, two.getInt("born"));
    assertEquals("Waverly", two.get("name"));
  }

  @Test
  public void test_converter_list_request() throws Exception {
    List<RequestConvertTestObj> objects = new ArrayList<>(2);
    objects.add(new RequestConvertTestObj("Aidan", 1995, 1, 2));
    objects.add(new RequestConvertTestObj("Waverly", 1997, 2, 1));

    Response response = Bridge.get("/post").body(objects).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    Ason responseJson = response.asAsonObject();
    assertNotNull(responseJson);
    AsonArray jsonAry = responseJson.get("json");
    assertNotNull(jsonAry);

    Ason one = jsonAry.getJsonObject(0);
    assertEquals(2, one.getInt("data.sort"));
    assertEquals(1, one.getInt("data.id"));
    assertEquals(1995, one.getInt("born"));
    assertEquals("Aidan", one.get("name"));

    Ason two = jsonAry.getJsonObject(1);
    assertEquals(1, two.getInt("data.sort"));
    assertEquals(2, two.getInt("data.id"));
    assertEquals(1997, two.getInt("born"));
    assertEquals("Waverly", two.get("name"));
  }

  @Test
  public void test_converter_object_response() throws Exception {
    RequestConvertTestObj object = new RequestConvertTestObj("Aidan", 1995, 1, 2);

    Response response = Bridge.get("/post").body(object).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("application/json; charset=utf-8", response.contentType());

    ResponseConvertTestObj result = response.asClass(ResponseConvertTestObj.class);
    assertNotNull(result);
    assertEquals(object.name, result.name);
    assertEquals(object.born, result.born);
    assertEquals(object.id, result.id);
    assertEquals(object.sort, result.sort);
  }

  @Test
  public void test_validators_pass() {
    Ason request = new Ason().put("test", true);
    try {
      Bridge.post("/post").body(request).throwIfNotSuccess().validators(testValidator).request();
    } catch (BridgeException ignored) {
      assertFalse("Validator was expected to pass!", false);
    }
  }

  @Test
  public void test_validators_fail() {
    Ason request = new Ason().put("test", false);
    try {
      Bridge.post("/post").body(request).throwIfNotSuccess().validators(testValidator).request();
      assertFalse("Validator was expected to fail!", false);
    } catch (BridgeException ignored) {
    }
  }

  @Test
  public void test_transfer_pipe() throws Exception {
    byte[] inData = "Hello, world!".getBytes();
    ByteArrayInputStream in = new ByteArrayInputStream(inData);
    Pipe pipe = Pipe.forStream(in, "text/plain", BridgeHashUtil.hash(inData));
    Response response = Bridge.post("/post").body(pipe).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());

    Ason body = response.asAsonObject();
    assertNotNull(body);
    String data = body.get("data");
    assertEquals("Hello, world!", data);
  }

  @Test
  public void test_file_pipe() throws Exception {
    FileOutputStream os = new FileOutputStream("testpipe.txt");
    os.write("Hello, world!".getBytes());
    os.close();
    Pipe pipe = Pipe.forFile("testpipe.txt");
    Response response = Bridge.post("/post").body(pipe).throwIfNotSuccess().response();
    assertNotNull(response);
    assertTrue(response.isSuccess());

    Ason body = response.asAsonObject();
    assertNotNull(body);
    String data = body.get("data");
    assertEquals("Hello, world!", data);
    new File("testpipe.txt").delete();
  }
}
