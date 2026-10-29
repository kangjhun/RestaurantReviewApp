<%@ page import="java.sql.*" %>
<%@ page contentType="application/json;charset=UTF-8" %>
<%
    request.setCharacterEncoding("UTF-8");
    
    // 데이터베이스 연결 관련 객체
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    // JSON 문자열을 구성하기 위한 StringBuilder 객체
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("[");
    
    try {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/db_kjh"; 
        String dbUser = "root";
        String dbPassword = "1234";
        
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
       
        // SQL 쿼리 (리뷰 테이블 조회, 평점 내림차순→식당명 오름차순 정렬)
        String sql = "SELECT RestaurantName, CategoryName, Address, ReviewContent, Rating FROM Review ORDER BY Rating DESC, RestaurantName ASC";
        
        // PreparedStatement 객체 생성 및 쿼리 설정
        pstmt = conn.prepareStatement(sql);
        
        // 쿼리 실행 및 결과 받기
        rs = pstmt.executeQuery();
        
        // JSON 배열 내 첫 번째 요소인지 확인하는 플래그 (콤마 처리용)
        boolean first = true;
        
        // 결과 집합의 모든 행을 순회하며 JSON 객체 생성
        while(rs.next()) {
            // 첫 번째 요소가 아니면 콤마 추가 (JSON 배열 형식)
            if(!first) jsonBuilder.append(",");
            
            // JSON 객체
            jsonBuilder.append("{");
            
            // 각 필드를 JSON 형식으로 추가 (문자열 필드는 이스케이프 처리)
            jsonBuilder.append("\"RestaurantName\":\"").append(rs.getString("RestaurantName").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"CategoryName\":\"").append(rs.getString("CategoryName").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"Address\":\"").append(rs.getString("Address").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"ReviewContent\":\"").append(rs.getString("ReviewContent").replace("\"", "\\\"")).append("\",");
            jsonBuilder.append("\"Rating\":").append(rs.getInt("Rating"));  // 숫자형은 따옴표 없이
            
            jsonBuilder.append("}");
            
            // 첫 번째 요소 처리 완료 표시
            first = false;
        }
        
    } catch(Exception e) {
        // 예외 발생 시 스택 트레이스 출력 (디버깅용)
        e.printStackTrace();
    } finally {
        // 리소스 해제 (역순으로 닫기)
        if (rs != null) rs.close();        // ResultSet 닫기
        if (pstmt != null) pstmt.close();  // PreparedStatement 닫기
        if (conn != null) conn.close();    // Connection 닫기
    }
    
    // JSON 배열 종료
    jsonBuilder.append("]");
    
    // 완성된 JSON 문자열을 사용자에게 출력
    out.print(jsonBuilder.toString());
%>