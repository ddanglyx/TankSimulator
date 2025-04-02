// javac GameServer.java
// java GameServer
// TankSimulation Game Server
// This is a simple Java game server that allows two players to connect and communicate with each other

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer
{
    private static final int PORT = 12345; // Port number for the server
    private static final int MAX_PLAYERS = 2; // Maximum number of players

    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>(); // List of connected clients

    public static void main(String[] args)
    {
        new GameServer().startServer(); // Start the server
    }

    public void startServer()
    {
        try
        {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Game server started on port " + PORT);

            while (clients.size() < MAX_PLAYERS) // Accept clients until max players reached
            {
                Socket clientSocket = serverSocket.accept(); // Accept a new client connection
                ClientHandler clientHandler = new ClientHandler(clientSocket, this); // Create a new client handler
                clients.add(clientHandler); // Add the client handler to the list
                new Thread(clientHandler).start(); // Start the client handler in a new thread

                System.out.println("Player connected: " + clientSocket.getInetAddress());
            }

            System.out.println("Maximum players reached. No longer accepting connections.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message, ClientHandler sender)
    {
        for (ClientHandler client : clients)
        {
            if (client != sender) // Send the message to the other player
            {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void removeClient(ClientHandler clientHandler)
    {
        clients.remove(clientHandler);
        System.out.println("Player disconnected: " + clientHandler.getClientName());
    }

    // Add a getter method for the clients list
    public synchronized List<ClientHandler> getClients()
    {
        return clients;
    }
}

class ClientHandler implements Runnable
{
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket socket, GameServer server)
    {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run()
    {
        try
        {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Get player name
            // out.println("Enter your name: ");
            clientName = in.readLine();
            System.out.println("Player joined: " + clientName);

            // Notify the player that the game is ready if both players are connected
            if (server.getClients().size() == 2)
            {
                // Get the opponent's name
                // String opponentName = server.getClients().get(0) == this
                //     ? server.getClients().get(1).getClientName()
                //     : server.getClients().get(0).getClientName();

                // Notify both players
                for (ClientHandler client : server.getClients())
                {
                    client.sendMessage("Game is starting!"); 
                    // You are playing against " + opponentName);
                    
                }
            }

            String message;
            while ((message = in.readLine()) != null)
            {
                System.out.println(clientName + ": " + message);
                server.broadcast(clientName + ": " + message, this); // Relay the message to the other player
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                server.removeClient(this);
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message)
    {
        out.println(message);
    }

    public String getClientName()
    {
        return clientName;
    }
}
