UPDATE reviews
SET hidden_reason = NULL,
    hidden_at = NULL,
    hidden_by_admin_id = NULL
WHERE deleted_at IS NOT NULL;
