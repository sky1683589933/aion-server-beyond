ALTER TABLE `inventory` ADD COLUMN `tempering` smallint(6) NOT NULL DEFAULT '0' AFTER `rnd_count`;