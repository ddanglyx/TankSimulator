// javac GameClient.java
// java GameClient.java

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

                System.out.println(in.readLine()); // Welcome message
                out.println(consoleInput.readLine()); // Send player name

                
            }
    }
}
