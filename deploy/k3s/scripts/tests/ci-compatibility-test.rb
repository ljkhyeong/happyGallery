# frozen_string_literal: true

require 'minitest/autorun'
require_relative '../ci-compatibility'

class CiCompatibilityTest < Minitest::Test
  def test_pr_uses_base_without_querying_production
    base = CiCompatibility.baseline({ 'pull_request' => { 'base' => { 'sha' => 'a' * 40 } } },
                                   production: false, repository: 'owner/repo',
                                   api: ->(_) { flunk 'PR은 배포 이력을 조회하지 않는다.' })
    assert_equal 'a' * 40, base
  end

  def test_skipped_deploy_is_not_a_baseline_and_failed_pushes_do_not_replace_it
    api = lambda do |path|
      if path.include?('/workflows/')
        [{ 'workflow_runs' => [
          { 'id' => 3, 'updated_at' => '2026-09-23T00:00:00Z', 'head_sha' => 'c' * 40 },
          { 'id' => 1, 'updated_at' => '2026-09-21T00:00:00Z', 'head_sha' => 'a' * 40 }
        ] }]
      else
        [{ 'jobs' => [{ 'name' => 'Roll out production',
                       'conclusion' => path.include?('/3/') ? 'skipped' : 'success' }] }]
      end
    end
    base = CiCompatibility.baseline({ 'before' => 'b' * 40 }, production: true,
                                   repository: 'owner/repo', api: api)
    assert_equal 'a' * 40, base
  end

  def test_rerun_of_older_run_can_be_the_latest_deploy
    api = lambda do |path|
      if path.include?('/workflows/')
        [{ 'workflow_runs' => [
          { 'id' => 3, 'updated_at' => '2026-09-23T00:00:00Z', 'head_sha' => 'c' * 40 },
          { 'id' => 1, 'updated_at' => '2026-09-24T00:00:00Z', 'head_sha' => 'a' * 40 }
        ] }]
      else
        [{ 'jobs' => [{ 'name' => 'Roll out production', 'conclusion' => 'success' }] }]
      end
    end
    assert_equal 'a' * 40, CiCompatibility.baseline({}, production: true, repository: 'owner/repo', api: api)
  end

  def test_missing_deploy_history_fails_closed
    error = assert_raises(RuntimeError) do
      CiCompatibility.baseline({}, production: true, repository: 'owner/repo',
                               api: ->(_) { [{ 'workflow_runs' => [] }] })
    end
    assert_includes error.message, '성공한 운영 배포 이력이 없습니다'
  end
end
