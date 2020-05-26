package com.amarasapps;

import java.util.ArrayList;

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

    public BJHand(){
        cards = new ArrayList<>();
    }

    public void addCard(Card card){
        cards.add(card);

        if(card.getRank() == Card.Rank.ACE && canPromoteAce()){
            hasFullValueAce = true;
        }

        if(isBust() && hasFullValueAce){
            hasFullValueAce = false;
        }
    }

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

    public boolean hasBlackjack() {
        return cards.size() == 2 && handValue() == 21;
    }

    public boolean isBust() {
        return handValue() > 21;
    }

    private boolean canPromoteAce(){
        return !hasFullValueAce && handValue() < 12;
    }

    public Card getCard(int i){
        return cards.get(i);
    }

    public int size(){
        return cards.size();
    }
}
