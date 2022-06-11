import bo.BookItem;
import bo.BookRsp;
import bo.DeviceItem;
import bo.DeviceRsp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author karottc@gmail.com
 * @date 2022-06-10 18:08
 */
public class Kindle {

    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/1AE148";

    private static final String AZW3_FORMAT = ".azw3";
    private static final String PAYLOAD_URL = "https://www.amazon.cn/hz/mycd/ajax";
    private static final String DOWNLOAD_URL = "https://cde-ta-g7g.amazon.com/FionaCDEServiceEngine/FSDownloadContent?type=EBOK&key=%s&fsn=%s&device_type=%s&customerId=%s&authPool=AmazonCN";

    private static HttpClient httpClient = HttpClient.newHttpClient();

    private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private String csrfToken = "";
    private String cookie = "";
    private String outPath = "";

    public Kindle(String csrfToken, String cookieFile, String outPath) {
        try {
            this.csrfToken = csrfToken;
            this.cookie = Files.readString(Paths.get(cookieFile));
            this.outPath = outPath;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public void downloadBooks() {
        DeviceItem device = getDevices();
        List<BookItem> books = getAllBooks();
        int allNum = books.size();
        AtomicInteger count = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(20);

        books.forEach(book -> {
            executorService.execute(() -> downloadOneBook(book, device, count, allNum) );
        });

        // 等待所有子线程结束
        try {
            executorService.shutdown();
            executorService.awaitTermination(300, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("All Done.");
    }

    private void downloadOneBook(BookItem book, DeviceItem device, AtomicInteger count, int allNum) {

        System.out.println("start download file:" + book.title);

        String url = DOWNLOAD_URL.formatted(book.asin, device.deviceSerialNumber, device.deviceType, device.customerId);

        try {
            do {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Cookie", cookie);
                conn.setInstanceFollowRedirects(false);
                if (300 <= conn.getResponseCode() && conn.getResponseCode() < 400) {
                    url = conn.getHeaderField("Location");
                    continue;
                }

                String fileName = book.title;
                if (!fileName.endsWith(AZW3_FORMAT) ) {
                    fileName += AZW3_FORMAT;
                }
                try (InputStream in = conn.getInputStream()) {
                    long length = Files.copy(in, Paths.get(outPath + "/" + fileName));

                    float fileSize = ((float) length) / (1024 * 1024);

                    int c = count.incrementAndGet();
                    String msg = "[%s] download %s done. %s M".formatted(c+"/"+allNum, fileName, fileSize);
                    System.out.println(msg);
                }
                break;
            } while (true);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private DeviceItem getDevices() {
        Map<String, Object> getAllDevices = new HashMap<>();
        getAllDevices.put("GetDevices", new HashMap<>());
        Map<String, Object> payload = new HashMap<>();
        payload.put("param", getAllDevices);

        String rspStr = getPayLoad(payload);

        DeviceRsp deviceRsp = gson.fromJson(rspStr, DeviceRsp.class);

        if (deviceRsp.error != null && deviceRsp.error.length() > 0) {
            System.out.println("get device error.[" + rspStr + "]");
            System.exit(0);
        }

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
    }

    private List<BookItem> getAllBooks() {
        int startIndex = 0;
        int batchSize = 100;

        Map<String, Object> ownershipData = new HashMap<>();
        ownershipData.put("sortOrder", "DESCENDING");
        ownershipData.put("sortIndex", "DATE");
        ownershipData.put("contentType", "Ebook");
        ownershipData.put("itemStatus", Arrays.asList("Active"));
        ownershipData.put("originType", Arrays.asList("Purchase"));
        ownershipData.put("startIndex", startIndex);
        ownershipData.put("batchSize", batchSize);

        Map<String, Object> dataP = new HashMap<>();
        dataP.put("OwnershipData", ownershipData);
        Map<String, Object> payload = new HashMap<>();
        payload.put("param", dataP);

        List<BookItem> ret = new ArrayList<>();
        while (true) {
            String rspStr = getPayLoad(payload);

            BookRsp rsp = gson.fromJson(rspStr, BookRsp.class);

            if (rsp.OwnershipData.items != null) {
                ret.addAll(rsp.OwnershipData.items);
            }

            if (!rsp.OwnershipData.hasMoreItems) {
                break;
            }
            startIndex += batchSize;
            ownershipData.put("startIndex", startIndex);
        }

        return ret;
    }


    private String getPayLoad(Map<String, Object> payload) {
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
            }
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
