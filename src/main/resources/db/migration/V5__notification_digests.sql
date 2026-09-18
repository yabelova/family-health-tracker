alter table t_users add column last_notified_date date;

create index t_users_notification_due_idx on t_users (notification_time, last_notified_date) where notification_time is not null;
