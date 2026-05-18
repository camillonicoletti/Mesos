CREATE TABLE IF NOT EXISTS match_result (
    id            BIGSERIAL    PRIMARY KEY,
    match_id      UUID         NOT NULL,
    nickname      VARCHAR(64)  NOT NULL,
    final_score   INT          NOT NULL,
    played_at     TIMESTAMP    NOT NULL,
    players_count SMALLINT     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_match_pcount_score
    ON match_result(players_count, final_score DESC);

CREATE INDEX IF NOT EXISTS idx_match_nickname
    ON match_result(nickname);