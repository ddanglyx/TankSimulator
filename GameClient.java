// javac -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" GameClient.java
// java -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" GameClient

import org.lwjgl.glfw.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameClient {
    private Terrain terrain;
    private TankSimulation simulation;
    private String playerName;
    private int playerNumber;
    private boolean gameStarted = false;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Map<String, TankState> otherTanks = new HashMap<>();

    private synchronized void setGameStarted(boolean started) {
        this.gameStarted = started;
        notifyAll(); // Notify waiting threads when the game starts
    }

    public GameClient(String playerName, Terrain terrain, TankSimulation simulation) {
        this.playerName = playerName;
        try {
            socket = new Socket("localhost", 12344);
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
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("Debug: Received from server: " + line);
                        if (line.equals("START")) {
                            System.out.println("Game is starting!");
                            setGameStarted(true); // Use synchronized setter
                            break; // Exit the read loop after receiving START
                        }
                    }
                    if (isGameStarted()) {
                        // Start a new thread for handling game updates
                        new Thread(() -> {
                            try {
                                String updateLine;
                                while ((updateLine = in.readLine()) != null) {
                                    if (updateLine.startsWith("BULLET:")) {
                                        String bulletData = updateLine.substring(7);
                                        Tank tank = simulation.getTank(); // Ensure this method exists to retrieve the
                                                                          // current tank
                                        Bullet bullet = Bullet.deserialize(bulletData, terrain, tank);
                                        simulation.addBullet(bullet);
                                    } else if (updateLine.contains(":")) {
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
                    setGameStarted(false); // Use synchronized setter
                }
            }).start();

            // NEW CODE: Debugging if failed to connect to server
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to server: " + e.getMessage(), e);
        }
    }

    public static GameClient initializeClient(String playerName, Terrain terrain, TankSimulation simulation) {
        return new GameClient(playerName, terrain, simulation);
    }

    public void sendBulletData(String bulletData) {
        // Logic to send bullet data to the server
        System.out.println("Sending bullet data: " + bulletData);
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

    // NEW CODE: parseTankStates method to handle incoming tank states
    private void parseTankStates(String data) {
        String[] segments = data.split(";");
        Map<String, TankState> updated = new HashMap<>();
        for (String seg : segments) {
            if (!seg.trim().isEmpty()) {
                String[] parts = seg.split(":");
                TankState state = TankState.fromString(parts[1]);
                if (parts[0].equals(playerName)) {
                    state.setColor("red"); // Client tank is red
                } else {
                    state.setColor("blue"); // Remote tanks are blue
                }
                updated.put(parts[0], state);
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

    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        GameClient client = GameClient.initializeClient(name, new Terrain("terrain.obj"),
                new TankSimulation(name, false, null));
        System.out.println("Waiting for other player to connect...");

        synchronized (client) {
            while (!client.isGameStarted()) {
                try {
                    client.wait(); // Wait until notified that the game has started
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for game to start.");
                    return;
                }
            }
        }

        System.out.println("Game started. Player number: " + client.getPlayerNumber());
        System.out.println("Starting TankSimulation...");

        // Pass the client instance to TankSimulation
        TankSimulation game = new TankSimulation(name, true, client);
        game.run();
    }
}