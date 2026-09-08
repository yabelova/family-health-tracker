create table t_medication_intakes (
    id serial primary key,
    profile_id int not null references t_profiles(id) on delete cascade,
    course_id int references t_medication_courses(id) on delete set null,
    created_by bigint references t_users(id) on delete set null,
    created_at timestamp with time zone not null,
    properties jsonb not null default '{}'
);
