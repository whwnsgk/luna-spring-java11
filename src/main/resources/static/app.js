const S={members:[],selected:[],blue:[],red:[],matches:[],seasons:[],seasonId:null,activeSeasonId:null,drag:null,discord:false,auction:null};
const POS=["TOP","JUNGLE","MID","ADC","SUPPORT","FILL"],PN={TOP:"탑",JUNGLE:"정글",MID:"미드",ADC:"원딜",SUPPORT:"서폿",FILL:"올라운더"};
document.addEventListener("DOMContentLoaded",async()=>{nav();modals();events();positions.innerHTML=POS.map(x=>`<label><input type="checkbox" name="pos" value="${x}"> ${PN[x]}</label>`).join("");await loadSeasons();await all();await discordState();dateNow()});
function nav(){document.querySelectorAll("[data-page]").forEach(b=>b.onclick=()=>{document.querySelectorAll(".page").forEach(p=>p.classList.remove("active"));document.querySelectorAll(".nav-btn").forEach(n=>n.classList.toggle("active",n.dataset.page===b.dataset.page));document.getElementById(b.dataset.page).classList.add("active");window.scrollTo({top:0,behavior:"smooth"})})}
function modals(){document.querySelectorAll(".x,.modal-cancel").forEach(x=>x.onclick=()=>x.closest(".modal").classList.remove("open"));document.querySelectorAll(".modal").forEach(m=>m.onclick=e=>{if(e.target===m)m.classList.remove("open")})}
function events(){
 addMember.onclick=()=>{memberForm.reset();mid.value="";puuid.value="";memberModal.classList.add("open")};
 bulkRiotRefresh.onclick=refreshAllRiotMembers;
 memberForm.onsubmit=saveMember;riotLookup.onclick=lookup;
 clear.onclick=()=>{S.selected=[];S.blue=[];S.red=[];drawDraft();report()};
 auto.onclick=balance;record.onclick=()=>openMatch(S.blue,S.red);discordCopy.onclick=copyDiscord;discordSend.onclick=sendDiscord;matchForm.onsubmit=saveMatch;forceRecord.onclick=openForceMatch;editMatchButton.onclick=editCurrentMatch;
 seasonFilter.onchange=async()=>{S.seasonId=seasonFilter.value?+seasonFilter.value:null;await refreshSeasonData()};
 manageSeason.onclick=()=>{renderSeasonList();seasonModal.classList.add("open")};seasonForm.onsubmit=createSeason;
 const goAuction=()=>{loadAuctionPlayersFromDraft();openPage("auction")};
 openAuctionFromDraft.onclick=goAuction;
 openAuctionQuick.onclick=goAuction;
 auctionLoadPlayers.onclick=loadAuctionPlayersFromDraft;
 auctionReset.onclick=resetAuction;
 auctionStart.onclick=createRealtimeRoom;
 auctionJoin.onclick=joinRealtimeRoom;
 liveAuctionStart.onclick=()=>wsAction("START");
 copyRoomCode.onclick=async()=>{if(auctionRoom){await navigator.clipboard.writeText(auctionRoom);toast("방 코드를 복사했습니다.")}};
 auctionHammer.onclick=()=>wsAction("START");
 auctionUnsold.onclick=()=>toast("포기 버튼으로 상대에게 즉시 넘길 수 있습니다.");
 blueSurrender.onclick=()=>wsAction("SURRENDER");redSurrender.onclick=()=>wsAction("SURRENDER");
 blueAppealSend.onclick=()=>wsAction("APPEAL",{message:blueAppealInput.value});redAppealSend.onclick=()=>wsAction("APPEAL",{message:redAppealInput.value});
 soundToggle.onclick=toggleSound;
 soundVolume.oninput=e=>AuctionSound.setVolume(+e.target.value/100);
 soundTest.onclick=()=>AuctionSound.testSequence();
 bgmToggle.onclick=()=>AuctionBgm.toggle();
 bgmVolume.oninput=e=>AuctionBgm.setVolume(+e.target.value/100);
 document.addEventListener("click",()=>AuctionBgm.unlock(),{once:true});
 updateBgmUi();
 auctionGoDraft.onclick=()=>openPage("draft");
 auctionRecordMatch.onclick=()=>openMatch(S.blue,S.red);
 auctionCopyDiscord.onclick=copyDiscord;
 document.querySelectorAll(".bid-btn").forEach(b=>b.onclick=()=>placeAuctionBid(b.dataset.team,b.dataset.add));
 document.querySelectorAll(".drop").forEach(z=>{z.ondragover=e=>e.preventDefault();z.ondrop=e=>{e.preventDefault();move(S.drag,z.id)}})
}
async function all(){await Promise.all([loadMembers(),loadMatches(),dashboard(),ranking()])}
async function refreshSeasonData(){await Promise.all([loadMatches(),dashboard(),ranking()]);if(memberDetailModal.classList.contains("open")&&memberDetail.dataset.memberId)await memberDetailView(+memberDetail.dataset.memberId)}
function qs(){return S.seasonId?`?seasonId=${S.seasonId}`:""}
async function loadSeasons(){S.seasons=await api("/api/seasons");let active=S.seasons.find(x=>x.activeYn);S.activeSeasonId=active?+active.seasonId:null;if(S.seasonId==null)S.seasonId=S.activeSeasonId;seasonFilter.innerHTML=`<option value="">전체 통산</option>`+S.seasons.map(x=>`<option value="${x.seasonId}" ${+x.seasonId===S.seasonId?"selected":""}>${esc(x.seasonName)}${x.activeYn?" · 진행 중":""}</option>`).join("");matchSeason.innerHTML=S.seasons.map(x=>`<option value="${x.seasonId}" ${x.activeYn?"selected":""}>${esc(x.seasonName)}</option>`).join("")}
function renderSeasonList(){seasonList.innerHTML=S.seasons.length?S.seasons.map(x=>`<div class="season-row"><div><b>${esc(x.seasonName)}</b><small>${x.startDate||"-"} ~ ${x.endDate||"진행 중"}</small></div>${x.activeYn?`<span class="active-season">활성</span>`:`<button class="btn btn-ghost btn-small" onclick="activateSeason(${x.seasonId})">활성화</button>`}</div>`).join(""):`<div class="empty">시즌이 없습니다.</div>`}
window.activateSeason=async id=>{await api(`/api/seasons/${id}/activate`,{method:"PUT"});toast("활성 시즌을 변경했습니다.");await loadSeasons();renderSeasonList();await refreshSeasonData()};
async function createSeason(e){
 e.preventDefault();
 const name=seasonName.value.trim();
 const start=seasonStart.value;
 const end=seasonEnd.value||null;
 if(!name){toast("시즌 이름을 입력해주세요.");return}
 if(!start){toast("시작일을 입력해주세요.");return}
 if(end&&end<start){toast("종료일은 시작일보다 빠를 수 없습니다.");return}
 try{await api("/api/seasons",{method:"POST",body:JSON.stringify({seasonName:name,startDate:start,endDate:end,activeYn:seasonActive.checked})});seasonForm.reset();seasonActive.checked=true;toast("새 시즌을 만들었습니다.");await loadSeasons();renderSeasonList();await refreshSeasonData()}catch(e){toast(e.message)}}
