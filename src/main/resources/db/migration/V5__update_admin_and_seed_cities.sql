-- V5: Update admin account and seed default cities

-- Update admin user to vishalghasoliya22@gmail.com with bcrypt hash of "Vishal@8696"
UPDATE users
SET email    = 'vishalghasoliya22@gmail.com',
    name     = 'Vishal Ghasoliya',
    password = '$2a$10$JOEii.FHzPWVWJoEuufQYe3q3mJLwxaY7zkGiLOPOZod8wA9cqhaG'
WHERE id = 1 AND role = 'ADMIN';

-- Seed default cities
INSERT IGNORE INTO cities (id, name, state, active) VALUES
(1, 'Mumbai',    'Maharashtra', 1),
(2, 'Delhi',     'Delhi',       1),
(3, 'Bangalore', 'Karnataka',   1),
(4, 'Hyderabad', 'Telangana',   1),
(5, 'Chennai',   'Tamil Nadu',  1),
(6, 'Pune',      'Maharashtra', 1),
(7, 'Ahmedabad', 'Gujarat',     1);
