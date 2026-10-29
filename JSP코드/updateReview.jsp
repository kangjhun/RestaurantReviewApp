<%@ page import="java.sql.*" %>
<%@ page contentType="application/json;charset=UTF-8" %>

<% request.setCharacterEncoding("UTF-8");
  
    //전달받은 파라미터를 변수로 받음
    String restaurantName = request.getParameter("restaurantName");
    String categoryName = request.getParameter("categoryName");
    String address = request.getParameter("address");
    String reviewContent = request.getParameter("reviewContent");
    int rating = Integer.parseInt(request.getParameter("rating"));

    //DB연결을 위한 객체 선언
    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/db_kjh";
        String dbUser = "root";
        String dbPassword = "1234";

        //JDBC 드라이버 로드 및 DB연결
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);

        //Review 테이블에서 조건에 맞는 레코드에 리뷰 내용과 별점 수정
        String sql = "UPDATE Review SET ReviewContent=?, Rating=? WHERE RestaurantName=? AND CategoryName=? AND Address=?";
        pstmt = conn.prepareStatement(sql);
        
        //수정한 데이터 입력
        pstmt.setString(1, reviewContent);
        pstmt.setInt(2, rating);
        
        //검색조건(가게명, 카테고리명, 주소)
        pstmt.setString(3, restaurantName);
        pstmt.setString(4, categoryName);
        pstmt.setString(5, address);

        //SQL 실행
        pstmt.executeUpdate();

    } catch(Exception e) { //예외 발생 시 서버 로그에 출력
        e.printStackTrace();
    } finally {//DB 연결 종료
        if (pstmt != null) pstmt.close();
        if (conn != null) conn.close();
    }
%>