async function loadMembers(){S.members=await api("/api/members");drawMembers();drawDraft()}
function drawMembers(){memberList.innerHTML=S.members.length?S.members.map(m=>`<div class="card" ondblclick="memberDetailView(${m.memberId})"><div class="card-top"><div class="member-avatar">${esc((m.realName||"?").slice(0,1))}</div><div><h3>${esc(m.realName)}</h3><p class="card-sub">${esc(m.gameName)}#${esc(m.tagLine)}</p></div></div><div class="tags">${(m.preferredPositions||[]).map(x=>`<span>${PN[x]}</span>`).join("")}</div><div class="member-summary"><div><small>솔랭</small><strong>${m.soloTier||"UNRANKED"} ${m.soloRank||""}</strong></div><div><small>최근 승률</small><strong>${m.recentWinRate||0}%</strong></div><div><small>내전 승률</small><strong>${m.inhouseWinRate||0}%</strong></div></div><p>LP ${m.soloLp||0} · 밸런스 코스트 <b>${m.balanceScore}</b></p><div class="member-card-buttons"><button onclick="event.stopPropagation();memberDetailView(${m.memberId})">상세 보기</button><button class="api-refresh" onclick="event.stopPropagation();riotRefresh(${m.memberId},this)">Riot 갱신</button><button onclick="event.stopPropagation();edit(${m.memberId})">정보 수정</button><button onclick="event.stopPropagation();del(${m.memberId})">멤버 삭제</button></div></div>`).join(""):`<div class="empty">등록된 멤버가 없습니다.</div>`}
window.edit=id=>{let m=S.members.find(x=>x.memberId===id);mid.value=id;realName.value=m.realName;gameName.value=m.gameName;tagLine.value=m.tagLine;puuid.value=m.puuid||"";tier.value=m.soloTier||"UNRANKED";rank.value=m.soloRank||"";lp.value=m.soloLp||0;score.value=m.balanceScore||"";document.querySelectorAll("[name=pos]").forEach(x=>x.checked=(m.preferredPositions||[]).includes(x.value));memberModal.classList.add("open")};
window.del=async id=>{if(confirm("삭제할까요?")){await api(`/api/members/${id}`,{method:"DELETE"});toast("삭제했습니다.");await all()}};
async function saveMember(e){e.preventDefault();let body={realName:realName.value.trim(),gameName:normalizeRiotGameName(gameName.value),tagLine:normalizeRiotTagLine(tagLine.value),puuid:puuid.value||null,soloTier:tier.value,soloRank:rank.value||null,soloLp:+lp.value||0,balanceScore:score.value?+score.value:null,preferredPositions:[...document.querySelectorAll("[name=pos]:checked")].map(x=>x.value)};try{await api(mid.value?`/api/members/${mid.value}`:"/api/members",{method:mid.value?"PUT":"POST",body:JSON.stringify(body)});memberModal.classList.remove("open");toast("저장했습니다.");await all()}catch(e){toast(e.message)}}


function normalizeRiotGameName(value){
  // 가운데 공백은 Riot ID의 일부이므로 보존합니다.
  return String(value||"")
    .replace(/\u00A0/g," ")
    .replace(/[\u200B\uFEFF]/g,"")
    .trim();
}

function normalizeRiotTagLine(value){
  return String(value||"")
    .replace(/[\s\u00A0\u200B\uFEFF]+/g,"")
    .toUpperCase();
}

async function refreshAllRiotMembers(){
 if(!S.members.length){
   toast("업데이트할 멤버가 없습니다.");
   return;
 }
 if(!confirm(`등록된 멤버 ${S.members.length}명의 Riot 정보를 순서대로 갱신할까요?\nAPI 호출이 많아 시간이 걸릴 수 있습니다.`))return;

 const button=document.getElementById("bulkRiotRefresh");
 const resultBox=document.getElementById("bulkRiotResult");
 const oldText=button.textContent;
 button.disabled=true;
 button.textContent="일괄 업데이트 중...";
 resultBox.classList.remove("hidden");
 resultBox.innerHTML=`<strong>멤버 일괄 업데이트 중</strong><p>Riot API에서 정보를 수집하고 있습니다. 창을 닫지 말아주세요.</p>`;

 try{
   const result=await api("/api/members/riot-refresh-all",{method:"POST"});
   const failures=(result.results||[]).filter(x=>!x.success);

   resultBox.innerHTML=`
     <div class="bulk-result-summary">
       <strong>일괄 업데이트 완료</strong>
       <span>성공 ${result.successCount}명 · 실패 ${result.failedCount}명 · 전체 ${result.total}명</span>
     </div>
     ${failures.length?`<div class="bulk-fail-list">${failures.map(x=>`<p><b>${esc(x.realName)}</b> — ${esc(x.message)}</p>`).join("")}</div>`:`<p>모든 멤버의 Riot 정보가 정상적으로 갱신되었습니다.</p>`}
   `;

   toast(`일괄 업데이트 완료: 성공 ${result.successCount}명, 실패 ${result.failedCount}명`);
   await all();
 }catch(e){
   resultBox.innerHTML=`<strong>일괄 업데이트 실패</strong><p>${esc(e.message)}</p>`;
   toast(e.message);
 }finally{
   button.disabled=false;
   button.textContent=oldText;
 }
}

async function lookup(){try{let d=await api(`/api/members/riot/lookup?gameName=${encodeURIComponent(normalizeRiotGameName(gameName.value))}&tagLine=${encodeURIComponent(normalizeRiotTagLine(tagLine.value))}`);puuid.value=d.puuid;gameName.value=d.gameName;tagLine.value=d.tagLine;riotResult.textContent=`확인 완료: ${d.gameName}#${d.tagLine}`}catch(e){riotResult.textContent=e.message}}
function chip(m){return `<div class="memberchip" draggable="true" data-id="${m.memberId}"><span><b>${esc(m.realName)}</b><small> ${m.soloTier||"UNRANKED"} · ${(m.preferredPositions||[]).map(x=>PN[x]).join("/")}</small></span><strong>${m.balanceScore}</strong></div>`}
function drawDraft(){
 let used=new Set([...S.selected,...S.blue,...S.red].map(Number));
 pool.innerHTML=S.members.filter(m=>!used.has(+m.memberId)).map(chip).join("")||`<div class="empty">남은 멤버 없음</div>`;
 selected.innerHTML=slots(S.selected,10);
 blue.innerHTML=slots(S.blue,5);
 red.innerHTML=slots(S.red,5);
 count.textContent=`${used.size}/10`;
 document.querySelectorAll(".memberchip").forEach(c=>{
   c.ondragstart=()=>S.drag=+c.dataset.id;
   c.ondblclick=()=>move(+c.dataset.id,"selected");
 });
 record.disabled=S.blue.length!==5||S.red.length!==5;
 discordCopy.disabled=record.disabled;
 discordSend.disabled=record.disabled||!S.discord;
 const auctionReady=used.size===10;
 openAuctionFromDraft.disabled=!auctionReady;
 openAuctionQuick.disabled=!auctionReady;
}
function slots(ids,n){return ids.map(id=>chip(S.members.find(x=>x.memberId===id))).join("")+Array.from({length:n-ids.length},()=>`<div class="empty">빈 슬롯</div>`).join("")}
function move(id,zone){if(!id)return;S.selected=S.selected.filter(x=>x!==id);S.blue=S.blue.filter(x=>x!==id);S.red=S.red.filter(x=>x!==id);if(zone==="pool"){}else if(zone==="selected"&&S.selected.length<10)S.selected.push(id);else if(zone==="blue"&&S.blue.length<5)S.blue.push(id);else if(zone==="red"&&S.red.length<5)S.red.push(id);drawDraft();evaluate()}
async function balance(){let ids=[...new Set([...S.selected,...S.blue,...S.red])];try{let r=await api("/api/team-balance/auto",{method:"POST",body:JSON.stringify({memberIds:ids})});S.selected=[];S.blue=r.blueTeam.map(x=>x.memberId);S.red=r.redTeam.map(x=>x.memberId);drawDraft();showReport(r)}catch(e){toast(e.message)}}
async function evaluate(){if(S.blue.length===5&&S.red.length===5){let r=await api("/api/team-balance/evaluate",{method:"POST",body:JSON.stringify({blueMemberIds:S.blue,redMemberIds:S.red})});showReport(r)}else report()}
function showReport(r){bcost.textContent=r.blueCost;rcost.textContent=r.redCost;bwin.textContent=r.blueExpectedWinRate+"%";rwin.textContent=r.redExpectedWinRate+"%";message.textContent=r.algorithmMessage+" · 차이 "+r.costDifference}
function report(){bcost.textContent=rcost.textContent="0";bwin.textContent=rwin.textContent="50.0%";message.textContent="10명을 선택해주세요."}
function openMatch(b,r){
 if(!S.seasons.length){toast("먼저 시즌을 생성해주세요.");seasonModal.classList.add("open");return}
 if(b.length!==5||r.length!==5)return;editingMatchId.value="";forceMode.value="N";matchModalTitle.textContent="경기 결과 등록";editMeta.classList.add("hidden");dateNow();if(S.activeSeasonId)matchSeason.value=S.activeSeasonId;players.innerHTML=teamRows("BLUE",b)+teamRows("RED",r);memo.value="";matchModal.classList.add("open")}
