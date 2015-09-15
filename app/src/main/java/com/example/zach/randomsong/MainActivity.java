package com.example.zach.randomsong;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import java.util.List;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.TrackSimple;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends Activity implements PlayerNotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "13352d0cc13443af9faa6ad2da794afa";
    private static final String REDIRECT_URI = "my-first-android-app-login://callback";
    private String mySong = "init";
    private Album album;
    private String myToken;

    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;

    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();


        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                myToken = response.getAccessToken();
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized (Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);

                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.e("MainActivity", "Login Failed");
    }

    @Override
    public void onTemporaryError() {
        Log.e("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.e("MainActivity", "Playback error received: " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    public void getRandomAlbumTrack (String Token) {

        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(Token);
        SpotifyService spotify = api.getService();

        spotify.getAlbum("7paOpVZ35xmBn9ijROCG5Q", new SpotifyCallback<Album>() {
            @Override
            public void success(Album myAlbum, Response response) {
                album = myAlbum;
                Log.d("Album success", album.name);
            }

            public void failure(SpotifyError error) {
                Log.e("Album failure", error.toString());
            }
        });

    }

    public void setSongID (View view) {
        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(myToken);
        SpotifyService spotify = api.getService();

        spotify.getAlbum("7paOpVZ35xmBn9ijROCG5Q", new SpotifyCallback<Album>() {
            @Override
            public void success(Album myAlbum, Response response) {
                album = myAlbum;
                Log.d("Album success", album.name);
                getRandomSong();
            }
            
            public void failure(SpotifyError error) {
                Log.e("Album failure", error.toString());
            }
        });
    }

    public void getRandomSong () {
        Pager<TrackSimple> albumTracks = album.tracks;
        List<TrackSimple> songs = albumTracks.items;
        Random randomGen = new Random();

        int index = randomGen.nextInt(songs.size());
        TrackSimple myTrack = songs.get(index);

        mySong = myTrack.id;
        Log.d("mySong = ", mySong);
        Log.d("myTrack ID = ", myTrack.id);
        playSong(mySong);
    }

    public void playSong(String songID) {
        mPlayer.play("spotify:track:" + songID);
    }

}
