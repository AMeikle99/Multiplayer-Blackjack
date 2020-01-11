package com.amarasapps;

import java.io.IOException;
import java.net.Socket;

/**
 * The object that initialises the client's socket and initialises the members which control gameplay.
 *
 * @author Aiden Meikle
 */


public class BlackjackClient {

    private static final int DEFAULT_SERVER_PORT = 8080;                //Default Server Port
    private static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";   //Default Server Address

    //Move the below Members to a separate class, create a member of the new class
    private int serverPort;
    private String serverAddress;

    private Socket clientSocket;

    /**
     * Main Method which creates a new Client object and begins the connection/initialization process
     */
    public static void main(String[] args) {
        BlackjackClient client = new BlackjackClient(DEFAULT_SERVER_PORT, DEFAULT_SERVER_ADDRESS);
        client.beginGame();
    }


    public BlackjackClient(int port, String address){
        this.serverAddress = address;
        this.serverPort = port;
    }

    public void beginGame(){
        try{
            System.out.println("Connecting to Server...");
            clientSocket = new Socket(DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT);
            System.out.println("Successfully Connected to Server!");
        }catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