function openForceMatch(){
 if(!S.seasons.length){toast("먼저 시즌을 생성해주세요.");seasonModal.classList.add("open");return}
 editingMatchId.value="";forceMode.value="Y";matchModalTitle.textContent="강제 경기 기록";editMeta.classList.remove("hidden");dateNow();if(S.activeSeasonId)matchSeason.value=S.activeSeasonId;players.innerHTML=forceRows();memo.value="강제 기록";updatedBy.value="";updateReason.value="강제 기록 등록";matchModal.classList.add("open")}


const CHAMPION_KO_TO_EN={
"가렌":"Garen",
"갈리오":"Galio",
"갱플랭크":"Gangplank",
"그라가스":"Gragas",
"그레이브즈":"Graves",
"그웬":"Gwen",
"나르":"Gnar",
"나미":"Nami",
"나서스":"Nasus",
"나피리":"Naafiri",
"노틸러스":"Nautilus",
"녹턴":"Nocturne",
"누누와 윌럼프":"Nunu & Willump",
"니달리":"Nidalee",
"니코":"Neeko",
"닐라":"Nilah",
"다리우스":"Darius",
"다이애나":"Diana",
"드레이븐":"Draven",
"라이즈":"Ryze",
"라칸":"Rakan",
"람머스":"Rammus",
"럭스":"Lux",
"럼블":"Rumble",
"레나타 글라스크":"Renata Glasc",
"레넥톤":"Renekton",
"레오나":"Leona",
"렉사이":"Rek'Sai",
"렐":"Rell",
"렝가":"Rengar",
"루시안":"Lucian",
"룰루":"Lulu",
"르블랑":"LeBlanc",
"리 신":"Lee Sin",
"리븐":"Riven",
"리산드라":"Lissandra",
"릴리아":"Lillia",
"마스터 이":"Master Yi",
"마오카이":"Maokai",
"말자하":"Malzahar",
"말파이트":"Malphite",
"모데카이저":"Mordekaiser",
"모르가나":"Morgana",
"문도 박사":"Dr. Mundo",
"미스 포츈":"Miss Fortune",
"밀리오":"Milio",
"바드":"Bard",
"바루스":"Varus",
"바이":"Vi",
"베이가":"Veigar",
"베인":"Vayne",
"벡스":"Vex",
"벨베스":"Bel'Veth",
"벨코즈":"Vel'Koz",
"볼리베어":"Volibear",
"브라움":"Braum",
"브라이어":"Briar",
"브랜드":"Brand",
"블라디미르":"Vladimir",
"블릿츠크랭크":"Blitzcrank",
"비에고":"Viego",
"빅토르":"Viktor",
"뽀삐":"Poppy",
"사미라":"Samira",
"사이온":"Sion",
"사일러스":"Sylas",
"샤코":"Shaco",
"세나":"Senna",
"세라핀":"Seraphine",
"세주아니":"Sejuani",
"세트":"Sett",
"소나":"Sona",
"소라카":"Soraka",
"쉔":"Shen",
"쉬바나":"Shyvana",
"스웨인":"Swain",
"스카너":"Skarner",
"시비르":"Sivir",
"신 짜오":"Xin Zhao",
"신드라":"Syndra",
"신지드":"Singed",
"쓰레쉬":"Thresh",
"아리":"Ahri",
"아무무":"Amumu",
"아우렐리온 솔":"Aurelion Sol",
"아이번":"Ivern",
"아지르":"Azir",
"아칼리":"Akali",
"아크샨":"Akshan",
"아트록스":"Aatrox",
"아펠리오스":"Aphelios",
"알리스타":"Alistar",
"암베사":"Ambessa",
"애니":"Annie",
"애니비아":"Anivia",
"오로라":"Aurora",
"애쉬":"Ashe",
"야스오":"Yasuo",
"에코":"Ekko",
"엘리스":"Elise",
"오공":"Wukong",
"오리안나":"Orianna",
"올라프":"Olaf",
"요네":"Yone",
"요릭":"Yorick",
"우디르":"Udyr",
"우르곳":"Urgot",
"워윅":"Warwick",
"유미":"Yuumi",
"이렐리아":"Irelia",
"이블린":"Evelynn",
"이즈리얼":"Ezreal",
"일라오이":"Illaoi",
"자르반 4세":"Jarvan IV",
"자야":"Xayah",
"자크":"Zac",
"잔나":"Janna",
"잭스":"Jax",
"제드":"Zed",
"제라스":"Xerath",
"제리":"Zeri",
"제이스":"Jayce",
"조이":"Zoe",
"직스":"Ziggs",
"진":"Jhin",
"질리언":"Zilean",
"징크스":"Jinx",
"초가스":"Cho'Gath",
"카르마":"Karma",
"카밀":"Camille",
"카시오페아":"Cassiopeia",
"카이사":"Kai'Sa",
"카직스":"Kha'Zix",
"카타리나":"Katarina",
"칼리스타":"Kalista",
"케넨":"Kennen",
"케이틀린":"Caitlyn",
"케일":"Kayle",
"케인":"Kayn",
"코그모":"Kog'Maw",
"코르키":"Corki",
"퀸":"Quinn",
"클레드":"Kled",
"키아나":"Qiyana",
"킨드레드":"Kindred",
"타릭":"Taric",
"탈리야":"Taliyah",
"탈론":"Talon",
"탐 켄치":"Tahm Kench",
"트런들":"Trundle",
"트리스타나":"Tristana",
"트린다미어":"Tryndamere",
"트위스티드 페이트":"Twisted Fate",
"트위치":"Twitch",
"티모":"Teemo",
"파이크":"Pyke",
"판테온":"Pantheon",
"피들스틱":"Fiddlesticks",
"피오라":"Fiora",
"피즈":"Fizz",
"하이머딩거":"Heimerdinger",
"헤카림":"Hecarim",
"흐웨이":"Hwei"
};
function normalizeChampionName(value){
 const text=String(value||"").trim();
 return CHAMPION_KO_TO_EN[text]||text;
}

function teamRows(team,ids){
 const positions=["TOP","JUNGLE","MID","ADC","SUPPORT"];
 return `<h3>${team}</h3>`+(ids||[]).map((rawId,i)=>{
   const id=+rawId;
   const m=findMember(id);
   const position=positions[i]||positions[0];
   return `<div class="playerrow" data-id="${id}" data-team="${team}">
     <b>${esc(m?.realName||"-")}</b>
     <select class="pp">
       ${positions.map(p=>`<option value="${p}" ${p===position?"selected":""}>${p}</option>`).join("")}
     </select>
     <input class="champ" list="championOptions" placeholder="한글 또는 영문 챔피언">
     <input class="k" type="number" min="0" value="0" aria-label="킬">
     <input class="d" type="number" min="0" value="0" aria-label="데스">
     <input class="a" type="number" min="0" value="0" aria-label="어시스트">
     <label><input class="mvp" type="checkbox">MVP</label>
   </div>`;
 }).join("");
}

