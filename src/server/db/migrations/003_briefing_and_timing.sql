ALTER TABLE games ADD COLUMN story text NOT NULL DEFAULT '';
ALTER TABLE games ADD COLUMN rules text NOT NULL DEFAULT '';
ALTER TABLE runs ADD COLUMN game_title text NOT NULL DEFAULT '';
ALTER TABLE runs ADD COLUMN story text NOT NULL DEFAULT '';
ALTER TABLE runs ADD COLUMN rules text NOT NULL DEFAULT '';
ALTER TABLE runs ADD COLUMN started_at timestamptz;

UPDATE runs r SET game_title = g.title, story = g.story, rules = g.rules
FROM games g WHERE g.id = r.game_id;

-- Existing runs have no explicit start event; preserve their earliest recorded play time.
UPDATE runs r SET started_at = COALESCE(
  (SELECT min(q.started_at) FROM run_quests q WHERE q.run_id = r.id), r.finished_at
);

UPDATE quest_catalog SET template = jsonb_set(template, '{instruction}',
  '"Сверьте трафареты через окно. Введите получившееся слово."') WHERE type = 'stencil';

-- Replace only the old default, leaving custom instructions and prepared props intact.
UPDATE game_quests SET config = jsonb_set(config, '{instruction}',
  '"Сверьте трафареты через окно. Введите получившееся слово."')
WHERE type = 'stencil' AND config->>'instruction' = 'Сверьте трафареты через окно. Введите слово и номер улики.';
UPDATE run_quests SET config = jsonb_set(config, '{instruction}',
  '"Сверьте трафареты через окно. Введите получившееся слово."')
WHERE type = 'stencil' AND config->>'instruction' = 'Сверьте трафареты через окно. Введите слово и номер улики.';
