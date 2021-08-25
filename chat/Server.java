package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message){
        try {
            for (Map.Entry entry : connectionMap.entrySet()) {
                ((Connection) entry.getValue()).send(message);
            }
        }
        catch (IOException e){
            System.out.println("There's a problem!");
        }
    }

    public static void main(String... args){
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())){
            System.out.println("chat.Server started!");
            while (true){
                new Handler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Handler extends Thread{

        private final Socket socket;

        public Handler(Socket socket){
            this.socket = socket;
        }

        public void run(){
            ConsoleHelper.writeMessage("chat.Connection established with " + socket.getRemoteSocketAddress());
            try (Connection connection = new Connection(socket)){
                String userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
                if (userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                    ConsoleHelper.writeMessage(String.format("chat.Connection with %s was closed", userName));
                }
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Error!");
            }

        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if (message.getType() != MessageType.USER_NAME)
                    continue;
                if (message.getData().equals(""))
                    continue;
                if (connectionMap.containsKey(message.getData()))
                    continue;
                connectionMap.put(message.getData(),connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return message.getData();
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException{
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()){
                if (!entry.getKey().equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT){
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": "  + message.getData()));
                }
                else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }
        }
    }
}