function forceRows(){return ["BLUE","RED"].map(team=>`<h3>${team}</h3>`+POS.slice(0,5).map((p,i)=>`<div class="playerrow" data-team="${team}"><select class="member-select">${S.members.map(m=>`<option value="${m.memberId}">${esc(m.realName)}</option>`).join("")}</select><select class="pp">${POS.map(x=>`<option ${x===p?'selected':''}>${x}</option>`).join("")}</select><input class="champ" list="championOptions" placeholder="한글 또는 영문 챔피언"><input class="k" type="number" value="0"><input class="d" type="number" value="0"><input class="a" type="number" value="0"><label><input class="mvp" type="checkbox">MVP</label></div>`).join("")).join("")}
function collectMatchPlayers(){return [...document.querySelectorAll(".playerrow")].map(x=>({memberId:+(x.dataset.id||x.querySelector('.member-select')?.value),teamCode:x.dataset.team,positionCode:x.querySelector(".pp").value,championName:normalizeChampionName(x.querySelector(".champ").value),kills:+x.querySelector(".k").value,deaths:+x.querySelector(".d").value,assists:+x.querySelector(".a").value,mvpYn:x.querySelector(".mvp").checked}))}
async function saveMatch(e){e.preventDefault();const payload={seasonId:matchSeason.value?+matchSeason.value:null,playedAt:playedAt.value,winnerTeam:winner.value,memo:memo.value,players:collectMatchPlayers(),updatedBy:updatedBy.value,updateReason:updateReason.value,forceYn:forceMode.value==='Y'};try{const id=editingMatchId.value;const url=id?`/api/matches/${id}`:(forceMode.value==='Y'?'/api/matches/force':'/api/matches');await api(url,{method:id?'PUT':'POST',body:JSON.stringify(payload)});matchModal.classList.remove("open");toast(id?"경기 기록을 수정했습니다.":"경기를 저장했습니다.");await all();document.querySelector('[data-page="matches"]').click()}catch(e){toast(e.message)}}
async function loadMatches(){S.matches=await api("/api/matches"+qs());const mods=await api('/api/matches/recent-modified');recentModified.classList.toggle('hidden',!mods.length);recentModified.innerHTML=mods.length?`<h3>최근 수정 기록</h3>`+mods.map(m=>`<div class="modified-row" onclick="detailMatch(${m.matchId})"><span><b>#${m.matchId}</b> ${esc(m.updateReason||'수정')}</span><small>${esc(m.updatedBy||'사용자')} · ${fmt(m.updatedAt)}</small></div>`).join(''):'';matchList.innerHTML=S.matches.length?S.matches.map(m=>`<div class="item" onclick="detailMatch(${m.matchId})"><span><b>#${m.matchId} ${fmt(m.playedAt)}</b> ${m.forceYn?'<span class="force-badge">강제</span>':''} ${m.updatedAt&&m.createdAt&&new Date(m.updatedAt)>new Date(m.createdAt).getTime()+1000?'<span class="match-edit-badge">수정됨</span>':''}<p>${esc(m.seasonName||"통산")} · ${esc(m.memo||"메모 없음")}</p></span><strong>${m.winnerTeam} WIN · ${m.blueCost}:${m.redCost}</strong></div>`).join(""):`<div class="empty">경기 없음</div>`;recent.innerHTML=S.matches.slice(0,5).map(m=>`<div class="item" onclick="detailMatch(${m.matchId})"><span>${fmt(m.playedAt)}</span><b>${m.winnerTeam} WIN</b></div>`).join("")||"경기 없음"}
let currentMatchDetail=null;window.detailMatch=async id=>{let m=await api(`/api/matches/${id}`);currentMatchDetail=m;detail.innerHTML=`<h2>#${id} ${m.winnerTeam} WIN ${m.forceYn?'<span class="force-badge">강제 기록</span>':''}</h2><p>${esc(m.seasonName||"통산")} · ${fmt(m.playedAt)} · ${esc(m.memo||"")}</p>${m.updateReason?`<div class="notice">최근 수정: ${esc(m.updateReason)} · ${esc(m.updatedBy||'사용자')} · ${fmt(m.updatedAt)}</div>`:''}`+["BLUE","RED"].map(t=>`<article><h3>${t}</h3>${m.players.filter(x=>(x.team_code||x.teamCode)===t).map(x=>`<div class="item"><b>${esc(x.realName)} ${x.mvp_yn||x.mvpYn?"👑":""}</b><span>${x.position_code||x.positionCode} · ${esc(x.champion_name||x.championName||"-")} ${x.kills}/${x.deaths}/${x.assists}</span></div>`).join("")}</article>`).join("");editMatchButton.classList.remove('hidden');detailModal.classList.add("open")};
async function editCurrentMatch(){const m=currentMatchDetail;if(!m)return;detailModal.classList.remove('open');editingMatchId.value=m.matchId;forceMode.value='N';matchModalTitle.textContent=`경기 #${m.matchId} 수정`;editMeta.classList.remove('hidden');matchSeason.value=m.seasonId||'';playedAt.value=String(m.playedAt).slice(0,16);winner.value=m.winnerTeam;memo.value=m.memo||'';updatedBy.value='';updateReason.value='';players.innerHTML=['BLUE','RED'].map(t=>`<h3>${t}</h3>`+m.players.filter(x=>(x.team_code||x.teamCode)===t).map(x=>{const id=x.member_id||x.memberId;return `<div class="playerrow" data-id="${id}" data-team="${t}"><b>${esc(x.realName)}</b><select class="pp">${POS.map(p=>`<option ${(x.position_code||x.positionCode)===p?'selected':''}>${p}</option>`).join('')}</select><input class="champ" list="championOptions" value="${esc(x.champion_name||x.championName||'')}"><input class="k" type="number" value="${x.kills||0}"><input class="d" type="number" value="${x.deaths||0}"><input class="a" type="number" value="${x.assists||0}"><label><input class="mvp" type="checkbox" ${x.mvp_yn||x.mvpYn?'checked':''}>MVP</label></div>`}).join('')).join('');matchModal.classList.add('open')}
async function dashboard(){let [d,h,a]=await Promise.all([api("/api/matches/dashboard"+qs()),api("/api/members/hall-of-fame"+qs()),api("/api/matches/awards"+qs())]);let m={};d.forEach(x=>m[x.metric]=x.value);metrics.innerHTML=[["멤버",m.memberCount||0],["경기",m.matchCount||0],["블루 승",m.blueWins||0],["레드 승",m.redWins||0]].map(x=>`<div class="metric"><span>${x[0]}</span><strong>${x[1]}</strong></div>`).join("");hall.innerHTML=h.map((x,i)=>`<div class="item"><b>${i+1}. ${esc(x.real_name)}</b><strong>${x.win_rate}%</strong></div>`).join("")||"최소 3경기 후 표시";awards.innerHTML=a.length?a.map(x=>`<div class="award-card"><small>${esc(x.award_name)}</small><strong>${esc(x.real_name)}</strong><span>${awardValue(x)}</span></div>`).join(""):`<div class="empty">경기 기록이 쌓이면 시즌 어워드가 표시됩니다.</div>`}
function awardValue(x){if(x.award_code==="WIN_RATE")return `${x.value}% · ${x.games}경기`;if(x.award_code==="SURVIVOR")return `평균 ${x.value}데스`;return `${x.value}`}
async function ranking(){let r=await api("/api/matches/ranking"+qs());document.getElementById("ranking").innerHTML=r.map((x,i)=>`<tr><td>${i+1}</td><td><button class="member-link" onclick="memberDetailView(${x.member_id})">${esc(x.real_name)}</button><br><small>${esc(x.game_name)}#${esc(x.tag_line)}</small></td><td>${x.solo_tier||""} ${x.solo_rank||""}</td><td>${x.match_count}</td><td>${x.win_count}/${x.loss_count}</td><td>${x.win_rate}%</td><td>${x.mvp_count}</td><td>${x.avg_kills}/${x.avg_deaths}/${x.avg_assists}</td><td>${x.balance_score}</td></tr>`).join("")}


/* =========================================================
   Web Audio API Sound Engine
   ========================================================= */

/* =========================================================
   Auction background music
   WAITING              -> /audio/draft-wait.mp3
   STARTING/RUNNING/SOLD -> /audio/draft-go.mp3
   COMPLETE/disconnect   -> stop
   ========================================================= */
