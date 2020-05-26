package com.amarasapps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

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
    private BufferedReader input;
    private PrintWriter output;

    private GameState gameState;
    private Scanner terminalIn;
    private boolean inputEntered;
    private  final Thread inputThread = new Thread(new InputThread());

    double availBalance;
    double tableMinBet;

    /**
     * Main Method which creates a new Client object and begins the connection/initialization process
     */
    public static void main(String[] args) {
        BlackjackClient client = new BlackjackClient(DEFAULT_SERVER_PORT, DEFAULT_SERVER_ADDRESS);
        client.beginGame();
    }


    /**
     * Constructor to Initialise the Blackjack Client
     * @param port      Port of the Server
     * @param address   Address of the Server
     */
    public BlackjackClient(int port, String address){
        this.serverPort = port;
        this.serverAddress = address;
        this.terminalIn = new Scanner(System.in);
    }

    /**
     * Begin the Blackjack Game, Connect to Server and Manage Flow
     */
    public void beginGame(){
        try{
            System.out.println("Connecting to Server...");
            this.clientSocket = new Socket(DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT);
            this.clientSocket.setSoTimeout(500);
            InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream());
            this.input = new BufferedReader(isr);
            this.output = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("Successfully Connected to Server!");
            gameState = GameState.NOTSTARTED;
            inputThread.start();
            System.out.println("+--------------------+\n");
            System.out.println("Game Not Started Yet. Waiting for Others to Join");

            inputEntered = true;
            do{
                try{
                    String message = input.readLine();
                    handleServerMessage(message);
                }catch(SocketTimeoutException ignored){}


            }while(gameState != GameState.GAMEOVER);

            try{
                inputThread.join();
            }catch (InterruptedException ignored){}
            clientSocket.close();
            terminalIn.close();
            System.out.println("Game Over! Thanks for Playing");
            System.out.println("Exiting...");

        }catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }

    }

    /**
     * Deals with Messages Received from Server
     * @param message   The Message from the Server
     */
    private void handleServerMessage(String message){
        synchronized (this){
            String[] messageBits = message.split("-");
            if (messageBits.length < 2){
                return;
            }
            switch (messageBits[1]){
                case "ADVANCE":
                    switch (messageBits[2]){
                        case "BETTINGSTAGE":
                            tableMinBet = Double.parseDouble(messageBits[3]);
                            availBalance = Double.parseDouble(messageBits[4]);
                            handleBetStage();
                            break;
                        case "PLAYINGSTAGE":
                            setPlayGameState();
                            break;
                        case "ROUNDOVER":
                        case "WAITINGOTHERS":
                            setWaitingState();
                            break;
                    }
                    break;
                case "PLAYERBALANCE":
                    System.out.println(String.format("Balance Available: %.2f", Double.parseDouble(messageBits[3])));
                case "GAMEOVER":
                    setGameOverState();
                    break;
            }
        }

        if(gameState != GameState.WAITINGOTHERS){
            synchronized (inputThread){
                inputThread.notify();
            }
        }


    }

    /**
     * Handles Messages to be sent to the Server
     * @param message   Message to Be Sent to Server
     */
    private void handleMessage(String message){
        switch (gameState){
            case WAITINGBET:
                double chosenBet = Double.parseDouble(message);
                if(chosenBet >= tableMinBet && chosenBet <= availBalance){
                    output.println(String.format("C-BET-%s", message));
                    inputThreadSleep();
                }else{
                    System.out.println("\n+--------------------+\n");
                    if(chosenBet < tableMinBet){
                        System.out.println("The bet entered is less than the Minimum Bet!");
                    }else if(chosenBet > availBalance){
                        System.out.println("The bet entered is greater than your Available Balance:");
                    }
                    System.out.println(String.format("Available Balance: %.2f", availBalance));
                    System.out.print(String.format("Enter Bet (Min: %.2f): ", tableMinBet));
                }

                break;
            case PLAYING:
                output.println(String.format("C-PLAYING-%s", message));
                break;
            case WAITINGOTHERS:
                break;
            case NOTSTARTED:
                break;
        }

    }

    /**
     * Sets the Game State for the Player to Collect its Bet
     */
    private void handleBetStage(){
        this.gameState = GameState.WAITINGBET;
        inputEntered = true;
        System.out.println("+-------------------+\n");
        System.out.println("It is your Turn to place a Bet");
        System.out.println(String.format("Available Balance: %.2f", availBalance));
        System.out.print(String.format("Enter Your Bet (Min: %.2f): ", tableMinBet));
    }

    /**
     * Sets the Game State for the Player to Place Cards
     */
    private void setPlayGameState(){
        System.out.println("+--------------------+\n");
        System.out.println("Time to Play Your Hand");
        this.gameState = GameState.PLAYING;
        inputEntered = true;
    }

    /**
     * Sets the Game State for the Player to Wait for Others
     */
    private void setWaitingState() {
        System.out.println("+--------------------+\n");
        System.out.println("Currently Waiting for Other Players.");
        this.gameState = GameState.WAITINGOTHERS;
        inputEntered = true;
    }

    /**
     * Sets the Game State for the Player to Wait for Next Game
     */
    private void setNotStartedState(){
        System.out.println("Currently Waiting for Other Players before Moving onto Next Round.");
        this.gameState = GameState.NOTSTARTED;
        inputEntered = true;
    }

    /**
     * Sets the Game State to Game Over
     */
    private void setGameOverState(){
        this.gameState = GameState.GAMEOVER;
    }

    /**
     * Puts the Thread Handling Text Input to Sleep to prevent busy waiting
     */
    private void inputThreadSleep(){
        try{
            synchronized (inputThread){
                inputThread.wait();
            }
        }catch (InterruptedException ignored){}
    }

    /**
     * Sub-Class Which is Thread Executable and Handles User Terminal Input
     */
    private class InputThread implements Runnable{

        @Override
        public void run() {

            while(gameState != GameState.GAMEOVER){
                String message = terminalIn.nextLine();
                handleMessage(message);
            }
            System.out.println("Input Thread Exiting");
        }
    }
}
