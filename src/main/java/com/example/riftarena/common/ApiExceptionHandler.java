package com.example.riftarena.common;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;import org.apache.catalina.connector.ClientAbortException;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ClientAbortException.class)
 public void clientAbort(ClientAbortException e){
  // 브라우저 새로고침·페이지 이동·오디오 요청 취소로 연결이 먼저 끊긴 정상 상황입니다.
 }

 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
 public ResponseEntity<Map<String,Object>> known(RuntimeException e){
  HttpStatus s=e instanceof IllegalArgumentException?HttpStatus.BAD_REQUEST:HttpStatus.CONFLICT;
  Map<String,Object>b=new LinkedHashMap<>();b.put("status",s.value());b.put("message",e.getMessage());return ResponseEntity.status(s).body(b);
 }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<Map<String,Object>> other(Exception e){e.printStackTrace();Map<String,Object>b=new LinkedHashMap<>();b.put("status",500);b.put("message","서버 오류가 발생했습니다.");return ResponseEntity.status(500).body(b);}
}