const AuctionBgm=(()=>{
 const waitAudio=new Audio("/audio/draft-wait.mp3");
 const goAudio=new Audio("/audio/draft-go.mp3");
 waitAudio.loop=true;goAudio.loop=true;
 waitAudio.preload="auto";goAudio.preload="auto";

 let enabled=localStorage.getItem("riftAuctionBgmEnabled")!=="false";
 let volume=Number(localStorage.getItem("riftAuctionBgmVolume")||0.35);
 let current=null;
 let blocked=false;

 function applyVolume(){
   waitAudio.volume=volume;
   goAudio.volume=volume;
 }
 applyVolume();

 async function play(audio){
   if(!enabled){stop();return}
   if(current===audio&&!audio.paused){blocked=false;updateBgmUi();return}
   const other=audio===waitAudio?goAudio:waitAudio;
   other.pause();other.currentTime=0;
   current=audio;
   try{
     await audio.play();
     blocked=false;
   }catch(e){
     blocked=true;
   }
   updateBgmUi();
 }

 function sync(status){
   if(status==="WAITING")play(waitAudio);
   else if(status==="STARTING"||status==="RUNNING"||status==="SOLD")play(goAudio);
   else stop();
 }

 function stop(){
   [waitAudio,goAudio].forEach(a=>{a.pause();a.currentTime=0});
   current=null;blocked=false;updateBgmUi();
 }

 function toggle(){
   enabled=!enabled;
   localStorage.setItem("riftAuctionBgmEnabled",String(enabled));
   if(!enabled)stop();
   else if(auctionState)sync(auctionState.status);
   updateBgmUi();
 }

 function setVolume(v){
   volume=Math.max(0,Math.min(1,v));
   localStorage.setItem("riftAuctionBgmVolume",String(volume));
   applyVolume();
   updateBgmUi();
 }

 function unlock(){
   if(enabled&&auctionState)sync(auctionState.status);
 }

 return {
   sync,stop,toggle,setVolume,unlock,
   get enabled(){return enabled},
   get volume(){return volume},
   get blocked(){return blocked},
   get current(){return current}
 };
})();

function updateBgmUi(){
 const toggle=document.getElementById("bgmToggle");
 const slider=document.getElementById("bgmVolume");
 const status=document.getElementById("bgmStatus");
 if(toggle)toggle.textContent=AuctionBgm.enabled?"BGM 끄기":"BGM 켜기";
 if(slider)slider.value=Math.round(AuctionBgm.volume*100);
 if(!status)return;

 if(!AuctionBgm.enabled)status.textContent="BGM 꺼짐";
 else if(AuctionBgm.blocked)status.textContent="재생 대기 · 화면을 한 번 클릭해주세요";
 else if(auctionState?.status==="WAITING")status.textContent="대기 음악 재생 중";
 else if(["STARTING","RUNNING","SOLD"].includes(auctionState?.status))status.textContent="경매 음악 재생 중";
 else status.textContent="BGM 대기";
}


const AuctionSound=(()=>{
 let ctx=null,master=null,muted=localStorage.getItem("riftSoundMuted")==="true";
 let volume=Number(localStorage.getItem("riftSoundVolume")||0.55);

 function ensure(){
   if(!ctx){
     const AC=window.AudioContext||window.webkitAudioContext;
     if(!AC)return null;
     ctx=new AC();
     master=ctx.createGain();
     master.gain.value=muted?0:volume;
     master.connect(ctx.destination);
   }
   if(ctx.state==="suspended")ctx.resume();
   return ctx;
 }
 function gainAt(start,peak,end,duration){
   const c=ensure();if(!c)return null;
   const g=c.createGain();
   g.gain.setValueAtTime(0.0001,start);
   g.gain.exponentialRampToValueAtTime(Math.max(0.0002,peak),start+Math.min(.025,duration/3));
   g.gain.exponentialRampToValueAtTime(0.0001,end);
   g.connect(master);
   return g;
 }
 function tone(freq,duration=.15,type="sine",delay=0,peak=.12,endFreq=null){
   const c=ensure();if(!c)return;
   const start=c.currentTime+delay,end=start+duration;
   const o=c.createOscillator(),g=gainAt(start,peak,end,duration);
   o.type=type;o.frequency.setValueAtTime(freq,start);
   if(endFreq)o.frequency.exponentialRampToValueAtTime(endFreq,end);
   o.connect(g);o.start(start);o.stop(end+.02);
 }
 function noise(duration=.12,delay=0,peak=.08,filterFreq=1200){
   const c=ensure();if(!c)return;
   const len=Math.max(1,Math.floor(c.sampleRate*duration));
   const buffer=c.createBuffer(1,len,c.sampleRate),data=buffer.getChannelData(0);
   for(let i=0;i<len;i++)data[i]=(Math.random()*2-1)*(1-i/len);
   const src=c.createBufferSource(),filter=c.createBiquadFilter();
   filter.type="bandpass";filter.frequency.value=filterFreq;filter.Q.value=.8;
   const start=c.currentTime+delay,end=start+duration,g=gainAt(start,peak,end,duration);
   src.buffer=buffer;src.connect(filter);filter.connect(g);src.start(start);src.stop(end+.02);
 }
 const fx={
   start(){tone(130,.32,"sawtooth",0,.09,260);tone(260,.26,"triangle",.18,.11,520);tone(520,.32,"sine",.38,.14,780);noise(.45,.05,.035,480)},
   reveal(){noise(.18,0,.07,1800);tone(280,.18,"triangle",0,.08,620);tone(840,.12,"sine",.12,.08)},
   bid(team="BLUE"){const base=team==="BLUE"?520:410;tone(base,.08,"square",0,.055,base*1.25);tone(base*1.5,.10,"sine",.055,.075)},
   tick(second){const f=second<=1?1050:second<=3?820:620;tone(f,.07,second<=2?"square":"sine",0,second<=2?.10:.06)},
   sold(team="BLUE"){noise(.16,0,.11,650);const base=team==="BLUE"?330:294;tone(base,.16,"sawtooth",0,.10,base*1.4);tone(base*1.5,.22,"triangle",.12,.13,base*2);tone(base*2,.32,"sine",.30,.16)},
   unsold(){tone(260,.20,"sawtooth",0,.08,120);tone(150,.25,"triangle",.16,.07,80)},
   error(){tone(180,.11,"square",0,.07);tone(140,.16,"square",.12,.08)},
   complete(){[392,494,587,784].forEach((f,i)=>tone(f,.32,"triangle",i*.14,.12));noise(.5,.4,.04,2200)}
 };
 function setVolume(v){volume=Math.max(0,Math.min(1,v));localStorage.setItem("riftSoundVolume",volume);if(master)master.gain.value=muted?0:volume}
 function toggle(){muted=!muted;localStorage.setItem("riftSoundMuted",muted);if(master)master.gain.value=muted?0:volume;return muted}
 async function testSequence(){ensure();fx.reveal();setTimeout(()=>fx.bid("BLUE"),350);setTimeout(()=>fx.tick(3),650);setTimeout(()=>fx.sold("BLUE"),950)}
 return {ensure,setVolume,toggle,testSequence,...fx,get muted(){return muted},get volume(){return volume}};
})();

/* =========================================================
   Realtime Auction Client (server authoritative WebSocket)
   ========================================================= */
