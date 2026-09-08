create table t_symptom_logs (
    id serial primary key,
    profile_id int not null references t_profiles(id) on delete cascade,
    created_by bigint references t_users(id) on delete set null,
    created_at timestamp with time zone not null,
    properties jsonb not null default '{}'
);

create table t_medication_courses (
    id serial primary key,
    profile_id int not null references t_profiles(id) on delete cascade,
    created_by bigint references t_users(id) on delete set null,
    created_at timestamp with time zone not null,
    properties jsonb not null default '{}',
    remaining_doses int
);
