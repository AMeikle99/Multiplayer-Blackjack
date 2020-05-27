package com.amarasapps;


/*
  Multiplayer Blackjack Game
  Author: Aiden Meikle
  Github: AMeikle99
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * This is the executable class, it will create a new Table Object Thread
 * This class manages initial socket connections with clients
 */

public class BlackjackServer {

    //Default Variables
    private static final int SERVER_PORT = 8080;        //Server Port
    private static String SERVER_ADDRESS;               //IP Address of Server

    private static final int PLAYERS_PER_TABLE = 4;     //Number of Connected Clients
    private static final double MINIMUM_BET = 100.00;         //Minimum Allowed Bet
    private static final int DECKS_USED = 8;            //Decks Kept in the Shoe
    private static final int CARDS_BEFORE_SHUFFLE = 80; //Cards remaining in the deck before a re-shuffle
    private static final double STARTING_MONEY = 500.00;      //Money that Each Player Would STart With

    public static void main(String[] args) {

        try{
            System.out.println("Server Starting...");

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);  //Initialise Server on specified Port
            SERVER_ADDRESS = InetAddress.getLocalHost().getHostAddress();    //Get the IP Address of Server

            System.out.println(String.format("Server Running:\n\tPort: %d\n\tIP Address: %s", SERVER_PORT, SERVER_ADDRESS));

            int connectedClients = 0;   //Number of Currently connected players
            Table playingTable = new Table(MINIMUM_BET, DECKS_USED, CARDS_BEFORE_SHUFFLE);

            do{
                Socket playerSocket = serverSocket.accept();    //Connect new client
                connectedClients++;

                int clientPort = playerSocket.getPort();    //Extract Server Port and IP Address
                String clientAddress = playerSocket.getInetAddress().getHostAddress();

                Player player = new Player(playerSocket, playingTable, STARTING_MONEY);
                playingTable.addPlayer(player);
                new Thread(player).start();
                System.out.println(String.format("Client %d Connected:\n\tPort: %s\n\tIP Address: %s", connectedClients, clientPort, clientAddress));

            }while(connectedClients < PLAYERS_PER_TABLE);

            System.out.println("All Clients Connected");
            playingTable.run();
        }catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }


    }


}
