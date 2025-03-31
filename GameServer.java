// javac GameServer.java
// java GameServer

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer
{
    private static final int PORT = 12345; // Port number for the server
    private static final int MAX_PLAYERS = 4; // Maximum number of players
    private static final int TEAM_SIZE = 2; // Number of players per team

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

            while(clients.size() < MAX_PLAYERS) // Accept clients until max players reached
            {
                Socket clientSocket = serverSocket.accept(); // Accept a new client connection
                ClientHandler clientHandler = new ClientHandler(clientSocket, this); // Create a new client handler
                clients.add(clientHandler); // Add the client handler to the list
                new Thread(clientHandler).start(); // Start the client handler in a new thread

                System.out.println("Player connected: " + clientSocket.getInetAddress());
            }

            System.out.println("Maximum players reached. No longer accept");
        }

        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message, ClientHandler sender)
    {
        for (ClientHandler client : clients)
        {
            if(client != sender)
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

    public synchronized String assignTeam(ClientHandler clientHandler)
    {
        long team1Count = clients.stream().filter(c -> "Team 1".equals(c.getTeam())).count();
        long team2Count = clients.stream().filter(c -> "Team 2".equals(c.getTeam())).count();

        if(team1Count < TEAM_SIZE)
        {
            clientHandler.setTeam("Team 1");
            return "Team 1";
        }
        else if (team2Count < TEAM_SIZE)
        {
            clientHandler.setTeam("Team 2");
            return "Team 2";
        }
        else 
        {
            return "Spectator"; // In case teams are full
        }
    }
}

class ClientHandler implements Runnable
{
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;
    private String team;

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
            in = new BufferedReader (new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Get player name 
            out.println("Enter your name: ");

            clientName = in.readLine();
            team = server.assignTeam(this);
            out.println("You are assigned to " + team);

            String message;
            while ((message = in.readLine()) != null)
            {
                System.out.println(clientName + " (" + team + ") : " + message);
                server.broadcast(clientName + " (" + team + ") : " + message, this);
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

    public String getTeam()
    {
        return team;
    }

    public void setTeam(String team)
    {
        this.team = team;
    }
}
