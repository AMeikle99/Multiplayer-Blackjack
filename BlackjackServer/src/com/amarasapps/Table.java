package com.amarasapps;


import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
 * Class which stores all the data regarding the execution of gameplay, runs as a thread controlling gameplay
 * Also spins of multiple threads for each player
 */

public class Table implements Runnable {

    private ArrayList<Player> players;          //List of Players in the Game
    private CardShoe cardShoe;                  //The CardShoe holding all the Decks for the Table
    private double minimumBet;                  //Minimum Bet That Can Be Placed
    private int decksUsed;                      //Decks Kept in the Shoe
    private int cardsBeforeShuffle;             //Card Limit Before Shoe is Reshuffled
    private BJHand dealersHand = new BJHand();  //The hand that represents the dealers hand

    private CountDownLatch betsPlacedLatch;      //Count of Players Who've Place their Bets
    private CountDownLatch playAgainLatch;      //Count of Players who are being asked to play again
    private CountDownLatch insuranceBetLatch;   //Count of Players who have responded to the offer of an Insurance Bet


    /**
     * Constructor to initialise the Playing Table where all the program logic is executed
     * @param minBet    The minimum bet that a player can place
     * @param decksUsed The number of decks to be stored in a card show
     * @param cardsBeforeShuffle The max number of cards left in Shoe before it is re-shuffled
     */
    public Table(double minBet, int decksUsed, int cardsBeforeShuffle){
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
            System.out.println("Playing a New Game");
            System.out.println("Player Count: " + playerCount());
            playGame();
        }while(playerCount() > 0);
    }

    /**
     * Execute a Single Round of the Game
     */
    private void playGame(){
        betsPlacedLatch = new CountDownLatch(players.size());

        for(Player player: players){
            player.setGetBetState();
        }
        try{
            betsPlacedLatch.await();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("Dealing Cards");
        dealInitialCards();
        if(getDealerUpCard().isAce()){
            handleInsuranceBets();
            informInsuranceOutcome();
            if(dealersHand.hasBlackjack()){
                resetTable();
                return;
            }
        }
        for (Player player : players) {
            player.setPlayGameState();
            player.handlePlayStage();
            try {
                player.waitPlayHandLatch();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        dealersTurn();
        resetTable();
    }

    /**
     * For each player, ask them to place an insurance bet
     */
    private void handleInsuranceBets(){
        insuranceBetLatch = new CountDownLatch(playerCount());
        for(Player player: players){
            player.handlePlayStage();
        }
        try{
            insuranceBetLatch.await();
        }catch(InterruptedException ignored){}
    }

    /**
     * Tell each player the outcome of the Insurance, payout or not
     */
    private void informInsuranceOutcome(){
        for(Player player: players){
            player.informInsuranceOutcome();
        }
    }

    /**
     * Resets the State of the Table for a new Game
     */
    private void resetTable(){
        System.out.println("Table Setup");
        if(cardShoe.cardsLeft() <= cardsBeforeShuffle){
            this.cardShoe = new CardShoe(decksUsed);
        }

        ArrayList<Player> inelligiblePlayers = getInelligiblePlayers();
        ArrayList<Player> elligiblePlayers = getElligiblePlayers();
        playAgainLatch = new CountDownLatch(elligiblePlayers.size());
        System.out.printf("Inelligible Count: %d\tElligible Count: %d\n", inelligiblePlayers.size(), elligiblePlayers.size());

        for(Player player: inelligiblePlayers){
            player.informLowBalance();
            player.setGameOverState();
        }

        for(Player player: elligiblePlayers){
            player.setPlayAgainState();
        }
        if(elligiblePlayers.size() > 0){
            try{
                playAgainLatch.await();
            }catch (InterruptedException ignored){}
            for(Player player: elligiblePlayers){
                if(player.hasChosenToQuit()){
                    inelligiblePlayers.add(player);
                }
            }

        }

        for(Player player: inelligiblePlayers){
            removePlayer(player);
        }

        dealersHand.clear();

        for(Player player: players){
            player.resetPlayer();
        }
    }

    /**
     * Deals every player and the dealer their initial 2 cards
     */
    private void dealInitialCards(){
        for(int i = 0; i < 2; i++){
            for(Player p: players){
                p.getCurrentHand().addCard(cardShoe.dealCard());
            }
            dealersHand.addCard(cardShoe.dealCard());
        }
    }

    private void dealersTurn(){
        while(!dealersHand.hasBlackjack() && !dealersHand.isBust() && dealersHand.handValue() < 17){
            dealersHand.addCard(cardShoe.dealCard());
        }

        for(Player player: players){
            player.sendDealerHandState();
            player.processPayout();
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
    private void removePlayer(Player player){
        players.remove(player);
    }

    //**Latch Countdown**//

    /**
     * Decrements the Bet Latch by 1
     */
    public void countDownBetLatch(){
        betsPlacedLatch.countDown();
    }

    /**
     * Decrements the Play Again Latch by 1
     */
    public void countDownPlayAgainLatch(){
        playAgainLatch.countDown();
    }

    /**
     * Decrements the Insurance Bet Latch by 1
     */
    public void countDownInsuranceBetLatch(){
        insuranceBetLatch.countDown();
    }

    //**Getters**//

    /**
     * Gets all the hand which represents the dealer
     * @return The hand that represents the dealer
     */
    public BJHand getDealersHand() {
        return dealersHand;
    }

    /**
     * Gets the Card from the Dealer which the Player can "see"
     * @return The first card in the dealer's hand
     */
    public Card getDealerUpCard(){
        return dealersHand.getCard(0);
    }

    /**
     * Gets the value of the dealer's hand which is currently visible to the player
     * @return  Value of the dealer's first card in their hand
     */
    public int getDealerVisibleValue(){
        if(getDealerUpCard().getRank() == Card.Rank.ACE){
            return getDealerUpCard().value() + 10;
        }else{
            return getDealerUpCard().value();
        }
    }

    /**
     * Mimics the action of dealing a card and removing it from the card shoe
     * @return The next card stored in the card shoe
     */
    public Card dealCard(){
        return cardShoe.dealCard();
    }

    /**
     * Gets a list of all the player's who are still elligible for another round
     * @return  A list of all elligible players
     */
    private ArrayList<Player> getElligiblePlayers(){
        return players.stream().filter(Player::isStillEligible).collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * Gets a list of all the player's who are not elligible for another round
     * @return  A list of all inelligible players
     */
    private ArrayList<Player> getInelligiblePlayers(){
        return players.stream().filter(Player::isNotStillElligible).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns the minimum bet allowed for the Table
     * @return minimum bet allowed to play
     */
    public double getMinimumBet(){
        return  this.minimumBet;
    }

    /**
     * Returns the Number of Players Still in the Game
     * @return Number of Players Left in Game
     */
    public int playerCount(){
        return players.size();
    }

    /**
     * Checks if the player is not the last player in the list of players at the table, i.e if there are players
     * after who would need to take their turn
     * @param p A Player
     * @return Whether the player is last in the list of players at the table
     */
    public boolean isNotLastPlayer(Player p){
        return players.indexOf(p) != players.size()-1;
    }
}
