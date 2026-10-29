package com.example.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.Manifest;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // 지도 변수
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isLocationUpdatesActive = false;
    private Geocoder geocoder;

    // 리뷰 관련 (리뷰 수정 및 추가 결과)
    private ActivityResultLauncher<Intent> modifyLauncher,addLauncher;

    Button reviewBtn;
    SearchView search_view;
    ListView searchResultList;
    ArrayAdapter<String> adapter;

    // 검색 결과 저장
    ArrayList<String> resultList = new ArrayList<>(); // 화면 표시용 문자열 리스트
    ArrayList<JSONObject> searchResults = new ArrayList<>(); // 실제 검색 결과
    ArrayList<JSONObject> allReviews = new ArrayList<>(); // 전체 리뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        reviewBtn = findViewById(R.id.reviewBtn);
        search_view = findViewById(R.id.search_view);
        searchResultList = findViewById(R.id.searchResultList);

        // 검색 결과 리스트뷰
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, resultList);
        searchResultList.setAdapter(adapter);
        searchResultList.setVisibility(View.GONE);

        // 지도 및 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this);
        createLocationRequest();
        setupLocationCallback();

        // 리뷰 수정 결과 (수정 완료 후 자동으로 리뷰 목록 갱신)
        modifyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                try{
                    if(result.getResultCode() ==RESULT_OK){
                        hideSearchResults();
                        loadAllReviews();
                        Toast.makeText(MainActivity.this, "리뷰가 수정되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e) {
                    Log.e(TAG, "Error in modifyLauncher callback", e);
                }
            }});

        // 리뷰 추가 결과 (추가 완료 후 자동으로 리뷰 목록 갱신)
        addLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                try{
                    if(result.getResultCode() == RESULT_OK){
                        hideSearchResults();
                        loadAllReviews();
                        Toast.makeText(MainActivity.this, "리뷰가 추가되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e) {
                    Log.e(TAG, "Error in addLauncher callback", e);
                }}
        });

        // 구글맵 초기화
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null)
            mapFragment.getMapAsync(this);

        // 리뷰 추가 버튼 클릭 리스너 (리뷰 작성 화면으로 이동)
        reviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Review.class);
                addLauncher.launch(intent);
            }
        });

        // 검색 기능 (검색어 입력 시 서버에 검색 요청)
        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchReview(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 검색어가 비어있으면 검색 결과 숨기고 전체 리뷰 표시
                if (newText.isEmpty()) {
                    hideSearchResults();
                    showAllReviews();
                }
                return false;
            }
        });

        // 검색 결과 리스트 항목 클릭 시 리뷰 수정 화면으로 이동
        searchResultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    JSONObject selected = searchResults.get(position);

                    // 선택된 리뷰 데이터를 수정 화면으로 전달
                    Intent intent = new Intent(MainActivity.this, ReviewModify.class);
                    intent.putExtra("restaurantName", selected.getString("RestaurantName"));
                    intent.putExtra("categoryName", selected.getString("CategoryName"));
                    intent.putExtra("address", selected.getString("Address"));
                    intent.putExtra("reviewContent", selected.getString("ReviewContent"));
                    intent.putExtra("rating", selected.getInt("Rating"));

                    modifyLauncher.launch(intent);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    // 위치 업데이트 요청
    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build();
    }

    // 위치 변경 콜백
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "New location: " + location.getLatitude() + ", " + location.getLongitude());
                    Log.d(TAG, "Location accuracy: " + location.getAccuracy() + "m");
                    Log.d(TAG, "Location provider: " + location.getProvider());
                    updateMapWithNewLocation(location);
                }
            }
        };
    }

    // 지도 카메라를 새로운 위치로 이동
    private void updateMapWithNewLocation(Location location) {
        if (mMap == null) return;

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!checkLocationPermission()) {
            return;
        }

        if (!isGPSEnabled()) {
            Toast.makeText(this, "GPS를 활성화해주세요", Toast.LENGTH_LONG).show();
            return;
        }

        if (isLocationUpdatesActive) {
            return;
        }

        Log.d(TAG, "Starting location updates");
        fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        Looper.getMainLooper())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        isLocationUpdatesActive = true;
                        Log.d(TAG, "Location updates started successfully");
                        getCurrentLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start location updates", e);
                });
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (!checkLocationPermission()) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Log.d(TAG, "Got last known location: " +
                                    location.getLatitude() + ", " + location.getLongitude());
                            updateMapWithNewLocation(location);
                        } else {
                            Log.d(TAG, "Last known location is null");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last location", e);
                });
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private boolean checkLocationPermission() {
        boolean fineLocationGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineLocationGranted || !coarseLocationGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Log.d(TAG, "Location permissions granted");
                if (mMap != null) {
                    enableMyLocation();
                }
                startLocationUpdates();
            } else {
                Toast.makeText(this,
                        "위치 권한이 필요합니다. 설정에서 권한을 허용해주세요",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (mMap != null && checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    // 지도 준비 완료 시 호출되는 콜백 메서드
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready");

        // 지도 UI
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // 기본 위치(명지전문대) 설정 (위치 관련 오류 발생 시 기본 위치 표시)
        LatLng defaultLocation = new LatLng(37.585079, 126.925285);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        if (checkLocationPermission()) {
            enableMyLocation();
            startLocationUpdates();
        }

        // 지도 준비 완료 후 전체 리뷰를 불러와 마커 표시
        loadAllReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (checkLocationPermission() && !isLocationUpdatesActive) {
            startLocationUpdates();
        }
        if (mMap != null) {
            loadAllReviews();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
            Log.d(TAG, "Location updates stopped");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    // 서버에서 전체 리뷰를 가져오는 메서드
    void loadAllReviews() {
        String url = "http://10.0.2.2:8080/getAllReviews.jsp";

        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                if (response == null || response.isEmpty()) {
                    Log.d(TAG, "No reviews found");
                    return;
                }
                JSONArray jsonArray = new JSONArray(response);

                allReviews.clear();

                // JSON 배열에서 각 리뷰 객체를 리스트에 저장
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    allReviews.add(obj);
                }

                // 지도에 전체 리뷰 위치를 마커로 표시
                showAllReviews();
                Log.d(TAG, "Loaded " + allReviews.size() + " reviews");

            } catch (Exception e) {
                Log.e(TAG, "Error loading all reviews", e);
            }
        }, error -> {
            Log.e(TAG, "Failed to load all reviews", error);
        });

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    // 지도에 전체 리뷰 위치를 마커로 표시하는 메서드
    private void showAllReviews() {
        if (mMap == null) return;

        mMap.clear();

        // 전체 리뷰를 확인하여 각 음식점 위치에 마커 추가
        for (JSONObject review : allReviews) {
            try {
                String restaurantName = review.getString("RestaurantName");
                String address = review.getString("Address");
                addMarkerToMap(restaurantName, address);
            } catch (Exception e) {
                Log.e(TAG, "Error showing review on map", e);
            }
        }
    }

    // 리뷰를 검색하는 메서드 (서버에 검색 요청을 보내고 결과를 처리)
    void searchReview(String keyword){
        String url = "http://10.0.2.2:8080/searchReview.jsp?keyword=" + keyword;

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (response == null || response.isEmpty()) {
                        Toast.makeText(MainActivity.this, "검색 결과 없음", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONArray jsonArray = new JSONArray(response);

                    resultList.clear();
                    searchResults.clear();

                    if (mMap != null) {
                        mMap.clear();
                    }

                    // 검색 결과 처리 및 지도 마커 추가
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        searchResults.add(obj);

                        // 리스트뷰에 표시할 정보 구성
                        String restaurantName = obj.getString("RestaurantName");
                        String categoryName = obj.getString("CategoryName");
                        String address = obj.getString("Address");
                        resultList.add("가게명: " + restaurantName + "\n카테고리: " + categoryName);

                        // 검색된 음식점 위치에 지도 마커 추가
                        addMarkerToMap(restaurantName, address);
                    }

                    // 검색 결과 리스트뷰 업데이트 및 표시
                    adapter.notifyDataSetChanged();
                    searchResultList.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(MainActivity.this, "검색 실패", Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    // 주소를 좌표로 변환하여 지도에 마커 추가하는 메서드
    private void addMarkerToMap(String restaurantName, String address) {
        if (mMap == null) return;

        try {
            // Geocoder를 사용하여 주소를 좌표로 변환
            List<Address> addressList = geocoder.getFromLocationName(address, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address location = addressList.get(0);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                // 좌표를 이용하여 지도에 마커 추가
                LatLng latLng = new LatLng(latitude, longitude);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(restaurantName)
                        .snippet(address);

                mMap.addMarker(markerOptions);

                Log.d(TAG, "Marker added for: " + restaurantName + " at " + latitude + ", " + longitude);
            } else {
                Log.w(TAG, "Could not find location for address: " + address);
            }
        }catch (Exception e) {
            Log.e(TAG, "Error adding marker for " + restaurantName, e);
        }
    }

    // 검색 결과 리스트를 숨기고 검색창을 초기화하는 메서드
    private void hideSearchResults() {
        resultList.clear();
        searchResults.clear();
        adapter.notifyDataSetChanged();
        searchResultList.setVisibility(View.GONE);

        String current = search_view.getQuery().toString();
        if(!current.isEmpty()){
            search_view.setQuery("", false);}
        search_view.clearFocus();
    }
}