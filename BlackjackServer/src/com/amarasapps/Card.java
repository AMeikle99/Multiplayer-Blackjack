package com.amarasapps;

/**
 * Multiplayer Blackjack Game
 * @author Aiden Meikle
 * Github: AMeikle99
 *
 * An object that represents an individual playing card
 */

public class Card {

    private Rank rank;      //Card's Rank
    private Suit suit;      //Card's Suit

    /**
     * Constuctor for a Card Object
     *
     * @param rank The Rank for the given card
     * @param suit The Suit for the given card
     */
    public Card(Rank rank, Suit suit){
        this.rank = rank;
        this.suit = suit;
    }

    /**
     * Returns the Numeric Value of the card
     *
     * @return value The Numeric Card Value
     */
    public int value(){
        return this.rank.getNumericValue();
    }

    /**
     * Returns the Rank of The Card
     * @return rank The Cards Rank (Ace, King...)
     */
    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return String.format("%2s%s", this.rank.toString(), this.suit.toString());
    }

    public enum Rank {
        ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K");

        private String cardValue;     //Value of the Card
        private int numericValue;

        /**
         * Initialise the Rank Item with the correct Rank Value
         *
         * @param value The value to be assigned to the Rank
         */
        Rank(String value){
            this.cardValue = value;

            switch(value){  //Assigns the numerical value of each Rank
                case("A"):
                    numericValue = 1;
                    break;
                case("J"):
                case("Q"):
                case("K"):
                    numericValue = 10;
                    break;
                default:
                    numericValue = Integer.parseInt(value);
            }

        }

        /**
         * Gets the Numeric Value of the given Rank
         *
         * @return numericValue The numeric value of the Rank
         */
        public int getNumericValue() {
            return numericValue;
        }

        @Override
        public String toString() {
            return this.cardValue;
        }
    }

    public enum Suit{
        SPADES("S"), CLUBS("C"), HEARTS("H"), DIAMONDS("D");

        private String suitName;

        /**
         * Initialise the Suit Item with the correct Suit
         *
         * @param suit The value of SUit to be assigned
         */
        Suit(String suit){
            this.suitName = suit;
        }

        @Override
        public String toString() {
            return suitName;
        }
    }

}
