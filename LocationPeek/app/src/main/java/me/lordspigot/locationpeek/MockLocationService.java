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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MockLocationService extends Service {
    public static final String ACTION_START = "me.lordspigot.locationpeek.START";
    public static final String ACTION_STOP = "me.lordspigot.locationpeek.STOP";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";

    private static final String CHANNEL_ID = "mock_location";
    private static final int NOTIFICATION_ID = 4201;

    private LocationManager locationManager;
    private ScheduledExecutorService executor;
    private double latitude;
    private double longitude;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopMocking();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_START.equals(intent.getAction())) {
            latitude = intent.getDoubleExtra(EXTRA_LAT, 47.0707);
            longitude = intent.getDoubleExtra(EXTRA_LON, 15.4395);
            startForeground(NOTIFICATION_ID, buildNotification());
            startMocking();
        }
        return START_STICKY;
    }

    private void startMocking() {
        stopExecutorOnly();
        try {
            prepareProvider(LocationManager.GPS_PROVIDER, Criteria.ACCURACY_FINE);
            prepareProvider(LocationManager.NETWORK_PROVIDER, Criteria.ACCURACY_COARSE);
        } catch (SecurityException se) {
            stopForeground(true);
            stopSelf();
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                push(LocationManager.GPS_PROVIDER, 3.0f);
                push(LocationManager.NETWORK_PROVIDER, 10.0f);
            } catch (SecurityException e) {
                stopSelf();
            } catch (Exception ignored) {
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void prepareProvider(String provider, int accuracy) {
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
            // Provider/test provider may already exist.
        }
        locationManager.setTestProviderEnabled(provider, true);
    }

    private void push(String provider, float accuracy) {
        Location location = new Location(provider);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        location.setAltitude(200.0);
        location.setBearing(0f);
        location.setSpeed(0f);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        if (Build.VERSION.SDK_INT >= 26) {
            location.setBearingAccuracyDegrees(1f);
            location.setSpeedAccuracyMetersPerSecond(0.1f);
            location.setVerticalAccuracyMeters(2f);
        }
        locationManager.setTestProviderLocation(provider, location);
    }

    private void stopMocking() {
        stopExecutorOnly();
        removeProvider(LocationManager.GPS_PROVIDER);
        removeProvider(LocationManager.NETWORK_PROVIDER);
    }

    private void stopExecutorOnly() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void removeProvider(String provider) {
        try { locationManager.setTestProviderEnabled(provider, false); } catch (Exception ignored) {}
        try { locationManager.removeTestProvider(provider); } catch (Exception ignored) {}
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Standortsimulation",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hält den simulierten Standort aktiv.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Peek aktiv")
                .setContentText(String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setContentIntent(pi)
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
