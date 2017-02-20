import com.afollestad.bridge.Bridge;

/**
 * @author Aidan Follestad (afollestad)
 */
public class AsyncTest {

    public static void main(String[] args) {
        Bridge.config().logging(true);
        Bridge.get("https://httpbin.org/get")
                .throwIfNotSuccess()
                .asAsonObject((response, object, e) -> {
                    if (e != null) {
                        System.out.println("Error: " + e.getMessage());
                    } else if (object != null) {
                        System.out.println(object.toString(4));
                    }
                });
    }
}
