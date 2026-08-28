create table t_profile_invites (
    id serial primary key,
    profile_id int not null references t_profiles(id) on delete cascade,
    code varchar(8) not null unique,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    used_by bigint references t_users(id) on delete set null
);
