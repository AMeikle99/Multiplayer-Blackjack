package com.amarasapps;


import java.util.ArrayList;

/**
 * An object to represent a playing deck, essentially an array of Cards
 *
 * @author Aiden Meikle
 */
public class Deck {

    private ArrayList<Card> deck;   //Array of cards representing the deck

    /**
     * Constructor which initialises a Playing Deck
     */
    public Deck(){
        deck = new ArrayList<>();

        for(Card.Rank rank: Card.Rank.values()){
            for(Card.Suit suit: Card.Suit.values()){
                deck.add(new Card(rank, suit));
            }
        }
    }

    /**
     * Removes the first card in the deck and returns it
     *
     * @return card The first card in the deck to be 'dealt'
     */
    public Card dealCard(){
        Card card = this.deck.get(0);
        this.deck.remove(0);

        return card;
    }

    /**
     * Returns the size of the deck
     *
     * @return size The number of cards in the deck
     */
    public int size(){
        return this.deck.size();
    }


}
