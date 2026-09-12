# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require 'tmpdir'
require 'fileutils'

class RollingLifecycleTest < Minitest::Test
  SUPPORT = File.expand_path('../rolling-support.sh', __dir__)

  def setup
    @dir = Dir.mktmpdir('happygallery-rolling-lifecycle-')
    @calls = File.join(@dir, 'calls')
    File.write(File.join(@dir, 'previous-app-template.json'), 'old')
    File.write(File.join(@dir, 'previous-app-pods.txt'), "pod/old\n")
  end

  def teardown
    FileUtils.remove_entry(@dir)
  end

  def run_flow(body, template: 'new', wait_exit: '0', exec_exit: '0')
    environment = { 'TEST_DIR' => @dir, 'TEST_CALLS' => @calls, 'TEST_TEMPLATE' => template,
                    'TEST_WAIT_EXIT' => wait_exit, 'TEST_EXEC_EXIT' => exec_exit }
    script = <<~'SH'
      set -eu
      . "$1"
      release_dir=$TEST_DIR
      NAMESPACE=happygallery
      MEDIA_HELPER_POD=helper
      rolling_guard_owner=owner
      rolling_guard_created=true
      info() { printf '%s\n' "$*"; }
      die() { printf '%s\n' "$*" >&2; exit 1; }
      stop_media_helper() { printf 'delete-helper\n' >> "$TEST_CALLS"; }
      kube() {
        printf '%s\n' "$*" >> "$TEST_CALLS"
        case "$*" in
          *'get deployment app --ignore-not-found'*) return 1 ;;
          *'get deployment app'*) printf '%s' "$TEST_TEMPLATE" ;;
          *'wait --for=delete'*) return "$TEST_WAIT_EXIT" ;;
          *'exec helper'*) return "$TEST_EXEC_EXIT" ;;
        esac
      }
      trap rolling_cleanup EXIT
    SH
    Open3.capture3(environment, 'sh', '-c', script + body, 'rolling-test', SUPPORT)
  end

  def calls
    File.exist?(@calls) ? File.read(@calls) : ''
  end

  def test_does_not_resume_batches_when_old_pod_termination_times_out
    output, _error, status = run_flow("rolling_apply_started=true\nrolling_wait_old_pods\n", wait_exit: '1')
    refute status.success?
    assert_includes output, '배치 일시정지 표식을 유지'
    refute_includes calls, 'rmdir'
    refute_match(/-n happygallery (?:scale|delete) /, calls)
  end

  def test_resumes_only_after_old_pods_are_gone
    _output, error, status = run_flow("rolling_apply_started=true\nrolling_wait_old_pods\n")
    assert status.success?, error
    assert_operator calls.index('wait --for=delete'), :<, calls.index('rmdir')
  end

  def test_idempotent_release_does_not_wait_for_unchanged_pod_deletion
    _output, error, status = run_flow("rolling_apply_started=true\nrolling_wait_old_pods\n", template: 'old')
    assert status.success?, error
    refute_includes calls, 'wait --for=delete'
    assert_includes calls, 'rmdir'
  end

  def test_failure_before_app_apply_releases_guard
    _output, _error, status = run_flow("exit 1\n")
    refute status.success?
    assert_includes calls, 'rmdir'
  end

  def test_lookup_failure_cannot_bypass_existing_release_checks
    _output, error, status = run_flow("rolling_guard_created=false\nrolling_preflight\n")
    refute status.success?
    assert_includes error, '첫 배포로 간주하지 않습니다'
    refute_includes calls, 'apply'
  end

  def test_guard_release_failure_is_not_reported_as_deployment_success
    output, _error, status = run_flow("rolling_apply_started=true\nrolling_old_gone=true\nrolling_resume_schedulers\nprintf success\n", exec_exit: '1')
    refute status.success?
    refute_includes output, 'success'
    assert_includes output, '표식 해제 실패'
  end
end
