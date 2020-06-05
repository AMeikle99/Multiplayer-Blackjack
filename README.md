# Multiplayer-Blackjack
My Version of Blackjack developed to allow for Multiplayer Access, either on the same device or others on the same LAN
The Game is Played in the Command Line and allows for 1 or more player to connect to the Blackjack Server as long as it is connected to the same Local Network as the Clients.

## Gameplay

Gameplay follows a traditional Casino Setup by offering the players all the standard options:
  - Splitting matching cards (No Limit)
  - Doubling Down (Hand Value of 9, 10 or 11 only!)
  - Insurance (If Dealer Shows Ace Card)
  - Hitting
  - Standing  

**Payout**:
  - BlackJack (3:2)
  - Win Against Dealer (1:1)
  - Lose Against Dealer or Bust (No Payout)
  - Match Dealer (Keep Bet)
  - Insurance & Dealer has BlackJack (1:1 on Insurance Bet, No Money Lost)

**Insurance**  
If a Dealer shows an Ace, all players are offered Insurance against a Blackjack. Insurance Bets equate to half the money bet on your hand and it is optional to take part.  
If the Dealer does have BlackJack then the original bet is lost but Insurance Bet is paid as noted above. The round will be over after this.  
If the Dealer does not have a BlackJack then any Insurance Bet is lost. The round will continue as normal.

**Double Down**  
If your hand value is worth 9, 10 or 11 you are able to double down. This means that you will double the bet on your hand but only get 1 card. Play continues to the next player immediately.

**Splitting Cards**  
If you have 2 cards of the same Rank (say 2 7's or 2 Aces) then you can split them to make 2 hands, one card from each. A new card is then added to each of the new hands and play proceeds as normal for each hand. In order to split you must have enough available balance to cover the original hand's bet amount.  
There is no limit on the number of times you can split in one round.

## Starting the Server
In a terminal execute the following command to start the server:  
```sh
java -jar BlackjackServer.jar [Options]
```
### Options
Below are the possible options which can be passed into the BlackjackServer.jar program:
```sh 
--PORT <Port Number> - Specifies the Port Number to run the Server on
--PLAYERS <Players Per Table> - The number of players that need to join before the game starts
--MONEY <Starting Money> - The amount of money a new player starts with
--BET <Minimum Bet> - The minimum bet allowed during play
--DECKS <Number of Decks Used> - The number of decks of cards to be used during play
```
#### Defaults:
  - PORT: 8080
  - PLAYERS: 2
  - MONEY: 500
  - BET: 100
  - DECKS: 8
  
## Starting a Client:
In a terminal execute the following command to start the server:
```sh
java -jar BlackjackClient.jar [Options]
```
### Options
Below are the possible options which can be passed into the BlackjackClient.jar program:
```sh
--PORT <Server Port Number> - Specifies the Port Number the Server is running on
--ADDRESS <IPv4 Server Address> - The IP address the Server is Running on (shown in Server Terminal)
```
#### Defaults:
  - PORT: 8080
  - ADDRESS 127.0.0.1 (Localhost)
