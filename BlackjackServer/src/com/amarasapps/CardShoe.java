package com.amarasapps;


import java.util.ArrayList;
import java.util.Collections;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
 * The object which represents the card shoe, holds 1 or more decks used in gameplay
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
            Deck newDeck = new Deck();
            newDeck.shuffleDeck();
            addDeck(newDeck);
        }
        for(int i = 0; i < 3; i++) this.shuffleCardShoe();
    }

    /**
     * Adds a new deck to the Card Shoe
     *
     * @param deck Deck Object storing an Array of Cards
     */
    private void addDeck(Deck deck){
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

    /**
     * Shuffles the order of all cards in the CardShoe
     */
    private void shuffleCardShoe(){
        Collections.shuffle(this.cardShoe);
    }



}
