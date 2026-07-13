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
 public List<Map<String,Object>> byIds(List<Long> ids){return mapper.byIds(ids);}
 public List<Map<String,Object>> hall(Long seasonId){return mapper.hallOfFame(seasonId);}
 public Map<String,Object> lookup(String gameName,String tagLine){return riot.lookupAccount(normalizeGameName(gameName),normalizeTagLine(tagLine));} public boolean riotConfigured(){return riot.configured();}
 @SuppressWarnings("unchecked")
 @Transactional public Map<String,Object> save(Map<String,Object> p, Long id){
  String real=trim(p.get("realName"));
  boolean external=Boolean.TRUE.equals(p.get("externalYn"));
  String game=normalizeGameName(p.get("gameName"));
  String tag=normalizeTagLine(p.get("tagLine"));

  if(real.isEmpty())throw new IllegalArgumentException("이름을 입력해주세요.");
  if(external){
   if(game.isEmpty())game="EXTERNAL_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
   if(tag.isEmpty())tag="EXT";
   p.put("puuid",null);
  }else if(game.isEmpty()||tag.isEmpty()){
   throw new IllegalArgumentException("외부인이 아니라면 Riot ID를 입력해주세요.");
  }

  if(mapper.duplicate(game,tag,id)>0)throw new IllegalStateException("이미 등록된 Riot ID입니다.");

  @SuppressWarnings("unchecked")
  List<Map<String,Object>> laneProfiles=(List<Map<String,Object>>)p.get("laneProfiles");
  validateLaneProfiles(laneProfiles);

  p.put("realName",real);
  p.put("gameName",game);
  p.put("tagLine",tag);
  p.put("externalYn",external);
  p.put("manualTierYn",Boolean.TRUE.equals(p.get("manualTierYn"))||external);
  if(p.get("soloTier")==null)p.put("soloTier","UNRANKED");
  if(p.get("balanceScore")==null)p.put("balanceScore",rankScore((String)p.get("soloTier"),(String)p.get("soloRank"),num(p.get("soloLp"))));

  if(id==null){
   mapper.insert(p);
   id=((Number)p.get("memberId")).longValue();
  }else{
   p.put("memberId",id);
   mapper.update(p);
  }

  mapper.deleteLaneProfiles(id);
  for(Map<String,Object> profile:laneProfiles){
   Map<String,Object> row=new HashMap<>(profile);
   row.put("memberId",id);
   mapper.insertLaneProfile(row);
  }

  // 티어/라인 선호도/챔피언 수를 수동 변경하면 즉시 전체 점수를 다시 계산합니다.
  recalculateAllBalanceScores();
  return find(id);
 }

 private void validateLaneProfiles(List<Map<String,Object>> profiles){
  if(profiles==null||profiles.size()!=5)
   throw new IllegalArgumentException("탑/정글/미드/원딜/서폿 5개 라인 정보를 모두 입력해주세요.");

  Set<String> lanes=new HashSet<>();
  int primaryCount=0;
  int secondaryCount=0;
  for(Map<String,Object> profile:profiles){
   String lane=textValue(profile,"positionCode","position_code").toUpperCase(Locale.ROOT);
   int preference=intValue(profile,"preferenceScore","preference_score");
   int championCount=intValue(profile,"championCount","champion_count");

   if(!Arrays.asList("TOP","JUNGLE","MID","ADC","SUPPORT").contains(lane)||!lanes.add(lane))
    throw new IllegalArgumentException("라인 정보가 중복되었거나 잘못되었습니다.");
   if(preference<0||preference>2)
    throw new IllegalArgumentException("라인 선호도는 0, 1, 2만 가능합니다.");
   if(championCount<0)
    throw new IllegalArgumentException("기용 가능한 챔피언 수는 0 이상이어야 합니다.");
   if(preference==2)primaryCount++;
   if(preference==1)secondaryCount++;
  }
  if(primaryCount!=1)throw new IllegalArgumentException("1순위 라인은 정확히 하나만 선택해주세요.");
  if(secondaryCount>1)throw new IllegalArgumentException("2순위 라인은 최대 하나만 선택할 수 있습니다.");
 }
 @SuppressWarnings("unchecked")
 @Transactional public Map<String,Object> refreshRiot(Long id,boolean overwriteManualTier){
  Map<String,Object> member=find(id);
  if(Boolean.TRUE.equals(member.get("externalYn")))
   throw new IllegalArgumentException("외부인은 Riot API 갱신 대상이 아닙니다.");

  String gameName=normalizeGameName(firstValue(member,"gameName","game_name"));
  String tagLine=normalizeTagLine(firstValue(member,"tagLine","tag_line"));

  if(gameName.isEmpty()||tagLine.isEmpty()){
   throw new IllegalArgumentException(
     "Riot ID를 확인해주세요. gameName='"+gameName+"', tagLine='"+tagLine+"'"
   );
  }

  // 과거에 공백이 포함된 값이 저장되어 있어도 갱신 시 정규화합니다.
  Map<String,Object> data=riot.collectProfile(gameName,tagLine);
  data.put("memberId",id);
  boolean preserveManualTier=Boolean.TRUE.equals(member.get("manualTierYn"))&&!overwriteManualTier;
  data.put("preserveManualTier",preserveManualTier);
  data.put("balanceScore",rankScore((String)data.get("soloTier"),(String)data.get("soloRank"),num(data.get("soloLp"))));
  mapper.updateRiotProfile(data);
  mapper.insertRiotSnapshot(data);
  mapper.deleteChampionStats(id);

  List<Map<String,Object>> champions=(List<Map<String,Object>>)data.get("champions");
  if(champions!=null)for(Map<String,Object> c:champions){
   c.put("memberId",id);
   mapper.insertChampionStat(c);
  }

  recalculateBalanceScore(id);
  return detail(id,null);
 }

 public Map<String,Object> refreshAllRiot(Map<String,Object> request){
  @SuppressWarnings("unchecked")
  List<Number> selectedRaw=request==null?null:(List<Number>)request.get("memberIds");
  @SuppressWarnings("unchecked")
  List<Number> overwriteRaw=request==null?null:(List<Number>)request.get("overwriteTierMemberIds");

  Set<Long> selected=new LinkedHashSet<>();
  if(selectedRaw!=null)for(Number value:selectedRaw)selected.add(value.longValue());
  Set<Long> overwrite=new HashSet<>();
  if(overwriteRaw!=null)for(Number value:overwriteRaw)overwrite.add(value.longValue());

  List<Map<String,Object>> targets=list();
  if(!selected.isEmpty())targets.removeIf(member->!selected.contains(((Number)member.get("memberId")).longValue()));
  targets.removeIf(member->Boolean.TRUE.equals(member.get("externalYn")));

  List<Map<String,Object>> results=new ArrayList<>();
  int success=0,failed=0;
  for(Map<String,Object> member:targets){
   Long memberId=((Number)member.get("memberId")).longValue();
   String realName=String.valueOf(member.get("realName"));
   Map<String,Object> result=new LinkedHashMap<>();
   result.put("memberId",memberId);
   result.put("realName",realName);
   try{
    refreshRiot(memberId,overwrite.contains(memberId));
    result.put("success",true);
    result.put("message",overwrite.contains(memberId)?"Riot 티어까지 덮어씀":"수동 티어 보존 후 갱신");
    success++;
   }catch(Exception e){
    result.put("success",false);
    result.put("message",safeMessage(e));
    failed++;
   }
   results.add(result);
  }

  Map<String,Object> response=new LinkedHashMap<>();
  response.put("total",targets.size());
  response.put("successCount",success);
  response.put("failedCount",failed);
  response.put("results",results);
  return response;
 }
 public Map<String,Object> detail(Long id,Long seasonId){
  Map<String,Object> d=new LinkedHashMap<>(find(id));
  d.put("soloChampions",mapper.championStats(id));
  d.put("inhouseChampions",mapper.inhouseChampionStats(id,seasonId));
  d.put("synergy",mapper.synergy(id,seasonId));
  d.put("rivalry",mapper.rivalry(id,seasonId));
  d.put("recentMatches",mapper.recentMatches(id,seasonId));
  Map<String,Object> summary=mapper.inhouseSummary(id,seasonId);
  d.put("inhouseSummary",summary==null?Collections.emptyMap():summary);
  return d;
 }
 public void recalculateBalanceScores(Collection<Long> memberIds){
  if(memberIds==null||memberIds.isEmpty())return;
  recalculateAllBalanceScores();
 }

 @Transactional public int recalculateBalanceScore(Long id){
  recalculateAllBalanceScores();
  Map<String,Object> member=find(id);
  return intValue(member,"balanceScore","balance_score");
 }

 /**
  * 수정본 R 로직을 Java로 옮긴 전체 레이팅 재계산.
  * 1) tier_score + lane_score + champ_score로 최초 Elo 생성
  * 2) 통산 승률/KDA와 5개 항목 Outlier 보너스 반영
  * 3) 기존 경기를 played_at, match_id 순으로 재생
  * 4) 맞라이너 Elo와 양 팀 평균 Elo를 함께 비교하여 K=32 갱신
  */
 @Transactional public Map<String,Object> recalculateAllBalanceScores(){
  List<Map<String,Object>> active=list();
  Map<String,Object> result=new LinkedHashMap<>();
  if(active.isEmpty()){
   result.put("memberCount",0);
   result.put("matchCount",0);
   return result;
  }

  final String[] lanes={"TOP","JUNGLE","MID","ADC","SUPPORT"};
  Map<Long,Map<String,Integer>> champCounts=new HashMap<>();
  for(Map<String,Object> row:mapper.ratingChampionCounts()){
   Long memberId=longValue(row,"memberId","member_id");
   String lane=textValue(row,"positionCode","position_code").toUpperCase(Locale.ROOT);
   int count=intValue(row,"championCount","champion_count");
   champCounts.computeIfAbsent(memberId,k->new HashMap<>()).put(lane,count);
  }

  int n=active.size();
  double[] tier=new double[n];
  double[] laneScore=new double[n];
  double[] champScore=new double[n];
  double[] gameWinRate=new double[n];
  double[] gameKda=new double[n];

  Map<String,Integer> laneEligibleCount=new HashMap<>();
  for(String lane:lanes)laneEligibleCount.put(lane,0);

  List<Map<String,Integer>> preferenceByMember=new ArrayList<>();
  for(int i=0;i<n;i++){
   Map<String,Object> member=active.get(i);
   Map<String,Integer> preferences=new HashMap<>();
   for(String lane:lanes)preferences.put(lane,0);

   @SuppressWarnings("unchecked")
   List<Map<String,Object>> profiles=(List<Map<String,Object>>)member.get("laneProfiles");
   if(profiles!=null){
    for(Map<String,Object> profile:profiles){
     String lane=textValue(profile,"positionCode","position_code").toUpperCase(Locale.ROOT);
     int preference=intValue(profile,"preferenceScore","preference_score");
     if(preferences.containsKey(lane))preferences.put(lane,preference);
    }
   }
   preferenceByMember.add(preferences);
   for(String lane:lanes){
    if(preferences.get(lane)>0)laneEligibleCount.put(lane,laneEligibleCount.get(lane)+1);
   }
  }

  for(int i=0;i<n;i++){
   Map<String,Object> member=active.get(i);
   Long memberId=longValue(member,"memberId","member_id");
   tier[i]=initialTierScore(
     textValue(member,"soloTier","solo_tier"),
     textValue(member,"soloRank","solo_rank")
   );

   Map<String,Object> summary=mapper.inhouseSummary(memberId,null);
   if(summary==null)summary=Collections.emptyMap();

   int matchCount=intValue(summary,"matchCount","match_count");
   double winRate=doubleValue(summary,"winRate","win_rate");
   double avgKills=doubleValue(summary,"avgKills","avg_kills");
   double avgDeaths=doubleValue(summary,"avgDeaths","avg_deaths");
   double avgAssists=doubleValue(summary,"avgAssists","avg_assists");

   // 경기 기록이 없는 멤버는 수정본의 공통 초기값을 사용합니다.
   gameWinRate[i]=matchCount>0?winRate/100.0:0.5;
   gameKda[i]=matchCount>0?(avgKills+avgAssists)/Math.max(1.0,avgDeaths):3.0;

   double laneTotal=0.0;
   double champTotal=0.0;
   Map<String,Integer> preferences=preferenceByMember.get(i);
   Map<String,Integer> counts=champCounts.getOrDefault(memberId,Collections.emptyMap());

   for(String lane:lanes){
    int eligible=laneEligibleCount.get(lane);
    int indicator=preferences.get(lane);
    if(eligible>0)laneTotal+=(500.0/eligible)*indicator;

    int championCount=counts.getOrDefault(lane,0);
    champTotal+=championCount<=7
      ?championCount
      :7.0+Math.log(Math.max(championCount-6,1));
   }
   laneScore[i]=laneTotal;
   champScore[i]=champTotal;
  }

  int topCount=(int)Math.ceil(n*0.1);
  double tierCut=topThreshold(tier,topCount);
  double laneCut=topThreshold(laneScore,topCount);
  double champCut=topThreshold(champScore,topCount);
  double winRateCut=topThreshold(gameWinRate,topCount);
  double kdaCut=topThreshold(gameKda,topCount);

  double[] zWinRate=zScores(gameWinRate);
  double[] zKda=zScores(gameKda);
  Map<Long,Double> ratings=new LinkedHashMap<>();

  for(int i=0;i<n;i++){
   int outlier=0;
   if(isTopPercentile(tier[i],tierCut))outlier++;
   if(isTopPercentile(laneScore[i],laneCut))outlier++;
   if(isTopPercentile(champScore[i],champCut))outlier++;
   if(isTopPercentile(gameWinRate[i],winRateCut))outlier++;
   if(isTopPercentile(gameKda[i],kdaCut))outlier++;

   /*
    * 친구분 최종 설명 반영:
    * 1) 최초 기반값은 tier_score + lane_score + champ_score
    * 2) 데이터가 쌓인 뒤에는 통산 승률/KDA와 Outlier 보너스를 추가
    *
    * 승률/KDA는 서로 단위가 다르므로 z-score로 보정하고,
    * 기존 가중치 비율(승률 0.25 : KDA 0.15)을 100 : 60점으로 환산합니다.
    */
   double initialRating=
     tier[i]
     +laneScore[i]
     +champScore[i]
     +zWinRate[i]*100.0
     +zKda[i]*60.0
     +outlier*20.0;

   Long memberId=longValue(active.get(i),"memberId","member_id");
   ratings.put(memberId,initialRating);
  }

  Map<Long,List<Map<String,Object>>> playersByMatch=new LinkedHashMap<>();
  for(Map<String,Object> player:mapper.ratingPlayers()){
   Long matchId=longValue(player,"matchId","match_id");
   playersByMatch.computeIfAbsent(matchId,k->new ArrayList<>()).add(player);
  }

  int appliedMatches=0;
  for(Map<String,Object> match:mapper.ratingMatches()){
   Long matchId=longValue(match,"matchId","match_id");
   String winner=textValue(match,"winnerTeam","winner_team").toUpperCase(Locale.ROOT);
   if(!"BLUE".equals(winner)&&!"RED".equals(winner))continue;

   List<Map<String,Object>> players=playersByMatch.get(matchId);
   if(players==null||players.isEmpty())continue;

   Map<String,Long> blue=new HashMap<>();
   Map<String,Long> red=new HashMap<>();
   for(Map<String,Object> player:players){
    Long memberId=longValue(player,"memberId","member_id");
    String team=textValue(player,"teamCode","team_code").toUpperCase(Locale.ROOT);
    String lane=textValue(player,"positionCode","position_code").toUpperCase(Locale.ROOT);
    if(!ratings.containsKey(memberId))continue;
    if("BLUE".equals(team))blue.put(lane,memberId);
    else if("RED".equals(team))red.put(lane,memberId);
   }

   if(blue.size()!=5||red.size()!=5)continue;

   double blueTeamAverage=teamAverageRating(blue.values(),ratings);
   double redTeamAverage=teamAverageRating(red.values(),ratings);
   double expectedBlueTeam=expectedScore(blueTeamAverage,redTeamAverage);
   double blueResult="BLUE".equals(winner)?1.0:0.0;

   boolean applied=false;
   Map<Long,Double> nextRatings=new LinkedHashMap<>();

   for(String lane:lanes){
    Long blueId=blue.get(lane);
    Long redId=red.get(lane);
    if(blueId==null||redId==null)continue;

    double blueRating=ratings.get(blueId);
    double redRating=ratings.get(redId);

    // 맞라이너 Elo와 양 팀 평균 Elo를 반반씩 반영합니다.
    double expectedBlueLane=expectedScore(blueRating,redRating);
    double expectedBlue=(expectedBlueLane+expectedBlueTeam)/2.0;

    double blueDelta=32.0*(blueResult-expectedBlue);
    double redDelta=32.0*((1.0-blueResult)-(1.0-expectedBlue));

    nextRatings.put(blueId,blueRating+blueDelta);
    nextRatings.put(redId,redRating+redDelta);
    applied=true;
   }

   // 한 경기 안에서는 모든 라인이 경기 시작 전 점수를 기준으로 계산됩니다.
   if(applied){
    ratings.putAll(nextRatings);
    appliedMatches++;
   }
  }

  for(Map.Entry<Long,Double> entry:ratings.entrySet()){
   mapper.updateBalanceScore(entry.getKey(),(int)Math.round(entry.getValue()));
  }

  result.put("memberCount",ratings.size());
  result.put("matchCount",appliedMatches);
  result.put("message","초기 합산 점수, 통산 승률/KDA, 팀·맞라인 Elo를 반영해 전체 재계산했습니다.");
  return result;
 }

 private double expectedScore(double a,double b){
  return 1.0/(1.0+Math.pow(10.0,(b-a)/400.0));
 }

 private double teamAverageRating(Collection<Long> memberIds,Map<Long,Double> ratings){
  if(memberIds==null||memberIds.isEmpty())return 0.0;
  double total=0.0;
  int count=0;
  for(Long memberId:memberIds){
   Double rating=ratings.get(memberId);
   if(rating!=null){
    total+=rating;
    count++;
   }
  }
  return count==0?0.0:total/count;
 }

 private int initialTierScore(String tier,String rank){
  String t=tier==null?"UNRANKED":tier.toUpperCase(Locale.ROOT);
  Map<String,Integer> bases=new HashMap<>();
  bases.put("IRON",0);bases.put("BRONZE",50);bases.put("SILVER",100);
  bases.put("GOLD",200);bases.put("PLATINUM",300);bases.put("EMERALD",400);
  bases.put("DIAMOND",500);bases.put("MASTER",600);bases.put("GRANDMASTER",700);
  bases.put("CHALLENGER",800);bases.put("UNRANKED",100);

  Map<String,Integer> ranks=new HashMap<>();
  ranks.put("IV",0);ranks.put("III",25);ranks.put("II",50);ranks.put("I",75);
  return bases.getOrDefault(t,100)+ranks.getOrDefault(rank,0);
 }

 private double[] zScores(double[] values){
  double[] out=new double[values.length];
  if(values.length<2)return out;

  double mean=0.0;
  for(double value:values)mean+=value;
  mean/=values.length;

  double sum=0.0;
  for(double value:values){
   double diff=value-mean;
   sum+=diff*diff;
  }
  double sd=Math.sqrt(sum/(values.length-1));
  if(sd==0.0||Double.isNaN(sd))return out;

  for(int i=0;i<values.length;i++)out[i]=(values[i]-mean)/sd;
  return out;
 }

 private double topThreshold(double[] values,int topCount){
  double[] copy=Arrays.copyOf(values,values.length);
  Arrays.sort(copy);
  int index=Math.max(0,copy.length-Math.max(1,topCount));
  return copy[index];
 }

 /**
  * R의 is_top_percentile(value, n_top)와 같은 용도입니다.
  * 컷오프와 동점인 멤버도 모두 상위 그룹에 포함합니다.
  */
 private boolean isTopPercentile(double value,double threshold){
  return Double.compare(value,threshold)>=0;
 }

 private Long longValue(Map<String,Object> map,String...keys){
  Object value=firstValue(map,keys);
  if(value instanceof Number)return ((Number)value).longValue();
  return value==null?null:Long.valueOf(String.valueOf(value));
 }

 @Transactional public void delete(Long id){find(id);mapper.deactivate(id);}
 private int rankScore(String tier,String rank,int lp){Map<String,Integer> t=new HashMap<>();String[] n={"UNRANKED","IRON","BRONZE","SILVER","GOLD","PLATINUM","EMERALD","DIAMOND","MASTER","GRANDMASTER","CHALLENGER"};int[]v={800,700,850,1000,1150,1300,1450,1600,1800,1950,2100};for(int i=0;i<n.length;i++)t.put(n[i],v[i]);Map<String,Integer>r=new HashMap<>();r.put("IV",0);r.put("III",35);r.put("II",70);r.put("I",105);return t.getOrDefault(tier,800)+r.getOrDefault(rank,0)+Math.max(0,Math.min(lp,100));}
 private String trim(Object o){return o==null?"":String.valueOf(o).trim();}
 private Object firstValue(Map<String,Object> map,String...keys){
  for(String key:keys){
   Object value=map.get(key);
   if(value!=null)return value;
  }
  return null;
 }
 private String normalizeGameName(Object o){
  if(o==null)return "";
  // Riot gameName 안쪽 공백은 실제 ID의 일부이므로 유지합니다.
  return String.valueOf(o)
    .replace('\u00A0',' ')
    .replace("\u200B","")
    .replace("\uFEFF","")
    .trim();
 }
 private String normalizeTagLine(Object o){
  if(o==null)return "";
  // tagLine은 공백을 허용하지 않으므로 모든 공백을 제거합니다.
  return String.valueOf(o)
    .replace("\u00A0","")
    .replace("\u200B","")
    .replace("\uFEFF","")
    .replaceAll("\\s+","")
    .toUpperCase(Locale.ROOT);
 }
 private String textValue(Map<String,Object> map,String...keys){
  Object value=firstValue(map,keys);
  return value==null?"":String.valueOf(value);
 }
 private int intValue(Map<String,Object> map,String...keys){
  if(map==null)return 0;
  Object value=firstValue(map,keys);
  if(value instanceof Number)return ((Number)value).intValue();
  try{return value==null?0:Integer.parseInt(String.valueOf(value));}
  catch(Exception e){return 0;}
 }
 private double doubleValue(Map<String,Object> map,String...keys){
  if(map==null)return 0.0;
  Object value=firstValue(map,keys);
  if(value instanceof Number)return ((Number)value).doubleValue();
  try{return value==null?0.0:Double.parseDouble(String.valueOf(value));}
  catch(Exception e){return 0.0;}
 }
 private double clamp(double value,double min,double max){
  return Math.max(min,Math.min(max,value));
 }
 private String safeMessage(Exception e){
  Throwable t=e;
  while(t.getCause()!=null)t=t.getCause();
  String m=t.getMessage();
  return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;
 }
 private int num(Object o){return o instanceof Number?((Number)o).intValue():0;}
}