let auctionSocket=null,auctionRole=null,auctionState=null,auctionRoom=null,lastAuctionEvent="";
const auctionClientId=sessionStorage.getItem("riftAuctionClientId")||((window.crypto&&crypto.randomUUID)?crypto.randomUUID():`client-${Date.now()}-${Math.random().toString(36).slice(2)}`);
sessionStorage.setItem("riftAuctionClientId",auctionClientId);
let auctionJoinRejected=false;
function openPage(pageName){document.querySelectorAll(".page").forEach(p=>p.classList.remove("active"));document.querySelectorAll(".nav-btn").forEach(n=>n.classList.toggle("active",n.dataset.page===pageName));document.getElementById(pageName)?.classList.add("active");if(pageName==="auction")renderAuctionSetup();window.scrollTo({top:0,behavior:"smooth"})}
function auctionMemberIds(){return [...new Set([...S.selected,...S.blue,...S.red].map(Number))]}
function findMember(id){return S.members.find(m=>+m.memberId===+id)}
function loadAuctionPlayersFromDraft(){const ids=auctionMemberIds();if(ids.length!==10){toast("내전 짜기에서 참가자 10명을 먼저 선택해주세요.");return}S.auction={setupMemberIds:ids,status:"SETUP"};renderAuctionSetup();toast("참가자 10명을 불러왔습니다.")}
function renderAuctionSetup(){const ids=(S.auction?.setupMemberIds)||auctionMemberIds(),ms=ids.map(findMember).filter(Boolean);auctionPlayerCount.textContent=`${ms.length} / 10명`;auctionPlayers.innerHTML=ms.length?ms.map(m=>`<div class="auction-player-chip"><span class="mini-avatar">${esc((m.realName||"?").slice(0,1))}</span><div><b>${esc(m.realName)}</b><small>${esc(m.soloTier||"UNRANKED")} · ${m.balanceScore||1000}</small></div></div>`).join(""):`<div class="empty">참가자 10명을 불러오거나 방 코드로 참가하세요.</div>`;const opts=`<option value="">호스트 선택</option>`+ms.map(m=>`<option value="${m.memberId}">${esc(m.realName)} · ${m.balanceScore||1000}</option>`).join("");if(blueHostSelect.options.length!==ms.length+1){blueHostSelect.innerHTML=opts;redHostSelect.innerHTML=opts;const so=[...ms].sort((a,b)=>(b.balanceScore||0)-(a.balanceScore||0));if(so.length===10){blueHostSelect.value=so[0].memberId;redHostSelect.value=so[1].memberId}}auctionStart.disabled=ms.length!==10;auctionCountdownSec.value=60;soundVolume.value=Math.round(AuctionSound.volume*100);updateSoundButton()}
async function createRealtimeRoom(){AuctionSound.ensure();const ids=(S.auction?.setupMemberIds)||auctionMemberIds();try{const st=await api('/api/auctions/rooms',{method:'POST',body:JSON.stringify({memberIds:ids,blueHostId:+blueHostSelect.value,redHostId:+redHostSelect.value,baseToken:+auctionBaseToken.value||1000,adjustRate:+auctionAdjustRate.value||.5})});connectAuction(st.roomCode,'DIRECTOR');toast(`경매방 ${st.roomCode} 생성 완료 · 진행자로 접속했습니다.`)}catch(e){toast(e.message);AuctionSound.error()}}
function joinRealtimeRoom(){const code=auctionJoinCode.value.trim().toUpperCase();if(code.length!==6){toast('6자리 방 코드를 입력하세요.');return}connectAuction(code,auctionJoinRole.value)}
function connectAuction(code,role){
 if(auctionSocket&&auctionSocket.readyState!==WebSocket.CLOSED)auctionSocket.close();
 auctionJoinRejected=false;auctionRoom=(code||"").trim().toUpperCase();auctionRole=(role||"SPECTATOR").toUpperCase();
 const proto=location.protocol==='https:'?'wss':'ws';
 auctionSocket=new WebSocket(`${proto}://${location.host}/ws/auction?roomCode=${encodeURIComponent(auctionRoom)}&role=${encodeURIComponent(auctionRole)}&clientId=${encodeURIComponent(auctionClientId)}`);
 auctionRoomStatus.classList.remove('hidden');auctionRoomStatus.className='room-status connecting';auctionRoomStatus.textContent=`${auctionRoom} 방에 ${roleLabel(auctionRole)}로 접속 중입니다...`;
 auctionSocket.onopen=()=>{auctionRoomStatus.className='room-status connected';auctionRoomStatus.textContent=`${auctionRoom} 방에 ${roleLabel(auctionRole)}로 접속했습니다.`};
 auctionSocket.onmessage=e=>{
  const incoming=JSON.parse(e.data);
  if(incoming.type==='JOIN_REJECTED'){auctionJoinRejected=true;const msg=incoming.message||`${roleLabel(incoming.role)} 자리는 이미 사용 중입니다.`;auctionRoomStatus.className='room-status rejected';auctionRoomStatus.textContent=msg;toast(msg);AuctionSound.error();return}
  if(incoming.type==='ERROR'){const msg=incoming.message||'경매 처리 오류';auctionRoomStatus.className='room-status rejected';auctionRoomStatus.textContent=msg;toast(msg);AuctionSound.error();return}
  const prev=auctionState;auctionState=incoming;handleAuctionEvent(prev,incoming);AuctionBgm.sync(incoming.status);auctionRoomStatus.className='room-status connected';auctionRoomStatus.textContent=`${incoming.roomCode} · ${incoming.presenceText||'접속 상태 확인 중'}`;
  if(!prev||prev.current?.memberId!==auctionState.current?.memberId)AuctionSound.reveal();
  if(prev&&prev.currentBid!==auctionState.currentBid&&auctionState.currentBid>0)AuctionSound.bid(auctionState.bidTeam);
  if(prev&&prev.current&&!auctionState.current)AuctionSound.sold(auctionState.bidTeam||'BLUE');
  renderRealtimeAuction();
 };
 auctionSocket.onerror=()=>{if(!auctionJoinRejected){auctionRoomStatus.className='room-status rejected';auctionRoomStatus.textContent='경매방 연결에 실패했습니다. 방 코드와 서버 상태를 확인해주세요.';toast('경매방 연결에 실패했습니다.')}};
 auctionSocket.onclose=e=>{AuctionBgm.stop();if(auctionJoinRejected)return;liveConnections.innerHTML='<span class="presence-item offline">연결 종료</span>';liveAuctionStart.disabled=true;auctionRoomStatus.className='room-status disconnected';auctionRoomStatus.textContent=e.reason?`연결 종료: ${e.reason}`:'경매방 연결이 종료되었습니다.'};
}
function roleLabel(role){return role==='DIRECTOR'?'진행자':role==='BLUE'?'블루 호스트':role==='RED'?'레드 호스트':'관전자'}
function wsAction(action,extra={}){if(!auctionSocket||auctionSocket.readyState!==1){toast('경매방에 먼저 접속하세요.');return}auctionSocket.send(JSON.stringify({action,...extra}))}

let lastAuctionEventSeq=0;
let auctionEventTimer=null;
function handleAuctionEvent(prev,a){
 if(!a||!a.eventSeq||a.eventSeq===lastAuctionEventSeq)return;
 lastAuctionEventSeq=a.eventSeq;
 if(a.eventType==='FIRST_BID'){
   showAuctionEvent(a,'turn',1900);
   AuctionSound.bid(a.eventTeam||'BLUE');
 }else if(a.eventType==='TURN'){
   showAuctionEvent(a,'turn',1200);
   AuctionSound.bid(a.eventTeam||'BLUE');
 }else if(a.eventType==='SOLD'){
   showAuctionEvent(a,`sold ${(a.eventTeam||'BLUE').toLowerCase()}`,2300);
   AuctionSound.sold(a.eventTeam||'BLUE');
 }else if(a.eventType==='COMPLETE'){
   showAuctionComplete();
   AuctionSound.complete();
 }
}
function showAuctionEvent(a,kind,duration){
 clearTimeout(auctionEventTimer);
 auctionEventPanel.className=`auction-event-panel ${kind||''}`;
 auctionEventKicker.textContent=a.eventType==='SOLD'?'SOLD!':a.eventType==='FIRST_BID'?'FIRST ATTACK':'NOW TURN';
 auctionEventTitle.textContent=a.eventTitle||'-';
 auctionEventMessage.textContent=a.eventMessage||'';
 auctionEventOverlay.classList.remove('hidden');
 void auctionEventPanel.offsetWidth;
 auctionEventPanel.classList.add('play');
 auctionEventTimer=setTimeout(()=>{auctionEventOverlay.classList.add('hidden');auctionEventPanel.classList.remove('play')},duration||1400);
}
function showAuctionComplete(){
 auctionCompleteOverlay.classList.remove('hidden');
 setTimeout(()=>auctionCompleteOverlay.classList.add('hidden'),4200);
}

