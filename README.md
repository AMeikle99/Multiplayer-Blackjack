# Multiplayer-Blackjack
My Version of Blackjack developed to allow for Multiplayer Access, either on the same device or others on the same LAN
The Game is Played in the Command Line and allows for 1 or more player to connect to the Blackjack Server as long as it is connected to the same Local Network as the Clients.

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
