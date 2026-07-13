# ERD
```mermaid
erDiagram
 MEMBER ||--o{ MEMBER_POSITION : prefers
 MEMBER ||--o{ INHOUSE_MATCH_PLAYER : participates
 INHOUSE_MATCH ||--|{ INHOUSE_MATCH_PLAYER : contains
 MEMBER { bigint member_id PK varchar real_name varchar game_name varchar tag_line varchar puuid varchar solo_tier varchar solo_rank int solo_lp int balance_score boolean active_yn }
 MEMBER_POSITION { bigint member_id PK_FK varchar position_code PK int priority_no }
 INHOUSE_MATCH { bigint match_id PK timestamp played_at varchar winner_team int blue_cost int red_cost varchar memo }
 INHOUSE_MATCH_PLAYER { bigint match_player_id PK bigint match_id FK bigint member_id FK varchar team_code varchar position_code varchar champion_name int kills int deaths int assists boolean win_yn boolean mvp_yn }
```
