-- FiscalYear/FiscalPeriod entities require updated_by (see @Column(name = "updated_by"))
-- but V2__fiscal_domain.sql never added it, unlike every other tenant table's migration.
ALTER TABLE fsc_fiscal_years ADD COLUMN updated_by VARCHAR(255) NOT NULL DEFAULT 'system';
ALTER TABLE fsc_periods ADD COLUMN updated_by VARCHAR(255) NOT NULL DEFAULT 'system';
