package com.example.riftarena.match;
import java.time.LocalDateTime;import java.time.format.DateTimeParseException;import java.util.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import com.example.riftarena.member.*;import com.example.riftarena.champion.*;import com.example.riftarena.balance.*;
@Service
public class MatchService{
 private final MatchMapper mapper;private final MemberService members;private final ChampionService champions;private final BalanceTuningService balance;
 public MatchService(MatchMapper mapper,MemberService members,ChampionService champions,BalanceTuningService balance){this.mapper=mapper;this.members=members;this.champions=champions;this.balance=balance;}
 @Transactional public Map<String,Object> create(Map<String,Object>p){Prepared x=prepare(p);p.putAll(x.match);mapper.insertMatch(p);Long id=((Number)p.get("matchId")).longValue();insertPlayers(id,x.players,x.winner);balance.saveFeature(id,p.get("balanceFeature"));members.recalculateAllBalanceScores();return detail(id);}
 @Transactional public Map<String,Object> update(Long id,Map<String,Object>p){detail(id);Prepared x=prepare(p);x.match.put("matchId",id);x.match.put("updatedBy",str(p.get("updatedBy"),"사용자"));x.match.put("updateReason",str(p.get("updateReason"),"경기 기록 수정"));mapper.updateMatch(x.match);mapper.deletePlayers(id);insertPlayers(id,x.players,x.winner);mapper.insertChangeLog(x.match);balance.saveFeature(id,p.get("balanceFeature"));members.recalculateAllBalanceScores();return detail(id);}
 @SuppressWarnings("unchecked") private Prepared prepare(Map<String,Object>p){List<Map<String,Object>>ps=(List<Map<String,Object>>)p.get("players");if(ps==null||ps.size()!=10)throw new IllegalArgumentException("참가자는 10명이어야 합니다.");String win=String.valueOf(p.get("winnerTeam")).toUpperCase();if(!win.equals("BLUE")&&!win.equals("RED"))throw new IllegalArgumentException("승리팀을 선택하세요.");Set<Long>u=new HashSet<>();List<Long>b=new ArrayList<>(),r=new ArrayList<>();int mvp=0;for(Map<String,Object>x:ps){Object raw=x.get("memberId");if(!(raw instanceof Number))throw new IllegalArgumentException("멤버를 선택하세요.");Long id=((Number)raw).longValue();if(!u.add(id))throw new IllegalArgumentException("중복 멤버가 있습니다.");String t=String.valueOf(x.get("teamCode")).toUpperCase();if(t.equals("BLUE"))b.add(id);else if(t.equals("RED"))r.add(id);else throw new IllegalArgumentException("잘못된 팀 코드입니다.");if(Boolean.TRUE.equals(x.get("mvpYn")))mvp++;
 Map<String,Object> champion=champions.resolve(String.valueOf(x.get("championName")));
 x.put("championCode",champion.get("championCode"));
 x.put("championName",champion.get("nameEn"));
 }if(b.size()!=5||r.size()!=5)throw new IllegalArgumentException("각 팀 5명이 필요합니다.");if(mvp>1)throw new IllegalArgumentException("MVP는 한 명만 선택할 수 있습니다.");int bc=cost(b),rc=cost(r);Object seasonId=p.get("seasonId");
 if(seasonId==null||String.valueOf(seasonId).trim().isEmpty())seasonId=mapper.activeSeasonId();
 if(seasonId==null)throw new IllegalStateException("활성 시즌이 없습니다. 먼저 시즌을 생성하거나 활성화해주세요.");
 p.put("seasonId",seasonId);
 Object at=p.get("playedAt");if(at==null||String.valueOf(at).trim().isEmpty())throw new IllegalArgumentException("경기 일시를 입력하세요.");LocalDateTime dt;try{dt=LocalDateTime.parse(String.valueOf(at));}catch(DateTimeParseException e){throw new IllegalArgumentException("경기 일시 형식이 올바르지 않습니다.");}Map<String,Object>m=new HashMap<>();m.put("seasonId",p.get("seasonId"));m.put("playedAt",dt);m.put("winnerTeam",win);m.put("memo",p.get("memo"));m.put("blueCost",bc);m.put("redCost",rc);m.put("forceYn",Boolean.TRUE.equals(p.get("forceYn")));m.put("updatedBy",str(p.get("updatedBy"),"사용자"));m.put("updateReason",str(p.get("updateReason"),""));return new Prepared(m,ps,win);}
 private List<Long> playerIds(List<Map<String,Object>> players){
  List<Long> ids=new ArrayList<>();
  for(Map<String,Object> player:players){
   Object raw=player.get("memberId");
   if(raw instanceof Number)ids.add(((Number)raw).longValue());
  }
  return ids;
 }
 private int cost(List<Long>ids){int c=0;for(Map<String,Object>m:members.byIds(ids))c+=((Number)m.get("balanceScore")).intValue();return c;}private void insertPlayers(Long id,List<Map<String,Object>>ps,String win){for(Map<String,Object>x:ps){x.put("matchId",id);x.put("winYn",win.equals(String.valueOf(x.get("teamCode")).toUpperCase()));mapper.insertPlayer(x);}}
 public List<Map<String,Object>> list(Long seasonId){return mapper.list(seasonId);}public Map<String,Object> detail(Long id){Map<String,Object>m=mapper.find(id);if(m==null)throw new IllegalArgumentException("경기를 찾을 수 없습니다.");m.put("players",mapper.players(id));return m;}public List<Map<String,Object>> dashboard(Long s){return mapper.dashboard(s);}public List<Map<String,Object>> ranking(Long s){return mapper.ranking(s);}public List<Map<String,Object>> awards(Long s){return mapper.awards(s);}public List<Map<String,Object>> recentModified(){return mapper.recentModified();}private String str(Object x,String d){String s=x==null?"":String.valueOf(x).trim();return s.isEmpty()?d:s;}static class Prepared{Map<String,Object>match;List<Map<String,Object>>players;String winner;Prepared(Map<String,Object>m,List<Map<String,Object>>p,String w){match=m;players=p;winner=w;}}
}
