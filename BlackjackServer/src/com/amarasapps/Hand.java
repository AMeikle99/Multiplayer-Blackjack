package com.amarasapps;

import java.util.ArrayList;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
 * Class Which is Used to Represent a Playing Hand. It has an associated Set of Cards and Value
 */

public class Hand {
    private ArrayList<Card> cards;  //All cards which define the Hand
    private int handValue;          //Value of the Hand
    private int fullValueAceCount;  //How Many Aces that are worth 11 (Can be disctounted to 1 if Value exceeds 21)
    private boolean isBlackjack;    //Signals if the Hand is a BlackJack
    private boolean isBust;         //Signals if the Hand is Bust

    public Hand(){
        cards = new ArrayList<>();
        handValue = 0;
        fullValueAceCount = 0;
    }

    public void addCard(Card card){
        cards.add(card);
        handValue += card.value();
        if(card.getRank() == Card.Rank.ACE && fullValueAceCount == 0){
            fullValueAceCount++;
            handValue += 10;
        }
        updateHandState();
    }

    private void updateHandState(){
        if(cards.size() == 2 && handValue == 21){
            isBlackjack = true;
            return;
        }


    }

    public boolean hasBlackjack() {
        return isBlackjack;
    }
}
