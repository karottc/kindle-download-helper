import bo.DeviceItem;
import bo.DeviceRsp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author karottc@gmail.com
 * @date 2022-06-10 18:08
 */
public class Kindle {

    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/1AE148";

    private static final String PAYLOAD_URL = "https://www.amazon.cn/hz/mycd/ajax";

    private static HttpClient httpClient = HttpClient.newHttpClient();

    private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private String csrfToken = "";
    private String cookie = "";

    public Kindle(String csrfToken, String cookieFile) {
        try {
            this.csrfToken = csrfToken;
            this.cookie = Files.readString(Paths.get(cookieFile));;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public void downloadBooks() {

    }

    private DeviceItem getDevices() {
        Map<String, Object> getAllDevices = new HashMap<>();
        getAllDevices.put("GetDevices", new HashMap<>());
        Map<String, Object> payload = new HashMap<>();
        payload.put("param", getAllDevices);

        String param = "csrfToken=" + csrfToken + "&data=" + gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PAYLOAD_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", USER_AGENT)
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(param))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("get device error! code: " + response.statusCode());
                System.exit(-1);
            }

            DeviceRsp deviceRsp = gson.fromJson(response.body(), DeviceRsp.class);

            List<DeviceItem> devices = deviceRsp.GetDevices.devices;
            if (devices == null) {
                System.out.println("没有可用的设备~");
                System.exit(0);
            }
            devices = devices.stream()
                    .filter(x -> x.deviceSerialNumber != null && x.deviceSerialNumber.length() > 0)
                    .toList();

            if (devices.size() == 0) {
                System.out.println("没有可用的设备~");
                System.exit(0);
            }

            return devices.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        String csrfToken = "";
        String cookieFile = "";
        Kindle kindle = new Kindle(csrfToken, cookieFile);
        System.out.println(gson.toJson(kindle.getDevices()) );
    }
}
