package com.example.riftarena.member;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class MemberService {
 private final MemberMapper mapper; private final RiotApiService riot;
 private static final Set<String> POS=new HashSet<>(Arrays.asList("TOP","JUNGLE","MID","ADC","SUPPORT","FILL"));
 public MemberService(MemberMapper mapper,RiotApiService riot){this.mapper=mapper;this.riot=riot;}
 public List<Map<String,Object>> list(){return mapper.list();}
 public Map<String,Object> find(Long id){Map<String,Object> m=mapper.find(id); if(m==null) throw new IllegalArgumentException("멤버를 찾을 수 없습니다."); return m;}
 public List<Map<String,Object>> byIds(List<Long> ids){return mapper.byIds(ids);} public List<Map<String,Object>> hall(){return mapper.hallOfFame();}
 public Map<String,Object> lookup(String gameName,String tagLine){return riot.lookupAccount(gameName,tagLine);} public boolean riotConfigured(){return riot.configured();}
 @Transactional public Map<String,Object> save(Map<String,Object> p, Long id){
  String real=trim(p.get("realName")), game=trim(p.get("gameName")), tag=trim(p.get("tagLine")).toUpperCase();
  if(real.isEmpty()||game.isEmpty()||tag.isEmpty()) throw new IllegalArgumentException("이름과 Riot ID를 입력해주세요.");
  if(mapper.duplicate(game,tag,id)>0) throw new IllegalStateException("이미 등록된 Riot ID입니다.");
  List<String> positions=(List<String>)p.get("preferredPositions"); if(positions==null||positions.isEmpty()||positions.size()>3) throw new IllegalArgumentException("선호 포지션은 1~3개 선택해주세요.");
  for(String pos:positions) if(!POS.contains(pos)) throw new IllegalArgumentException("잘못된 포지션입니다.");
  p.put("realName",real);p.put("gameName",game);p.put("tagLine",tag); if(p.get("soloTier")==null)p.put("soloTier","UNRANKED");
  if(p.get("balanceScore")==null)p.put("balanceScore",rankScore((String)p.get("soloTier"),(String)p.get("soloRank"),num(p.get("soloLp"))));
  if(id==null){mapper.insert(p);id=((Number)p.get("memberId")).longValue();}else{p.put("memberId",id);mapper.update(p);} mapper.deletePositions(id);int i=1;for(String pos:positions)mapper.insertPosition(id,pos,i++);return find(id);
 }
 @Transactional public Map<String,Object> refreshRiot(Long id){
  Map<String,Object> member=find(id); Map<String,Object> data=riot.collectProfile(String.valueOf(member.get("gameName")),String.valueOf(member.get("tagLine")));
  data.put("memberId",id); data.put("balanceScore",rankScore((String)data.get("soloTier"),(String)data.get("soloRank"),num(data.get("soloLp")))); mapper.updateRiotProfile(data); mapper.insertRiotSnapshot(data); mapper.deleteChampionStats(id);
  List<Map<String,Object>> champions=(List<Map<String,Object>>)data.get("champions"); if(champions!=null)for(Map<String,Object> c:champions){c.put("memberId",id);mapper.insertChampionStat(c);} return detail(id);
 }
 public Map<String,Object> detail(Long id){Map<String,Object> d=new LinkedHashMap<>(find(id));d.put("soloChampions",mapper.championStats(id));d.put("inhouseChampions",mapper.inhouseChampionStats(id));List<Map<String,Object>> syn=mapper.synergy(id);d.put("synergy",syn);d.put("recentMatches",mapper.recentMatches(id));return d;}
 @Transactional public void delete(Long id){find(id);mapper.deactivate(id);}
 private int rankScore(String tier,String rank,int lp){Map<String,Integer> t=new HashMap<>();String[] n={"UNRANKED","IRON","BRONZE","SILVER","GOLD","PLATINUM","EMERALD","DIAMOND","MASTER","GRANDMASTER","CHALLENGER"};int[]v={800,700,850,1000,1150,1300,1450,1600,1800,1950,2100};for(int i=0;i<n.length;i++)t.put(n[i],v[i]);Map<String,Integer>r=new HashMap<>();r.put("IV",0);r.put("III",35);r.put("II",70);r.put("I",105);return t.getOrDefault(tier,800)+r.getOrDefault(rank,0)+Math.max(0,Math.min(lp,100));}
 private String trim(Object o){return o==null?"":String.valueOf(o).trim();} private int num(Object o){return o instanceof Number?((Number)o).intValue():0;}
}
