package com.amarasapps;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
 * A class which is used to to represent a Playing Hand in the game of BlackJack as used by a player.
 */
public class BJHand {

    private ArrayList<Card> cards;  //All cards which define the Hand
    private boolean hasFullValueAce;  //Tracks if the hand has a full value Ace
    private double handBet;                //The amount that a player has bet on this hand
    private boolean isDoubledDown;          //Tracks if the player has doubled down on this hand
    private boolean hasInsurance;

    /**
     * Constructor to initialise an empty playing hand
     */
    public BJHand(){
        cards = new ArrayList<>();
        hasFullValueAce = false;
        handBet = 0;
        isDoubledDown = false;
        hasInsurance = false;
    }

    /**
     * Adds a new card to the playing hand, handles appropriately if the card is an ACE
     * @param card
     */
    public void addCard(Card card){
        cards.add(card);

        if(card.getRank() == Card.Rank.ACE && canPromoteAce()){
            hasFullValueAce = true;
        }

        if(isBust() && hasFullValueAce){
            hasFullValueAce = false;
        }
    }

    /**
     * Returns the value of the hand, handles any cases where an ACE may be worth 11 not 1
     * @return The value of the player's hand
     */
    public int handValue(){
        int value = 0;
        for(Card card: cards){
            value += card.value();
        }

        if(hasFullValueAce){
            value += 10;
        }
        return value;
    }

    /**
     * Calculates whether the player has a BlackJack
     * @return True if the player has a blackjack, false otherwise
     */
    public boolean hasBlackjack() {
        return cards.size() == 2 && handValue() == 21;
    }

    /**
     * Calculates if the player is bust
     * @return True if the player's hand exceeds 21, false otherwise
     */
    public boolean isBust() {
        return handValue() > 21;
    }

    /**
     * Determines if an ACE added to the hand should be worth 11 or 1
     * @return True if the ACE can be treated as 11, false otherwise
     */
    private boolean canPromoteAce(){
        return !hasFullValueAce && handValue() < 12;
    }

    /**
     * Gets the Card in the player's hand at the specified index
     * @param index The numerical position in the hand of the card to return
     * @return The Card at the position index
     */
    public Card getCard(int index){
        return cards.get(index);
    }

    /**
     * Checks if the player is able to double down on their bet
     * @return True if the hand is eligible to Double Down
     */
    public boolean canDouble(double playerBalance){
        return handValue() >= 9 && handValue() <= 11 && size() == 2 && playerBalance >= 2 * handBet;
    }

    public boolean canSplit(){
        return size() == 2 && getCard(0).getRank() == getCard(1).getRank();
    }

    /**
     * Returns how many cards are in the hand
     * @return The number of cards held by the player
     */
    public int size(){
        return cards.size();
    }

    /**
     * Returns the amount the player has bet on this hand
     * @return The amount bet on the hand
     */
    public double getHandBet() {
        return handBet;
    }

    /**
     * Sets the amount to be bet on this hand
     * @param handBet The amount to be bet on the hand
     */
    public void setHandBet(double handBet) {
        this.handBet = handBet;
    }

    /**
     * Sets the Hand doubledDown to be true
     */
    public void setDoubledDown() {
        isDoubledDown = true;
        handBet *= 2;
    }

    /**
     * Returns whether this hand has been doubled down or not
     * @return True if the Hand has been doubled down, false otherwise
     */
    public boolean isDoubledDown() {
        return isDoubledDown;
    }

    /**
     * Removes a card at the specified index from the hand
     * @param i Index of card in hand array
     * @return The card specified by index i
     */
    public Card removeCard(int i){
        Card card = getCard(i);
        cards.remove(i);
        return card;
    }

    /**
     * Empties the Players Playing Hand and resets the FullRank Ace boolean to false
     */
    public void clear(){
        hasFullValueAce = false;
        isDoubledDown = false;
        hasInsurance = false;
        cards.clear();
    }

}
