package com.amarasapps;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

/**
 * Class which represents a player within the Blackjack Game. Ran as an individual thread and is blocked at each different game stage
 * by a Latch until the Table Thread unlocks it and allows it to advance.
 *
 * Stores the players Hand, Money, Placed Bets
 */
public class Player implements Runnable {

    private BufferedReader input;   //Input Stream from Player
    private PrintWriter output;     //Output Stream to Player
    private Socket clientSocket;    //Socket COnnecting to Client

    private BJHand hand;        //The Players Hand
    private Table gameTable;        //The Table the player belongs to
    private double balance;     //The Money the player has in the bank
    private double placedBet;   //The money that was placed as a bet

    private boolean isDone = false; //Tracks the State of the Player
    private GameState gameState;    //Tacks the Position in the Game

    private CountDownLatch startGameLatch;  //Latch to make player wiat until game has started
    private CountDownLatch playHandLatch;

    /**
     * Constructor to Create a Runnable Player Object
     * @param socket    The Socket to Communicate with the Client
     * @param table     The Playing Table the Player is Part of
     * @param startingMoney How much money the Player Starts with
     */
    public Player(Socket socket, Table table, double startingMoney){
        this.gameTable = table;
        this.balance = startingMoney;
        clientSocket = socket;
        playHandLatch = new CountDownLatch(1);
        try {
            socket.setSoTimeout(500);
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            input = new BufferedReader(isr);
            output = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Executed when the Class is Run, as a new thread
     */
    @Override
    public void run() {
        boolean inputEntered = true;
        gameState = GameState.NOTSTARTED;
        System.out.println("Running New Player");
        do{
            try {

                if(gameState != GameState.WAITINGOTHERS && gameState != GameState.NOTSTARTED){
                    String clientMessage = input.readLine();
                    handleClientMessage(clientMessage);
                }else{
                    Thread.sleep(500);
                }

            }catch (SocketTimeoutException e){
                inputEntered = false;
            }catch(InterruptedException | IOException ignored){}
        }while(!isDone);

    }

    private void handleClientMessage(String message){
        String[] messageBits = message.split("-");
        if (messageBits.length < 2){
            return;
        }
        switch (messageBits[1]){
            case "BET":
                System.out.println("Bet: " + messageBits[2]);
                double placedBet = Double.parseDouble(messageBits[2]);
                setPlacedBet(placedBet);
                decrementBalance(placedBet);
                setWaitingState();
                gameTable.countDownBetLatch();
                break;
            case "PLAYING":
                System.out.println("Play: " + messageBits[2]);
                setWaitingState();
                playHandLatch.countDown();
                break;
            default:
                break;
        }
    }

    /**
     * Sets the Game State for the Player to Collect its Bet
     */
    public void setGetBetState(){
        System.out.println("Set Bet State");
        this.gameState = GameState.WAITINGBET;

        output.println(String.format("S-ADVANCE-BETTINGSTAGE-%.2f-%.2f", gameTable.getMinimumBet(), getBalance()));
    }

    /**
     * Sets the Game State for the Player to Place Cards
     */
    public void setPlayGameState(){
        this.gameState = GameState.PLAYING;
        output.println("S-ADVANCE-PLAYINGSTAGE");
    }

    /**
     * Sets the Game State for the Player to Wait for Others
     */
    public void setWaitingState() {
        this.gameState = GameState.WAITINGOTHERS;
        output.println("S-ADVANCE-WAITINGOTHERS");
    }

    /**
     * Sets the Game State for the Player to Wait for Next Game
     */
    public void setNotStartedState(){
        this.gameState = GameState.NOTSTARTED;
        output.println("S-ADVANCE-NOTSTARTED");
    }

    public void setGameOverState(){
        isDone = true;
        gameState = GameState.GAMEOVER;
        output.println("S-GAMEOVER");
        try{
            input.close();
            output.close();
            clientSocket.close();
        }catch(IOException ignored){}


    }

    /**
     * Resets the State of the Player, Resetting all Latches and Game State
     */
    public void resetPlayer(){
        gameState = GameState.WAITINGOTHERS;
        playHandLatch = new CountDownLatch(1);
    }

    /**
     * Forces the Table to Wait until the Playing State is Finished
     */
    public void waitPlayHandLatch() throws InterruptedException {
        playHandLatch.await();
    }


    public void setPlacedBet(double placedBet) {
        this.placedBet = placedBet;
    }

    /**
     * Increments the value of the player's balance
     * @param changeAmount The amount to increment the balance by.
     */
    public void incrementBalance(double changeAmount){
        if(changeAmount < 0){
            return;
        }
        this.balance += changeAmount;
    }

    public void decrementBalance(double changeAmount){
        if(changeAmount < 0){
            return;
        }

        this.balance -= changeAmount;
    }

    public double getBalance() {
        return balance;
    }
}