function renderRealtimeAuction(){const a=auctionState;if(!a)return;renderAuctionStartOverlay(a);auctionSetup.classList.add('hidden');auctionStage.classList.remove('hidden');auctionComplete.classList.toggle('hidden',a.status!=='COMPLETE');liveRoomCode.textContent=a.roomCode;liveRole.textContent=roleLabel(auctionRole);liveConnections.innerHTML=`<span class="presence-item ${a.connectedDirector?'online':'waiting'}">진행자 ${a.connectedDirector?'● 접속':'○ 대기'}</span><span class="presence-item ${a.connectedBlue?'online':'waiting'}">BLUE ${esc(a.blueHostName||'-')} ${a.connectedBlue?'● 접속':'○ 대기'}</span><span class="presence-item ${a.connectedRed?'online':'waiting'}">RED ${esc(a.redHostName||'-')} ${a.connectedRed?'● 접속':'○ 대기'}</span><span class="presence-item spectator">관전자 ${a.spectatorCount||0}명</span>`;liveAuctionStart.disabled=!(auctionRole==='DIRECTOR'&&a.connectedDirector&&a.connectedBlue&&a.connectedRed&&a.status==='WAITING');liveAuctionStart.classList.toggle('hidden',auctionRole!=='DIRECTOR');blueAuctionHost.textContent=(a.blueTeam[0]?.realName)||'-';redAuctionHost.textContent=(a.redTeam[0]?.realName)||'-';blueToken.textContent=a.blueTokens;redToken.textContent=a.redTokens;blueAuctionTeam.innerHTML=serverRoster(a.blueTeam);redAuctionTeam.innerHTML=serverRoster(a.redTeam);auctionRound.textContent=`${a.round} / 8`;const c=a.current;candidateAvatar.textContent=c?(c.realName||'?').slice(0,1):'?';candidateName.textContent=c?.realName||(a.status==='WAITING'?'호스트 접속 대기':a.status==='STARTING'?'경매 시작 준비 중':'경매 완료');candidateRiotId.textContent=c?`${c.gameName||''}#${c.tagLine||''}`:'-';candidateRank.textContent=c?`${c.soloTier||'UNRANKED'} ${c.soloRank||''}`:'UNRANKED';candidateCost.textContent=c?.balanceScore||0;candidateRecord.textContent=c?`${c.recentWins||0}승 ${c.recentLosses||0}패`:'0승 0패';candidateKda.textContent=c?`${c.recentAvgKills||0}/${c.recentAvgDeaths||0}/${c.recentAvgAssists||0}`:'0/0/0';candidateLane.textContent=PN[c?.mostPosition]||c?.mostPosition||'-';blueAppealText.textContent=a.appeals?.BLUE||'-';redAppealText.textContent=a.appeals?.RED||'-';auctionCountdown.textContent=c?a.remainingSeconds:'-';currentBid.textContent=a.currentBid||0;currentBidTeam.textContent=a.bidTeam?`${a.bidTeam==='BLUE'?a.blueHostName:a.redHostName} 최고 입찰`:'기본 베팅 10원';currentBidTeam.className=a.bidTeam||'';countdownRing.classList.toggle('warning',a.remainingSeconds<=10&&a.remainingSeconds>3);countdownRing.classList.toggle('danger',a.remainingSeconds<=3&&!!c);auctionQueue.innerHTML=a.queue?.length?a.queue.map(m=>`<div class="queue-chip"><b>${esc(m.realName)}</b><small>${m.balanceScore||1000}</small></div>`).join(''):'<span class="empty">남은 후보 없음</span>';auctionLog.innerHTML=(a.logs||[]).slice().reverse().map(l=>`<div class="log-row"><span>${esc(l.time)}</span><b>${esc(l.message)}</b></div>`).join('');blueReserveInfo.textContent=`최대 입찰 ${a.blueMaxBid}원 · 남은 영입 횟수에 10원씩 자동 보존`;redReserveInfo.textContent=`최대 입찰 ${a.redMaxBid}원 · 남은 영입 횟수에 10원씩 자동 보존`;const myTurn=a.status==='RUNNING'&&a.turnTeam===auctionRole;
 auctionTurnBanner.classList.toggle('hidden',!c||!a.turnTeam);
 auctionTurnName.textContent=a.turnHostName?`${a.turnHostName}님의 TURN`:'-';
 auctionTurnBanner.className=`auction-turn-banner ${a.turnTeam?a.turnTeam.toLowerCase():''} ${(!c||!a.turnTeam)?'hidden':''}`;
 document.querySelectorAll('.bid-btn').forEach(b=>{b.disabled=!c||auctionRole!==b.dataset.team||a.turnTeam!==b.dataset.team||a.status!=='RUNNING'});
 blueSurrender.disabled=!c||auctionRole!=='BLUE'||a.turnTeam!=='BLUE'||a.surrendered!=null||a.status!=='RUNNING';
 redSurrender.disabled=!c||auctionRole!=='RED'||a.turnTeam!=='RED'||a.surrendered!=null||a.status!=='RUNNING';blueAppealSend.disabled=auctionRole!=='BLUE'||!c;redAppealSend.disabled=auctionRole!=='RED'||!c;if(a.status==='COMPLETE'){S.blue=a.blueTeam.map(x=>+x.memberId);S.red=a.redTeam.map(x=>+x.memberId);S.selected=[];drawDraft();report()}}

let auctionStartFxPlayedForRoom=null;
function renderAuctionStartOverlay(a){
 const overlay=document.getElementById('auctionStartOverlay');
 const count=document.getElementById('auctionStartCount');
 if(!overlay||!count)return;

 if(a.status==='STARTING'){
   overlay.classList.remove('hidden');
   count.textContent=Math.max(1,Math.min(3,a.startRemainingSeconds||3));
   if(auctionStartFxPlayedForRoom!==a.roomCode){
     auctionStartFxPlayedForRoom=a.roomCode;
     AuctionSound.start();
   }else{
     AuctionSound.tick(Math.max(1,a.startRemainingSeconds||1));
   }
 }else{
   overlay.classList.add('hidden');
   if(a.status==='WAITING')auctionStartFxPlayedForRoom=null;
 }
}

function serverRoster(team){return (team||[]).map(x=>`<div class="auction-roster-card ${x.host?'host':''}"><div><b>${esc(x.realName)}</b><small>${x.host?'HOST':`${x.price}원`}</small></div><span class="sold-price">${x.balanceScore||0}</span></div>`).join('')+Array.from({length:Math.max(0,5-(team||[]).length)},()=>'<div class="auction-roster-card"><small>빈 슬롯</small></div>').join('')}
function placeAuctionBid(team,add){wsAction('BID',{amount:add==='unit'?10:+add})}function startAuctionCountdown(){wsAction('START')}function passCurrentCandidate(){toast('실시간 경매에서는 유찰 대신 포기 버튼을 사용합니다.')}function resetAuction(){if(auctionSocket)auctionSocket.close();auctionSocket=null;auctionState=null;auctionRoom=null;auctionRole=null;auctionSetup.classList.remove('hidden');auctionStage.classList.add('hidden');auctionComplete.classList.add('hidden');renderAuctionSetup()}
function toggleSound(){AuctionSound.toggle();updateSoundButton()}function updateSoundButton(){if(!window.soundToggle)return;soundToggle.textContent=AuctionSound.muted?'효과음 켜기':'음소거';soundIcon.textContent=AuctionSound.muted?'🔇':'🔊'}

let globalLoadingCount=0;
let globalLoadingTimer=null;

function showGlobalLoading(message){
 globalLoadingCount++;
 clearTimeout(globalLoadingTimer);
 globalLoadingTimer=setTimeout(()=>{
   const overlay=document.getElementById("globalLoading");
   const text=document.getElementById("globalLoadingMessage");
   if(text&&message)text.textContent=message;
   if(overlay)overlay.classList.remove("hidden");
 },350);
}

