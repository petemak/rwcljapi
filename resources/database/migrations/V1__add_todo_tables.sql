create table todo
(
    todo_id       uuid primary key default gen_random_uuid(),
    creation_date timestamp not null default current_timestamp,
    title         text      not null,
    description   text      not null    
);

create table todo_item
(
    todo_item_id  uuid primary key default gen_random_uuid(),
    todo_id       uuid references todo (todo_id),
    creation_date timestamp not null default current_timestamp,    
    name          text      not null,
    description   text      not null    
);
