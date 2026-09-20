package me.lordspigot.locationpeek;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MockLocationService extends Service {

    public static final String ACTION_START =
            "me.lordspigot.locationpeek.START";

    public static final String ACTION_STOP =
            "me.lordspigot.locationpeek.STOP";

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";

    private static final String CHANNEL_ID = "mock_location";
    private static final int NOTIFICATION_ID = 4201;
    private static final String TAG = "LocationPeek";

    private LocationManager locationManager;
    private FusedLocationProviderClient fusedClient;

    private ScheduledExecutorService executor;

    private double latitude;
    private double longitude;

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        fusedClient =
                LocationServices.getFusedLocationProviderClient(this);

        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null &&
                ACTION_STOP.equals(intent.getAction())) {

            stopMocking();
            stopForeground(true);
            stopSelf();

            return START_NOT_STICKY;
        }

        if (intent != null &&
                ACTION_START.equals(intent.getAction())) {

            latitude = intent.getDoubleExtra(
                    EXTRA_LAT,
                    47.0707
            );

            longitude = intent.getDoubleExtra(
                    EXTRA_LON,
                    15.4395
            );

            startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
            );

            startMocking();
        }

        return START_STICKY;
    }

    private void startMocking() {

        stopExecutorOnly();

        /*
         * 1. Klassische Android LocationManager Provider
         */
        try {

            prepareProvider(
                    LocationManager.GPS_PROVIDER,
                    Criteria.ACCURACY_FINE
            );

            prepareProvider(
                    LocationManager.NETWORK_PROVIDER,
                    Criteria.ACCURACY_COARSE
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "LocationManager Mock konnte nicht gestartet werden",
                    e
            );
        }

        /*
         * 2. Google Fused Location Provider
         *
         * Wichtig:
         * setMockMode(true) sorgt dafür, dass Googles FLP
         * nur noch unsere Mock-Position verwendet.
         */
        try {

            fusedClient
                    .setMockMode(true)
                    .addOnSuccessListener(unused -> {

                        Log.i(TAG, "Fused Mock Mode aktiviert");

                        startLocationLoop();

                    })
                    .addOnFailureListener(e -> {

                        Log.e(
                                TAG,
                                "Fused Mock Mode fehlgeschlagen",
                                e
                        );

                        /*
                         * LocationManager trotzdem weiter benutzen.
                         */
                        startLocationLoop();
                    });

        } catch (SecurityException e) {

            Log.e(
                    TAG,
                    "Keine Mock-Location-Berechtigung",
                    e
            );

            stopForeground(true);
            stopSelf();
        }
    }

    private void startLocationLoop() {

        if (executor != null) {
            return;
        }

        executor =
                Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {

            /*
             * Klassische Provider
             */
            try {

                pushLocationManager(
                        LocationManager.GPS_PROVIDER,
                        3f
                );

                pushLocationManager(
                        LocationManager.NETWORK_PROVIDER,
                        8f
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "LocationManager Update fehlgeschlagen",
                        e
                );
            }

            /*
             * Google Fused Location Provider
             */
            try {

                Location fusedLocation =
                        buildLocation(
                                "fused",
                                2.5f
                        );

                fusedClient
                        .setMockLocation(fusedLocation)
                        .addOnFailureListener(e ->
                                Log.e(
                                        TAG,
                                        "Fused Location Update fehlgeschlagen",
                                        e
                                )
                        );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Fused Location Fehler",
                        e
                );
            }

        }, 0, 750, TimeUnit.MILLISECONDS);
    }

    private void prepareProvider(
            String provider,
            int accuracy
    ) {

        try {

            locationManager.addTestProvider(
                    provider,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    Criteria.POWER_LOW,
                    accuracy
            );

        } catch (IllegalArgumentException ignored) {
        }

        locationManager.setTestProviderEnabled(
                provider,
                true
        );
    }

    private void pushLocationManager(
            String provider,
            float accuracy
    ) {

        Location location =
                buildLocation(
                        provider,
                        accuracy
                );

        locationManager.setTestProviderLocation(
                provider,
                location
        );
    }

    private Location buildLocation(
            String provider,
            float accuracy
    ) {

        Location location =
                new Location(provider);

        location.setLatitude(latitude);
        location.setLongitude(longitude);

        location.setAccuracy(accuracy);

        location.setAltitude(365.0);

        location.setBearing(0f);
        location.setSpeed(0f);

        location.setTime(
                System.currentTimeMillis()
        );

        location.setElapsedRealtimeNanos(
                SystemClock.elapsedRealtimeNanos()
        );

        if (Build.VERSION.SDK_INT >= 26) {

            location.setBearingAccuracyDegrees(1f);

            location.setSpeedAccuracyMetersPerSecond(
                    0.1f
            );

            location.setVerticalAccuracyMeters(
                    2f
            );
        }

        return location;
    }

    private void stopMocking() {

        stopExecutorOnly();

        /*
         * Google FLP wieder in den Normalmodus setzen.
         */
        try {

            fusedClient.setMockMode(false);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Fused Mock Mode konnte nicht beendet werden",
                    e
            );
        }

        removeProvider(
                LocationManager.GPS_PROVIDER
        );

        removeProvider(
                LocationManager.NETWORK_PROVIDER
        );
    }

    private void stopExecutorOnly() {

        if (executor != null) {

            executor.shutdownNow();
            executor = null;
        }
    }

    private void removeProvider(String provider) {

        try {

            locationManager.setTestProviderEnabled(
                    provider,
                    false
            );

        } catch (Exception ignored) {
        }

        try {

            locationManager.removeTestProvider(
                    provider
            );

        } catch (Exception ignored) {
        }
    }

    private void createChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Standortsimulation",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Hält den simulierten Standort aktiv."
            );

            getSystemService(
                    NotificationManager.class
            ).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {

        Intent open =
                new Intent(
                        this,
                        MainActivity.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        open,
                        PendingIntent.FLAG_IMMUTABLE |
                                PendingIntent.FLAG_UPDATE_CURRENT
                );

        return new Notification.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "Location Peek aktiv"
                )
                .setContentText(
                        String.format(
                                java.util.Locale.US,
                                "%.5f, %.5f",
                                latitude,
                                longitude
                        )
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_mylocation
                )
                .setOngoing(true)
                .setContentIntent(
                        pendingIntent
                )
                .build();
    }

    @Override
    public void onDestroy() {

        stopMocking();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}
