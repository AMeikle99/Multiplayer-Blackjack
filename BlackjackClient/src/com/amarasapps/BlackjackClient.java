package com.amarasapps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
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

    private int serverPort;                                             //Port Used by the Server
    private String serverAddress;                                       //Address Used by Server
    private Socket serverSocket;                                        //Socket to Communicate with Server
    private BufferedReader input;                                       //Input Stream from the Server
    private PrintWriter output;                                         //Output Stream to the Server

    private GameState gameState;                                        //The Current State for the Player
    private Scanner terminalIn;                                         //Input Stream from the Terminal
    private final Thread inputThread = new Thread(new InputThread());   //Thread that manages input from the User

    private double availBalance;                                        //The Balance Available to the User
    private double tableMinBet;                                         //The Minimum Bet for the Table
    private String gamePlayOptions;                                     //The possible options a player can choose from for the PlayStage
    private double betAmount;                                           //The amount the player has bet for this round

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
            this.serverSocket = new Socket(DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT);
            this.serverSocket.setSoTimeout(500);
            InputStreamReader isr = new InputStreamReader(serverSocket.getInputStream());
            this.input = new BufferedReader(isr);
            this.output = new PrintWriter(serverSocket.getOutputStream(), true);

            System.out.println("Successfully Connected to Server!");
            gameState = GameState.NOTSTARTED;
            inputThread.start();
            System.out.println("+--------------------+");
            System.out.println("Game Not Started Yet. Waiting for Others to Join");

            do{
                try{
                    String message = input.readLine();
                    handleServerMessage(message);
                }catch(SocketTimeoutException ignored){}

            }while(gameState != GameState.GAMEOVER);

            try{
                synchronized (inputThread){
                    inputThread.notify();
                }
                inputThread.join();
            }catch (InterruptedException ignored){}
            serverSocket.close();
            terminalIn.close();
            System.out.println("+--------------------+");
            System.out.println("Game Over! Thanks for Playing");
            System.out.println("Exiting...");
            System.out.println("+--------------------+");

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
                            synchronized (inputThread){
                                inputThread.notify();
                            }
                            handleBetStage();
                            break;
                        case "PLAYINGSTAGE":
                            setPlayGameState();
                            break;
                        case "ROUNDOVER":
                            setNotStartedState();
                            break;
                        case "WAITINGOTHERS":
                            setWaitingState();
                            break;
                        case "PLAYAGAIN":
                            synchronized (inputThread){
                                inputThread.notify();
                            }
                            setPlayAgainState();
                            break;

                    }
                    break;
                case "PLAYERHAND": {
                    try {
                        Thread.sleep(500);
                    }catch (InterruptedException ignored){}
                    int handNumber = Integer.parseInt(messageBits[2]);
                    int handValue = Integer.parseInt(messageBits[3]);
                    String[] cards = Arrays.copyOfRange(messageBits, 4, messageBits.length);
                    printPlayerHand(handNumber, handValue, cards);
                    break;
                }
                case "DEALERHAND": {
                    int handValue = Integer.parseInt(messageBits[2]);
                    String[] cards = Arrays.copyOfRange(messageBits, 3, messageBits.length);
                    printDealerHand(handValue, cards);
                    break;
                }
                case "PLAYERBALANCE":
                    availBalance = Double.parseDouble(messageBits[2]);
                    System.out.println(String.format("Available Balance: %.2f", availBalance));
                case "INSURANCE":
                    switch(messageBits[2]){
                        case "DEALERBJ":
                            System.out.println("The Dealer has a BlackJack");
                            break;
                        case "NODEALERBJ":
                            System.out.println("The Dealer did not have a BlackJack");
                            break;
                        case "WININSURANCE":
                            System.out.println("Your Insurance has covered your bet. No Money won or Lost");
                            break;
                        case "LOSEINSURANCE":
                            double insuranceAmount = Double.parseDouble(messageBits[3]);
                            System.out.println(String.format("You have lost your insurance bet of %.2f", insuranceAmount));
                            break;
                        case "BJNOPAYOUT":
                            System.out.println("Looks like you should have taken the Insurance you Bampot!");
                            break;
                        case "NOBJNOPAYOUT":
                            System.out.println("Can you see the future? Good call not taking Insurance you Legend!");
                            break;
                    }
                case "PLAYINGSTAGE":
                    switch (messageBits[2]){
                        case "OFFERINSURANCE":
                            gameState = GameState.OFFERINSURANCE;
                            synchronized (inputThread){
                                inputThread.notify();
                            }
                            System.out.println("The Dealer Has an Ace Showing!");
                            System.out.print("Do you want to buy insurance (Y/N): ");
                            break;
                        case "TOOPOORINSURANCE":
                            synchronized (inputThread){
                                inputThread.notify();
                            }
                            System.out.println("The Dealer Has an Ace Showing!");
                            System.out.println("Sadly you don't have enough money to buy insurance.");
                            System.out.println("Good Luck!");
                            break;
                        case "PLAYERBJ":
                            System.out.println("You Have BlackJack! Well Done!");
                            try{
                                Thread.sleep(500);
                            }catch(InterruptedException ignored){}
                            break;
                        case "PLAYERBUST":
                            System.out.println("You Have Gone Bust! Nice One Ya Idgit!");
                            try{
                                Thread.sleep(500);
                            }catch(InterruptedException ignored){}
                            break;
                        case "PLAYERMAXVAL":
                            System.out.println("Your Hand Is Worth 21. Moving On...");
                            try{
                                Thread.sleep(500);
                            }catch(InterruptedException ignored){}
                            break;
                        case "HITSTAND":
                        case "HITSTANDDOUBLE":
                        case "HITSTANDDOUBLESPLIT":
                        case "HITSTANDSPLIT":
                            synchronized (inputThread){
                                inputThread.notify();
                            }
                            gamePlayOptions = messageBits[2];
                            printPlayOptions();
                            break;
                        case "DD":
                            betAmount *= 2;
                            availBalance = Double.parseDouble(messageBits[3]);
                            System.out.println(String.format("New Bet Amount: %.2f", betAmount));
                            System.out.println(String.format("New Balance Amount: %.2f", availBalance));
                            break;
                        case "SPLITHAND":
                            System.out.println("Hands Split");
                            System.out.println("+--------------------+");
                            break;
                    }
                    break;
                case "PAYOUTSTAGE":
                    switch(messageBits[2]){
                        case "DEALERBJ":
                            System.out.println("Dealer Has BlackJack!");
                            break;
                        case "DEALERBUST":
                            System.out.println("Dealer Has Bust!");
                            break;
                        case "HANDPUSH": {
                            int thisHand = Integer.parseInt(messageBits[3]);
                            System.out.printf("Hand %d Push!\n", thisHand);
                            break;
                        }
                        case "HANDWIN": {
                            int thisHand = Integer.parseInt(messageBits[3]);
                            System.out.printf("Hand %d Wins!\n", thisHand);
                            break;
                        }
                        case "HANDLOSE": {
                            int thisHand = Integer.parseInt(messageBits[3]);
                            System.out.printf("Hand %d Loses!\n", thisHand);
                            break;
                        }
                        case "ROUNDLOSE": {
                            try {
                                Thread.sleep(500);
                            }catch (InterruptedException ignored){}
                            availBalance = Double.parseDouble(messageBits[3]);
                            double payout = Double.parseDouble(messageBits[4]);
                            System.out.println("+--------------------+");
                            System.out.println("Round Update:");
                            System.out.printf("\tNew Balance: %.2f\n", availBalance);
                            System.out.printf("\tBalance Change: -%.2f\n", payout);
                            break;
                        }
                        case "ROUNDWIN": {
                            try {
                                Thread.sleep(500);
                            }catch (InterruptedException ignored){}
                            availBalance = Double.parseDouble(messageBits[3]);
                            double payout = Double.parseDouble(messageBits[4]);
                            System.out.println("+--------------------+");
                            System.out.println("Round Update:");
                            System.out.printf("\tNew Balance: %.2f\n", availBalance);
                            System.out.printf("\tBalance Change: +%.2f\n", payout);
                            break;
                        }
                    }
                    break;
                case "LOWBALANCE":
                    System.out.println("Your Balance is Less than the Table Minimum Bet.");
                    break;
                case "GAMEOVER":
                    setGameOverState();
                    break;
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
                    betAmount = chosenBet;
                    inputThreadSleep();
                }else{
                    System.out.println();
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
                switch (gamePlayOptions){
                    case "HITSTAND":
                        validatePlayChoice("H-S", message);
                        break;
                    case "HITSTANDDOUBLE":
                        validatePlayChoice("H-S-D", message);
                        break;
                    case "HITSTANDSPLIT":
                        validatePlayChoice("H-S-SP", message);
                        break;
                    case "HITSTANDDOUBLESPLIT":
                        validatePlayChoice("H-S-D-SP", message);
                        break;
                }
                break;
            case OFFERINSURANCE:
                if(!message.equals("Y") && !message.equals("N")){
                    System.out.println("Invalid Choice!");
                    System.out.print("Would you like to buy insurance (Y/N): ");
                }else{
                    output.println("C-INSURANCE-" + message);
                    inputThreadSleep();
                }
                break;
            case PLAYAGAIN:
                if(!message.equals("Y") && !message.equals("N")){
                    System.out.println("Invalid Choice!");
                    System.out.print("Would you like to Play Again (Y/N): ");
                }else{
                    output.println("C-PLAYAGAIN-" + message);
                    inputThreadSleep();
                }
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
        try {
            Thread.sleep(300);
        }catch (InterruptedException ignored){}
        System.out.println("+--------------------+");
        System.out.println("New Round - It is your Turn to place a Bet");
        System.out.println(String.format("Available Balance: %.2f", availBalance));
        System.out.print(String.format("Enter Your Bet (Min: %.2f): ", tableMinBet));
    }

    /**
     * Sets the Game State for the Player to Place Cards
     */
    private void setPlayGameState(){
        System.out.println("+--------------------+");
        System.out.println("Time to Play Your Hand");
        this.gameState = GameState.PLAYING;
    }

    /**
     * Sets the Game State for the Player to Wait for Others
     */
    private void setWaitingState() {
        System.out.println("+--------------------+");
        System.out.println("Currently Waiting for Other Players.");
        this.gameState = GameState.WAITINGOTHERS;
    }

    /**
     * Sets the Game State for the Player to Wait for Next Game
     */
    private void setNotStartedState(){
        System.out.println("+--------------------+");
        System.out.println("Currently Waiting for Other Players before Moving onto Next Round.");
        this.gameState = GameState.NOTSTARTED;
    }

    private void setPlayAgainState(){
        try {
            Thread.sleep(300);
        }catch (InterruptedException ignored){}
        System.out.println("+--------------------+");
        System.out.print("Would you like to Play Again (Y/N): ");
        gameState = GameState.PLAYAGAIN;
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
     * Prints the Players Hand to the Terminal
     * @param handValue The Numeric Value of the Hand
     * @param cards An Array of Cards (Rank then Suit formatted)
     */
    private void printPlayerHand(int handNumber, int handValue, String[] cards){
        StringBuilder handString = new StringBuilder();
        handString.append(String.format("Hand %d (%d):", handNumber, handValue));
        for(String card: cards){
            handString.append(String.format(" %s", card));
        }
        System.out.println(handString);
    }

    /**
     * Prints the Dealers Hand to the Terminal
     * @param handValue The Numeric Value of the Hand
     * @param cards An Array of Cards (Rank then Suit formatted)
     */
    private void printDealerHand(int handValue, String[] cards){
        System.out.println("+--------------------+");
        StringBuilder handString = new StringBuilder();
        handString.append(String.format("Dealer's Hand (%d):", handValue));
        for(String card: cards){
            handString.append(String.format(" %s", card));
        }
        System.out.println(handString);
    }

    /**
     * Prints out the correct prompt depending on what the game play option sent was
     */
    private void printPlayOptions(){
        switch(gamePlayOptions){
            case "HITSTAND":
                System.out.print("Hit (H) or Stand (S): ");
                break;
            case "HITSTANDDOUBLE":
                System.out.print("Hit (H), Stand (S) or Double (D): ");
                break;
            case "HITSTANDSPLIT":
                System.out.print("Hit (H), Stand (S) or Split (SP): ");
                break;
            case "HITSTANDDOUBLESPLIT":
                System.out.print("Hit (H), Stand (S), Double (D) or Split (SP): ");
                break;
        }
    }

    private void validatePlayChoice(String options, String choice){
        String[] splitOptions = options.split("-");
        boolean validOption = false;

        for(String option: splitOptions){
            if(option.equals(choice)){
                validOption = true;
            }
        }
        if(validOption){
            output.println(String.format("C-PLAYING-%s",choice));
            inputThreadSleep();
        }else{
            System.out.println("Invalid Choice!");
            printPlayOptions();
        }
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
        }
    }
}
