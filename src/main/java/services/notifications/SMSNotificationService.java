package services.notifications;

import org.smpp.Connection;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;

import java.util.Properties;

public class SMSNotificationService {
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;


    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(SMSNotificationService.class.getClassLoader()
                    .getResourceAsStream("sms.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sms configuration", e);
        }
    }

    public SMSNotificationService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddress = props.getProperty("smpp.source_addr");
    }

    public void sendCode(String destination, String code) {
        Connection connection;
        Session session;

        try {
            // 1. Установка соединения
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);
            // 2. Подготовка Bind Request
            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34); // SMPP v3.4
            bindRequest.setAddressRange(sourceAddress);
            // 3. Выполнение привязки
            BindResponse bindResponse = session.bind(bindRequest);
            if (bindResponse.getCommandStatus() != 0) {
                System.err.println("Bind failed: " + bindResponse.getCommandStatus());
            }
            // 4. Отправка сообщения
            SubmitSM submitSM = new SubmitSM();
            submitSM.setSourceAddr(sourceAddress);
            submitSM.setDestAddr(destination);
            submitSM.setShortMessage("Your code: " + code);

            session.submit(submitSM);
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e);
        }
    }
}
