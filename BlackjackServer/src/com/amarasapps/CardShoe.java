package com.amarasapps;


import java.util.ArrayList;

/**
 * The object which represents the card shoe, holds 1 or more decks used in gameplay
 *
 * @author Aiden Meikle
 */
public class CardShoe {

    private ArrayList<Card> cardShoe;       //Array of cards from 1 or more Deck ready to be dealt to players

    /**
     * Default Constructor to Initialise Card Array
     */
    public CardShoe(){
        cardShoe = new ArrayList<>();
    }

    /**
     * Constructor to Initialise Card Array with specified number of decks
     *
     * @param deckCount The number of decks to add to the card shoe
     */
    public CardShoe(int deckCount){
        this();
        for(int i = 0; i<deckCount; i++){
            addDeck(new Deck());
        }
    }

    /**
     * Adds a new deck to the Card Shoe
     *
     * @param deck Deck Object storing an Array of Cards
     */
    public void addDeck(Deck deck){
        while(deck.size() > 0){
            this.cardShoe.add(deck.dealCard());
        }
    }

    /**
     * Gets the first card in the CardShoe and returns it
     *
     * @return dealtCard The first card in the Array
     */
    public Card dealCard(){
        Card dealtCard = this.cardShoe.get(0);
        this.cardShoe.remove(0);

        return dealtCard;
    }

    /**
     * Returns how many cards are left in the shoe
     *
     * @return size The number of cards left in card array
     */
    public int cardsLeft(){
        return this.cardShoe.size();
    }




}
