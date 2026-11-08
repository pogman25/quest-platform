CREATE TABLE quest_catalog (
  type text PRIMARY KEY,
  title text NOT NULL,
  badge text NOT NULL,
  description text NOT NULL,
  position integer NOT NULL UNIQUE CHECK (position > 0),
  template jsonb NOT NULL CHECK (template->>'type' IS NOT DISTINCT FROM type)
);

CREATE TABLE icon_catalog (
  id text PRIMARY KEY,
  label text NOT NULL,
  position integer NOT NULL UNIQUE CHECK (position > 0)
);

INSERT INTO quest_catalog (type, title, badge, description, position, template) VALUES
  ('nfc', 'Метка на месте преступления', 'NFC', 'Правильная метка, ложные улики и лимит попыток.', 1, '{"type":"nfc","instruction":"Ищи там, где согреваются руки.","tags":[{"id":"tag-1","label":"У варежек","token":"template-nfc-1"},{"id":"tag-2","label":"На книжной полке","token":"template-nfc-2"},{"id":"tag-3","label":"Рядом с ёлкой","token":"template-nfc-3"}],"correctId":"tag-1","limit":3}'),
  ('qr', 'Разорванная фотография', 'QR-пазл', 'Картинка и QR-код на двух сторонах одного листа.', 2, '{"type":"qr","instruction":"Найдите девять частей. Соберите картинку и отсканируйте её оборот.","picture":{"name":"Зимняя иллюстрация · пример","src":"data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20800%20800%22%3E%0A%20%20%3Crect%20width%3D%22800%22%20height%3D%22800%22%20fill%3D%22%23163b3d%22%2F%3E%3Cpath%20d%3D%22M0%20603Q166%20516%20367%20605T800%20568V800H0Z%22%20fill%3D%22%23ecf2eb%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M0%20690Q200%20624%20418%20690T800%20655V800H0Z%22%20fill%3D%22%23d0e4db%22%2F%3E%3Ccircle%20cx%3D%22570%22%20cy%3D%22182%22%20r%3D%22103%22%20fill%3D%22%23f6d48a%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M270%20109%20129%20335h66L91%20492h111L66%20655h398L339%20492h101L335%20335h60Z%22%20fill%3D%22%236aa18b%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M267%20158%20164%20323h208ZM267%20304%20137%20467h257ZM267%20446%20113%20620h315Z%22%20fill%3D%22%23285b51%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M267%20102l13%2027%2030%204-22%2021%205%2030-26-15-26%2015%205-30-22-21%2030-4Z%22%20fill%3D%22%23ffdb85%22%2F%3E%0A%20%20%3Cg%20fill%3D%22%23e9ac76%22%3E%3Ccircle%20cx%3D%22215%22%20cy%3D%22311%22%20r%3D%2213%22%2F%3E%3Ccircle%20cx%3D%22304%22%20cy%3D%22393%22%20r%3D%2217%22%2F%3E%3Ccircle%20cx%3D%22214%22%20cy%3D%22489%22%20r%3D%2215%22%2F%3E%3Ccircle%20cx%3D%22323%22%20cy%3D%22559%22%20r%3D%2214%22%2F%3E%3C%2Fg%3E%0A%20%20%3Cg%20fill%3D%22%23ebf1e4%22%3E%3Ccircle%20cx%3D%2263%22%20cy%3D%22115%22%20r%3D%227%22%2F%3E%3Ccircle%20cx%3D%22396%22%20cy%3D%22165%22%20r%3D%228%22%2F%3E%3Ccircle%20cx%3D%22683%22%20cy%3D%22108%22%20r%3D%226%22%2F%3E%3Ccircle%20cx%3D%22710%22%20cy%3D%22364%22%20r%3D%2211%22%2F%3E%3Ccircle%20cx%3D%22536%22%20cy%3D%22389%22%20r%3D%226%22%2F%3E%3Ccircle%20cx%3D%2286%22%20cy%3D%22405%22%20r%3D%226%22%2F%3E%3Ccircle%20cx%3D%22479%22%20cy%3D%22282%22%20r%3D%225%22%2F%3E%3C%2Fg%3E%0A%20%20%3Cpath%20d%3D%22M542%20529h115v117H542z%22%20fill%3D%22%23ad553c%22%2F%3E%3Cpath%20d%3D%22M529%20527h141v28H529z%22%20fill%3D%22%23c76c4b%22%2F%3E%3Cpath%20d%3D%22M590%20527h20v119h-20z%22%20fill%3D%22%23f6d48a%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M599%20528c-60%205-66-50-35-41%2015%204%2029%2020%2035%2041m1%200c60%205%2066-50%2035-41-15%204-29%2020-35%2041%22%20fill%3D%22none%22%20stroke%3D%22%23f6d48a%22%20stroke-width%3D%229%22%2F%3E%0A%3C%2Fsvg%3E"},"token":"template-qr","size":150,"fit":"cover"}'),
  ('objects', 'Три предмета в комнате', 'Выбор предметов', 'Сетка из девяти иконок и три правильных предмета.', 3, '{"type":"objects","instruction":"Найдите глазами три предмета из списка.","slots":[{"id":"slot-1","icon":"clock","label":"Часы"},{"id":"slot-2","icon":"cup","label":"Кружка"},{"id":"slot-3","icon":"candle","label":"Свеча"},{"id":"slot-4","icon":"book","label":"Книга"},{"id":"slot-5","icon":"key","label":"Ключ"},{"id":"slot-6","icon":"lamp","label":"Настольная лампа"},{"id":"slot-7","icon":"umbrella","label":"Зонт"},{"id":"slot-8","icon":"ball","label":"Мяч"},{"id":"slot-9","icon":"chair","label":"Кресло"}],"answers":["slot-1","slot-4","slot-9"]}'),
  ('photo', 'Фото до метели', 'Слово', 'Фото комнаты и название исчезнувшей вещи.', 4, '{"type":"photo","instruction":"Сравните снимок с комнатой. Какой предмет исчез?","picture":{"name":"Зимняя иллюстрация · пример","src":"data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20800%20800%22%3E%0A%20%20%3Crect%20width%3D%22800%22%20height%3D%22800%22%20fill%3D%22%23163b3d%22%2F%3E%3Cpath%20d%3D%22M0%20603Q166%20516%20367%20605T800%20568V800H0Z%22%20fill%3D%22%23ecf2eb%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M0%20690Q200%20624%20418%20690T800%20655V800H0Z%22%20fill%3D%22%23d0e4db%22%2F%3E%3Ccircle%20cx%3D%22570%22%20cy%3D%22182%22%20r%3D%22103%22%20fill%3D%22%23f6d48a%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M270%20109%20129%20335h66L91%20492h111L66%20655h398L339%20492h101L335%20335h60Z%22%20fill%3D%22%236aa18b%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M267%20158%20164%20323h208ZM267%20304%20137%20467h257ZM267%20446%20113%20620h315Z%22%20fill%3D%22%23285b51%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M267%20102l13%2027%2030%204-22%2021%205%2030-26-15-26%2015%205-30-22-21%2030-4Z%22%20fill%3D%22%23ffdb85%22%2F%3E%0A%20%20%3Cg%20fill%3D%22%23e9ac76%22%3E%3Ccircle%20cx%3D%22215%22%20cy%3D%22311%22%20r%3D%2213%22%2F%3E%3Ccircle%20cx%3D%22304%22%20cy%3D%22393%22%20r%3D%2217%22%2F%3E%3Ccircle%20cx%3D%22214%22%20cy%3D%22489%22%20r%3D%2215%22%2F%3E%3Ccircle%20cx%3D%22323%22%20cy%3D%22559%22%20r%3D%2214%22%2F%3E%3C%2Fg%3E%0A%20%20%3Cg%20fill%3D%22%23ebf1e4%22%3E%3Ccircle%20cx%3D%2263%22%20cy%3D%22115%22%20r%3D%227%22%2F%3E%3Ccircle%20cx%3D%22396%22%20cy%3D%22165%22%20r%3D%228%22%2F%3E%3Ccircle%20cx%3D%22683%22%20cy%3D%22108%22%20r%3D%226%22%2F%3E%3Ccircle%20cx%3D%22710%22%20cy%3D%22364%22%20r%3D%2211%22%2F%3E%3Ccircle%20cx%3D%22536%22%20cy%3D%22389%22%20r%3D%226%22%2F%3E%3Ccircle%20cx%3D%2286%22%20cy%3D%22405%22%20r%3D%226%22%2F%3E%3Ccircle%20cx%3D%22479%22%20cy%3D%22282%22%20r%3D%225%22%2F%3E%3C%2Fg%3E%0A%20%20%3Cpath%20d%3D%22M542%20529h115v117H542z%22%20fill%3D%22%23ad553c%22%2F%3E%3Cpath%20d%3D%22M529%20527h141v28H529z%22%20fill%3D%22%23c76c4b%22%2F%3E%3Cpath%20d%3D%22M590%20527h20v119h-20z%22%20fill%3D%22%23f6d48a%22%2F%3E%0A%20%20%3Cpath%20d%3D%22M599%20528c-60%205-66-50-35-41%2015%204%2029%2020%2035%2041m1%200c60%205%2066-50%2035-41-15%204-29%2020-35%2041%22%20fill%3D%22none%22%20stroke%3D%22%23f6d48a%22%20stroke-width%3D%229%22%2F%3E%0A%3C%2Fsvg%3E"},"answers":"фонарик, фонарь"}'),
  ('tree', 'След на ёлке', 'Последовательность', 'Клавиатура из игрушек и последовательность ответа.', 5, '{"type":"tree","instruction":"Красная нить выдаст четверых. Идите от макушки к подаркам.","slots":[{"id":"slot-1","icon":"star","label":"Звезда"},{"id":"slot-2","icon":"bell","label":"Колокольчик"},{"id":"slot-3","icon":"gift","label":"Подарок"},{"id":"slot-4","icon":"candy","label":"Конфета"},{"id":"slot-5","icon":"flake","label":"Снежинка"},{"id":"slot-6","icon":"glove","label":"Варежка"},{"id":"slot-7","icon":"snowman","label":"Снеговик"},{"id":"slot-8","icon":"ribbon","label":"Бант"},{"id":"slot-9","icon":"rabbit","label":"Заяц"}],"answers":["slot-1","slot-5","slot-2","slot-6"]}'),
  ('stencil', 'Два слоя снега', 'Трафареты', 'Две карточки со штрихами и восемь трафаретов.', 6, '{"type":"stencil","instruction":"Сверьте трафареты через окно. Введите слово и номер улики.","word":"СНЕГОПАД","font":"rounded","width":168,"correct":1,"strokeSeed":0,"strokeVersion":2}');

INSERT INTO icon_catalog (id, label, position) VALUES
  ('clock', 'Часы', 1),
  ('cup', 'Кружка', 2),
  ('candle', 'Свеча', 3),
  ('book', 'Книга', 4),
  ('key', 'Ключ', 5),
  ('cloud', 'Снег', 6),
  ('lamp', 'Настольная лампа', 7),
  ('umbrella', 'Зонт', 8),
  ('ball', 'Мяч', 9),
  ('chair', 'Кресло', 10),
  ('star', 'Звезда', 11),
  ('bell', 'Колокольчик', 12),
  ('gift', 'Подарок', 13),
  ('candy', 'Конфета', 14),
  ('flake', 'Снежинка', 15),
  ('glove', 'Варежка', 16),
  ('snowman', 'Снеговик', 17),
  ('ribbon', 'Бант', 18),
  ('rabbit', 'Заяц', 19),
  ('tree', 'Ёлка', 20),
  ('camera', 'Камера', 21);

ALTER TABLE game_quests ADD FOREIGN KEY (type) REFERENCES quest_catalog(type);
ALTER TABLE run_quests ADD FOREIGN KEY (type) REFERENCES quest_catalog(type);
