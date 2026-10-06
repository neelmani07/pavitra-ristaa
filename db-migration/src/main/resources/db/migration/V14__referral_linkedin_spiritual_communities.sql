-- Referral code the user signed up with. Stored as given (normalised to upper case); there is no referrer
-- lookup or reward logic yet - that is a product decision still to be made. This just stops the code the
-- client already sends at registration from being dropped on the floor in the meantime.
ALTER TABLE "user" ADD COLUMN referral_code VARCHAR(64);
CREATE INDEX idx_user_referral_code ON "user" (referral_code) WHERE referral_code IS NOT NULL;

-- LinkedIn profile URL on the career section. Owner-only in API responses (see ProfileMapper.career).
ALTER TABLE profile_career ADD COLUMN linkedin_url VARCHAR(255);

-- Spiritual communities offered in the profile dropdown. A user can also type a community that is not listed;
-- that creates an inactive row here (is_active = FALSE) so it is kept for an admin to review and promote, but
-- never shows up in the shared dropdown on its own.
INSERT INTO master_category (code, name, description, is_active) VALUES
    ('SPIRITUAL_COMMUNITY', 'Spiritual community', 'Spiritual teachers, organisations and movements', TRUE);

INSERT INTO master_value (category_id, code, name, display_order)
SELECT c.id, v.code, v.name, v.display_order
FROM master_category c
CROSS JOIN (VALUES
    ('THE_SATSANG_FOUNDATION', 'The Satsang Foundation (Sri M)', 1),
    ('ECKHART_TOLLE', 'Eckhart Tolle (The Power of Now)', 2),
    ('DEEPAK_CHOPRA', 'Deepak Chopra (The Chopra Foundation)', 3),
    ('MOOJI', 'Mooji (Advaita Vedanta)', 4),
    ('THICH_NHAT_HANH', 'Thich Nhat Hanh (Plum Village)', 5),
    ('DALAI_LAMA', 'Dalai Lama (Tibetan Buddhism)', 6),
    ('SADHGURU', 'Sadhguru (Isha Foundation)', 7),
    ('RAM_DASS', 'Ram Dass (Love Serve Remember Foundation)', 8),
    ('ADYASHANTI', 'Adyashanti', 9),
    ('MICHAEL_A_SINGER', 'Michael A. Singer (The Untethered Soul)', 10),
    ('BYRON_KATIE', 'Byron Katie (The Work)', 11),
    ('JON_KABAT_ZINN', 'Jon Kabat-Zinn (Mindfulness-Based Stress Reduction)', 12),
    ('WIM_HOF_METHOD', 'Wim Hof Method', 13),
    ('RUPERT_SPIRA', 'Rupert Spira (Direct Path)', 14),
    ('GANGAJI', 'Gangaji', 15),
    ('ALAN_WATTS_ORGANIZATION', 'Alan Watts Organization', 16),
    ('TARA_BRACH', 'Tara Brach (Insight Meditation)', 17),
    ('JACK_KORNFIELD', 'Jack Kornfield (Spirit Rock)', 18),
    ('PEMA_CHODRON', 'Pema Chödrön', 19),
    ('THOMAS_HUBL', 'Thomas Hübl', 20),
    ('ART_OF_LIVING_FOUNDATION', 'Art of Living Foundation', 21),
    ('ISKCON', 'ISKCON (International Society for Krishna Consciousness)', 22),
    ('BRAHMA_KUMARIS', 'Brahma Kumaris', 23),
    ('MATA_AMRITANANDAMAYI_MATH', 'Mata Amritanandamayi Math', 24),
    ('RAMAKRISHNA_MISSION', 'Ramakrishna Mission', 25),
    ('SELF_REALIZATION_FELLOWSHIP', 'Self-Realization Fellowship (Yogoda Satsanga Society of India)', 26),
    ('SIVANANDA_YOGA_VEDANTA_CENTRES', 'Sivananda Yoga Vedanta Centres', 27),
    ('OSHO_INTERNATIONAL', 'Osho International Meditation Resort', 28),
    ('SATHYA_SAI_INTERNATIONAL', 'Sathya Sai International Organisation', 29),
    ('RADHA_SOAMI_SATSANG_BEAS', 'Radha Soami Satsang Beas', 30),
    ('SAHAJA_YOGA', 'Sahaja Yoga', 31),
    ('SCIENCE_OF_SPIRITUALITY', 'Science of Spirituality', 32),
    ('SRI_AUROBINDO_ASHRAM', 'Sri Aurobindo Ashram', 33),
    ('SHIRDI_SAI_BABA_MOVEMENT', 'Shirdi Sai Baba movement', 34),
    ('TRANSCENDENTAL_MEDITATION', 'Transcendental Meditation (TM) movement', 35),
    ('KRISHNAMURTI_FOUNDATION', 'Krishnamurti Foundation', 36),
    ('CHINMAYA_MISSION', 'Chinmaya Mission', 37),
    ('DIVINE_LIFE_SOCIETY', 'Divine Life Society', 38),
    ('ART_OF_MEDITATION', 'Art of Meditation (Sahaj Samadhi)', 39)
) AS v(code, name, display_order)
WHERE c.code = 'SPIRITUAL_COMMUNITY';
