create table t_profiles (
    id serial primary key,
    name VARCHAR(100) NOT NULL,
    created_at timestamp with time zone
);

create table t_users (
    id bigint primary key,
    username varchar(100),
    first_name varchar(100) not null,
    notification_time time,
    created_at timestamp with time zone
);

create table tr_user_profile (
    user_id bigint not null,
    profile_id int not null,
    role varchar(30) default 'MEMBER',
    is_active boolean not null default false,
    created_at timestamp with time zone default current_timestamp,

    primary key (user_id, profile_id),
    foreign key (user_id) references t_users(id) on delete cascade,
    foreign key (profile_id) references t_profiles(id) on delete cascade
);

create unique index tr_user_profile_one_active_idx on tr_user_profile (user_id) where is_active;
