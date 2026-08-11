-- V2: Seed Data

-- Categories
INSERT INTO categories (name, description) VALUES
    ('Plumbing',            'Pipe repair, installation, drain cleaning'),
    ('Electrical',          'Wiring, fixtures, panel upgrades'),
    ('Home Cleaning',       'Deep cleaning, regular housekeeping'),
    ('Tutoring',            'Academic tutoring for all subjects'),
    ('Graphic Design',      'Logos, branding, print & digital design'),
    ('Web Development',     'Custom websites and web applications'),
    ('Carpentry',           'Furniture, woodwork, repairs'),
    ('AC & Appliance',      'AC service, washing machine, refrigerator repair'),
    ('Painting',            'Interior and exterior painting services'),
    ('Personal Training',   'Fitness coaching and workout planning');

-- Admin user (password: Admin@123 — bcrypt hash)
INSERT INTO users (name, email, password, phone, role) VALUES (
    'System Admin',
    'admin@marketplace.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh',
    '+910000000000',
    'ADMIN'
);
