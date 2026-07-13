package com.example.riftarena.member;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class RiotApiService {
    private final RestTemplate rest = new RestTemplate();

    @Value("${riot.api.key:}") private String apiKey;
    @Value("${riot.api.account-base-url:https://asia.api.riotgames.com}") private String accountBaseUrl;
    @Value("${riot.api.platform-base-url:https://kr.api.riotgames.com}") private String platformBaseUrl;
    @Value("${riot.api.match-base-url:https://asia.api.riotgames.com}") private String matchBaseUrl;
    @Value("${riot.api.recent-match-count:20}") private int recentMatchCount;

    public boolean configured(){ return StringUtils.hasText(apiKey); }

    public Map<String,Object> lookupAccount(String gameName,String tagLine){
        requireKey();
        String url=accountBaseUrl+"/riot/account/v1/accounts/by-riot-id/"+enc(gameName)+"/"+enc(tagLine);
        return getMap(url);
    }

    public Map<String,Object> collectProfile(String gameName,String tagLine){
        Map<String,Object> account=lookupAccount(gameName,tagLine);
        String puuid=str(account.get("puuid"));
        if(puuid.isEmpty()) throw new IllegalStateException("Riot 응답에 PUUID가 없습니다.");

        Map<String,Object> summoner=getMap(platformBaseUrl+"/lol/summoner/v4/summoners/by-puuid/"+enc(puuid));
        String summonerId=str(summoner.get("id"));

        // League-V4의 최신 PUUID 기반 조회 경로를 사용합니다.
        List<Map<String,Object>> leagues=getList(platformBaseUrl+"/lol/league/v4/entries/by-puuid/"+enc(puuid));
        Map<String,Object> solo=null;
        for(Map<String,Object> entry:leagues){
            if("RANKED_SOLO_5x5".equals(str(entry.get("queueType")))){ solo=entry; break; }
        }

        // 솔로랭크가 있으면 솔랭 최근 전적을 사용합니다.
        // 솔로랭크가 없으면 일반 소환사의 협곡 전적(드래프트/일반/스위프트/빠른 대전)으로 대체합니다.
        List<String> matchIds;
        String recentMatchScope;
        if(solo!=null){
            matchIds=getStringList(matchBaseUrl+"/lol/match/v5/matches/by-puuid/"+enc(puuid)
                    +"/ids?queue=420&start=0&count="+recentMatchCount);
            recentMatchScope="RANKED_SOLO";
        }else{
            matchIds=getNormalMatchIds(puuid);
            recentMatchScope="NORMAL";
        }

        List<Map<String,Object>> matches=new ArrayList<>();
        for(String matchId:matchIds){
            try{ matches.add(getMap(matchBaseUrl+"/lol/match/v5/matches/"+enc(matchId))); }
            catch(RuntimeException e){
                System.err.println("[Riot API] 경기 상세 조회 실패 - matchId="+matchId+", message="+e.getMessage());
            }
        }

        int wins=0,losses=0,kills=0,deaths=0,assists=0;
        Map<String,Integer> champGames=new HashMap<>(), champWins=new HashMap<>();
        Map<String,Integer> positionGames=new HashMap<>();

        for(Map<String,Object> match:matches){
            Map<String,Object> info=map(match.get("info"));
            List<Map<String,Object>> participants=listOfMap(info.get("participants"));
            Map<String,Object> me=null;
            for(Map<String,Object> p:participants){ if(puuid.equals(str(p.get("puuid")))){me=p;break;} }
            if(me==null) continue;
            boolean win=bool(me.get("win")); if(win)wins++;else losses++;
            kills+=num(me.get("kills")); deaths+=num(me.get("deaths")); assists+=num(me.get("assists"));
            String champ=str(me.get("championName"));
            if(!champ.isEmpty()){
                champGames.put(champ,champGames.getOrDefault(champ,0)+1);
                if(win)champWins.put(champ,champWins.getOrDefault(champ,0)+1);
            }
            String pos=normalizePosition(str(me.get("teamPosition")));
            if(!pos.isEmpty()) positionGames.put(pos,positionGames.getOrDefault(pos,0)+1);
        }

        int games=wins+losses;
        List<Map<String,Object>> champions=new ArrayList<>();
        List<Map.Entry<String,Integer>> sorted=new ArrayList<>(champGames.entrySet());
        sorted.sort((a,b)->Integer.compare(b.getValue(),a.getValue()));
        int rankNo=1;
        for(Map.Entry<String,Integer> e:sorted){
            if(rankNo>3) break;
            Map<String,Object> c=new LinkedHashMap<>();
            c.put("championName",e.getKey()); c.put("games",e.getValue()); c.put("wins",champWins.getOrDefault(e.getKey(),0)); c.put("rankNo",rankNo++);
            champions.add(c);
        }
        String mostPosition=positionGames.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Map<String,Object> result=new LinkedHashMap<>();
        result.put("puuid",puuid); result.put("gameName",account.get("gameName")); result.put("tagLine",account.get("tagLine"));
        result.put("summonerId",summonerId); result.put("profileIconId",summoner.get("profileIconId")); result.put("summonerLevel",summoner.get("summonerLevel"));
        result.put("soloTier",solo==null?"UNRANKED":solo.get("tier")); result.put("soloRank",solo==null?null:solo.get("rank")); result.put("soloLp",solo==null?0:solo.get("leaguePoints"));
        result.put("recentWins",wins); result.put("recentLosses",losses); result.put("recentWinRate",games==0?0.0:round(wins*100.0/games));
        result.put("recentAvgKills",games==0?0.0:round(kills*1.0/games)); result.put("recentAvgDeaths",games==0?0.0:round(deaths*1.0/games)); result.put("recentAvgAssists",games==0?0.0:round(assists*1.0/games));
        result.put("mostPosition",mostPosition); result.put("champions",champions);
        result.put("recentMatchScope",recentMatchScope);
        result.put("collectedAt",LocalDateTime.now().toString());
        return result;
    }

    private List<String> getNormalMatchIds(String puuid){
        // Riot 공식 Queue ID:
        // 400 Draft Pick, 430 Blind Pick, 480 Swiftplay, 490 Normal Quickplay
        int[] normalQueues={400,430,480,490};
        Set<String> uniqueIds=new LinkedHashSet<>();
        for(int queueId:normalQueues){
            String url=matchBaseUrl+"/lol/match/v5/matches/by-puuid/"+enc(puuid)
                    +"/ids?queue="+queueId+"&start=0&count="+recentMatchCount;
            try{
                uniqueIds.addAll(getStringList(url));
            }catch(RuntimeException e){
                System.err.println("[Riot API] 일반게임 목록 조회 실패 - queueId="+queueId
                        +", message="+e.getMessage());
            }
        }

        List<String> sorted=new ArrayList<>(uniqueIds);
        sorted.sort((a,b)->Long.compare(matchSequence(b),matchSequence(a)));
        if(sorted.size()>recentMatchCount){
            return new ArrayList<>(sorted.subList(0,recentMatchCount));
        }
        return sorted;
    }

    private long matchSequence(String matchId){
        if(matchId==null) return 0L;
        int idx=matchId.lastIndexOf('_');
        String number=idx>=0?matchId.substring(idx+1):matchId;
        try{return Long.parseLong(number);}
        catch(NumberFormatException e){return 0L;}
    }

    private void requireKey(){ if(!configured()) throw new IllegalStateException("RIOT_API_KEY가 없습니다. Riot Developer Portal에서 키를 발급한 뒤 환경변수로 등록해주세요."); }
    private HttpHeaders headers(){ HttpHeaders h=new HttpHeaders(); h.set("X-Riot-Token",apiKey); return h; }
    @SuppressWarnings("unchecked") private Map<String,Object> getMap(String url){ try{return rest.exchange(URI.create(url),HttpMethod.GET,new HttpEntity<Void>(headers()),Map.class).getBody();}catch(HttpClientErrorException.NotFound e){throw new IllegalArgumentException("Riot ID 또는 전적 정보를 찾지 못했습니다.");}catch(HttpClientErrorException.Unauthorized|HttpClientErrorException.Forbidden e){throw new IllegalStateException("Riot API 키가 만료되었거나 권한이 없습니다.");}catch(HttpClientErrorException.TooManyRequests e){throw new IllegalStateException("Riot API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");} }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> getList(String url){ try{List<?> raw=rest.exchange(URI.create(url),HttpMethod.GET,new HttpEntity<Void>(headers()),List.class).getBody(); return raw==null?Collections.emptyList():(List<Map<String,Object>>)(List<?>)raw;}catch(HttpClientErrorException e){throw new IllegalStateException("Riot 랭크 조회에 실패했습니다: "+e.getStatusCode());} }
    @SuppressWarnings("unchecked") private List<String> getStringList(String url){ try{List<?> raw=rest.exchange(URI.create(url),HttpMethod.GET,new HttpEntity<Void>(headers()),List.class).getBody(); if(raw==null)return Collections.emptyList(); List<String> out=new ArrayList<>(); for(Object x:raw)out.add(String.valueOf(x)); return out;}catch(HttpClientErrorException e){throw new IllegalStateException("Riot 최근 전적 조회에 실패했습니다: "+e.getStatusCode());} }
    private String enc(String x){return URLEncoder.encode(x==null?"":x,StandardCharsets.UTF_8);} private String str(Object x){return x==null?"":String.valueOf(x);} private int num(Object x){return x instanceof Number?((Number)x).intValue():0;} private boolean bool(Object x){return Boolean.TRUE.equals(x);} private double round(double x){return Math.round(x*10.0)/10.0;}
    @SuppressWarnings("unchecked") private Map<String,Object> map(Object x){return x instanceof Map?(Map<String,Object>)x:Collections.emptyMap();}
    @SuppressWarnings("unchecked") private List<Map<String,Object>> listOfMap(Object x){return x instanceof List?(List<Map<String,Object>>)(List<?>)x:Collections.emptyList();}
    private String normalizePosition(String p){ if("UTILITY".equals(p))return "SUPPORT"; if("MIDDLE".equals(p))return "MID"; if("BOTTOM".equals(p))return "ADC"; return p; }
}
