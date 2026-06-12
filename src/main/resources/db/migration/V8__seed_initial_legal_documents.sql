INSERT INTO legal_documents (
    id,
    document_type,
    version,
    title,
    content_url,
    content_sha256,
    is_active,
    published_at
) VALUES
    (
        '00000000-0000-0000-0000-000000000101',
        'TERMS_OF_SERVICE',
        'MVP-0.1',
        'Terms of Service',
        NULL,
        NULL,
        TRUE,
        '2026-06-12 00:00:00+00'
    ),
    (
        '00000000-0000-0000-0000-000000000102',
        'PRIVACY_POLICY',
        'MVP-0.1',
        'Privacy Policy',
        NULL,
        NULL,
        TRUE,
        '2026-06-12 00:00:00+00'
    ),
    (
        '00000000-0000-0000-0000-000000000103',
        'MEDICAL_DISCLAIMER',
        'MVP-0.1',
        'Non-medical Use Disclaimer',
        NULL,
        NULL,
        TRUE,
        '2026-06-12 00:00:00+00'
    ),
    (
        '00000000-0000-0000-0000-000000000104',
        'COMPANION_CONSENT',
        'MVP-0.1',
        'Companion Request Consent',
        NULL,
        NULL,
        TRUE,
        '2026-06-12 00:00:00+00'
    ),
    (
        '00000000-0000-0000-0000-000000000105',
        'COMPANION_DATA_SHARING',
        'MVP-0.1',
        'Companion Data Sharing Notice',
        NULL,
        NULL,
        TRUE,
        '2026-06-12 00:00:00+00'
    )
ON CONFLICT (document_type, version) DO UPDATE
SET title = EXCLUDED.title,
    content_url = EXCLUDED.content_url,
    content_sha256 = EXCLUDED.content_sha256,
    is_active = EXCLUDED.is_active,
    published_at = EXCLUDED.published_at,
    updated_at = now();
