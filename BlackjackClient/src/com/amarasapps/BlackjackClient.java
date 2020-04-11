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
    private final Thread gameProgress = new Thread(new HandleGameProgress());
    private  final Thread inputThread = new Thread(new InputThread());
    /**
     * Main Method which creates a new Client object and begins the connection/initialization process
     */
    public static void main(String[] args) {
        BlackjackClient client = new BlackjackClient(DEFAULT_SERVER_PORT, DEFAULT_SERVER_ADDRESS);
        client.beginGame();
    }


    public BlackjackClient(int port, String address){
        this.serverPort = port;
        this.serverAddress = address;
        this.terminalIn = new Scanner(System.in);
    }

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
            gameProgress.start();

            inputEntered = true;
            do{
                try{
                    String message = input.readLine();
                    System.out.println(message);
                    handleServerMessage(message);
                }catch(SocketTimeoutException ignored){}


            }while(gameState != GameState.GAMEOVER);

            try{
                gameProgress.join();
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

    private void handleServerMessage(String message){
        synchronized (gameProgress){
            System.out.println(message);
            String[] mesageBits = message.split("-");
            if (mesageBits.length < 2){
                return;
            }
            switch (mesageBits[1]){
                case "ADVANCE":
                    switch (mesageBits[2]){
                        case "BETTINGSTAGE":
                            setGetBetState();
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
                case "GAMEOVER":
                    setGameOverState();
                    break;
            }
        }

        synchronized (gameProgress){
            gameProgress.notify();
        }
        if(gameState != GameState.WAITINGOTHERS){
            synchronized (inputThread){
                inputThread.notify();
            }
        }


    }

    private void handleMessage(String message){
        System.out.println(message);
        switch (gameState){
            case WAITINGBET:
                output.println(String.format("C-BET-%s", message));
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
    private void setGetBetState(){
        System.out.println("Setting Bet State");
        this.gameState = GameState.WAITINGBET;
        inputEntered = true;
    }

    /**
     * Sets the Game State for the Player to Place Cards
     */
    private void setPlayGameState(){
        this.gameState = GameState.PLAYING;
        inputEntered = true;
    }

    /**
     * Sets the Game State for the Player to Wait for Others
     */
    private void setWaitingState() {
        this.gameState = GameState.WAITINGOTHERS;
        inputEntered = true;
    }

    /**
     * Sets the Game State for the Player to Wait for Next Game
     */
    private void setNotStartedState(){
        this.gameState = GameState.NOTSTARTED;
        inputEntered = true;
    }

    private void setGameOverState(){
        this.gameState = GameState.GAMEOVER;
    }

    private class HandleGameProgress implements Runnable {

        @Override
        public void run() {
            while(gameState != GameState.GAMEOVER){
                System.out.println("Game Progress");
                synchronized (gameProgress){
                    switch (gameState){
                        case NOTSTARTED:
                            System.out.println("Game Not Started Yet. Waiting for Others to Join");
                            break;
                        case WAITINGBET:
                            System.out.println("It is your Turn to place a Bet");
                            System.out.print("Enter Bet: ");
                            break;
                        case PLAYING:
                            System.out.println("Time to Play Your Hand");
                            break;
                        case WAITINGOTHERS:
                            System.out.println("Currently Waiting for Other Players.");
                            break;
                    }
                }

                try{
                    synchronized (gameProgress){
                        gameProgress.wait();
                    }

                }catch(InterruptedException e){
                    e.printStackTrace();
                }

            }
            System.out.println("Progress Thread Exiting");
        }
    }

    private class InputThread implements Runnable{

        @Override
        public void run() {
            while(gameState != GameState.GAMEOVER){
                System.out.println(gameState);
                String message = terminalIn.nextLine();
                handleMessage(message);
                try{
                    synchronized (inputThread){
                        inputThread.wait();
                        System.out.println("Input Thread Woken");
                    }
                }catch (InterruptedException ignored){}
            }
            System.out.println("Input Thread Exiting");
        }
    }
}
