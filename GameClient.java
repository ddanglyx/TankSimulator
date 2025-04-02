// javac GameClient.java
// java GameClient

// GameClient for the TankSimulator game.
// 1v1 game
import java.io.*;
import java.net.*;

public class GameClient
{
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) 
    {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in)))
        {
            // Send player name to the server
            System.out.print("Enter your player name: ");
            String playerName = consoleInput.readLine();
            out.println(playerName); // Send player name to the server

            // Start a thread to listen for server messages
            new Thread(() -> 
            {
                String serverMessage;
                try
                {
                    while ((serverMessage = in.readLine()) != null)
                    {
                        // Handle server messages (e.g., game state updates)
                        System.out.println(serverMessage);
                    }
                }
                catch (IOException e)
                {
                    System.out.println("Connection to server lost.");
                }
            }).start();
                
            // Main game loop: send user input to the server
            String userInput;
            while((userInput = consoleInput.readLine()) != null)
            {
                // Send game actions to the server
                out.println(userInput);
            }
        }
        catch (IOException e)
        {
            System.out.println("Unable to connect to the server.");
            e.printStackTrace();
        }
    }
}