function hideGlobalLoading(){
 globalLoadingCount=Math.max(0,globalLoadingCount-1);
 if(globalLoadingCount===0){
   clearTimeout(globalLoadingTimer);
   const overlay=document.getElementById("globalLoading");
   if(overlay)overlay.classList.add("hidden");
 }
}

async function api(u,o={}){
 const background=o&&o.background===true;
 const requestOptions={...o};
 delete requestOptions.background;
 if(!background)showGlobalLoading("서버 응답을 기다리고 있어요. 무료 서버는 처음 접속할 때 조금 느릴 수 있습니다.");
 try{
   let r=await fetch(u,{headers:{"Content-Type":"application/json"},...requestOptions});
   if(r.status===204)return;
   let d=await r.json().catch(()=>({}));
   if(!r.ok)throw Error(d.message||"요청 실패");
   return d;
 }finally{
   if(!background)hideGlobalLoading();
 }
}
function dateNow(){let d=new Date(Date.now()-new Date().getTimezoneOffset()*60000);playedAt.value=d.toISOString().slice(0,16)}
function fmt(x){return x?new Date(x).toLocaleString("ko-KR"):"-"}function esc(x){return String(x??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c]))}
function toast(x){let t=document.getElementById("toast");t.textContent=x;t.classList.add("show");setTimeout(()=>t.classList.remove("show"),2800)}
window.riotRefresh=async (id,button)=>{let oldText=button?button.textContent:"";try{if(button){button.disabled=true;button.textContent="갱신 중..."}toast("Riot API에서 최근 20경기를 수집 중입니다...");await api(`/api/members/${id}/riot-refresh`,{method:"POST"});toast("Riot 정보를 갱신했습니다.");await all();await memberDetailView(id)}catch(e){toast(e.message)}finally{if(button){button.disabled=false;button.textContent=oldText}}};
window.memberDetailView=async id=>{try{let m=await api(`/api/members/${id}${qs()}`),syn=m.synergy||[],riv=m.rivalry||[],best=syn[0],worst=syn.length?syn[syn.length-1]:null,easy=riv[0],hard=riv.length?riv[riv.length-1]:null,sum=m.inhouseSummary||{};let icon=m.profileIconId?`https://ddragon.leagueoflegends.com/cdn/15.14.1/img/profileicon/${m.profileIconId}.png`:"";memberDetail.dataset.memberId=id;memberDetail.innerHTML=`<div class="memberhero">${icon?`<img class="profileicon" src="${icon}" alt="프로필">`:`<div class="profileicon"></div>`}<div><h2>${esc(m.realName)}</h2><p>${esc(m.gameName)}#${esc(m.tagLine)} · Lv.${m.summonerLevel||"-"}</p><p>${m.soloTier||"UNRANKED"} ${m.soloRank||""} ${m.soloLp||0}LP · 최근 갱신 ${m.riotUpdatedAt?fmt(m.riotUpdatedAt):"없음"}</p></div><button class="btn btn-riot api-refresh" onclick="riotRefresh(${id},this)">Riot 정보 갱신</button></div><h3>솔랭 최근 통계</h3><div class="statgrid"><div class="statbox"><small>최근 20판</small><strong>${m.recentWins||0}승 ${m.recentLosses||0}패</strong></div><div class="statbox"><small>솔랭 승률</small><strong>${m.recentWinRate||0}%</strong></div><div class="statbox"><small>평균 K/D/A</small><strong>${m.recentAvgKills||0}/${m.recentAvgDeaths||0}/${m.recentAvgAssists||0}</strong></div><div class="statbox"><small>모스트 라인</small><strong>${PN[m.mostPosition]||m.mostPosition||"-"}</strong></div></div><h3>내전 상세 통계</h3><div class="statgrid five"><div class="statbox"><small>경기</small><strong>${sum.match_count||0}</strong></div><div class="statbox"><small>승 / 패</small><strong>${sum.win_count||0} / ${sum.loss_count||0}</strong></div><div class="statbox"><small>승률</small><strong>${sum.win_rate||0}%</strong></div><div class="statbox"><small>MVP</small><strong>${sum.mvp_count||0}</strong></div><div class="statbox"><small>최고 연승</small><strong>${sum.best_win_streak||0}</strong></div></div><p class="kda-line">내전 평균 K/D/A <b>${sum.avg_kills||0}/${sum.avg_deaths||0}/${sum.avg_assists||0}</b></p><h3>솔랭 최근 모스트 챔피언</h3><div class="champgrid">${champCards(m.soloChampions)}</div><h3>내전 모스트 챔피언</h3><div class="champgrid">${champCards(m.inhouseChampions)}</div><h3>내전 궁합과 상대 전적</h3><div class="synergygrid four">${relationCard("최고의 궁합",best,"good")}${relationCard("최악의 궁합",worst,"bad")}${relationCard("가장 강한 상대",easy,"good")}${relationCard("가장 어려운 상대",hard,"bad")}</div><h3>최근 내전 5경기</h3><div class="recent-member-matches">${recentMatchCards(m.recentMatches)}</div>`;memberDetailModal.classList.add("open")}catch(e){toast(e.message)}};
function relationCard(title,x,css){return x?`<div class="syn-card ${css}"><b>${title}</b><p>${esc(x.real_name)} · ${x.games}경기 ${x.wins}승 · ${x.win_rate}%</p></div>`:`<div class="syn-card"><b>${title}</b><p>기록 부족</p></div>`}
function recentMatchCards(xs){return xs&&xs.length?xs.map(x=>`<div class="recent-member-row ${x.win_yn?"win":"lose"}"><b>${x.win_yn?"승리":"패배"}</b><span>${fmt(x.played_at)} · ${x.season_name||"통산"}</span><span>${x.position_code||"-"} · ${esc(x.champion_name||"-")} ${x.kills}/${x.deaths}/${x.assists}${x.mvp_yn?" 👑":""}</span></div>`).join(""):`<div class="empty">아직 내전 기록이 없습니다.</div>`}
function champCards(xs){return (xs&&xs.length)?xs.map((x,i)=>`<div class="champcard"><b>${i+1}. ${esc(x.champion_name)}</b><p>${x.games}경기 · ${x.wins}승 · ${x.win_rate||0}%</p></div>`).join(""):`<div class="champcard">아직 기록이 없습니다.</div>`}
function discordText(){let line=(title,ids)=>`${title}\n`+ids.map((id,i)=>`${["TOP","JUNGLE","MID","ADC","SUPPORT"][i]} ${S.members.find(x=>x.memberId===id).realName}`).join("\n");let season=S.seasons.find(x=>+x.seasonId===S.activeSeasonId);return `[오늘의 롤 내전${season?` · ${season.seasonName}`:""}]\n\n${line("🔵 블루팀",S.blue)}\n\n${line("🔴 레드팀",S.red)}\n\n예상 승률 ${bwin.textContent} : ${rwin.textContent}`}
async function copyDiscord(){if(S.blue.length!==5||S.red.length!==5)return;try{await navigator.clipboard.writeText(discordText());toast("Discord용 팀 구성을 복사했습니다.")}catch(e){toast("클립보드 복사에 실패했습니다.")}}
async function discordState(){try{let x=await api("/api/discord/status");S.discord=!!x.configured;discordStatus.textContent=S.discord?"Webhook 연결됨":"Webhook 미설정 · 복사 기능 사용 가능";drawDraft()}catch(e){discordStatus.textContent="Webhook 상태 확인 실패"}}
async function sendDiscord(){if(!S.discord)return;try{discordSend.disabled=true;discordSend.textContent="전송 중...";await api("/api/discord/send",{method:"POST",body:JSON.stringify({content:discordText()})});toast("Discord 채널에 전송했습니다.")}catch(e){toast(e.message)}finally{discordSend.textContent="Discord 채널 전송";drawDraft()}}
