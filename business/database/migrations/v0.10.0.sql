ALTER TABLE `user_storage` DROP `max_energy`;

-- Remove unsigned
ALTER TABLE `improvements` CHANGE `more_upgrade_research_speed` `more_upgrade_research_speed` FLOAT NULL DEFAULT NULL, CHANGE `more_unit_build_speed` `more_unit_build_speed` FLOAT NULL DEFAULT NULL; 
