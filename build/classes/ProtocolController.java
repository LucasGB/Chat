import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Properties;


public class ProtocolController {

    private final MulticastSocket multicastSocket;
    private final DatagramSocket udpSocket;
    private final InetAddress ip;
    private final InetAddress group;
    private final Integer mport, uport;
    private final String nick;
    private final HashMap<String, InetAddress> onlineUsers;
    private final UIControl ui;

    protected byte[] buffer;

    public ProtocolController(Properties properties) throws IOException {
        mport = (Integer) properties.get("multicastPort");
        uport = (Integer) properties.get("udpPort");
        group = (InetAddress) properties.get("multicastIP");
        ip = (InetAddress) properties.get("ipaddress");
        nick = (String) properties.get("nickname");
        ui = (UIControl) properties.get("UI");
        
        buffer = new byte[1024];

        multicastSocket = new MulticastSocket(mport);
        udpSocket = new DatagramSocket(uport);

        onlineUsers = new HashMap<>();
        onlineUsers.put("Todos", group);
    }

    public void send(String targetUser, String msg) throws IOException {
        Message message;
        byte typeMsg;
        if (targetUser.equals("Todos")) {
            typeMsg = (byte) 0x03;
            message = new Message(typeMsg, nick, msg);
            this.sendMessageGroup(message);
        } else {
            typeMsg = (byte) 0x04;
            message = new Message(typeMsg, nick, msg);
            this.sendMessage(message, onlineUsers.get(targetUser));
        }
    }

    private void sendMessageGroup(Message msg) throws IOException {
        byte[] messagePacket = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(messagePacket, messagePacket.length, group, mport);
        multicastSocket.send(packet);
    }

    private void sendMessage(Message msg, InetAddress target) throws IOException {
        byte[] messagePacket = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(messagePacket, messagePacket.length, target, uport);
        udpSocket.send(packet);
    }

    public void join() throws IOException {
        multicastSocket.joinGroup(group);

        byte typeMsg = (byte) 0x01;
        String msg = "";

        Message message = new Message(typeMsg, nick, msg);
        sendMessageGroup(message);
    }

    public void leave() throws IOException {
        byte typeMsg = (byte) 0x05;
        String msg = "";

        Message message = new Message(typeMsg, nick, msg);
        this.sendMessageGroup(message);

        multicastSocket.leaveGroup(group);
        close();
    }

    public void close() throws IOException {
        if (udpSocket != null) {
            udpSocket.close();
        }
        if (multicastSocket != null) {
            multicastSocket.close();
        }
    }

    public void processPacket(DatagramPacket packet) throws IOException {
        Message message = new Message(packet.getData());

        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String localAddress = "/" + socket.getLocalAddress().getHostAddress();
            if(packet.getAddress().toString().equals(localAddress)) {
                return;
            }
        }

        switch (message.getType()) {
            case 1:
                ui.update(message);
                onlineUsers.put(message.getSource(), packet.getAddress());
                byte type = (byte) 0x02;
                Message JOINACK = new Message(type, nick, "");
                this.sendMessage(JOINACK, packet.getAddress());
                break;
            case 2:
                ui.update(message);
                onlineUsers.put(message.getSource(), packet.getAddress());
                break;
            case 3:
                ui.update(message);
                break;
            case 4:
                ui.update(message);
                break;
            case 5:
                ui.update(message);
                break;
        }
    }

    public void receiveMulticastPacket() throws IOException {
        buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        multicastSocket.receive(packet);
        processPacket(packet);
    }

    public void receiveUdpPacket() throws IOException {
        buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(packet);
        processPacket(packet);
    }
}
