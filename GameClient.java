// javac -classpath ".;C:\lwjgl-release-3.3.6-custom\*" GameClient.java
// java -classpath ".;C:\lwjgl-release-3.3.6-custom\*" GameClient

import org.lwjgl.glfw.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameClient {
    // NEW CODE: Variables for player name and number, game state, and socket connection
    private String playerName;
    private int playerNumber;
    private boolean gameStarted = false;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Map<String, TankState> otherTanks = new HashMap<>();

    private GameClient(String playerName) {
        this.playerName = playerName;
        try {
            // NEW CODE: replace with your IP and port
            socket = new Socket("192.168.4.57", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // NEW CODE: Send player name to server
            out.println(playerName);

            // NEW CODE: Read player number first
            String response = in.readLine();
            if (response == null || response.equals("SERVER_FULL")) {
                throw new IOException("Server is full. Please try again later.");
            }

            try {
                playerNumber = Integer.parseInt(response.trim());
                System.out.println("Connected as Player " + playerNumber);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid player number format: " + response);
            }

            // NEW CODE: Start a new thread to read from the server
            // This thread will handle the initial connection and game start signal
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("Debug: Received from server: " + line);
                        if (line.equals("START")) {
                            System.out.println("Game is starting!");
                            gameStarted = true;
                            break; // Exit the read loop after receiving START
                        }
                    }
                    // NEW CODE: Start a new thread for handling game updates after START
                    if (gameStarted) {
                        new Thread(() -> {
                            try {
                                String updateLine;
                                while ((updateLine = in.readLine()) != null) {
                                    if (updateLine.contains(":")) {
                                        parseTankStates(updateLine);
                                    }
                                }
                            } catch (IOException e) {
                                System.err.println("Lost connection during game: " + e.getMessage());
                            }
                        }).start();
                    }
                } catch (IOException e) {
                    System.err.println("Connection lost: " + e.getMessage());
                    gameStarted = false;
                }
            }).start();

        // NEW CODE: Debugging if failed to connect to server
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to server: " + e.getMessage(), e);
        }
    }

    // NEW CODE: Factory method to initialize the client with a player name
    public static GameClient initializeClient(String playerName) {
        return new GameClient(playerName);
    }

    // NEW CODE: Getters for player name, number, and game state
    public boolean isGameStarted() {
        return gameStarted;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void sendTankState(TankState state) {
        out.println(state.toString());
    }

    // NEW CODE: parseTankStates method to handle incoming tank states from the server
    private void parseTankStates(String data) {
        // Format "playerName:x,y,z,angle;playerName2:..."
        String[] segments = data.split(";");
        Map<String, TankState> updated = new HashMap<>();
        for (String seg : segments) {
            if (!seg.trim().isEmpty()) {
                String[] parts = seg.split(":");
                updated.put(parts[0], TankState.fromString(parts[1]));
            }
        }
        otherTanks = updated;
    }

    // NEW CODE: getOtherTanks method to get the current state of other tanks
    public Map<String, TankState> getOtherTanks() {
        return otherTanks;
    }

    // NEW CODE: stop method to close the socket connection
    public void stop() {
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    // NEW CODE: Main method to run the client and connect to the server
    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        GameClient client = GameClient.initializeClient(name);

        System.out.println("Waiting for other player to connect...");
        while (!client.isGameStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Game started. Player number: " + client.getPlayerNumber());
        System.out.println("Starting TankSimulation...");

        // pass the client instance to TankSimulation
        TankSimulation game = new TankSimulation(name, true, client);
        game.run();
    }
}