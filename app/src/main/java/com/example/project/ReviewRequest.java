package com.example.project;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class ReviewRequest extends StringRequest {//서버에 리뷰 데이터를 전송하기 위한 요청 클래스

    //서버의 JSP 파일 URL
    final static private String URL = "http://10.0.2.2:8080/insertReview.jsp";

    private Map<String, String> parameters;

    public ReviewRequest(String restaurantName, String categoryName, String address, String reviewContent, int rating, Response.Listener<String> listener,Response.ErrorListener errorListener) {
        super(Method.POST, URL, listener, errorListener);
        
        //전달받은 정보를 HashMap에 담아서 parameters에 초기화
        parameters = new HashMap<>();
        parameters.put("restaurantName", restaurantName);
        parameters.put("categoryName", categoryName);
        parameters.put("address", address);
        parameters.put("reviewContent", reviewContent);
        parameters.put("rating", String.valueOf(rating));//int를 String으로 변환하여 전달함
    }

    protected Map<String, String> getParams() throws AuthFailureError {//초기화한 parameters를 반환
        return parameters;
    }
}

