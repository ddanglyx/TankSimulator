import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GameServer server;
    private int playerIndex;

    public ClientHandler(Socket socket, GameServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("ACTION:")) {
                    String action = inputLine.substring(7);
                    server.handleAction(playerIndex, action);
                }
            }
        } catch (IOException e) {
            server.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setPlayerIndex(int index) { this.playerIndex = index; }
    public int getPlayerIndex() { return playerIndex; }
}