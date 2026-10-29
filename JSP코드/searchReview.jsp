<%@ page import="java.sql.*" %>
<%@ page contentType="application/json;charset=UTF-8" %>

<%
    request.setCharacterEncoding("UTF-8");
    //전달받은 검색어의 파라미터를 읽어옴
    String keyword = request.getParameter("keyword");

    //검색어가 null인 경우 빈 문자열로 초기화
    if(keyword == null) keyword = "";

   //DB연결 및 SQL 실행에 필요한 변수를 선언함
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    //JSON 응답을 만들기 위한 StringBuilder  준비
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("[");//JSON 배열 시작

    try {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/db_kjh";
        String dbUser = "root";
        String dbPassword = "1234";

        //JDBC 드라이버 로드 및 DB 연결 생성
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);

        //식당명 또는 카테고리명에 검색어가 포함되어 있으면 검색
        String sql = "SELECT RestaurantName, CategoryName, Address, ReviewContent, Rating FROM Review WHERE RestaurantName LIKE ? OR CategoryName LIKE ?";
        pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, "%" + keyword + "%");
        pstmt.setString(2, "%" + keyword + "%");

        rs = pstmt.executeQuery();
       
        //JSON 배열에서 첫 번째 요소인지 확인하기 위한 변수
        boolean first = true;
        while(rs.next()) {//검색 결과 반복 처리
            //첫번째가 아니면 콤마를 추가
            if(!first) jsonBuilder.append(",");
            
            //각 값을 JSON형식으로 변환하여 StringBuilder에 추가
            jsonBuilder.append("{");
            jsonBuilder.append("\"RestaurantName\":\"").append(rs.getString("RestaurantName").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"CategoryName\":\"").append(rs.getString("CategoryName").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"Address\":\"").append(rs.getString("Address").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"ReviewContent\":\"").append(rs.getString("ReviewContent").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"Rating\":").append(rs.getInt("Rating"));
            jsonBuilder.append("}");//JSON 객체 종료

            first = false;//첫번째  이후부터는 콤마를 붙이기 위해 false를 사용
        }

    } catch(Exception e) {//예외 발생 시 로그 출력
        e.printStackTrace();
    } finally {//DB 연결 해제
        if (rs != null) rs.close();
        if (pstmt != null) pstmt.close();
        if (conn != null) conn.close();
    }

    jsonBuilder.append("]");//JSON 배열 종료
    out.print(jsonBuilder.toString()); //결과를 JSON형태로 응답
%>