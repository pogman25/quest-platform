CREATE TABLE organizers (
  id uuid PRIMARY KEY,
  email text NOT NULL UNIQUE,
  password_hash text NOT NULL
);

CREATE TABLE organizer_sessions (
  token_hash text PRIMARY KEY,
  organizer_id uuid NOT NULL REFERENCES organizers(id) ON DELETE CASCADE,
  expires_at timestamptz NOT NULL
);

CREATE TABLE games (
  id text PRIMARY KEY,
  organizer_id uuid NOT NULL REFERENCES organizers(id) ON DELETE CASCADE,
  title text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX games_organizer_idx ON games (organizer_id, created_at);

CREATE TABLE game_quests (
  game_id text NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  type text NOT NULL,
  position integer CHECK (position >= 1),
  config jsonb NOT NULL,
  PRIMARY KEY (game_id, type),
  UNIQUE (game_id, position)
);

CREATE TABLE runs (
  id uuid PRIMARY KEY,
  game_id text NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  join_code text NOT NULL UNIQUE,
  team_name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  finished_at timestamptz
);
CREATE INDEX runs_game_idx ON runs (game_id, created_at DESC);

CREATE TABLE player_sessions (
  token_hash text PRIMARY KEY,
  run_id uuid NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
  expires_at timestamptz NOT NULL
);

CREATE TABLE run_quests (
  run_id uuid NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
  type text NOT NULL,
  position integer NOT NULL CHECK (position >= 1),
  config jsonb NOT NULL,
  reward_digit text NOT NULL CHECK (reward_digit ~ '^[0-9]$'),
  started_at timestamptz,
  solved_at timestamptz,
  PRIMARY KEY (run_id, type),
  UNIQUE (run_id, position)
);

CREATE TABLE attempts (
  run_id uuid NOT NULL,
  request_id uuid NOT NULL,
  quest_type text NOT NULL,
  submitted_at timestamptz NOT NULL DEFAULT now(),
  answer jsonb NOT NULL,
  result jsonb NOT NULL,
  PRIMARY KEY (run_id, request_id),
  FOREIGN KEY (run_id, quest_type) REFERENCES run_quests(run_id, type) ON DELETE CASCADE
);
CREATE INDEX attempts_progress_idx ON attempts (run_id, quest_type, submitted_at);
