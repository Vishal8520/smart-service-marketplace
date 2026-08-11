-- V2: Seed Default Categories and Admin User
INSERT IGNORE INTO categories (id, name, description) VALUES
(1, 'Plumbing',            'Pipe repair, installation, drain cleaning and emergency plumbing'),
(2, 'Electrical',          'Wiring, fixtures, panel upgrades and electrical safety'),
(3, 'Home Cleaning',       'Deep cleaning, regular housekeeping, sofa & carpet care'),
(4, 'Tutoring',            'Academic tutoring for school & college subjects'),
(5, 'Graphic Design',      'Logos, branding, print & digital visual assets'),
(6, 'Web Development',     'Custom websites, web apps and API integration'),
(7, 'Carpentry',           'Furniture assembly, custom woodwork and repairs'),
(8, 'Pest Control',        'Extermination of cockroaches, termites and bed bugs');

-- Admin user (password: Admin@123)
INSERT IGNORE INTO users (id, name, email, password, phone, role) VALUES (
1,
'System Admin',
'admin@marketplace.com',
'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh',
'+910000000000',
'ADMIN'
);
