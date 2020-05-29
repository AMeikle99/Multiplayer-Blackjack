package com.amarasapps;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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

    private ArrayList<BJHand> hands;        //The Players Hand
    private BJHand currentHand;             //The current Players Hand
    private Table gameTable;        //The Table the player belongs to
    private double balance;     //The Money the player has in the bank

    private boolean isDone = false; //Tracks the State of the Player
    private GameState gameState;    //Tacks the Position in the Game
    private boolean askedForInsurance = false;   //Tracks whether the Insurance Stage has completed
    private boolean tookInsurance = false;      //Tracks if the player took insurance or not
    private double insuranceAmount = 0.0;       //The amount the insurance bet is worth

    private CountDownLatch playHandLatch;   //Latch to make the table wait until the player has had their turn
    private CountDownLatch stillPlayingLatch; //Latch to make sure the table wait until the player has decided if they wish to keep playing

    /**
     * Constructor to Create a Runnable Player Object
     * @param socket    The Socket to Communicate with the Client
     * @param table     The Playing Table the Player is Part of
     * @param startingMoney How much money the Player Starts with
     */
    public Player(Socket socket, Table table, double startingMoney){
        this.gameTable = table;
        this.balance = startingMoney;
        hands = new ArrayList<>();
        currentHand = new BJHand();
        hands.add(currentHand);
        clientSocket = socket;
        playHandLatch = new CountDownLatch(1);
        stillPlayingLatch = new CountDownLatch(1);
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
        }while(isStillEligible() && !hasChosenToQuit());
        System.out.println("Exiting Player Thread");
    }

    /**
     * Handles any messages received from the client process and handles appropriately
     * @param message The Message received from the client, a hyphen separated list of commands
     */
    private void handleClientMessage(String message){
        System.out.println("Message: " + message);
        String[] messageBits = message.split("-");
        if (messageBits.length < 2){
            return;
        }
        switch (messageBits[1]){
            case "BET":
                System.out.println("Bet: " + messageBits[2]);
                double placedBet = Double.parseDouble(messageBits[2]);
                setPlacedBet(placedBet);
                if (gameTable.playerCount() > 1) {
                    setWaitingState();
                }

                gameTable.countDownBetLatch();
                break;
            case "PLAYING":
                System.out.println("Play: " + messageBits[2]);
                handlePlayChoice(messageBits[2]);
                break;
            case "INSURANCE":
                if(messageBits[2].equals("Y")){
                    tookInsurance = true;
                }
                setWaitingState();
                gameTable.countDownInsuranceBetLatch();
                break;
            case "PLAYAGAIN":
                System.out.println("Play Again: " + messageBits[2]);
                if(messageBits[2].equals("N")){
                    isDone = true;
                    setGameOverState();
                }else{
                    if(gameTable.playerCount() > 1) {
                        setNotStartedState();
                    }
                }

                gameTable.countDownPlayAgainLatch();
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

    private void setNotStartedState(){
        gameState = GameState.NOTSTARTED;
        output.println("S-ADVANCE-ROUNDOVER");
    }

    /**
     * Sends the Client The State of the Game so as to begin the Play Stage
     */
    public void handlePlayStage(){
        sendInitialTableState();
        sendPlayOptions();
    }

    /**
     * Informs the Player of their Game Options, i.e if they are bust or can Hit or Stand
     */
    private void sendPlayOptions(){
        if(gameTable.getDealerUpCard().isAce() && !askedForInsurance){
            if(canOfferInsurance()){
                askedForInsurance = true;
                gameState = GameState.OFFERINSURANCE;
                output.println("S-PLAYINGSTAGE-OFFERINSURANCE");
            }else{
                output.println("S-PLAYINGSTAGE-TOOPOORINSURANCE");   //The player doesn't have enough momney to take insurance
            }
        }else if(currentHand.hasBlackjack()) {
            output.println("S-PLAYINGSTAGE-PLAYERBJ");
            if(gameTable.playerCount() > 1)
                setWaitingState();
            playHandLatch.countDown();
        }else if(currentHand.isBust()) {
            output.println("S-PLAYINGSTAGE-PLAYERBUST");
            if(gameTable.playerCount() > 1)
                setWaitingState();
            playHandLatch.countDown();
        }else if(currentHand.handValue() == 21){
            output.println("S-PLAYINGSTAGE-PLAYERMAXVAL");
            if(gameTable.playerCount() > 1)
                setWaitingState();
            playHandLatch.countDown();
        }else if(!currentHand.isDoubledDown()){
            StringBuilder playOption = new StringBuilder();
            playOption.append("HITSTAND");
            if(currentHand.canDouble(balance)){
                playOption.append("DOUBLE");
            }
            if(currentHand.canSplit()){
                playOption.append("SPLIT");
            }
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
                currentHand.addCard(gameTable.dealCard());
                sendPlayerHandState(currentHand);
                sendPlayOptions();
                break;
            case "S":
                if(isNotFinalHand()){
                    currentHand = getNextHand();
                    handlePlayStage();
                }else{
                    if (gameTable.playerCount() > 1 && !gameTable.isLastPlayer(this)) {
                        setWaitingState();
                    }
                    playHandLatch.countDown();
                }
                break;
            case "D":
                currentHand.addCard(gameTable.dealCard());
                currentHand.setDoubledDown();
                sendPlayerHandState(currentHand);
                output.println(String.format("S-PLAYINGSTAGE-DD-%.2f", currentHand.getHandBet()));
                if(isNotFinalHand()){
                    currentHand = getNextHand();
                    handlePlayStage();
                }else{
                    if (gameTable.playerCount() > 1 && !gameTable.isLastPlayer(this)) {
                        setWaitingState();
                    }
                    playHandLatch.countDown();
                }
                break;
            case "SP":
                splitHand();
                output.println("S-PLAYINGSTAGE-SPLITHAND");
                sendPlayerHandState(currentHand);
                sendPlayOptions();
        }
    }

    /**
     * Sends the client what cards are in both their hand and the dealers hand
     */
    public void sendInitialTableState(){
        String dealerHandState = String.format("S-DEALERHAND-%d-%s-XX", gameTable.getDealerVisibleValue(), gameTable.getDealerUpCard());
        output.println(dealerHandState);
        sendPlayerHandState(currentHand);
    }

    /**
     * Send the client the cards present in their own hand
     */
    public void sendPlayerHandState(BJHand hand){
        StringBuilder playerHandState = new StringBuilder();
        playerHandState.append(String.format("S-PLAYERHAND-%d-%d",hands.indexOf(hand)+1,hand.handValue()));

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

        double totalPayout = 0;

        for(int i=0; i<hands.size(); i++){
            BJHand hand = hands.get(i);
            sendPlayerHandState(hand);
            if(hand.isBust()){
                totalPayout -= hand.getHandBet();
                decrementBalance(hand.getHandBet());
                output.println(String.format("S-PAYOUTSTAGE-HANDLOSE-%d",i+1));
                continue;
            }

            if(hand.handValue() == getDealersHand().handValue()){
                output.println(String.format("S-PAYOUTSTAGE-HANDPUSH-%d", i+1));
                continue;
            }

            if(hand.hasBlackjack()){
                double payout = 1.5 * hand.getHandBet();
                totalPayout += payout;
                incrementBalance(payout);
                output.println(String.format("S-PAYOUTSTAGE-HANDWIN-%d", i+1));
                continue;
            }

            if(hand.handValue() > getDealersHand().handValue() || getDealersHand().isBust()){
                totalPayout += hand.getHandBet();
                incrementBalance(hand.getHandBet());
                output.println(String.format("S-PAYOUTSTAGE-HANDWIN-%d", i+1));
                continue;
            }

            if(hand.handValue() < getDealersHand().handValue()){
                totalPayout -= hand.getHandBet();
                decrementBalance(hand.getHandBet());
                output.println(String.format("S-PAYOUTSTAGE-HANDLOSE-%d", i+1));
            }
        }

        totalPayout -= insuranceAmount;

        if(totalPayout < 0){
            output.println(String.format("S-PAYOUTSTAGE-ROUNDLOSE-%.2f-%.2f",balance, -1*totalPayout));
        }else{
            output.println(String.format("S-PAYOUTSTAGE-ROUNDWIN-%.2f-%.2f",balance, totalPayout));
        }
    }

    public void informInsuranceOutcome(){
        if(gameTable.getDealersHand().hasBlackjack()){
            sendDealerHandState();
            output.println("S-INSURANCE-DEALERBJ");
            if(tookInsurance){
                output.println("S-INSURANCE-WININSURANCE");
                output.println(String.format("S-PAYOUTSTAGE-ROUNDWIN-%.2f-%.2f",balance,0));
            }else {
                if (askedForInsurance) {
                    output.println("S-INSURANCE-BJNOPAYOUT");
                }
                balance -= currentHand.getHandBet();
                output.println(String.format("S-PAYOUTSTAGE-ROUNDLOSE-%.2f-%.2f", balance, currentHand.getHandBet()));
            }
        }else{
            output.println("S-INSURANCE-NODEALERBJ");
            if(tookInsurance){
                balance -= 0.5 * currentHand.getHandBet();
                insuranceAmount = 0.5*currentHand.getHandBet();
                output.println(String.format("S-INSURANCE-LOSEINSURANCE-%.2f", insuranceAmount));
            }else{
                if(askedForInsurance){
                    output.println("S-INSURANCE-NOBJNOPAYOUT");
                }
            }
        }

        if(gameTable.playerCount() > 1 && !gameTable.isLastPlayer(this)){
            setWaitingState();
        }
    }

    /**
     * Sets the Game State for the Player to Wait for Others
     */
    private void setWaitingState() {
        this.gameState = GameState.WAITINGOTHERS;
        output.println("S-ADVANCE-WAITINGOTHERS");

    }

    /**
     * Sets the Game State fot the client to be game over and closes the connection with them
     */
    public void setGameOverState(){
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
        playHandLatch = new CountDownLatch(1);
        stillPlayingLatch = new CountDownLatch(1);
        askedForInsurance = false;
        tookInsurance = false;
        insuranceAmount = 0.0;
        hands.clear();
        currentHand = new BJHand();
        hands.add(currentHand);
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
        this.currentHand.setHandBet(placedBet);
    }

    /**
     * Increments the value of the player's balance
     * @param changeAmount The amount to increment the balance by.
     */
    private void incrementBalance(double changeAmount){
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
     * Returns the current hand the player has
     * @return The Hand the player has
     */
    public BJHand getCurrentHand() {
        return currentHand;
    }

    public BJHand getDealersHand(){
        return gameTable.getDealersHand();
    }

    public boolean isStillEligible(){
        return balance >= gameTable.getMinimumBet();
    }

    public boolean isNotStillElligible(){
        return !isStillEligible();
    }

    public void setPlayAgainState(){
        gameState = GameState.PLAYAGAIN;
        output.println("S-ADVANCE-PLAYAGAIN");
    }

    public void informLowBalance(){
        output.println("S-LOWBALANCE");
    }

    public boolean hasChosenToQuit(){
        return isDone;
    }

    private boolean isNotFinalHand(){
        return hands.indexOf(currentHand) < hands.size()-1;
    }

    private BJHand getNextHand(){
        return hands.get(hands.indexOf(currentHand)+1);
    }

    private void splitHand(){
        BJHand newHand = new BJHand();
        newHand.addCard(currentHand.removeCard(1));
        newHand.setHandBet(currentHand.getHandBet());

        currentHand.addCard(gameTable.dealCard());
        newHand.addCard(gameTable.dealCard());
        hands.add(hands.indexOf(currentHand)+1, newHand);
    }

    public boolean hasInsuranceBet(){
        return askedForInsurance;
    }

    public boolean canOfferInsurance(){
        return balance >= 1.5 * currentHand.getHandBet();
    }
}


