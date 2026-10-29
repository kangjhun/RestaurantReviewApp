<%@ page import="java.sql.*" %>
<%@ page contentType="application/json;charset=UTF-8" language="java" %>

<%
    request.setCharacterEncoding("UTF-8");
 
    //전달받은 파라미터 읽어옴
    String restaurantName = request.getParameter("restaurantName");
    String categoryName = request.getParameter("categoryName");
    String address = request.getParameter("address");
    String reviewContent = request.getParameter("reviewContent");
   String ratingStr = request.getParameter("rating");
   int rating = 3;//별점 기본값 3으로 초기화
   if (ratingStr != null && !ratingStr.equals("")) {
    rating = Integer.parseInt(ratingStr);//문자열 rating을 정수로 변환
    }

    boolean success = false;//성공 여부를 저장할 변수

   //DB연결 및 SQL 실행에 필요한 변수를 선언함
    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/db_kjh";
        String dbUser = "root";
        String dbPassword = "1234";

        //JDBC 드라이버 로드 및 DB 연결 생성
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);

        //INSERT SQL문 작성
        pstmt = conn.prepareStatement(
            "INSERT INTO Review (RestaurantName, CategoryName, Address, ReviewContent, Rating) VALUES (?, ?, ?, ?, ?)"
        );

        //SQL문 내 ?에 파라미터 연결
        pstmt.setString(1, restaurantName);
        pstmt.setString(2, categoryName);
        pstmt.setString(3, address);
        pstmt.setString(4, reviewContent);
        pstmt.setInt(5, rating);

        //SQL insert실행
        int result = pstmt.executeUpdate();

        //insert 성공 시 success = true로 변경
        if (result > 0) {
            success = true;
        }

    } catch(Exception e) {//예외 발생 시 로그 출력
        e.printStackTrace(); 
    } finally { //DB 연결 해제
        if (pstmt != null) pstmt.close();
        if (conn != null) conn.close();
    }

    // 결과를 JSON형태로 응답
    out.print("{\"success\":" + success + "}");
%>