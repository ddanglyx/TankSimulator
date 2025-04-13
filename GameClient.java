// javac -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" GameClient.java
// java -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" GameClient

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
    private List<Bullet> remoteBullets = new ArrayList<>();
    private long lastBulletTime = 0;
    private static final long BULLET_COOLDOWN = 250; // 250ms cooldown to prevent spamming bullets, desync

    private GameClient(String playerName) {
        this.playerName = playerName;
        try {
            socket = new Socket("localhost", 12344);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send player name to server
            out.println(playerName);

            // Read player number first
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

            // Start listener thread for server messages
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

    public void sendBulletState(Bullet bullet) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBulletTime > BULLET_COOLDOWN) {
            String bulletData = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f",
                bullet.getX(), bullet.getY(), bullet.getZ(),
                bullet.getDirectionX(), bullet.getDirectionY(), bullet.getDirectionZ(),
                bullet.getR(), bullet.getG(), bullet.getB());
            out.println("BULLET:" + bulletData);
            lastBulletTime = currentTime;
        }
    }

    public List<Bullet> getRemoteBullets() {  // Changed return type from BulletState to Bullet
        synchronized(remoteBullets) {
            List<Bullet> bullets = new ArrayList<>(remoteBullets);
            remoteBullets.clear();
            return bullets;
        }
    }

    private void parseTankStates(String data) {
        if (data.startsWith("BULLET:")) {
            // Handle bullet state
            String bulletData = data.substring(7);
            String[] parts = bulletData.split(",");
            float[] values = new float[parts.length];
            try {
                for (int i = 0; i < parts.length; i++) {
                    values[i] = Float.parseFloat(parts[i]);
                }

                synchronized(remoteBullets) {
                    // Only add the bullet if we don't have any recent bullets at similar positions
                    boolean isDuplicate = remoteBullets.stream().anyMatch(b ->
                        Math.abs(b.getX() - values[0]) < 0.1f &&
                        Math.abs(b.getZ() - values[2]) < 0.1f
                    );

                    if (!isDuplicate) {
                        Bullet bullet = new Bullet(
                            values[0], values[1], values[2],
                            values[3], values[4], values[5],
                            values[6], values[7], values[8]
                        );
                        remoteBullets.add(bullet);
                        System.out.println("Added remote bullet at: " + values[0] + "," + values[1] + "," + values[2]);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing bullet data: " + bulletData);
            }
        } else {
            // Existing tank state parsing
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

        // Pass the client instance to TankSimulation
        TankSimulation game = new TankSimulation(name, true, client);
        game.run();
    }
}