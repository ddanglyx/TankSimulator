// javac GameServer.java
// java GameServer

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12344;
    private static final int MAX_PLAYERS = 2;
    private List<ClientHandler> clients = new ArrayList<>();
    // NEW CODE: Added a map to store tank states for each player
    private Map<String, TankState> tankStates = new HashMap<>();
    // NEW CODE: Added a counter to track the number of players connected
    private int playerCount = 0;

    public static void main(String[] args) {
        new GameServer().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Waiting for 2 players...");

            while (clients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                if (clients.size() >= MAX_PLAYERS) {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("SERVER_FULL");
                    clientSocket.close();
                    continue;
                }

                ClientHandler client = new ClientHandler(clientSocket, this);
                clients.add(client);
                new Thread(client).start();

                // If we now have 2 players, send START signal
                if (clients.size() == MAX_PLAYERS) {
                    System.out.println("Two players connected. Starting game...");
                    Thread.sleep(1000); // Give clients time to initialize
                    broadcast("START");
                    System.out.println("START signal sent!");
                }
            }

            // Keep server running to handle game updates
            while (true) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    // NEW CODE: Method to update tank state and broadcast it to all clients
    public synchronized void updateTankState(String playerName, TankState state) {
        tankStates.put(playerName, state);
        broadcastTankStates();
    }

    // NEW CODE: Method to broadcast tank states to all clients
    public synchronized void broadcastTankStates() {
        String broadcastMessage = TankState.toBroadcastString(tankStates);
        for (ClientHandler client : clients) {
            client.send(broadcastMessage);
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private GameServer server;
        private String playerName;

        public ClientHandler(Socket socket, GameServer server) throws IOException {
            this.socket = socket;
            this.server = server;
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

                // Get player name first
                playerName = in.readLine();
                int playerNumber = ++playerCount;
                System.out.println("Player " + playerNumber + " connected: " + playerName);

                // Send player number first
                out.println(playerNumber);
                out.flush();

                // Handle tank state updates
                String input;
                while ((input = in.readLine()) != null) {
                    if (!input.equals("START")) { // Ignore START messages in state updates
                        TankState state = TankState.fromString(input);
                        server.updateTankState(playerName, state);
                    }
                }
            } catch (IOException e) {
                System.out.println("Player disconnected: " + playerName);
            }
        }

        public void send(String message) {
            out.println(message);
            out.flush(); // Ensure the message is sent immediately
        }

        // NEW CODE: Method to send tank states to this client
        public void sendTankStates(Map<String, TankState> tankStates) {
            out.println(TankState.toBroadcastString(tankStates));
        }
    }
}