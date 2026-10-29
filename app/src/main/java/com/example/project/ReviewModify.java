package com.example.project;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewModify extends AppCompatActivity {//리뷰 수정

    GoogleMap map;
    TextView textRestaurantName, textCategory, textAddress;
    EditText reviewEdit;
    RatingBar ratingBar;
    Button updateBtn;

    //Intent로 전달된 리뷰 정보를 저장할 변수
    String restaurantName, categoryName,address,reviewContent;
    int rating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reviewmodify);

        //툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("리뷰 수정");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//뒤로가기 활성화

        textRestaurantName = findViewById(R.id.textRestaurantName);
        textCategory = findViewById(R.id.textCategory);
        textAddress = findViewById(R.id.textAdress);
        reviewEdit = findViewById(R.id.reviewEdit);
        ratingBar = findViewById(R.id.ratingBar);
        updateBtn = findViewById(R.id.updateButton);

        //메인 액티비티에서 리뷰 수정으로 넘어올 때 전달받은 데이터를 읽어옴
        Intent intent = getIntent();
        restaurantName = intent.getStringExtra("restaurantName");
        categoryName = intent.getStringExtra("categoryName");
        address = intent.getStringExtra("address");
        reviewContent = intent.getStringExtra("reviewContent");
        rating = intent.getIntExtra("rating",3);

        //전달받은 값들을 화면에 표시해줌
        textRestaurantName.setText(restaurantName);
        textCategory.setText(categoryName);
        textAddress.setText(address);
        reviewEdit.setText(reviewContent);
        ratingBar.setRating(rating);

        //GoogleMap
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if(mapFragment != null){
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {
                    map=googleMap;
                    showLocationOnMap(address);//주소로 지도 마커 표시 호출
                }
            });
        }

        updateBtn.setOnClickListener(new View.OnClickListener() {//수정 버튼 클릭 시 updateReview 함수 호출
            @Override
            public void onClick(View v) {
                updateReview(); 
            }
        });


    }

    void updateReview(){//서버로 수정 요청을 보내는 함수
        String updateReview = reviewEdit.getText().toString(); //수정된 리뷰 내용 추출
        int updateRating = (int)ratingBar.getRating(); //수정된 별점 추출

        String url = "http://10.0.2.2:8080/updateReview.jsp";

        //StringRequest 생성
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {//서버 응답 성공 시
                Toast.makeText(ReviewModify.this, "수정 성공", Toast.LENGTH_SHORT).show();

                //수정 성공 후 이전 화면에 결과 전달 후 종료
                Intent resultIntent = new Intent();
                resultIntent.putExtra("modified", true); //수정 완료 플래그 전달
                setResult(RESULT_OK, resultIntent);
                finish(); //액티비티 종료
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {//서버 통신 오류 시 처리
                error.printStackTrace();
                Toast.makeText(ReviewModify.this,"수정 실패",Toast.LENGTH_SHORT).show();
            }
        })
        {
            //서버로 보낼 파라미터를 구성함
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("restaurantName",restaurantName);
                params.put("categoryName",categoryName);
                params.put("address",address);
                params.put("reviewContent",updateReview);
                params.put("rating",String.valueOf(updateRating));
                return params; //POST 파라미터로 전송
            }
        };
        //큐에 요청 추가하여 전송
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    void showLocationOnMap(String addressStr){//주소를 위도,경도로 변환하여 지도에 마커 표시
        Geocoder geocoder = new Geocoder(this);
        try{
            List<Address> addressList = geocoder.getFromLocationName(addressStr,1);
            if(addressList != null && !addressList.isEmpty()){
                Address location = addressList.get(0);
                double latitude = location.getLatitude(); //위도 값 추출
                double longitude = location.getLongitude(); //경도 값 추출

                LatLng latLng = new LatLng(latitude,longitude);
                //카메라 위치 이동 및 줌 레벨 16
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,16)); 
                //식당명 마커 표시
                map.addMarker(new MarkerOptions().position(latLng).title(restaurantName));//마커 추가, 식당명을 보여줌
            }else{ //주소를 찾지 못했을 경우
                Toast.makeText(this,"위치를 찾을 수 없습니다",Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,"주소 변환 실패",Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onSupportNavigateUp() {//툴바 뒤로가기 눌렀을때 액티비티 종료
        finish();
        return true;
    }
}