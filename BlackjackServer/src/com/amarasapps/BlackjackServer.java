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
    private static int SERVER_PORT = 8080;        //Server Port
    private static String SERVER_ADDRESS;               //IP Address of Server

    private static int PLAYERS_PER_TABLE = 2;     //Number of Connected Clients
    private static double MINIMUM_BET = 100.00;         //Minimum Allowed Bet
    private static int DECKS_USED = 8;            //Decks Kept in the Shoe
    private static int CARDS_BEFORE_SHUFFLE = 80; //Cards remaining in the deck before a re-shuffle
    private static double STARTING_MONEY = 500.00;      //Money that Each Player Would STart With

    public static void main(String[] args) {
        try{
            if((args.length%2) != 0){
                expectedUsage();
            }

            for(int i = 0; i < args.length; i+=2){
                try{
                    switch(args[i]){
                        case "--PORT":
                            SERVER_PORT = Integer.parseInt(args[i+1]);
                            if(SERVER_PORT <= 0){
                                System.out.println("Invalid Port Number.");
                                throw new NumberFormatException();
                            }
                            break;
                        case "--PLAYERS":
                            PLAYERS_PER_TABLE = Integer.parseInt(args[i+1]);
                            if(PLAYERS_PER_TABLE <= 0){
                                System.out.println("Invalid Number of Players. Must be greater than 0.");
                                throw new NumberFormatException();
                            }
                            break;
                        case "--MONEY":
                            STARTING_MONEY = Integer.parseInt(args[i+1]);
                            if(STARTING_MONEY <= 0){
                                System.out.println("Invalid Amount of Starting Money. Must be greater than 0.");
                                throw new NumberFormatException();
                            }
                            break;
                        case "--BET":
                            MINIMUM_BET = Integer.parseInt(args[i+1]);
                            if(MINIMUM_BET <= 0){
                                System.out.println("Invalid Minimum Bet. Must be greater than 0.");
                                throw new NumberFormatException();
                            }
                            break;
                        case "--DECKS":
                            DECKS_USED = Integer.parseInt(args[i+1]);
                            if(DECKS_USED <= 0){
                                System.out.println("Invalid Number of Decks. Must be greater than 0.");
                                throw new NumberFormatException();
                            }
                            break;
                        default:
                            expectedUsage();
                    }
                }catch(NumberFormatException e){
                    expectedUsage();
                }
            }
            if(STARTING_MONEY < MINIMUM_BET){
                System.out.println("Starting Money must be greater than/equal to the Minimum Bet");
                expectedUsage();
            }
            if(CARDS_BEFORE_SHUFFLE > 0.2*(52 * DECKS_USED)){
                CARDS_BEFORE_SHUFFLE = (int) Math.floor(0.2 * 52 * DECKS_USED);
            }

            System.out.println("Server Starting...");

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);  //Initialise Server on specified Port
            SERVER_ADDRESS = InetAddress.getLocalHost().getHostAddress();    //Get the IP Address of Server

            System.out.println(String.format("Server Running:\n\tPort: %d\n\tIP Address: %s", SERVER_PORT, SERVER_ADDRESS));
            System.out.println(String.format("\tNumber of Players: %d\n\tDecks Used: %d\n\tCards Before Shuffle: %d" +
                    "\n\tStarting Money: %.2f\n\tMinimum Bet: %.2f",
                    PLAYERS_PER_TABLE, DECKS_USED, CARDS_BEFORE_SHUFFLE, STARTING_MONEY, MINIMUM_BET));

            int connectedClients = 0;   //Number of Currently connected players
            Table playingTable = new Table(MINIMUM_BET, DECKS_USED, CARDS_BEFORE_SHUFFLE);
            System.out.println("Waiting for Clients to Connect...");

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
            System.out.println("Playing Table Finished!");
        }catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }


    }

    private static void expectedUsage(){
        System.out.println("Usage: java -jar BlackjackServer.jar [OPTIONS]");
        System.out.println("Options:");
        System.out.println("\t--PORT <PORT NUMBER>\n\t--PLAYERS <PLAYERS PER TABLE>\n\t--MONEY <STARTING MONEY>");
        System.out.println("\t--BET <MIN BET>\n\t--DECKS <NUMBER OF DECKS USED IN GAME>");
        System.exit(-1);
    }


}
