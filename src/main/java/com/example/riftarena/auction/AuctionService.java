package com.example.riftarena.auction;
import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;import java.util.concurrent.*;import javax.annotation.PostConstruct;import org.springframework.stereotype.Service;import org.springframework.web.socket.*;import com.example.riftarena.member.MemberService;import com.fasterxml.jackson.databind.ObjectMapper;
@Service
public class AuctionService{
 private final MemberService members;private final ObjectMapper om;private final Map<String,Room> rooms=new ConcurrentHashMap<>();private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
 public AuctionService(MemberService members,ObjectMapper om){this.members=members;this.om=om;}
 @PostConstruct public void init(){timer.scheduleAtFixedRate(this::tick,1,1,TimeUnit.SECONDS);}
 @SuppressWarnings("unchecked") public Map<String,Object> create(Map<String,Object>p){List<Number> raw=(List<Number>)p.get("memberIds");if(raw==null||raw.size()!=10)throw new IllegalArgumentException("참가자 10명이 필요합니다.");List<Long>ids=new ArrayList<>();for(Number n:raw)ids.add(n.longValue());if(new HashSet<>(ids).size()!=10)throw new IllegalArgumentException("중복 참가자가 있습니다.");Long b=num(p.get("blueHostId")),r=num(p.get("redHostId"));if(b==null||r==null||b.equals(r)||!ids.contains(b)||!ids.contains(r))throw new IllegalArgumentException("서로 다른 호스트를 선택하세요.");List<Map<String,Object>> ms=members.byIds(ids);if(ms.size()!=10)throw new IllegalArgumentException("활성 멤버 10명을 찾을 수 없습니다.");int base=intv(p.get("baseToken"),1000),ratePct=(int)Math.round(doublev(p.get("adjustRate"),.5)*100);Room room=new Room(code(),b,r,base,ratePct);for(Map<String,Object>m:ms)room.members.put(((Number)m.get("memberId")).longValue(),new LinkedHashMap<>(m));int bs=score(room,b),rs=score(room,r),ded=(int)Math.round(Math.abs(bs-rs)*(ratePct/100.0));room.blueTokens=base;room.redTokens=base;if(bs>rs)room.blueTokens=Math.max(100,base-ded);else if(rs>bs)room.redTokens=Math.max(100,base-ded);room.blueTeam.add(new Pick(b,0,true));room.redTeam.add(new Pick(r,0,true));ids.stream().filter(x->!x.equals(b)&&!x.equals(r)).sorted(Comparator.comparingInt(x->score(room,x))).forEach(room.queue::add);log(room,"SYSTEM","실시간 경매방이 생성되었습니다.");rooms.put(room.code,room);return state(room.code);}
 public boolean join(String code,String role,String clientId,WebSocketSession s){
  Room r=room(code);
  synchronized(r){
   role=normRole(role);
   if(role.equals("DIRECTOR")||role.equals("BLUE")||role.equals("RED")){
    WebSocketSession existing=r.sessions.get(role);
    String existingClientId=r.roleClientIds.get(role);
    if(existing!=null&&existing.isOpen()&&!Objects.equals(existingClientId,clientId)){
     String roleName=role.equals("DIRECTOR")?"진행자":role+" 호스트";
     sendJoinRejected(s,role,roleName+"가 이미 접속 중입니다. 다른 역할이나 관전자로 접속해주세요.");
     return false;
    }
    if(existing!=null&&existing!=s){
     try{existing.close(CloseStatus.NORMAL.withReason("같은 역할 재연결"));}catch(Exception ignore){}
    }
    r.sessions.put(role,s);
    r.roleClientIds.put(role,clientId);
    log(r,"SYSTEM",roleDisplayName(r,role)+" 접속");
   }else{
    String key="SPECTATOR:"+s.getId();
    r.sessions.put(key,s);
    r.spectatorKeys.put(s.getId(),key);
    log(r,"SYSTEM","관전자 입장 · 현재 "+spectatorCount(r)+"명");
   }
   broadcast(r);
   return true;
  }
 }
 public void leave(String code,String role,WebSocketSession s){
  Room r=rooms.get(norm(code));if(r==null)return;
  synchronized(r){
   role=normRole(role);
   if(role.equals("DIRECTOR")||role.equals("BLUE")||role.equals("RED")){
    if(r.sessions.get(role)==s){
     r.sessions.remove(role);
     r.roleClientIds.remove(role);
     log(r,"SYSTEM",roleDisplayName(r,role)+" 연결 종료");
    }
   }else{
    String key=r.spectatorKeys.remove(s.getId());
    if(key!=null&&r.sessions.get(key)==s){
     r.sessions.remove(key);
     log(r,"SYSTEM","관전자 퇴장 · 현재 "+spectatorCount(r)+"명");
    }
   }
   broadcast(r);
  }
 }
 public void action(String code,String role,Map<String,Object>p){Room r=room(code);String a=String.valueOf(p.get("action")).toUpperCase();synchronized(r){if(a.equals("START")){needRole(role,"DIRECTOR");if(!connected(r,"DIRECTOR"))throw new IllegalStateException("진행자가 접속 중이어야 합니다.");if(!connected(r,"BLUE")||!connected(r,"RED"))throw new IllegalStateException("블루와 레드 호스트가 모두 접속해야 시작할 수 있습니다.");if(!r.status.equals("WAITING"))return;r.status="STARTING";r.startDeadline=Instant.now().plusSeconds(4);log(r,"SYSTEM","진행자가 경매 시작을 선언했습니다.");broadcast(r);}
 else if(a.equals("BID")){host(role);bid(r,role,intv(p.get("amount"),10));}
 else if(a.equals("SURRENDER")){host(role);surrender(r,role);}
 else if(a.equals("SYNC"))broadcast(r);}}
 private void bid(Room r,String role,int amount){
  ensureRunning(r);
  if(r.currentId==null)throw new IllegalStateException("현재 매물이 없습니다.");

  List<Pick> team=role.equals("BLUE")?r.blueTeam:r.redTeam;
  int token=role.equals("BLUE")?r.blueTokens:r.redTokens;
  if(team.size()>=5)throw new IllegalStateException("이미 팀 구성이 완료됐습니다.");

  int desired=Math.max(10,amount);
  int reserve=Math.max(0,5-team.size()-1)*10;
  int max=Math.max(0,token-reserve);
  if(desired>max)throw new IllegalStateException("남은 "+(5-team.size()-1)+"회 기본 베팅용 "+reserve+"원을 남겨야 합니다. 최대 "+max+"원까지 가능합니다.");

  if(r.openingBid){
   if(r.passedTeams.contains(role))throw new IllegalStateException("이미 이번 매물을 포기했습니다.");
   if(r.currentBid>0)throw new IllegalStateException("이미 첫 입찰이 시작됐습니다.");
   r.currentBid=desired;
   r.bidTeam=role;
   r.turnTeam=opponent(role);
   r.openingBid=false;
   r.passedTeams.clear();
   r.deadline=Instant.now().plusSeconds(60);
   event(r,"FIRST_BID",role,hostName(r,role)+" 첫 입찰!",name(r,r.currentId)+" · "+desired+"원",name(r,r.currentId),desired);
   log(r,role,desired+"원 첫 입찰 → "+hostName(r,r.turnTeam)+" 차례");
   broadcast(r);
   return;
  }

  if(!role.equals(r.turnTeam))throw new IllegalStateException(hostName(r,r.turnTeam)+"님의 입찰 차례입니다.");
  if(desired<r.currentBid+10)throw new IllegalStateException("현재 입찰가보다 최소 10원 높은 금액을 입력해주세요.");

  r.currentBid=desired;
  r.bidTeam=role;
  r.turnTeam=opponent(role);
  r.deadline=Instant.now().plusSeconds(60);
  r.surrendered=null;
  event(r,"TURN",r.turnTeam,hostName(r,r.turnTeam)+"님의 TURN",name(r,r.currentId)+" · 현재 "+desired+"원",name(r,r.currentId),desired);
  log(r,role,desired+"원 입찰 → "+hostName(r,r.turnTeam)+" 차례");
  broadcast(r);
 }
 private void surrender(Room r,String role){
  ensureRunning(r);
  if(r.currentId==null)throw new IllegalStateException("현재 매물이 없습니다.");

  if(r.openingBid){
   if(!r.passedTeams.add(role))throw new IllegalStateException("이미 포기했습니다.");
   log(r,role,"첫 입찰 포기");
   if(r.passedTeams.contains("BLUE")&&r.passedTeams.contains("RED")){
    unsold(r,"양 팀 모두 포기");
   }else{
    broadcast(r);
   }
   return;
  }

  if(!role.equals(r.turnTeam))throw new IllegalStateException("현재 포기할 수 있는 차례가 아닙니다.");
  if(r.bidTeam==null){unsold(r,"입찰 없이 포기");return;}

  String winner=r.bidTeam;
  List<Pick> winTeam=winner.equals("BLUE")?r.blueTeam:r.redTeam;
  if(winTeam.size()>=5)throw new IllegalStateException("상대 팀은 더 이상 선수를 받을 수 없습니다.");
  r.surrendered=role;

  int price=Math.max(10,r.currentBid);
  int token=winner.equals("BLUE")?r.blueTokens:r.redTokens;
  int reserve=Math.max(0,5-winTeam.size()-1)*10;
  if(price>token-reserve)price=Math.max(10,token-reserve);

  log(r,role,"경매 포기 → "+hostName(r,winner)+" 즉시 낙찰");
  settle(r,winner,price);
 }
 private void tick(){for(Room r:rooms.values())try{synchronized(r){
  if(r.status.equals("STARTING")&&r.startDeadline!=null){
   long sec=Duration.between(Instant.now(),r.startDeadline).getSeconds();
   if(sec<0){r.startDeadline=null;r.status="RUNNING";next(r);}
   else broadcast(r);
  }else if(r.status.equals("RUNNING")&&r.currentId!=null&&r.deadline!=null){
   long sec=Duration.between(Instant.now(),r.deadline).getSeconds();
   if(sec<0){
    if(r.openingBid&&r.bidTeam==null){
     unsold(r,"첫 30초 동안 입찰 없음");
    }else{
     String winner=r.bidTeam;
     if(winner==null)unsold(r,"입찰자 없음");
     else{
      log(r,r.turnTeam==null?"SYSTEM":r.turnTeam,"입찰 시간 종료");
      settle(r,winner,Math.max(10,r.currentBid));
     }
    }
   }else broadcast(r);
  }
 }}catch(Exception e){}}
 private void next(Room r){
  if(r.blueTeam.size()>=5||r.redTeam.size()>=5){autoAssign(r);return;}
  if(r.queue.isEmpty()){finish(r);return;}

  r.status="RUNNING";
  r.currentId=r.queue.remove(0);
  r.surrendered=null;
  r.passedTeams.clear();
  r.round++;

  List<String> eligible=new ArrayList<>();
  if(r.blueTeam.size()<5&&maxBid(r,"BLUE")>=10)eligible.add("BLUE");
  if(r.redTeam.size()<5&&maxBid(r,"RED")>=10)eligible.add("RED");
  if(eligible.isEmpty()){finish(r);return;}

  r.currentBid=0;
  r.bidTeam=null;
  r.turnTeam=null;
  r.openingBid=true;
  r.deadline=Instant.now().plusSeconds(30);

  event(r,"OPEN_BID","SYSTEM","첫 입찰 30초",name(r,r.currentId)+" · 양 팀 누구나 먼저 금액을 입력할 수 있습니다.",name(r,r.currentId),0);
  log(r,"SYSTEM",name(r,r.currentId)+" 매물 등장 · 30초 자유 첫 입찰");
  broadcast(r);
 }
 private void unsold(Room r,String reason){
  Long id=r.currentId;
  if(id==null)return;

  String item=name(r,id);
  r.queue.add(id);
  r.status="UNSOLD";
  r.deadline=null;
  r.turnTeam=null;
  r.bidTeam=null;
  r.currentBid=0;
  r.openingBid=false;
  r.passedTeams.clear();

  log(r,"SYSTEM",item+" 유찰 · 대기열 맨 뒤로 이동 ("+reason+")");
  event(r,"UNSOLD","SYSTEM","유찰",item+" · 대기열 맨 뒤로 이동",item,0);
  broadcast(r);
  r.currentId=null;

  timer.schedule(()->{synchronized(r){
   if(r.status.equals("UNSOLD")){
    r.status="RUNNING";
    next(r);
   }
  }},2200,TimeUnit.MILLISECONDS);
 }

