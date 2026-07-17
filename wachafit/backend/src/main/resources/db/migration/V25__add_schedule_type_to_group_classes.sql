ALTER TABLE group_classes
  ADD COLUMN schedule_type VARCHAR(10) NOT NULL DEFAULT 'FLEX',
  ADD COLUMN start_time    VARCHAR(5),
  ADD COLUMN end_time      VARCHAR(5);
