package com.example.project;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Review extends AppCompatActivity {//리뷰 작성 클래스

    EditText restaurantNameEdit; //음식점 이름 입력칸
    EditText categoryEdit; //카테고리 입력칸
    EditText addressEdit; //주소 입력칸
    EditText reviewEdit; //리뷰 내용 입력칸
    RatingBar ratingBar; //별점
    Button reviewPlusButton; //리뷰 추가 버튼

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review);

        restaurantNameEdit = findViewById(R.id.restaurantNameEdit);
        categoryEdit = findViewById(R.id.categoryEdit);
        addressEdit = findViewById(R.id.addressEdit);
        reviewEdit = findViewById(R.id.reviewEdit);
        ratingBar = findViewById(R.id.ratingBar);
        reviewPlusButton = findViewById(R.id.reviewPlusButton);

        //툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("리뷰 추가");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//뒤로 가기 표시 활성화

        reviewPlusButton.setOnClickListener(new View.OnClickListener() {//추가 버튼을 클릭시 insertData() 호출
            @Override
            public void onClick(View v) {
                insertData();
            }
        });

    }

    void insertData(){//입력값을 추출하고 필수 항목 검증 및 주소 확인 호출

        //입력된 값을 추출
        String restaurantName = restaurantNameEdit.getText().toString();
        String categoryName = categoryEdit.getText().toString();
        String address = addressEdit.getText().toString();
        String reviewContent = reviewEdit.getText().toString();
        int rating = (int)ratingBar.getRating();

        //식당명과 카테고리명, 주소가 비어있으면 메시지를 출력해줌
        if (restaurantName.isEmpty()) {
            Toast.makeText(this, "식당명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "카테고리를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }


        //주소의 유효성 검사 호출
        checkAddress(restaurantName,categoryName,address,reviewContent,rating);
    }

    void clearInputs(){//입력 창 초기 상태로 되돌리는 함수 
        restaurantNameEdit.setText("");
        categoryEdit.setText("");
        addressEdit.setText("");
        reviewEdit.setText("");
        ratingBar.setRating(3f); //기본 별점 3점 설정
    }

    public boolean onSupportNavigateUp() {//툴바 뒤로 가기 눌렀을 때 액티비티 종료
        finish();
        return true;
    }

    //Gecoder를 사용해 주소가 유효한지 확인하는 함수
    void checkAddress(String restaurantName, String categoryName, String address, String reviewContent, int rating ){
        Geocoder geocoder = new Geocoder(this);

        try{
            //주소 문자열로 위치 정보 검색
            List<Address> addressList = geocoder.getFromLocationName(address,1);

            if(addressList != null && !addressList.isEmpty()){//주소가 유효하면 서버에 전송
                //서버 응답 리스너
                Response.Listener<String> responseListener = new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            //서버에서 JSON 형태로 결과를 수신
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            if(success){//서버 저장 성공
                                Toast.makeText(Review.this, "저장 성공",Toast.LENGTH_SHORT).show();
                                clearInputs();//입력창 초기화

                                //메인화면으로 결과를 전달(화면 갱신)하고 종료
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("added",true);
                                setResult(RESULT_OK,resultIntent);
                                finish();
                            }else{//서버가 실패를 응답한 경우
                                Toast.makeText(Review.this,"저장 실패",Toast.LENGTH_SHORT).show();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };

                //서버 요청을 전송
                ReviewRequest request = new ReviewRequest(restaurantName,categoryName,address,reviewContent,rating,responseListener,errorListener);
                RequestQueue queue = Volley.newRequestQueue(Review.this);
                queue.add(request); //큐에 request를 추가
            }else { //주소가 유효하지 않으면
                Toast.makeText(this,"존재하지 않는 주소입니다. 정확히 입력해주세요!",Toast.LENGTH_SHORT).show();
            }
        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(this,"주소 확인 중 오류 발생",Toast.LENGTH_SHORT).show();
        }
    }

    Response.ErrorListener errorListener = new Response.ErrorListener() {//서버 통신 오류 리스너
        @Override
        public void onErrorResponse(VolleyError error) {
            error.printStackTrace();
            Toast.makeText(Review.this, "서버 통신 오류 발생", Toast.LENGTH_SHORT).show();
        }
    };
}
