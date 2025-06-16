package services.notifications;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class TelegramNotificationService {
    private final String apiBaseUrl;
    private final String token;
    private final String defaultChatId;

    public TelegramNotificationService() {
        Properties props = loadConfig();
        this.apiBaseUrl    = props.getProperty("telegram.apiUrl");
        this.token         = props.getProperty("telegram.token");
        this.defaultChatId = props.getProperty("telegram.chatId");
    }

    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("telegram.properties")) {
            if (is == null) throw new IllegalStateException("telegram.properties not found in classpath");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Could not load Telegram configuration", e);
        }
    }

    public void sendCode(String code) {
        String chatId = defaultChatId;
        String text = "Your one-time confirmation code is: " + code;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URI uri = new URIBuilder(apiBaseUrl + token + "/sendMessage")
                    .addParameter("chat_id", chatId)
                    .addParameter("text", text)
                    .build();

            HttpGet request = new HttpGet(uri);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    System.err.println("Telegram API returned" + status);
                }
                System.out.println("OTP code sent via Telegram to chatId " + chatId);
            }
        } catch (URISyntaxException e) {
            System.err.println("Invalid Telegram API URI" + e);
        } catch (Exception e) {
            System.err.println("Telegram sending failed." + e);
        }
    }
}
