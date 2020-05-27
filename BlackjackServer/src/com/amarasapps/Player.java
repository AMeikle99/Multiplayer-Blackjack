package com.amarasapps;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
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

    private boolean isDone = false; //Tracks the State of the Player
    private GameState gameState;    //Tacks the Position in the Game

    private CountDownLatch playHandLatch;   //Latch to make the table wait until the player has had their turn

    /**
     * Constructor to Create a Runnable Player Object
     * @param socket    The Socket to Communicate with the Client
     * @param table     The Playing Table the Player is Part of
     * @param startingMoney How much money the Player Starts with
     */
    public Player(Socket socket, Table table, double startingMoney){
        this.gameTable = table;
        this.balance = startingMoney;
        hand = new BJHand();
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

    /**
     * Handles any messages received from the client process and handles appropriately
     * @param message The Message received from the client, a hyphen separated list of commands
     */
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
                handlePlayChoice(messageBits[2]);
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
     * Sends the Client The State of the Game so as to begin the Play Stage
     */
    public void handlePlayStage(){
        setPlayGameState();
        sendInitialTableState();
        sendPlayOptions();
    }

    /**
     * Informs the Player of their Game Options, i.e if they are bust or can Hit or Stand
     */
    private void sendPlayOptions(){
        if(hand.isDoubledDown()){

        }
        if(hand.hasBlackjack()) {
            output.println("S-PLAYINGSTAGE-PLAYERBJ");
            setWaitingState();
            playHandLatch.countDown();
        }else if(hand.isBust()) {
            output.println("S-PLAYINGSTAGE-PLAYERBUST");
            setWaitingState();
            playHandLatch.countDown();
        }else if(hand.handValue() == 21){
            output.println("S-PLAYINGSTAGE-PLAYERMAXVAL");
            setWaitingState();
            playHandLatch.countDown();
        }else if(!hand.isDoubledDown()){
            StringBuilder playOption = new StringBuilder();
            playOption.append("HITSTAND");
            if(hand.canDouble(balance)){
                playOption.append("DOUBLE");
            }
            //TODO: Implement Splitting Ability, Need to have array of hands for player
            /*if(hand.canSplit()){
                playOption.append("SPLIT");
            }*/
            output.println(String.format("S-PLAYINGSTAGE-%s", playOption));
        }
    }

    /**
     * Handles the Play Stage Choice sent from the client, takes appropriate action
     * @param choice The play option sent from the client
     */
    private void handlePlayChoice(String choice){
        switch(choice){
            case "H":
                hand.addCard(gameTable.dealCard());
                sendPlayerHandState();
                sendPlayOptions();
                break;
            case "S":
                setWaitingState();
                playHandLatch.countDown();
                break;
            case "D":
                hand.addCard(gameTable.dealCard());
                balance -= hand.getHandBet();
                hand.setDoubledDown();
                sendPlayerHandState();
                sendPlayOptions();
                output.println(String.format("S-PLAYINGSTAGE-DD-%.2f", balance));
                setWaitingState();
                playHandLatch.countDown();
                break;
        }
    }

    /**
     * Sends the client what cards are in both their hand and the dealers hand
     */
    private void sendInitialTableState(){
        String dealerHandState = String.format("S-DEALERHAND-%d-%s-XX", gameTable.getDealerVisibleValue(), gameTable.getDealerUpCard());
        output.println(dealerHandState);
        sendPlayerHandState();
    }

    /**
     * Send the client the cards present in their own hand
     */
    public void sendPlayerHandState(){
        StringBuilder playerHandState = new StringBuilder();
        playerHandState.append(String.format("S-PLAYERHAND-%d",hand.handValue()));

        for(int i = 0; i<hand.size(); i++){
            Card card = hand.getCard(i);
            playerHandState.append(String.format("-%s", card.toString()));
        }

        output.println(playerHandState.toString());
    }

    /**
     * Sends the client the cards that were in the dealers hand
     */
    public void sendDealerHandState(){
        StringBuilder dealerHandState = new StringBuilder();
        dealerHandState.append(String.format("S-DEALERHAND-%d",getDealersHand().handValue()));

        for(int i = 0; i<getDealersHand().size(); i++){
            Card card = getDealersHand().getCard(i);
            dealerHandState.append(String.format("-%s", card.toString()));
        }

        output.println(dealerHandState.toString());
        if(getDealersHand().hasBlackjack()){
            output.println("S-PAYOUTSTAGE-DEALERBJ");
        }else if(getDealersHand().isBust()){
            output.println("S-PAYOUTSTAGE-DEALERBUST");
        }
    }

    public void processPayout(){
        if(hand.isBust()){
            output.println(String.format("S-PAYOUTSTAGE-LOSE-%.2f-%.2f", balance, hand.getHandBet()));
            return;
        }

        if(hand.handValue() == getDealersHand().handValue()){
            balance += hand.getHandBet();
            output.println(String.format("S-PAYOUTSTAGE-PUSH-%.2f", balance));
            return;
        }

        if(hand.hasBlackjack()){
            double payout = 1.5 * hand.getHandBet();
            balance += (payout + hand.getHandBet());
            output.println(String.format("S-PAYOUTSTAGE-WIN-%.2f-%.2f", balance, payout));
            return;
        }

        if(hand.handValue() > getDealersHand().handValue() || getDealersHand().isBust()){
            double payout = hand.getHandBet();
            balance += (payout + hand.getHandBet());
            output.println(String.format("S-PAYOUTSTAGE-WIN-%.2f-%.2f", balance, payout));
            return;
        }

        if(hand.handValue() < getDealersHand().handValue()){
            output.println(String.format("S-PAYOUTSTAGE-LOSE-%.2f-%.2f", balance, hand.getHandBet()));
        }
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

    /**
     * Sets the Game State fot the client to be game over and closes the connection with them
     */
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

    /**
     * Sets the Bet that the User has Placed
     * @param placedBet The amount that the player has opted to bet on their hand
     */
    private void setPlacedBet(double placedBet) {
        this.hand.setHandBet(placedBet);
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

    /**
     * Decrement the amount of money available to the player
     * @param changeAmount  Amount to decrement by
     */
    private void decrementBalance(double changeAmount){
        if(changeAmount < 0){
            return;
        }

        this.balance -= changeAmount;
    }

    /**
     * Returns the balance available to the player
     * @return  The player's available balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Returns the hand the player has
     * @return The Hand the player has
     */
    public BJHand getHand() {
        return hand;
    }

    public BJHand getDealersHand(){
        return gameTable.getDealersHand();
    }
}


