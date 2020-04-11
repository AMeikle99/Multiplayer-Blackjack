package com.amarasapps;


import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
 * Class which stores all the data regarding the execution of gameplay, runs as a thread controlling gameplay
 * Also spins of multiple threads for each player
 */

public class Table implements Runnable {

    private ArrayList<Player> players;      //List of Players in the Game
    private CardShoe cardShoe;              //The CardShoe holding all the Decks for the Table
    private int minimumBet;                 //Minimum Bet That Can Be Placed
    private int decksUsed;                  //Decks Kept in the Shoe
    private int cardsBeforeShuffle;         //Card Limit Before Shoe is Reshuffled

    private CountDownLatch betsPlacedLatch;      //Count of Players Who've Place their Bets


    /**
     * Constructor to initialise the Playing Table where all the program logic is executed
     * @param minBet    The minimum bet that a player can place
     * @param decksUsed The number of decks to be stored in a card show
     * @param cardsBeforeShuffle The max number of cards left in Shoe before it is re-shuffled
     */
    public Table(int minBet, int decksUsed, int cardsBeforeShuffle){
        this.minimumBet = minBet;
        this.decksUsed = decksUsed;
        this.cardsBeforeShuffle = cardsBeforeShuffle;
        players = new ArrayList<>();
    }

    /**
     * Executed when this Class is Run as a new Thread
     */
    @Override
    public void run() {
        System.out.println("Table Thread has Started");
        cardShoe = new CardShoe(decksUsed);

        do{
            playGame();
        }while(playerCount() > 0);
    }

    /**
     * Execute a Single Round of the Game
     */
    private void playGame(){
        setup();
        for(Player player: players){
            player.setGetBetState();
        }
        try{
            betsPlacedLatch.await();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        for(Player player: players){
            player.setPlayGameState();
            try{
                player.getPlayHandLatch().await();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        while(playerCount() > 0){
            players.get(0).setGameOverState();
            players.remove(0);
        }
    }

    /**
     * Resets the State of the Table for a new Game
     */
    private void setup(){
        System.out.println("Table Setup");
        if(cardShoe.cardsLeft() <= cardsBeforeShuffle){
            this.cardShoe = new CardShoe(decksUsed);
        }
        //TODO: Check all players are still connected
        //TODO: Check Players are still Eligible

        betsPlacedLatch = new CountDownLatch(players.size());
        for(Player player: players){
            player.resetPlayer();
        }
    }
    /**
     * Adds a new Player to the Playing Table
     * @param player    The new Player to be added to the Table
     */
    public void addPlayer(Player player){
        players.add(player);
    }

    /**
     * Remove the Specified Player from the Playing Table
     * @param player    The player to be removed from the Table
     */
    public void removePlayer(Player player){
        players.remove(player);
    }

    /**
     * Returns the Number of Players Still in the Game
     * @return Number of Players Left in Game
     */
    public int playerCount(){
        return players.size();
    }

    public void countDownBetLatch(){
        this.betsPlacedLatch.countDown();
    }
}
