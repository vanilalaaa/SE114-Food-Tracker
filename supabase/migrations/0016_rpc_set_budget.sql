CREATE OR REPLACE FUNCTION rpc_set_budget(
  p_daily double precision,
  p_weekly double precision,
  p_monthly double precision,
  p_yearly double precision
)
RETURNS void LANGUAGE sql SECURITY DEFINER AS $$
  INSERT INTO budget (user_id, daily, weekly, monthly, yearly, updated_at)
  VALUES (auth.uid(), p_daily, p_weekly, p_monthly, p_yearly, now())
  ON CONFLICT (user_id) DO UPDATE
    SET daily = EXCLUDED.daily,
        weekly = EXCLUDED.weekly,
        monthly = EXCLUDED.monthly,
        yearly = EXCLUDED.yearly,
        updated_at = EXCLUDED.updated_at;
$$;