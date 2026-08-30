INSERT INTO review_evidence_snapshots (
    id,
    review_id,
    content_revision,
    rating,
    content,
    edited_at,
    provenance,
    images_complete,
    captured_at,
    retention_until
)
SELECT rr.id,
       rr.review_id,
       1,
       rr.snapshot_rating,
       rr.snapshot_content,
       rr.snapshot_edited_at,
       'LEGACY_REPORT',
       FALSE,
       rr.created_at,
       CASE
           WHEN rr.status = 'PENDING' THEN NULL
           ELSE DATE_ADD(COALESCE(rr.decided_at, rr.created_at), INTERVAL 3 YEAR)
       END
FROM review_reports rr;
