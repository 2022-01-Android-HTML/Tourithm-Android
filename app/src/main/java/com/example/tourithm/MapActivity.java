package com.example.tourithm;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.Coord;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.LocationButtonView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    LatLng[] latlng = new LatLng[100];
    String[][] infodata = new String[100][6];
    LinearLayout info;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        new MapActivity.Select_local_Request().execute();

        info = findViewById(R.id.map_mark_info);
        // ????????? ?????????
        info.setVisibility(View.GONE);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_view);

        if(mapFragment == null){
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map_view, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
        locationSource = new FusedLocationSource(this,LOCATION_PERMISSION_REQUEST_CODE);

    }

    // DB - ????????? ????????????
    class Select_local_Request extends AsyncTask<String, Integer, String> {
        String result = null;
        @Override
        protected String doInBackground(String... rurls) {
            try {
                URL url = new URL("https://idox23.cafe24.com/Tourithm/PlaceData_result.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                if(conn.getResponseCode()==HttpURLConnection.HTTP_OK) {
                    InputStreamReader inputStreamReader = new InputStreamReader(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    result = stringBuilder.toString();
                } else {
                    result = "error";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(String _result) {
            try {
                JSONObject root = new JSONObject(_result);
                JSONArray results = new JSONArray(root.getString("results"));

                for (int index = 0; index < results.length(); index++) {
                    JSONObject Content = results.getJSONObject(index);

                    // ????????????, ????????????, ??????, ??????, ?????? ???
                    String name = Content.getString("name");
                    String road = Content.getString("add_road");
                    double latitude = Content.getDouble("latitude");
                    double longitude = Content.getDouble("longitude");
                    String tel = Content.getString("tel");

                    // ????????? ?????? ?????? ????????? ??????
                    latlng[index] = new LatLng(latitude, longitude);

                    // double -> String
                    String st_latitude = Double.toString(latitude);
                    String st_longitude = Double.toString(longitude);
                    // ????????? ????????? ?????? ( ??????, ??????, ??????, ??????, ??????, ????????? )
                    infodata[index][0] = st_latitude;
                    infodata[index][1] = st_longitude;
                    infodata[index][2] = name;
                    infodata[index][3] = road;
                    infodata[index][4] = tel;
                    infodata[index][5] = "false";

                    // ?????? ??????
                    addInfo(latlng[index]);

                    Log.d("[latlng]", latlng[index].toString());
                    for(int i = 0; i < 6; i++){
                        Log.d("[infodata]", infodata[index][i]);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // ?????? ??????
        naverMap.setMapType(NaverMap.MapType.Basic);

        // ?????? ??????
        LatLng exlatlng = new LatLng(37.3479964585, 126.9005247934);
        // ?????? ?????? (??????)
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(exlatlng);
        naverMap.moveCamera(cameraUpdate);

        // ui : GPS
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(false);
        LocationButtonView locationButtonView = findViewById(R.id.map_gps_bt);
        locationButtonView.setMap(naverMap);
        // ???????????? ???????????? ( ????????? ?????? ?????? ?????? )
        //naverMap.setLocationSource(locationSource);
        //naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // ?????? ??????
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);

    }

    // ?????? ??????
    public void addInfo(LatLng latLng) {

        ImageButton telbtn = (ImageButton) findViewById(R.id.mark_tel_bt);
        ImageButton scrapbtn = (ImageButton) findViewById(R.id.mark_scrap);
        ImageButton findway = (ImageButton) findViewById(R.id.mark_find_way);

        TextView title = (TextView) findViewById(R.id.mark_title);
        TextView addr = (TextView) findViewById(R.id.mark_addr);
        TextView tel = (TextView) findViewById(R.id.mark_telnum);

        Marker marker = new Marker();

        marker.setPosition(latLng);
        marker.setMap(naverMap);

        // ?????? ??????
        marker.setWidth(70);
        marker.setHeight(100);

        // ?????? ??????
        marker.setIcon(OverlayImage.fromResource(R.drawable.place_mark));

        // ?????? ?????????
        marker.setOnClickListener(new Overlay.OnClickListener() {
            @Override
            public boolean onClick(@NonNull Overlay overlay) {
                if(overlay instanceof Marker) {

                    // ????????? ??????
                    LatLng lat = marker.getPosition().toLatLng();

                    // ?????? ??????
                    for(int i = 0; i < 100; i++){
                        Double aa = Double.parseDouble(infodata[i][0]);
                        Double ab = Double.parseDouble(infodata[i][1]);
                        LatLng c = new LatLng(aa, ab); // ????????? ?????? ????????? lanlng??? ??????

                        // ??? ????????? ?????? ?????????
                        boolean B = lat.equals(c);
                        if(B == true){ // ?????????
                            // ??????, ????????? ??? ????????
                            String titl = infodata[i][2];
                            String adr = infodata[i][3];
                            String call = infodata[i][4];
                            String scrap = infodata[i][5];

                            System.out.println("titl : "+titl+"/ adr : "+adr+"/ call : "+call+"/scrap"+scrap);

                            title.setText(titl);
                            addr.setText(adr);
                            tel.setText(call);
                            findViewById(R.id.map_mark_info).setVisibility(View.VISIBLE); // ????????? ?????????

                            // ????????? ?????? ???????????????
                            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
                                    new LatLng(aa, ab),14)
                                    .animate(CameraAnimation.Fly, 1000);
                            naverMap.moveCamera(cameraUpdate);

                            String finalCall = "tel:" + call;
                            //Boolean ima = Boolean.parseBoolean(scrap);

                            // ?????? ?????? ??????
                            telbtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent("android.intent.action.DIAL", Uri.parse(finalCall)));
                                }
                            });

                            // ????????? ??????
                            scrapbtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(scrap.equals("false")){

                                        scrapbtn.setImageResource(R.drawable.star_color);
                                        Intent intent = new Intent(getApplicationContext(), Scrap.class);
                                        intent.putExtra("latlng", c); // ?????? ??????
                                        scrap.replace("false","true");
                                        System.out.println(scrap);
                                    }else if(scrap.equals("true")) {
                                        scrapbtn.setImageResource(R.drawable.star);
                                        scrap.replace("true","false");
                                        System.out.println(scrap);
                                    }

                                }
                            });

                            // ????????? ??????
                            findway.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    /*
                                    Intent intent = new Intent(getApplicationContext(), ?.class);
                                    intent.putExtra("latlng", c); // ?????? ??????
                                    startActivity(intent);*/
                                }
                            });

                        }
                    }
                    return true;
                }
                return false;
            }
        });

        // ????????? ???????????? ?????? ?????? ??????
        naverMap.setOnMapClickListener((coord, point) -> {
            info.setVisibility(View.GONE);
        });

    }

    // ???????????? ?????? ??????
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // ?????? ??????
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
                return;
            } else { // ?????? ??????
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}