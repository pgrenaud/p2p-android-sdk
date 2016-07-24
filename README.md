p2p-android-sdk
===============

[![Travis](https://img.shields.io/travis/pgrenaud/p2p-android-sdk.svg)](https://travis-ci.org/pgrenaud/p2p-android-sdk)
[![Release](https://img.shields.io/github/release/pgrenaud/p2p-android-sdk.svg)](https://github.com/pgrenaud/p2p-android-sdk/releases)
[![MIT License](https://img.shields.io/badge/license-MIT-8469ad.svg)](https://tldrlegal.com/license/mit-license)

Peer-to-peer file sharing SDK for Android platform.

Services
--------

The SDK provides you services, including but not limited to:

* Files: The service manage all files, including loading, listing and serving them. You only need to provide a path and the service will handle the rest.
* Peers: The service manage all peers, including saving and reloading across activity lifecycle. You only need to add or remove peers.
* Queues: The service manage all event queues, including creating and dispatching event. You only need to send events to it.
* Workers: The service manage all workers, who are responsible to perform long lived request to obtain event from other peers. You only need to start or stop workers as needed.
* Server: The service manage the web server, including starting the server and handling all request. Everything is handle internally, no interaction are needed.

Javadoc
-------

The Javadoc is available here: https://jitpack.io/com/github/pgrenaud/p2p-android-sdk/master/javadoc/

Gradle
------

To build an application using this SDK, you need to add the `p2p-android-sdk` dependency to the list of your module dependencies.

First, add the `jitpack.io` repository:

```gradle
allprojects {
    repositories {
        jcenter()
        maven { url "https://jitpack.io" } // Add this line
    }
}
```

Then, add the `p2p-android-sdk` dependency:

```gradle
dependencies {
    compile 'com.github.pgrenaud:p2p-android-sdk:1.0.0'
}
```

Setup
-----

This SDK is built around an Android service named `PeerService`. All interaction with the SDK will be done through this service.

First, you need to start the service from your activity:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = new Intent(this, PeerService.class);
    intent.putExtra(PeerService.EXTRA_DIRECTORY_PATH, directoryPath);
    intent.putExtra(PeerService.EXTRA_PEER_NAME, peerName);
    startService(intent);
}

@Override
protected void onDestroy() {
    super.onDestroy();

    Intent intent = new Intent(this, PeerService.class);
    stopService(intent);
}
```

You need to create an `Intent` and provide two extras:

1. `directoryPath`: The directory path where the files can be loaded from
2. `peerName`: The display name of the device peer

You can also choose to not provide any of this information at this point and do it later.

Using the intent, you start the service using `startService(intent)` inside the `onCreate(...)` method.
Using a similar intent, you can call `stopService(intent)` inside the `onDestroy()` method.

Second, you need to bind to your activity to the service:

```java
private boolean bound = false;

@Override
protected void onStart() {
    super.onStart();

    Intent intent = new Intent(this, PeerService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE);
}

@Override
protected void onStop() {
    super.onStop();

    if (bound) {
        unbindService(connection);
        bound = false;
        service.setListener(null);
    }
}
```

Using an `Intent`, you bind your activity to the service using `bindService(...)` inside the `onStart()` method.
Inside the `onStop()`, you unbind the service using `unbindService(connection)` if the service was bound.

Third, you also need to provide a `ServiceConnection` instance to the `bindService(...)` method:

```java
private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        PeerServiceBinder binder = (PeerServiceBinder) iBinder;
        service = binder.getService();
        service.setListener(listener);

        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service.setListener(null);

        bound = false;
    }
};
```

Inside the `onServiceConnected(...)` method, you get the `PeerService` instance.
Inside the `onServiceDisconnected(...)` method, you unset the listener instance.
Inside both, you keep track of the bounding state using the `bound` boolean.

Fourth, you need to provide a `PeerServiceListener` instance to the `setListener(...)` method:

```java
private PeerServiceListener listener = new PeerServiceListener() {
    @Override
    public void onPeerConnection(PeerEntity peerEntity) {
    }

    @Override
    public void onPeerDisplayNameUpdate(PeerEntity peerEntity) {
    }

    @Override
    public void onPeerLocationUpdate(PeerEntity peerEntity) {
    }
};
```
Note that when you use those callbacks to perform UI update, you need to wrap your code inside a `runOnUiThread(...)` callback.

Usage
-----

Before you actually start using the SDK, you need to understand the thinking behind it.
The `PeerService` is a service provider and a data container. It is there to _provide_ services and _contain_ data.
Not the other way around. That means that you need to wait for the `PeerService` to provide you the data you need.

With that in mind, the most part of the interaction between your activity and the service should go in one of the callback showed above.
Inside those callbacks, you may read data from the service instance. Here few examples:

* `getSelfPeerEntity()`: Returns the `PeerEntity` instance that represent the local peer. Useful to access the last accessed date or the hostname.
* `getPeerRepository()`: Returns the repository of all known peers. Useful to display the list of all known peers.
* `getFileRepository()`: Returns the repository of all known files. Useful to display a list of files in the selected directory.

Outside of those callbacks, your application may also need to write data and interact with the services. Here few examples:

* `getSelfPeerEntity()`: Returns the `PeerEntity` instance that represent the local peer. Useful to update the display name or the current location.
* `getPeerRepository()`: Returns the repository of all known peers. Useful to update the display name or location of other peers.
* `getFileRepository()`: Returns the repository of all known files. Useful to load a new list of files when selecting a new directory.
* `getQueueRepository()`: Returns the repository of the outgoing event queues. Useful to broadcast an event to all known peers.
* `getPeerHive()`: Returns the worker hive. Useful to start or stop peer workers.

Also, helpers are available to assist you in specific tasks:

* `ApiEndpoints`: Provide you method to obtain all api endpoints. Useful to build an url to perform a request.
* `HttpClientWrapper`: Provide a simple HTTP client to perform HTTP client. You need to handle the threading yourself if you use this class.

Example
-------

Here's an example of an activity with everything we showed you:

```java
public class MainActivity extends AppCompatActivity {

    private PeerService service;
    private boolean bound = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            PeerServiceBinder binder = (PeerServiceBinder) iBinder;
            service = binder.getService();
            service.setListener(listener);

            service.getPeerRepository(); // TODO: Initialize your UI
            service.getSelfPeerEntity(); // TODO: Initialize your UI
            service.getFileRepository(); // TODO: Initialize your UI

            service.getPeerHive().sync(); // Start workers for known peers

            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            service.setListener(null);

            bound = false;
        }
    };

    private PeerServiceListener listener = new PeerServiceListener() {
        @Override
        public void onPeerConnection(PeerEntity peerEntity) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO: Update your UI
                }
            });
        }

        @Override
        public void onPeerDisplayNameUpdate(PeerEntity peerEntity) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO: Update your UI
                }
            });
        }

        @Override
        public void onPeerLocationUpdate(PeerEntity peerEntity) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO: Update your UI
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, PeerService.class);
        intent.putExtra(PeerService.EXTRA_DIRECTORY_PATH, serverDirectory);
        intent.putExtra(PeerService.EXTRA_PEER_NAME, serverName);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, PeerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (bound) {
            unbindService(connection);
            bound = false;
            service.setListener(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, PeerService.class);
        stopService(intent);
    }
}
```
