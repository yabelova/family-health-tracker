create table t_medication_intakes (
    id serial primary key,
    profile_id int not null references t_profiles(id) on delete cascade,
    course_id int references t_medication_courses(id) on delete set null,
    created_by int references t_users(id) on delete set null,
    created_at timestamp with time zone not null,
    properties jsonb not null default '{}'
);

-- Защита от двойной отметки одного и того же приема
create unique index uq_med_intake_dedup
    on t_medication_intakes (
        profile_id,
        course_id,
        (properties ->> 'medication'),
        (properties ->> 'takenAt')
    ) nulls not distinct;