 private void settle(Room r,String team,int price){
  Long id=r.currentId;if(id==null)return;
  if(team.equals("BLUE")){r.blueTokens-=price;r.blueTeam.add(new Pick(id,price,false));}
  else{r.redTokens-=price;r.redTeam.add(new Pick(id,price,false));}
  String buyer=hostName(r,team),item=name(r,id);
  log(r,team,item+" "+price+"원 낙찰");
  r.status="SOLD";r.deadline=null;r.turnTeam=null;
  event(r,"SOLD",team,buyer+" 낙찰!",item+" · "+price+"원",item,price);
  broadcast(r);
  r.currentId=null;
  timer.schedule(()->{synchronized(r){if(r.status.equals("SOLD")){r.status="RUNNING";next(r);}}},2600,TimeUnit.MILLISECONDS);
 }
 private String opponent(String team){return "BLUE".equals(team)?"RED":"BLUE";}
 private void event(Room r,String type,String team,String title,String message,String memberName,int price){r.eventSeq++;r.eventType=type;r.eventTeam=team;r.eventTitle=title;r.eventMessage=message;r.eventMemberName=memberName;r.eventPrice=price;}
 private String chooseAuto(Room r){int bn=5-r.blueTeam.size(),rn=5-r.redTeam.size();if(bn>rn)return "BLUE";if(rn>bn)return "RED";return r.blueTokens>=r.redTokens?"BLUE":"RED";}
 private void autoAssign(Room r){List<Long>left=new ArrayList<>();if(r.currentId!=null)left.add(r.currentId);left.addAll(r.queue);r.currentId=null;r.queue.clear();for(Long id:left){String t=r.blueTeam.size()>=5?"RED":r.redTeam.size()>=5?"BLUE":chooseAuto(r);if(t.equals("BLUE"))r.blueTeam.add(new Pick(id,10,false));else r.redTeam.add(new Pick(id,10,false));log(r,"SYSTEM",name(r,id)+" 자동 배정 → "+t);}finish(r);}
 private void finish(Room r){r.status="COMPLETE";r.deadline=null;r.turnTeam=null;event(r,"COMPLETE","SYSTEM","경매 종료!","최종 팀 구성이 완료되었습니다.",null,0);log(r,"SYSTEM","경매 완료");broadcast(r);}
 public Map<String,Object> state(String code){Room r=room(code);synchronized(r){return stateMap(r);}}
 private Map<String,Object> stateMap(Room r){Map<String,Object>m=new LinkedHashMap<>();m.put("roomCode",r.code);m.put("status",r.status);m.put("blueHostId",r.blueHostId);m.put("redHostId",r.redHostId);m.put("blueTokens",r.blueTokens);m.put("redTokens",r.redTokens);m.put("blueTeam",picks(r,r.blueTeam));m.put("redTeam",picks(r,r.redTeam));m.put("queue",memberList(r,r.queue));m.put("current",r.currentId==null?null:r.members.get(r.currentId));m.put("currentBid",r.currentBid);m.put("bidTeam",r.bidTeam);m.put("turnTeam",r.turnTeam);m.put("turnHostName",r.turnTeam==null?null:hostName(r,r.turnTeam));m.put("round",r.round);m.put("remainingSeconds",r.deadline==null?0:Math.max(0,Duration.between(Instant.now(),r.deadline).getSeconds()+1));m.put("connectedDirector",connected(r,"DIRECTOR"));m.put("connectedBlue",connected(r,"BLUE"));m.put("connectedRed",connected(r,"RED"));m.put("directorName","진행자");m.put("blueHostName",hostName(r,"BLUE"));m.put("redHostName",hostName(r,"RED"));m.put("spectatorCount",spectatorCount(r));m.put("startRemainingSeconds",r.startDeadline==null?0:Math.max(0,Duration.between(Instant.now(),r.startDeadline).getSeconds()+1));m.put("presenceText","진행자 "+(connected(r,"DIRECTOR")?"접속":"대기")+" · BLUE "+hostName(r,"BLUE")+" "+(connected(r,"BLUE")?"접속":"대기")+" · RED "+hostName(r,"RED")+" "+(connected(r,"RED")?"접속":"대기")+" · 관전자 "+spectatorCount(r)+"명");m.put("logs",r.logs);m.put("surrendered",r.surrendered);m.put("openingBid",r.openingBid);m.put("passedTeams",new ArrayList<>(r.passedTeams));m.put("minimumBid",10);m.put("blueMaxBid",maxBid(r,"BLUE"));m.put("redMaxBid",maxBid(r,"RED"));m.put("eventSeq",r.eventSeq);m.put("eventType",r.eventType);m.put("eventTeam",r.eventTeam);m.put("eventTitle",r.eventTitle);m.put("eventMessage",r.eventMessage);m.put("eventMemberName",r.eventMemberName);m.put("eventPrice",r.eventPrice);return m;}
 private List<Map<String,Object>>picks(Room r,List<Pick>ps){List<Map<String,Object>>o=new ArrayList<>();for(Pick p:ps){Map<String,Object>x=new LinkedHashMap<>(r.members.get(p.memberId));x.put("price",p.price);x.put("host",p.host);o.add(x);}return o;}
 private List<Map<String,Object>>memberList(Room r,List<Long>ids){List<Map<String,Object>>o=new ArrayList<>();for(Long id:ids)o.add(r.members.get(id));return o;}
 private int maxBid(Room r,String team){List<Pick>t=team.equals("BLUE")?r.blueTeam:r.redTeam;int tok=team.equals("BLUE")?r.blueTokens:r.redTokens;return Math.max(0,tok-Math.max(0,5-t.size()-1)*10);}
 private void broadcast(Room r){String json;try{json=om.writeValueAsString(stateMap(r));}catch(Exception e){return;}for(WebSocketSession s:new ArrayList<>(r.sessions.values()))try{if(s!=null&&s.isOpen())s.sendMessage(new TextMessage(json));}catch(Exception e){}}
 private void sendJoinRejected(WebSocketSession s,String role,String message){
  try{
   Map<String,Object>x=new LinkedHashMap<>();
   x.put("type","JOIN_REJECTED");x.put("role",role);x.put("message",message);
   s.sendMessage(new TextMessage(om.writeValueAsString(x)));
   s.close(CloseStatus.POLICY_VIOLATION.withReason("호스트 역할 사용 중"));
  }catch(Exception ignore){}
 }
 private String normRole(String role){String x=role==null?"SPECTATOR":role.trim().toUpperCase();return x.equals("DIRECTOR")||x.equals("BLUE")||x.equals("RED")?x:"SPECTATOR";}
 private String hostName(Room r,String role){Long id=role.equals("BLUE")?r.blueHostId:r.redHostId;return id==null?"-":name(r,id);}
 private String roleDisplayName(Room r,String role){if(role.equals("DIRECTOR"))return "진행자(DIRECTOR)";return hostName(r,role)+"("+role+")";}
 private int spectatorCount(Room r){int n=0;for(Map.Entry<String,WebSocketSession>e:r.sessions.entrySet())if(e.getKey().startsWith("SPECTATOR:")&&e.getValue()!=null&&e.getValue().isOpen())n++;return n;}
 private boolean connected(Room r,String role){WebSocketSession s=r.sessions.get(role);return s!=null&&s.isOpen();}private void ensureRunning(Room r){if(!r.status.equals("RUNNING"))throw new IllegalStateException("경매 진행 중이 아닙니다.");}private void host(String r){if(!r.equals("BLUE")&&!r.equals("RED"))throw new IllegalStateException("호스트만 조작할 수 있습니다.");}private void needRole(String r,String n){if(!r.equals(n))throw new IllegalStateException(n+" 호스트만 실행할 수 있습니다.");}
 private Room room(String c){Room r=rooms.get(norm(c));if(r==null)throw new IllegalArgumentException("경매방을 찾을 수 없습니다.");return r;}private String norm(String s){return s==null?"":s.trim().toUpperCase();}private String code(){String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";Random x=new Random();StringBuilder b=new StringBuilder();do{b.setLength(0);for(int i=0;i<6;i++)b.append(chars.charAt(x.nextInt(chars.length())));}while(rooms.containsKey(b.toString()));return b.toString();}
 private int score(Room r,Long id){Object x=r.members.get(id).get("balanceScore");return x instanceof Number?((Number)x).intValue():1000;}private String name(Room r,Long id){return String.valueOf(r.members.get(id).get("realName"));}private Long num(Object x){return x instanceof Number?((Number)x).longValue():null;}private int intv(Object x,int d){return x instanceof Number?((Number)x).intValue():d;}private double doublev(Object x,double d){return x instanceof Number?((Number)x).doubleValue():d;}private void log(Room r,String team,String msg){Map<String,Object>x=new LinkedHashMap<>();x.put("team",team);x.put("message",msg);x.put("time",LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));r.logs.add(x);if(r.logs.size()>100)r.logs.remove(0);}
 static class Pick{Long memberId;int price;boolean host;Pick(Long i,int p,boolean h){memberId=i;price=p;host=h;}}
 static class Room{String code,status="WAITING";Long blueHostId,redHostId,currentId;int blueTokens,redTokens,round,currentBid,eventPrice;long eventSeq;String bidTeam,turnTeam,surrendered,eventType,eventTeam,eventTitle,eventMessage,eventMemberName;boolean openingBid;Instant deadline,startDeadline;Map<Long,Map<String,Object>>members=new LinkedHashMap<>();List<Long>queue=new ArrayList<>();List<Pick>blueTeam=new ArrayList<>(),redTeam=new ArrayList<>();Map<String,WebSocketSession>sessions=new LinkedHashMap<>();Map<String,String>roleClientIds=new HashMap<>();Map<String,String>spectatorKeys=new HashMap<>();Set<String>passedTeams=new HashSet<>();List<Map<String,Object>>logs=new ArrayList<>();Room(String c,Long b,Long r,int base,int rate){code=c;blueHostId=b;redHostId=r;}}
}
