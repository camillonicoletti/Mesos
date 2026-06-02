CREATE TABLE IF NOT EXISTS match_result (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    match_id      BINARY(16)   NOT NULL,
    nickname      VARCHAR(64)  NOT NULL,
    final_score   INT          NOT NULL,
    played_at     DATETIME(6)  NOT NULL,
    players_count SMALLINT     NOT NULL,
    INDEX idx_match_pcount_score (players_count, final_score DESC),
    INDEX idx_match_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
#db copy