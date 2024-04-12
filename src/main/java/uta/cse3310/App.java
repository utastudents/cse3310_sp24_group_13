
// This is example code provided to CSE3310 Fall 2022
// You are free to use as is, or changed, any of the code provided

// Please comply with the licensing requirements for the
// open source packages being used.

// This code is based upon, and derived from the this repository
//            https:/thub.com/TooTallNate/Java-WebSocket/tree/master/src/main/example

// http server include is a GPL licensed package from
//            http://www.freeutils.net/source/jlhttp/

/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package uta.cse3310;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.time.Instant;
import java.time.Duration;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uta.cse3310.WordBankGame;


public class App extends WebSocketServer
{

  // All games currently underway on this server are stored in
  // the vector activeGames
  private Vector<GameSession> activeGames = new Vector<GameSession>();

  private int gameId = 1;

  private int connectionId = 0;

  private Instant startTime;

  private Statistics stats;

  public App(int port)
  {
    super(new InetSocketAddress(port));
  }

  public App(InetSocketAddress address)
  {
    super(address);
  }

  public App(int port, Draft_6455 draft)
  {
    super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake)
  {
    connectionId++;
    System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected");

    ServerEvent E = new ServerEvent();
    GameSession G = null;
    int gameType; 

    // match found
    for(GameSession i : activeGames)
    {
      G = i;
      System.out.println("found a match");
      //System.out.println("" + G.player);
      //change based off the game type
      if(G.players[0] == uta.cse3310.PlayerType.Player1)
      {
        E.YouAre = PlayerType.Player2;
        G.addPlayer(E.YouAre);
        System.out.println(G.players[1]);
        break;
      }

      else if(G.players[1] == uta.cse3310.PlayerType.Player2)
      {
        E.YouAre = PlayerType.Player3;
        G.addPlayer(E.YouAre);
        break;
      }

      else if(G.players[2] == uta.cse3310.PlayerType.Player3)
      {
        E.YouAre = PlayerType.Player4;
        G.addPlayer(E.YouAre);
        break;
      }

      else
      {
        System.out.println("Error: too many players are trying to join");
        return;
      }
    }

      // create new game
      if(G == null)
      {
        G = new GameSession(stats);
        G.gameId = gameId;
        gameId++;

        // add first player
        E.YouAre = PlayerType.Player1;
        G.addPlayer(E.YouAre);
        activeGames.add(G);
        System.out.println("\ncreating a new game");
        System.out.println(G.players[0] + "\n");
      }

      // allows the websocket to give us the Game when a message arrives..
      // it stores a pointer to G, and will give that pointer back to us
      // when we ask for it
      conn.setAttachment(G);
      E.gameId = G.gameId;

      Gson gson = new Gson();

      // note only send to the single connection
      String jsonString = gson.toJson(E);
      broadcast(jsonString);

      // needs to be updated to handle different player amounts
      if(E.YouAre == PlayerType.Player2)
      {
        G.startGame();
      }

      System.out.println(""+ gameId);
      String boardJson = gson.toJson(G.board);
      //System.out.println(boardJson);
      conn.send(boardJson);
      //System.out.println("> " + Duration.between(startTime, Instant.now()).toMillis() + " " + connectionId + " " + escape(jsonString));
      // Update the running time
      stats.setRunningTime(Duration.between(startTime, Instant.now()).toSeconds());
      // The state of the game has changed, so lets send it to everyone
      jsonString = gson.toJson(G);
      //System.out.println("< " + Duration.between(startTime, Instant.now()).toMillis() + " " + "*" + " " + escape(jsonString));
      broadcast(jsonString);

  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote)
  {
    System.out.println(conn + " has closed");
    // Retrieve the game tied to the websocket connection
    GameSession G = conn.getAttachment();
    G = null;
  }

  @Override
  public void onMessage(WebSocket conn, String message)
  {
    //System.out.println("< " + Duration.between(startTime, Instant.now()).toMillis() + " " + "-" + " " + escape(message));

    // Get our Game Object
    GameSession G = conn.getAttachment();

    // Bring in the data from the webpage
    // A UserEvent is all that is allowed at this point
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    UserEvent U = gson.fromJson(message, UserEvent.class);
    
    // print message to see what's being passed from Index.html
    System.out.println(message);

    // selectCharacter action from Index.html
    if("selectCharacter".equals(U.getAction()))
    {
      int row = U.getRow();
      int column = U.getColumn();
      // convert index message from Index.html into an int (easier to deal with than enum)
      String idxParse = message.substring(message.indexOf("\"idx\":") + 6, message.indexOf("}"));
      int typeInt = Integer.parseInt(idxParse.trim());

      //System.out.println("\n Row: " + row + " Column: " + column + " Type: " + typeInt + "\n");

      // call charSelected function in GameSession.java
      boolean character = G.charSelected(row * 50 + column, typeInt);

      // broadcast gameOver function to Index.html
      if(!character)
      {
        U.setAction("gameOver");
        G.update(U);
        String jsonString;
        jsonString = gson.toJson(G);
        broadcast(jsonString);
      }

      // find word positions
      List<Integer> wordPositions = G.wordPositions;

      if(wordPositions != null)
      {
        sendHighlightPositions(conn, wordPositions);
      }
    }

    // Update the running time
    stats.setRunningTime(Duration.between(startTime, Instant.now()).toSeconds());

    // update the game's status
    G.update(U);

    // send out the game state every time
    // to everyone
    String jsonString;
    jsonString = gson.toJson(G);

    System.out.println("> " + Duration.between(startTime, Instant.now()).toMillis() + " " + "*" + " " + escape(jsonString));
    broadcast(jsonString);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message)
  {
    System.out.println(conn + ": " + message);
  }

  @Override
  public void onError(WebSocket conn, Exception ex)
  {
    ex.printStackTrace();
    if (conn != null)
    {
      // some errors like port binding failed may not be assignable to a specific
      // websocket
    }
  }

  @Override
  public void onStart()
  {
    setConnectionLostTimeout(0);
    stats = new Statistics();
    startTime = Instant.now();
  }

  private String escape(String S)
  {
    // turns " into \"
    String retval = new String();
    // this routine is very slow.
    // but it is not called very often
    for (int i = 0; i < S.length(); i++)
    {
      Character ch = S.charAt(i);
      if (ch == '\"')
      {
        retval = retval + '\\';
      }
      retval = retval + ch;
    }
    return retval;
  }

  // method to send positions to index.html
  public void sendHighlightPositions(WebSocket conn, List<Integer> positions)
  {
    Gson gson = new Gson();
    Map<String, Object> message = new HashMap<>();
    message.put("action", "highlightWords");
    message.put("positions", positions);
    String jsonString = gson.toJson(message);
    System.out.println("\n" + jsonString + "\n");
    conn.send(jsonString);
  }

  public static void main(String[] args)
  {
    // Set up the http server
    int port = 9013;
    HttpServer H = new HttpServer(port, "./html");
    H.start();
    System.out.println("http Server started on port: " + port);

    // create and start the websocket server

    port = 9113;
    App A = new App(port);
    A.setReuseAddr(true);
    A.start();
    System.out.println("websocket Server started on port: " + port);

  }
}