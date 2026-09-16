-- V9__coa_flags.sql
-- Adds overdraft-enforcement flag and year-end special role to COA nodes.
ALTER TABLE coa_nodes
    ADD COLUMN allow_negative_balance BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN node_role VARCHAR(30) DEFAULT NULL;

COMMENT ON COLUMN coa_nodes.allow_negative_balance IS
    'TRUE (default): balance may go freely negative. FALSE: overdraft check enforced at posting.';
COMMENT ON COLUMN coa_nodes.node_role IS
    'Special accounting role: RETAINED_EARNINGS | INCOME_SUMMARY | null';
