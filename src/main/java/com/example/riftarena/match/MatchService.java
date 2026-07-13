package com.example.riftarena.match;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.example.riftarena.member.*;
@Service
public class MatchService{
 private final MatchMapper mapper;private final MemberService members;
 public MatchService(MatchMapper mapper,MemberService members){this.mapper=mapper;this.members=members;}
 @SuppressWarnings("unchecked")
 @Transactional public Map<String,Object> create(Map<String,Object>p){
  List<Map<String,Object>> ps=(List<Map<String,Object>>)p.get("players");if(ps==null||ps.size()!=10)throw new IllegalArgumentException("참가자는 10명이어야 합니다.");
  String win=String.valueOf(p.get("winnerTeam")).toUpperCase();if(!win.equals("BLUE")&&!win.equals("RED"))throw new IllegalArgumentException("승리팀을 선택하세요.");
  Set<Long>uniq=new HashSet<>();List<Long>b=new ArrayList<>(),r=new ArrayList<>();int bc=0,rc=0,mvp=0;
  for(Map<String,Object>x:ps){Long id=((Number)x.get("memberId")).longValue();if(!uniq.add(id))throw new IllegalArgumentException("중복 멤버가 있습니다.");String team=String.valueOf(x.get("teamCode")).toUpperCase();if(team.equals("BLUE"))b.add(id);else if(team.equals("RED"))r.add(id);else throw new IllegalArgumentException("잘못된 팀 코드입니다.");if(Boolean.TRUE.equals(x.get("mvpYn")))mvp++;}
  if(b.size()!=5||r.size()!=5)throw new IllegalArgumentException("각 팀 5명이 필요합니다.");if(mvp>1)throw new IllegalArgumentException("MVP는 한 명만 선택할 수 있습니다.");
  for(Map<String,Object>m:members.byIds(b))bc+=((Number)m.get("balanceScore")).intValue();for(Map<String,Object>m:members.byIds(r))rc+=((Number)m.get("balanceScore")).intValue();
  if(p.get("seasonId")==null)p.put("seasonId",mapper.activeSeasonId());
  Object playedAtValue=p.get("playedAt");
  if(playedAtValue==null||String.valueOf(playedAtValue).trim().isEmpty())throw new IllegalArgumentException("경기 일시를 입력하세요.");
  try{p.put("playedAt",LocalDateTime.parse(String.valueOf(playedAtValue)));}
  catch(DateTimeParseException e){throw new IllegalArgumentException("경기 일시 형식이 올바르지 않습니다.");}
  p.put("blueCost",bc);p.put("redCost",rc);p.put("winnerTeam",win);mapper.insertMatch(p);Long mid=((Number)p.get("matchId")).longValue();
  for(Map<String,Object>x:ps){x.put("matchId",mid);x.put("winYn",win.equals(String.valueOf(x.get("teamCode")).toUpperCase()));mapper.insertPlayer(x);}
  return detail(mid);
 }
 public List<Map<String,Object>> list(Long seasonId){return mapper.list(seasonId);}
 public Map<String,Object> detail(Long id){Map<String,Object>m=mapper.find(id);if(m==null)throw new IllegalArgumentException("경기를 찾을 수 없습니다.");m.put("players",mapper.players(id));return m;}
 public List<Map<String,Object>> dashboard(Long seasonId){return mapper.dashboard(seasonId);}
 public List<Map<String,Object>> ranking(Long seasonId){return mapper.ranking(seasonId);}
 public List<Map<String,Object>> awards(Long seasonId){return mapper.awards(seasonId);}
}
