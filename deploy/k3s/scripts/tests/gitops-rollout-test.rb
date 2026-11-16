# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require 'open3'

class GitopsRolloutTest < Minitest::Test
  SCRIPTS = File.expand_path('..', __dir__)

  def setup
    @directory = Dir.mktmpdir('happygallery-gitops-phase-')
    @release = File.join(@directory, 'candidate')
    FileUtils.mkdir_p(@release)
    FileUtils.cp(File.join(SCRIPTS, 'rollout.sh'), @directory)
    FileUtils.cp(File.join(SCRIPTS, 'rolling-support.sh'), @directory)
    @calls = File.join(@directory, 'calls')
    File.write(File.join(@directory, 'release.env'), "IMAGE_TAG=candidate\n")
    File.write(File.join(@directory, 'gitops-active'), @release)
    File.write(File.join(@release, 'previous-app-template.json'), 'old')
    File.write(File.join(@release, 'previous-app-pods.txt'), "pod/old\n")
    File.write(File.join(@release, 'gitops-state.sh'), <<~SH)
      IMAGE_TAG=candidate
      PUBLIC_HOST=happy-gallery.com
      APP_IMAGE=app
      FRONTEND_IMAGE=frontend
      APP_IMAGE_DIGEST=sha256:a
      FRONTEND_IMAGE_DIGEST=sha256:b
      state_root="$TEST_ROOT"
      release_dir="$TEST_ROOT/candidate"
      MEDIA_HELPER_POD=helper
      rolling_guard_owner=owner
      rolling_guard_created=true
      rolling_existing=true
    SH
    File.write(File.join(@directory, 'common.sh'), <<~'SH')
      set -eu
      SCRIPT_DIR=$TEST_ROOT
      NAMESPACE=happygallery
      die() { echo "$*" >&2; exit 1; }
      info() { echo "$*"; }
      validate_env_file() { test -f "$1"; }
      release_state_dir() { echo "$TEST_ROOT"; }
      require_env_value() { sed -n "s/^$1=//p" "$2"; }
      deployment_container_image() {
        if [ "$TEST_WRONG_IMAGE" = true ]; then echo wrong; return; fi
        case "$1" in app) echo app@sha256:a ;; frontend) echo frontend@sha256:b ;; esac
      }
      start_media_helper() { echo start-helper >> "$TEST_CALLS"; }
      stop_media_helper() { echo stop-helper >> "$TEST_CALLS"; }
      kube() {
        printf '%s\n' "$*" >> "$TEST_CALLS"
        case "$*" in
          *'get deployment app'*) echo new ;;
          *'wait --for=delete'*) return "$TEST_DELETE_EXIT" ;;
        esac
      }
    SH
    # 실제 helper 로직은 Kubernetes 접근을 수행하므로 이 fixture에서만 공통 함수 뒤에 덮어쓴다.
    File.open(File.join(@directory, 'rolling-support.sh'), 'a') do |file|
      file.puts 'start_media_helper() { echo start-helper >> "$TEST_CALLS"; }'
      file.puts 'stop_media_helper() { echo stop-helper >> "$TEST_CALLS"; }'
    end
    File.write(File.join(@directory, 'verify.sh'), "#!/bin/sh\necho verify >> \"$TEST_CALLS\"\nexit \"$TEST_VERIFY_EXIT\"\n")
    File.chmod(0o700, File.join(@directory, 'verify.sh'))
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def run_phase(phase, wrong_image: false, delete_exit: '0', verify_exit: '0')
    Open3.capture3({ 'TEST_ROOT' => @directory, 'TEST_CALLS' => @calls, 'TEST_WRONG_IMAGE' => wrong_image.to_s,
                     'TEST_DELETE_EXIT' => delete_exit, 'TEST_VERIFY_EXIT' => verify_exit },
                  'bash', File.join(@directory, 'rollout.sh'), File.join(@directory, 'release.env'), phase)
  end

  def calls
    File.exist?(@calls) ? File.read(@calls) : ''
  end

  def test_complete_waits_for_old_pods_and_records_release_before_public_verification
    _output, error, status = run_phase('complete')
    assert status.success?, error
    assert_equal @release, File.readlink(File.join(@directory, 'current'))
    refute File.exist?(File.join(@directory, 'gitops-active'))
    assert_operator calls.index('wait --for=delete'), :<, calls.index('verify')
    assert_operator calls.index('verify'), :<, calls.index('rmdir')
    refute_includes calls, 'apply -f'
  end

  def test_complete_rejects_image_drift_and_keeps_pending_release
    _output, _error, status = run_phase('complete', wrong_image: true)
    refute status.success?
    assert File.exist?(File.join(@directory, 'gitops-active'))
    refute_includes calls, 'verify'
    refute_includes calls, 'rmdir'
  end

  def test_mixed_rollout_failure_keeps_guard_and_supports_retry
    _output, _error, status = run_phase('complete', delete_exit: '1')
    refute status.success?
    assert File.exist?(File.join(@directory, 'gitops-active'))
    refute_includes calls, 'rmdir'
    _output, error, retry_status = run_phase('resume')
    assert retry_status.success?, error
    assert_includes calls, 'mkdir -p /media/.deployment-in-progress'
  end

  def test_public_verification_failure_can_complete_on_retry
    _output, _error, status = run_phase('complete', verify_exit: '1')
    refute status.success?
    assert File.exist?(File.join(@directory, 'gitops-active'))
    _output, error, retry_status = run_phase('complete')
    assert retry_status.success?, error
    refute File.exist?(File.join(@directory, 'gitops-active'))
  end

  def test_failed_hook_preserves_pending_release
    _output, error, status = run_phase('failed')
    assert status.success?, error
    assert File.exist?(File.join(@directory, 'gitops-active'))
    refute_includes calls, 'rmdir'
  end
end
