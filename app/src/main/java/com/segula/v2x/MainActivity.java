package com.segula.v2x;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.segula.v2x.databinding.ActivityMainBinding;
import com.segula.v2x.ui.V2X.V2XFragment;
import com.segula.v2x.utils.GlobalConstants;
import com.segula.v2x.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import fr.segula.tcp.TCP;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private V2XFragment v2XFragment;
    private String ipAddress;
    public TCP tcpClient;
    Button btnStatusConnected;
    TextView tvStatusConnection, tvFRLanguage, tvENLanguage;
    Switch switchLanguage;
    private GlobalConstants.Language language;
    private int alive = 0;
    private boolean network_receive = false;
    private Resources res;
    private int languageState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        com.segula.v2x.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        res = getResources();

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_v2x, R.id.nav_parking, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        View headerImageView= navigationView.getHeaderView(0);
        btnStatusConnected = headerImageView.findViewById(R.id.btnStatusConnected);
        tvStatusConnection = headerImageView.findViewById(R.id.tvStatusConnection);
        tvFRLanguage = headerImageView.findViewById(R.id.tvFRLanguage);
        tvENLanguage = headerImageView.findViewById(R.id.tvENLanguage);
        tvENLanguage.setTextSize(10);
        tvFRLanguage.setTextSize(14);
        switchLanguage = headerImageView.findViewById(R.id.switchLanguage);


        ipAddress = getWifiIpAddress(this);
        tcpClient = new TCP(true, TCP.ReadDataType.DATA_JSON_PACKET_IN_BYTES);
        connect();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        switchLanguage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(switchLanguage.isChecked()) {
                    setCurrentLanguageFromLanguageOptions(1);
                    tvENLanguage.setTextSize(14);
                    tvFRLanguage.setTextSize(10);
                }
                else {
                    setCurrentLanguageFromLanguageOptions(0);
                    tvENLanguage.setTextSize(10);
                    tvFRLanguage.setTextSize(14);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private static String getWifiIpAddress(Context context) {

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddress = 0;
        if (wifiManager != null) {
            //ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            ipAddress = wifiManager.getDhcpInfo().ipAddress;
        }

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            ipAddressString = ipAddressString.substring(0, ipAddressString.lastIndexOf(".") + 1) + "1";
        } catch (UnknownHostException ex) {
            Timber.d("Unable to get host address.");
            ipAddressString = "";
        }

        Timber.d("getWifiIpAddress:  %s", ipAddressString);
        return ipAddressString;
    }

    private synchronized void updateTcpUi() {
        if (tcpClient != null) {
            final TCP.ConnectionStatus tcpState = tcpClient.getConnectionStatus();

            Utils.runOnUi(new Runnable() {
                @Override
                public void run() {
                    if (tcpState == TCP.ConnectionStatus.CONNECTED) {
                        tvStatusConnection.setText(R.string.connected);
                        btnStatusConnected.setBackgroundResource(R.drawable.connected);
                        //enableButtonSurvey();
                    } else if ((tcpState == TCP.ConnectionStatus.DISCONNECTED) || (tcpState == TCP.ConnectionStatus.CONNECTION_ERROR)) {
                        tvStatusConnection.setText(R.string.disconnected);
                        btnStatusConnected.setBackgroundResource(R.drawable.disconnected);
                        //disableButtonSurvey();
                    } else if (tcpState == TCP.ConnectionStatus.CONNECTING) {
                        tvStatusConnection.setText(R.string.connecting);
                        btnStatusConnected.setBackgroundResource(R.drawable.connecting);
                    }
                }
            });
        }
    }

    synchronized private void connect() {
        Timber.d("connect: ");
        if (tcpClient != null) {
            tcpClient.disconnect();
            tcpClient.registerConnectionStatusListener(new TcpStatusListener());
            tcpClient.registerReadyReadListener(new ReadOnTcpListener());
            tcpClient.connect(ipAddress,
                    res.getInteger(R.integer.tcp_port));
        }
    }

    private class TcpStatusListener implements TCP.ConnectionStatusListener {
        @Override
        public void onConnectionChanged(final TCP.ConnectionStatus status) {
            Timber.d("status %s",status.getString());
            //storeTCPStateInSharedPreference();
            if (status == TCP.ConnectionStatus.DISCONNECTED) {
            }
            if (status == TCP.ConnectionStatus.CONNECTED) {
                alive = 0;
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (!network_receive) {
                            alive++;
                            Timber.d("run: alive ++ %s", alive);
                        } else {
                            alive = 0;
                        }
                        if (alive == 5) {
                            Timber.d("run: alive 10 %s", alive);
                            connect();
                            cancel();
                        }
                        network_receive = false;
                    }
                }, 0, 2000);
            }
            updateTcpUi();
        }
    }

    private class ReadOnTcpListener implements TCP.ReadyReadListener {

        @Override
        public void onReadyRead(byte[] bytes, Socket socket) {
            String tcpDataReceived = new String(bytes);

            if (!tcpDataReceived.contains("network")) {
                processTCPData(bytes);
            } else if (tcpDataReceived.contains("network")) {
                //Log.v("timer", "network true " + network_receive);
                network_receive = true;
            }
        }
    }

    private void processTCPData(byte[] bytes) {
        JSONObject json;
        //currentSegment = null;
        //currentSubSegment = null;

        try {
            json = new JSONObject(new String(bytes));

            if (json.length() != 0) {
//                if (json.has(GlobalConstants.SEGMENT_POSITION)) {
//                    //processInitialPosition(json);
//                    //commonData.setCurrentPosition(getCurrentPosition());
//                } else if (json.has(GlobalConstants.SEGMENT_OPERATOR)) {
//                    //processSurveyTypeJSON(json);
//                } else {
//                    //do nothing!
//                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateResources(Context context, String language) {
        Resources res = context.getResources();

        Configuration configuration;
        configuration = new Configuration(res.getConfiguration());

        configuration.setLocale(new Locale(language));

        res.updateConfiguration(configuration, res.getDisplayMetrics());
    }



    private void setCurrentLanguageFromLanguageOptions(int pos) {

        switch (pos) {
            case 0:
                language = GlobalConstants.Language.FRENCH;
                updateResources(getBaseContext(), "fr");
                //updateResources(homeFragment.requireContext(), "fr");

                break;

            case 1:

                language = GlobalConstants.Language.ENGLISH;
                updateResources(getBaseContext(), "en");
                //updateResources(homeFragment.requireContext(), "en");

                break;
            default:
                break;
        }
        updateTcpUi();
        //updateUILanguage();
    }
}