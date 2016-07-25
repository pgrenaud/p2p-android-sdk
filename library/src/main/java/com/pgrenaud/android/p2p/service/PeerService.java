package com.pgrenaud.android.p2p.service;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.pgrenaud.android.p2p.R;
import com.pgrenaud.android.p2p.entity.PeerEntity;
import com.pgrenaud.android.p2p.web.RequestHandler;
import com.pgrenaud.android.p2p.peering.PeerHive;
import com.pgrenaud.android.p2p.repository.FileRepository;
import com.pgrenaud.android.p2p.repository.PeerRepository;
import com.pgrenaud.android.p2p.repository.QueueRepository;
import com.pgrenaud.android.p2p.web.RoutableWebServer;

public class PeerService extends Service {

    public static final String EXTRA_DIRECTORY_PATH = "com.pgrenaud.android.p2p.service.EXTRA_DIRECTORY_PATH";
    public static final String EXTRA_PEER_NAME = "com.pgrenaud.android.p2p.service.EXTRA_PEER_NAME";
    public static final String EXTRA_SERVER_PORT = "com.pgrenaud.android.p2p.service.EXTRA_SERVER_PORT";

    public static final int DEFAULT_SERVER_PORT = 8099;

    private final IBinder binder = new PeerServiceBinder();
    private final QueueRepository queueRepository = new QueueRepository();
    private final FileRepository fileRepository = new FileRepository();
    private final PeerRepository peerRepository = new PeerRepository(this);
    private final PeerHive peerHive = new PeerHive(this, peerRepository);

    private PeerServiceListener listener;
    private PeerEntity selfPeer;
    private RoutableWebServer server;

    private boolean running = false;

    @Override
    public void onCreate() {
        Log.d("PeerService", "Creating PeerService");

        // Loading peers from persistent storage
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getService());
        String json = prefs.getString(getString(R.string.pref_json_peer_list_key), null);

        if (json != null) {
            peerRepository.addAll(PeerRepository.decode(json));
        }

        Log.d("PeerService", "PeerService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Skip initialization if already running
        if (!running) {
            Log.d("PeerService", "Starting PeerService");

            String path = intent.getStringExtra(EXTRA_DIRECTORY_PATH);
            if (path != null) {
                fileRepository.addAll(path);
            }

            String peerName = intent.getStringExtra(EXTRA_PEER_NAME);
            if (path == null) {
                peerName = getString(R.string.pref_peer_name_default);
            }

            int serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, DEFAULT_SERVER_PORT);

            // TODO: Move to a proper place
            WifiManager wifiMgr = (WifiManager) getService().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            String serverAddress = Formatter.formatIpAddress(ip);

            selfPeer = new PeerEntity(peerName, serverAddress, serverPort);
            Log.d("PeerService", "Self peer is " + selfPeer);

            RequestHandler requestHandler = new RequestHandler(queueRepository, fileRepository, peerRepository, peerHive);

            server = new RoutableWebServer(serverPort, requestHandler);

            try {
                server.start();
            } catch (IOException e) {
                Log.e("PeerService", "Failed to start RoutableWebServer", e); // TODO: Send error to client
            }

            running = true;

            Log.d("PeerService", "PeerService started");
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("PeerService", "PeerService bound");

        if (!running) {
            throw new IllegalStateException("Service must be started before it can be bonded.");
        }

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("PeerService", "PeerService unbound");

        return false;
    }

    @Override
    public void onDestroy() {
        if (running) {
            Log.d("PeerService", "Destroying PeerService");

            peerHive.stop();
            server.stop();

            // Saving peers to persistent storage
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getService());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.pref_json_peer_list_key), peerRepository.encode());
            editor.apply();

            Log.d("PeerService", "PeerService destroyed");
        }
    }

    public void registerNfcCallback(Activity activity) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            // Register callback
            nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    return new NdefMessage(
                        new NdefRecord[] {
                            NdefRecord.createMime(
                                "application/vnd.com.pgrenaud.android.p2p.beam",
                                getPeerRepository().encode().getBytes()
                            )
                        }
                    );
                }
            }, activity);

            Log.d("PeerService", "NdefPushMessageCallback registered");
        }
    }

    public void unregisterNfcCallback(Activity activity) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            // Unregister callback
            nfcAdapter.setNdefPushMessageCallback(null, activity);

            Log.d("PeerService", "NdefPushMessageCallback unregistered");
        }
    }

    /**
     * Merge all the known peers by an other peer sent over Android Beam (NFC).
     * You must call {@link PeerHive#sync()} and update your UI yourself after calling this method.
     *
     * @param intent Intent sent by Android Beam containing a NDEF payload.
     */
    public void handleNfcIntent(@Nullable Intent intent) {
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            try {
                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                // only one message sent during the beam
                NdefMessage msg = (NdefMessage) rawMsgs[0];

                // record 0 contains the MIME type, record 1 is the AAR, if present
                String json = new String(msg.getRecords()[0].getPayload());

                getPeerRepository().mergeAll(PeerRepository.decode(json));
            } catch (JsonSyntaxException | IndexOutOfBoundsException e) {
                Log.e("PeerService", "handleNfcIntent", e);
            }
        }
    }

    public PeerServiceListener getListener() {
        return listener;
    }

    public void setListener(PeerServiceListener listener) {
        this.listener = listener;
    }

    public QueueRepository getQueueRepository() {
        return queueRepository;
    }

    public FileRepository getFileRepository() {
        return fileRepository;
    }

    public PeerRepository getPeerRepository() {
        return peerRepository;
    }

    public PeerHive getPeerHive() {
        return peerHive;
    }

    public PeerEntity getSelfPeerEntity() {
        return selfPeer;
    }

    public Service getService() {
        return this;
    }

    public class PeerServiceBinder extends Binder {
        public PeerService getService() {
            return PeerService.this;
        }
    }

    public interface PeerServiceListener {
        void onPeerConnection(PeerEntity peerEntity);
        void onPeerDisplayNameUpdate(PeerEntity peerEntity);
        void onPeerLocationUpdate(PeerEntity peerEntity);
    }
}
